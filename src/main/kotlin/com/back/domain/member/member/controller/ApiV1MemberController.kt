package com.back.domain.member.member.controller

import com.back.domain.api.request.TokenRefreshRequest
import com.back.domain.member.member.dto.request.*
import com.back.domain.member.member.dto.response.*
import com.back.domain.member.member.error.MemberErrorCode
import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@RestController
@RequestMapping("api/v1/members")
class ApiV1MemberController(
    private val memberService: MemberService,
    private val rq: Rq
) {

    // ================= 회원용 API =================
    @Operation(summary = "회원가입 API", description = "이메일, 비밀번호 등을 받아 회원가입을 처리합니다.")
    @PostMapping("/auth/register")
    fun register(
        @Valid @RequestBody memberRegisterDto: MemberRegisterDto,
        response: HttpServletResponse
    ): RsData<MemberAuthResponse> {
        val memberAuthResponse = memberService.registerMember(memberRegisterDto)
        response.addCookie(createAccessTokenCookie(memberAuthResponse.accessToken, false))
        return RsData.of(200, "회원가입 성공", memberAuthResponse)
    }

    @Operation(summary = "로그인 API", description = "이메일과 비밀번호를 받아 로그인을 처리합니다.")
    @PostMapping("/auth/login")
    fun login(
        @Valid @RequestBody memberLoginDto: MemberLoginDto,
        response: HttpServletResponse
    ): RsData<MemberAuthResponse> {
        val memberAuthResponse = memberService.loginMember(memberLoginDto)
        response.addCookie(createAccessTokenCookie(memberAuthResponse.accessToken, false))
        return RsData.of(200, "로그인 성공", memberAuthResponse)
    }

    @Operation(summary = "로그아웃 API", description = "로그아웃 처리 API입니다.")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/auth/logout")
    fun logout(response: HttpServletResponse): RsData<MemberAuthResponse> {
        response.addCookie(deleteCookie())
        return RsData.of(200, "로그아웃 성공")
    }

    @Operation(summary = "회원탈퇴 API", description = "회원탈퇴 처리 API 입니다.")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me")
    fun withdrawMembership(response: HttpServletResponse): RsData<MemberWithdrawMembershipResponse> {
        val user = rq.actor ?: throw ServiceException(401, "인증이 필요합니다.")
        val responseDto = memberService.withdrawMember(
                        user.nickname,
                        user.tag ?: throw ServiceException(
                            MemberErrorCode.MEMBER_NOT_FOUND.status,
                            MemberErrorCode.MEMBER_NOT_FOUND.message
                        )
        )
        response.addCookie(deleteCookie())
        return RsData.of(200, "회원탈퇴 성공", responseDto)
    }

    // ================= 내 정보 관련 API =================
    @Operation(summary = "내 정보 반환 API", description = "현재 로그인한 유저 정보를 반환하는 API 입니다.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    fun getMyInfo(response: HttpServletResponse): RsData<MemberDetailInfoResponse> {
        val user = rq.actor ?: throw ServiceException(401, "인증이 필요합니다.")
        val memberDetailInfoResponse = memberService.getMemberInfo(user.id ?: throw ServiceException(401, "회원을 찾지 못했습니다."))
        return RsData.of(200, "유저 정보 반환 성공", memberDetailInfoResponse)
    }

    @Operation(summary = "내 정보 수정 API", description = "현재 로그인한 유저 정보를 수정하는 API 입니다.")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    @Throws(IOException::class)
    fun updateInfo(
        @RequestPart("data") dto: UpdateMemberInfoDto,
        @RequestPart("profileImage", required = false) profileImage: MultipartFile?,
    ): RsData<MemberDetailInfoResponse> {
        val user = rq.actor ?: throw ServiceException(401, "인증이 필요합니다.")
        val memberId = user.id ?: throw ServiceException(
                        MemberErrorCode.MEMBER_NOT_FOUND.status,
                        MemberErrorCode.MEMBER_NOT_FOUND.message)
        val memberDetailInfoResponse = memberService.updateMemberInfo(memberId, dto, profileImage)
        return RsData.of(200, "유저 정보 수정 성공", memberDetailInfoResponse)
    }

    // ================= 비회원용 API =================
    @Operation(summary = "비회원 모임 등록 API", description = "비회원 모임 등록 API 입니다.")
    @PostMapping("/auth/guest-register")
    fun registerGuest(
        response: HttpServletResponse,
        @Valid @RequestBody dto: GuestDto
    ): RsData<GuestResponse> {
        val guestResponse = memberService.registerGuestMember(dto)
        response.addCookie(createAccessTokenCookie(guestResponse.accessToken, true))
        return RsData.of(200, "비회원 모임 가입 성공", guestResponse)
    }

    @Operation(summary = "비회원 임시 로그인 API", description = "비회원 임시 로그인 API 입니다.")
    @PostMapping("/auth/guest-login")
    fun guestLogin(
        response: HttpServletResponse,
        @Valid @RequestBody guestDto: GuestDto
    ): RsData<GuestResponse> {
        val guestAuthResponse = memberService.loginGuestMember(guestDto)
        response.addCookie(createAccessTokenCookie(guestAuthResponse.accessToken, true))
        return RsData.of(200, "비회원 로그인 성공", guestAuthResponse)
    }

    // ================= 회원, 비회원용 쿠키 생성/삭제 =================
    private fun createAccessTokenCookie(accessToken: String, isGuest: Boolean): Cookie {
        return Cookie("accessToken", accessToken).apply {
            isHttpOnly = true
            secure = true
            path = "/"
            maxAge = if (isGuest) 60 * 60 * 24 * 30 else 60 * 60 * 24
            setAttribute("SameSite", "Strict")
        }
    }

    private fun deleteCookie(): Cookie {
        return Cookie("accessToken", "").apply {
            isHttpOnly = true
            secure = true
            path = "/"
            maxAge = 0
        }
    }

    // ================= 기타 API =================
    @Operation(summary = "비밀번호 유효성 검사 API", description = "비밀번호의 유효성을 인증하는 API 입니다.")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/auth/verify-password")
    fun checkPasswordValidity(@Valid @RequestBody dto: PasswordCheckRequestDto): RsData<MemberPasswordResponse> {
        val user = rq.actor ?: throw ServiceException(401, "인증이 필요합니다.")
        val memberId = user.id ?: throw ServiceException(
            MemberErrorCode.MEMBER_NOT_FOUND.status,
            MemberErrorCode.MEMBER_NOT_FOUND.message
        )
        val response = memberService.checkPasswordValidity(memberId, dto.password)
        return RsData.of(200, "비밀번호 유효성 반환 성공", response)
    }

    @Operation(summary = "access token 재발급 API", description = "리프레시 토큰으로 access token을 재발급하는 API 입니다.")
    @PostMapping("/auth/refresh")
    fun apiTokenReissue(
        @Valid @RequestBody requestBody: TokenRefreshRequest,
        response: HttpServletResponse
    ): RsData<MemberAuthResponse> {
        val apiKey = requestBody.refreshToken

        val member = memberService.findMemberByApiKey(apiKey)
        val newAccessToken = memberService.generateAccessToken(member)
        response.addCookie(createAccessTokenCookie(newAccessToken, false))

        return RsData.of(200, "Access Token 재발급 성공", MemberAuthResponse(apiKey, newAccessToken))
    }
}
