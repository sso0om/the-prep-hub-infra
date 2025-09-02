package com.back.domain.member.member.service

import com.back.domain.api.service.ApiKeyService
import com.back.domain.auth.service.AuthService
import com.back.domain.club.club.repository.ClubRepository
import com.back.domain.club.clubMember.entity.ClubMember
import com.back.domain.club.clubMember.repository.ClubMemberRepository
import com.back.domain.member.member.dto.request.GuestDto
import com.back.domain.member.member.dto.request.MemberLoginDto
import com.back.domain.member.member.dto.request.MemberRegisterDto
import com.back.domain.member.member.dto.request.UpdateMemberInfoDto
import com.back.domain.member.member.dto.response.*
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.entity.Member.Companion.createGuest
import com.back.domain.member.member.entity.Member.Companion.createMember
import com.back.domain.member.member.entity.MemberInfo
import com.back.domain.member.member.repository.MemberInfoRepository
import com.back.domain.member.member.repository.MemberRepository
import com.back.global.aws.S3Service
import com.back.global.enums.ClubMemberRole
import com.back.global.enums.ClubMemberState
import com.back.global.exception.ServiceException
import com.back.standard.util.orServiceThrow
import jakarta.validation.Valid
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.util.*

@Service
@Validated
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val memberInfoRepository: MemberInfoRepository,
    private val apiKeyService: ApiKeyService,
    private val authService: AuthService,
    private val s3Service: S3Service,
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val passwordEncoder: PasswordEncoder
) {

    // ============================== [회원] 회원가입 ==============================
    @Transactional
    fun registerMember(dto: MemberRegisterDto): MemberAuthResponse {
        validateDuplicateMember(dto)

        val tag = generateMemberTag(dto.nickname)
        val apiKey = apiKeyService.generateApiKey()

        val member = createAndSaveMember(dto, tag)
        createAndSaveMemberInfo(dto, member, apiKey)

        val accessToken = generateAccessToken(member)
        return MemberAuthResponse(apiKey, accessToken)
    }

    // ============================== [비회원] 모임 가입 ==============================
    @Transactional
    fun registerGuestMember(@Valid dto: GuestDto): GuestResponse {
        validateDuplicateGuest(dto)

        val tag = generateMemberTag(dto.nickname)
        val guest = createAndSaveGuestMember(dto, tag)

        val club = clubRepository.findByIdOrNull(dto.clubId).orServiceThrow("클럽을 찾을 수 없습니다.")

        val clubMember = ClubMember(
            guest,
            ClubMemberRole.PARTICIPANT,
            ClubMemberState.APPLYING
        ).apply { setClub(club) }

        clubMemberRepository.save(clubMember)

        val accessToken = generateAccessToken(guest)
        return GuestResponse(dto.nickname, accessToken, dto.clubId)
    }

    // ============================== [회원] 로그인 ==============================
    fun loginMember(@Valid dto: MemberLoginDto): MemberAuthResponse {
        val memberInfo = memberInfoRepository.findByEmail(dto.email).orServiceThrow("해당 사용자를 찾을 수 없습니다.")

        val member = memberInfo.getMember().orServiceThrow("해당 사용자를 찾을 수 없습니다.")

        validatePassword(dto.password, member)

        val apiKey = member.getMemberInfo()?.apiKey.orServiceThrow("API 키가 존재하지 않습니다.")
        val accessToken = authService.generateAccessToken(member)

        return MemberAuthResponse(apiKey, accessToken)
    }

    // ============================== [비회원] 임시 로그인 ==============================
    fun loginGuestMember(@Valid dto: GuestDto): GuestResponse {
        val member : Member = memberRepository.findByGuestNicknameInClub(dto.nickname, dto.clubId).orServiceThrow("해당 사용자를 찾을 수 없습니다.")

        validatePassword(dto.password, member)

        val accessToken = authService.generateAccessToken(member)
        return GuestResponse(dto.nickname, accessToken, dto.clubId)
    }

    // ============================== [회원] 탈퇴 ==============================
    @Transactional
    fun withdrawMember(nickname: String, tag: String): MemberWithdrawMembershipResponse {
        val member = findMemberByNicknameAndTag(nickname, tag)
        deleteMember(member)
        return MemberWithdrawMembershipResponse(member.nickname, member.tag ?: "")
    }

    // ============================== [회원] 정보 조회/수정 ==============================
    fun getMemberInfo(id: Long): MemberDetailInfoResponse {
        val member: Member = findMemberById(id).orServiceThrow("해당 id의 유저가 없습니다.")
        val info: MemberInfo = member.getMemberInfo().orServiceThrow("해당 id의 유저가 없습니다.")

        val email = requireNotNull(info.email) { "회원 이메일이 누락되었습니다." }
        return MemberDetailInfoResponse(
            member.nickname,
            email,
            info.bio,
            info.profileImageUrl,
            member.tag
        )
    }

    @Transactional
    fun updateMemberInfo(id: Long, dto: UpdateMemberInfoDto, image: MultipartFile?): MemberDetailInfoResponse {
        val member = findMemberById(id).orServiceThrow("해당 id의 유저가 없습니다.")
        val info = member.getMemberInfo().orServiceThrow("해당 id의 유저가 없습니다.")

        val password = dto.password?.takeIf { it.isNotBlank() }?.let { passwordEncoder.encode(it) } ?: member.password
        val nickname = dto.nickname ?: member.nickname
        val tag = dto.nickname?.let { generateMemberTag(it) } ?: member.tag
        val bio = dto.bio ?: info.bio

        member.updateInfo(nickname, tag, password)
        info.updateBio(bio)

        image?.takeIf { !it.isEmpty }?.let {
            val contentType = it.contentType ?: ""
            if (!contentType.startsWith("image/")) {
                throw ServiceException(400, "이미지 파일만 업로드 가능합니다.")
            }

            try {
                val imageUrl = s3Service.upload(it, "member/${info.id}/profile")
                info.updateImageUrl(imageUrl)
            } catch (e: IOException) {
                throw ServiceException(400, "이미지 업로드 중 오류가 발생했습니다.: ${e.message}")
            }
        }

        val email = requireNotNull(info.email) { "회원 이메일이 누락되었습니다." }
        return with(info) {
            MemberDetailInfoResponse(member.nickname, email, bio, profileImageUrl, member.tag)
        }
    }

    // ============================== [검증 메소드] ==============================
    private fun validateDuplicateMember(dto: MemberRegisterDto) {
        memberInfoRepository.findByEmail(dto.email)?.let {
            throw ServiceException(400, "이미 사용 중인 이메일입니다.")
        }
    }

    private fun validateDuplicateGuest(dto: GuestDto) {
        if (memberRepository.existsGuestNicknameInClub(dto.nickname, dto.clubId)) {
            throw ServiceException(400, "이미 사용 중인 닉네임입니다.")
        }
    }

    private fun validatePassword(password: String, member: Member) {
        if (!passwordEncoder.matches(password, member.password)) {
            throw ServiceException(400, "해당 사용자를 찾을 수 없습니다.")
        }
    }

    // ============================== [생성 메소드] ==============================
    private fun generateMemberTag(nickname: String): String {
        return generateSequence { UUID.randomUUID().toString().substring(0, 6) }
            .first { tag -> !memberRepository.existsByNicknameAndTag(nickname, tag) }
    }

    private fun createAndSaveMember(dto: MemberRegisterDto, tag: String): Member =
        createMember(dto.nickname, passwordEncoder.encode(dto.password), tag)
            .let(memberRepository::save)

    private fun createAndSaveGuestMember(dto: GuestDto, tag: String): Member =
        createGuest(dto.nickname, passwordEncoder.encode(dto.password), tag)
            .let(memberRepository::save)

    private fun createAndSaveMemberInfo(dto: MemberRegisterDto, member: Member, apiKey: String): MemberInfo {
        val info = MemberInfo(
            email = dto.email,
            bio = dto.bio,
            profileImageUrl = "",
            apiKey = apiKey,
            member = member
        )
        return memberInfoRepository.save(info).also { member.setMemberInfo(it) }
    }

    // ============================== [유틸 / 기타] ==============================
    fun checkPasswordValidity(memberId: Long, password: String): MemberPasswordResponse {
        val member : Member = findMemberById(memberId).orServiceThrow("해당 id의 유저가 없습니다.")
        return try {
            validatePassword(password, member)
            MemberPasswordResponse(true)
        } catch (e: ServiceException) {
            MemberPasswordResponse(false)
        }
    }

//    private fun <T> T?.orServiceThrow(message: String): T =
//        this ?: throw ServiceException(400, message)

    fun payload(accessToken: String) = authService.payload(accessToken)

    fun findMemberByEmail(email: String): Member =
            memberInfoRepository.findByEmail(email)?.getMember()
                ?: throw ServiceException(400, "사용자를 찾을 수 없습니다.")

    private fun deleteMember(member: Member) = memberRepository.delete(member)

    fun findMemberByNicknameAndTag(nickname: String, tag: String) =
        memberRepository.findByNicknameAndTag(nickname, tag)
            ?: throw ServiceException(400, "회원 정보를 찾을 수 없습니다.")

    fun findMemberById(id: Long): Member? =
        memberRepository.findById(id).orElse(null)

    fun getMember(memberId: Long) =
        findMemberById(memberId)
            ?: throw ServiceException(404, "멤버가 존재하지 않습니다.")

    fun generateAccessToken(member: Member) = authService.generateAccessToken(member)

    fun findMemberByApiKey(apiKey: String): Member =
                memberInfoRepository.findByApiKey(apiKey)?.getMember()
            ?: throw ServiceException(401, "유효하지 않은 API Key입니다.")
}
