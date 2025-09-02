package com.back.domain.club.club.service

import com.back.domain.club.club.dtos.*
import com.back.domain.club.club.entity.Club
import com.back.domain.club.club.error.ClubErrorCode
import com.back.domain.club.club.repository.ClubRepository
import com.back.domain.club.club.repository.ClubSpecification.equalCategory
import com.back.domain.club.club.repository.ClubSpecification.equalEventType
import com.back.domain.club.club.repository.ClubSpecification.isPublic
import com.back.domain.club.club.repository.ClubSpecification.likeMainSpot
import com.back.domain.club.club.repository.ClubSpecification.likeName
import com.back.domain.club.clubMember.entity.ClubMember
import com.back.domain.club.clubMember.service.ClubMemberValidService
import com.back.domain.member.member.service.MemberService
import com.back.global.aws.S3Service
import com.back.global.enums.ClubCategory
import com.back.global.enums.ClubMemberRole
import com.back.global.enums.ClubMemberState
import com.back.global.enums.EventType
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.time.LocalDate
import java.util.*

@Service
class ClubService(
    private val clubRepository: ClubRepository,
    private val memberService: MemberService,
    private val clubMemberValidService: ClubMemberValidService,
    private val s3Service: S3Service,
    private val rq: Rq
){

    val lastCreatedClub: Club
        /**
         * 마지막으로 생성된 클럽을 반환합니다.
         * @return 마지막으로 생성된 클럽
         */
        get() {
            val lastClub = clubRepository.findFirstByOrderByIdDesc()
            checkNotNull(lastClub) { "마지막으로 생성된 클럽이 없습니다." }
            return lastClub
        }

    /**
     * 클럽 ID로 클럽을 조회합니다.
     * @param clubId 클럽 ID
     * @return 클럽 정보
     */
    fun getClubById(clubId: Long): Club {
        return clubRepository.findById(clubId)
            .orElseThrow { NoSuchElementException(ClubErrorCode.CLUB_NOT_FOUND.message) }
    }

    /**
     * 모임 ID로 활성화된 모임 조회
     * @param clubId 모임 ID
     * @return 활성화된 모임 엔티티
     */
    fun getActiveClub(clubId: Long): Club {
        return clubRepository.findByIdAndStateIsTrue(clubId) ?: throw NoSuchElementException(ClubErrorCode.CLUB_NOT_FOUND.message)
    }

    /**
     * 모임 ID로 종료일 안지난 활성화된 모임 조회
     * @param clubId 모임 ID
     * @return 활성화된 모임 엔티티
     */
    fun getValidAndActiveClub(clubId: Long): Club {
        return clubRepository.findValidAndActiveClub(clubId) ?: throw NoSuchElementException(ClubErrorCode.CLUB_NOT_FOUND.message)
    }

    /**
     * 클럽을 생성합니다. (테스트용. controller에서 사용하지 않음)
     * @param club 클럽 정보
     * @return 생성된 클럽
     */
    @Transactional
    fun createClub(club: Club): Club {
        return clubRepository.save<Club>(club)
    }

    @Transactional
    @Throws(IOException::class)
    fun createClub(
        reqBody: CreateClubRequest,
        image: MultipartFile?
    ): Club {
        // 1. 이미지 없이 클럽 생성
        val club = clubRepository.saveAndFlush<Club>(
            Club(
                name = reqBody.name,
                bio = reqBody.bio,  // Kotlin에서는 nullable이므로 그대로 전달 가능
                category = ClubCategory.fromString(reqBody.category.uppercase(Locale.getDefault())),
                mainSpot = reqBody.mainSpot,
                maximumCapacity = reqBody.maximumCapacity,
                recruitingStatus = true,  // recruitingStatus 기본값
                eventType = EventType.fromString(reqBody.eventType.uppercase(Locale.getDefault())),
                startDate = LocalDate.parse(reqBody.startDate),
                endDate = LocalDate.parse(reqBody.endDate),
                isPublic = reqBody.isPublic,
                leaderId = rq.actor?.id ?: throw ServiceException(401, "인증되지 않은 사용자입니다."),
                state = true // state 기본값
            )
        )
        // 2. 이미지가 제공된 경우 S3에 업로드
        if (image != null && !image.isEmpty) {
            // 이미지 파일 크기 제한 (5MB)
            if (image.size > (5 * 1024 * 1024)) { // 5MB
                throw ServiceException(400, "이미지 파일 크기는 5MB를 초과할 수 없습니다.")
            }

            val imageUrl = s3Service.upload(image, "club/" + club.id + "/profile")
            club.updateImageUrl(imageUrl) // 클럽에 이미지 URL 설정
            clubRepository.save<Club?>(club) // 이미지 URL 업데이트 후 클럽 정보 저장
        }

        // 클럽 생성 시 유저를 리더로 설정하고 멤버에 추가
        val leader = memberService.findMemberById(rq.actor?.id!!)
            ?: throw NoSuchElementException("ID ${rq.actor!!.id}에 해당하는 리더를 찾을 수 없습니다.")

        val clubLeader = ClubMember(
            leader,
            ClubMemberRole.HOST,  // 클럽 생성자는 HOST 역할
            ClubMemberState.JOINING // 초기 상태는 JOINING으로 설정
        )

        club.addClubMember(clubLeader) // 연관관계 편의 메서드를 사용하여 Club에 ClubMember 추가

        // 클럽 멤버 설정
        reqBody.clubMembers.forEach { memberInfo: CreateClubRequestMemberInfo? ->
            // 멤버 ID로 Member 엔티티 조회
            val member = memberService.findMemberById(memberInfo!!.id)
                ?: throw NoSuchElementException("ID ${memberInfo.id}에 해당하는 멤버를 찾을 수 없습니다.")

            // ClubMember 엔티티 생성
            val clubMember = ClubMember(
                member,
                ClubMemberRole.fromString(memberInfo.role.uppercase(Locale.getDefault())),  // 문자열을 Enum으로 변환
                ClubMemberState.INVITED // 초기 상태는 INVITED로 설정
            )

            // 연관관계 편의 메서드를 사용하여 Club에 ClubMember 추가
            club.addClubMember(clubMember)
        }
        return club
    }

    /**
     * 클럽 정보를 업데이트합니다.
     * @param clubId 클럽 ID
     * @param dto 클럽 정보 업데이트 요청 DTO
     * @param image 클럽 이미지 파일 (선택적)
     * @return 업데이트된 클럽 정보
     * @throws IOException 이미지 업로드 중 발생할 수 있는 예외
     */
    @Transactional
    @Throws(IOException::class)
    fun updateClub(clubId: Long, dto: @Valid UpdateClubRequest, image: MultipartFile?): Club {
        val club = clubRepository.findById(clubId)
            .orElseThrow { ServiceException(404, "해당 ID의 클럽을 찾을 수 없습니다.") }

        // 클럽 정보 업데이트
        val name: String? = dto.name ?: club.name
        val bio = dto.bio ?: club.bio
        val category =
            if (dto.category != null) ClubCategory.fromString(dto.category.uppercase(Locale.getDefault())) else club.category
        val mainSpot: String? = dto.mainSpot ?: club.mainSpot
        val maximumCapacity = dto.maximumCapacity ?: club.maximumCapacity
        val recruitingStatus = dto.recruitingStatus ?: club.recruitingStatus
        val eventType =
            if (dto.eventType != null) EventType.fromString(dto.eventType.uppercase(Locale.getDefault())) else club.eventType
        val startDate = if (dto.startDate != null) LocalDate.parse(dto.startDate) else club.startDate
        val endDate = if (dto.endDate != null) LocalDate.parse(dto.endDate) else club.endDate
        val isPublic: Boolean = (if (dto.isPublic != null) dto.isPublic else club.isPublic)!!

        club.updateInfo(
            name!!,
            bio,
            category,
            mainSpot!!,
            maximumCapacity,
            recruitingStatus,
            eventType,
            startDate,
            endDate,
            isPublic
        )

        // 이미지가 제공된 경우 S3에 업로드
        if (image != null && !image.isEmpty) {
            // 이미지 파일 크기 제한 (5MB)
            if (image.size > (5 * 1024 * 1024)) { // 5MB
                throw ServiceException(400, "이미지 파일 크기는 5MB를 초과할 수 없습니다.")
            }

            val imageUrl = s3Service.upload(image, "club/" + club.id + "/profile")
            club.updateImageUrl(imageUrl) // 클럽에 이미지 URL 설정
        }


        return clubRepository.save<Club>(club)
    }

    fun deleteClub(clubId: Long) {
        val club = clubRepository.findById(clubId)
            .orElseThrow { ServiceException(404, "해당 ID의 클럽을 찾을 수 없습니다.") }

        // 클럽 삭제
        club.changeState(false) // 클럽 상태를 비활성화로 변경
        clubRepository.save<Club?>(club) // 변경된 클럽 상태를 저장
    }

    /**
     * 클럽 정보를 조회합니다.
     * @param clubId 클럽 ID
     * @return 클럽 정보 DTO
     */
    @Transactional(readOnly = true)
    fun getClubInfo(clubId: Long): ClubInfoResponse {
        val club = clubRepository.findById(clubId)
            .orElseThrow { ServiceException(404, "해당 ID의 클럽을 찾을 수 없습니다.") }

        val leader = memberService.findMemberById(club.leaderId!!)
            ?: throw ServiceException(404, "해당 ID의 클럽을 찾을 수 없습니다.")

        // 비공개 클럽인 경우, 현재 로그인한 유저가 클럽 멤버인지 확인
        if (!club.isPublic) {
            if (rq.actor == null || !clubMemberValidService.isClubMember(clubId, rq.actor!!.id ?: throw ServiceException(401, "인증되지 않은 사용자입니다."))) {
                throw ServiceException(403, "비공개 클럽 정보는 클럽 멤버만 조회할 수 있습니다.")
            }
        }

        // 비활성화된 클럽인 경우 예외 처리
        if (!club.state) {
            throw ServiceException(404, "해당 클럽은 비활성화 상태입니다.")
        }

        return ClubInfoResponse(
            club.id!!,
            club.name,
            club.bio!!,
            club.category.toString(),
            club.mainSpot,
            club.maximumCapacity,
            club.recruitingStatus,
            club.eventType.toString(),
            club.startDate.toString(),
            club.endDate.toString(),
            club.isPublic,
            club.imageUrl,
            club.leaderId!!,
            leader.nickname
        )
    }

    /**
     * 공개 클럽 목록을 페이징하여 조회합니다.
     * @param pageable 페이징 정보
     * @param name 클럽 이름 (선택적)
     * @param mainSpot 지역 (선택적)
     * @param category 카테고리 (선택적)
     * @param eventType 모집 유형 (선택적)
     * @return 공개 클럽 목록 페이지
     */
    @Transactional(readOnly = true)
    fun getPublicClubs(
        pageable: Pageable, name: String?, mainSpot: String?, category: ClubCategory?, eventType: EventType?
    ): Page<SimpleClubInfoWithoutLeader?> {
        // 1. 기본 조건인 '공개된 클럽'으로 Specification을 시작합니다.
        var spec: Specification<Club> = isPublic()

        // 2. 각 필터 조건이 존재할 경우, 'and'로 연결합니다.
        if (StringUtils.hasText(name)) {
            spec = spec.and(likeName(name))
        }
        if (StringUtils.hasText(mainSpot)) {
            spec = spec.and(likeMainSpot(mainSpot))
        }
        if (category != null) {
            spec = spec.and(equalCategory(category))
        }
        if (eventType != null) {
            spec = spec.and(equalEventType(eventType))
        }

        // 3. 최종적으로 조합된 Specification과 Pageable 객체로 JpaSpecificationExecutor의 findAll을 호출합니다.
        return clubRepository.findAll(spec, pageable)
            .map { club ->
                SimpleClubInfoWithoutLeader(
                    club.id!!,
                    club.name,
                    club.category.toString(),
                    club.imageUrl,
                    club.mainSpot,
                    club.eventType.toString(),
                    club.startDate.toString(),
                    club.endDate.toString(),
                    club.bio ?: ""
                )
            }
    }

    fun findClubById(clubId: Long): Optional<Club?> {
        return clubRepository.findById(clubId)
    }
}
