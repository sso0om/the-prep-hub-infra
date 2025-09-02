package com.back.domain.member.member

import com.back.domain.api.service.ApiKeyService
import com.back.domain.auth.service.AuthService
import com.back.domain.club.club.entity.Club
import com.back.domain.club.club.repository.ClubRepository
import com.back.domain.club.clubMember.repository.ClubMemberRepository
import com.back.domain.member.member.dto.request.GuestDto
import com.back.domain.member.member.dto.request.MemberRegisterDto
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.entity.MemberInfo
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.member.service.MemberService
import com.back.domain.member.member.support.MemberFixture
import com.back.global.aws.S3Service
import com.back.global.enums.MemberType
import com.back.global.exception.ServiceException
import com.back.global.security.SecurityUser
import com.jayway.jsonpath.JsonPath
import jakarta.servlet.http.Cookie
import org.assertj.core.api.AssertionsForClassTypes
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.util.*

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class ApiV1MemberControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc   //  필요 없음

    @Autowired
    lateinit var memberFixture: MemberFixture

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var apiKeyService: ApiKeyService

    @Autowired
    lateinit var authService: AuthService

    @Autowired
    lateinit var clubRepository: ClubRepository

    @Autowired
    lateinit var memberService: MemberService

    @Autowired
    lateinit var clubMemberRepository: ClubMemberRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @MockBean
    lateinit var s3Service: S3Service

    @Test
    @DisplayName("회원가입 - 정상 기입 / 객체 정상 생성")
    fun memberObjectCreationTest() {
        val memberInfo = MemberInfo()
        memberInfo.email = "qkqek6223@naver.com"
        memberInfo.bio = "안녕하세요 반갑습니다"
        memberInfo.profileImageUrl = "https://picsum.photos/seed/picsum/200/300"

        // Member 클래스의 필수 인자 (nickname, memberType)를 주 생성자에 직접 전달
        val member = Member(
            nickname = "안수지",
            password = "password123",
            memberType = MemberType.MEMBER // MemberType 열거형 사용
        )
        member.setMemberInfo(memberInfo)

        Assertions.assertEquals("안수지", member.nickname)
        Assertions.assertEquals("password123", member.password)
        Assertions.assertNotNull(member.getMemberInfo())
        Assertions.assertEquals("qkqek6223@naver.com", member.getMemberInfo()!!.email)
        Assertions.assertEquals("안녕하세요 반갑습니다", member.getMemberInfo()!!.bio)
    }

    @Test
    @DisplayName("회원가입 - 정상 기입 / POST 정상 작동")
    @Throws(Exception::class)
    fun memberPostTest() {
        val requestBody = """
                {
                    "email": "qkek6223@naver.com",
                    "password": "password123",
                    "nickname": "안수지",
                    "bio": "안녕하세요"
                }
                
                """.trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("회원가입 성공"))
    }

    @Test
    @DisplayName("회원가입 - 이메일 중복 기입 / POST 실패")
    @Throws(Exception::class)
    fun memberPostTestException1() {
        val requestBody = """
                {
                    "email": "qkek6223@naver.com",
                    "password": "password123",
                    "nickname": "안수지",
                    "bio": "안녕하세요"
                }
                
                """.trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("회원가입 성공"))

        val requestBody2 = """
                {
                    "email": "qkek6223@naver.com",
                    "password": "password123",
                    "nickname": "안수지1",
                    "bio": "안녕하세요"
                }
                
                """.trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody2)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("이미 사용 중인 이메일입니다."))
    }

    @Test
    @DisplayName("회원가입 - 이메일 형식 오류로 실패")
    @Throws(Exception::class)
    fun registerWithInvalidEmailFormat() {
        val requestBody = """
        {
            "email": "invalid-email-format",
            "password": "password123",
            "nickname": "userInvalidEmail",
            "bio": "bio"
        }
        
        """.trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("email-Email-이메일 형식이 올바르지 않습니다."))
    }

    @Test
    @DisplayName("회원가입 - 비밀번호 미입력(빈값)으로 실패")
    @Throws(Exception::class)
    fun registerWithBlankPassword() {
        val requestBody = """
        {
            "email": "user@example.com",
            "password": "",
            "nickname": "userNoPassword",
            "bio": "bio"
        }
        
        """.trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("password-NotBlank-비밀번호는 필수 입력값입니다."))
    }

    @Test
    @DisplayName("회원가입 - 닉네임 누락으로 실패")
    @Throws(Exception::class)
    fun registerWithMissingNickname() {
        val requestBody = """
        {
            "email": "user@example.com",
            "password": "password123",
            "bio": "bio"
        }
        
        """.trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("요청 본문이 올바르지 않습니다."))
    }


    @Test
    @DisplayName("API key 발급 - 정상")
    @Throws(Exception::class)
    fun generateApiKey_success() {
        val apiKey = apiKeyService.generateApiKey()

        Assertions.assertNotNull(apiKey)
        Assertions.assertTrue(apiKey.startsWith("api_"))
    }

    @Test
    @DisplayName("AccessToken 발급 - 정상")
    @Throws(Exception::class)
    fun generateAccessToken_success() {
        val member = memberFixture.createMember(1)

        val accessToken = authService.generateAccessToken(member)

        Assertions.assertNotNull(accessToken)
    }

    @Test
    @DisplayName("로그인 - 정상 기입")
    @Throws(Exception::class)
    fun loginSuccess() {
        memberFixture.createMember(1)

        val requestBody = """
        {
            "email": "test1@example.com",
            "password": "password123"
        }
        
        """.trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.apikey").isNotEmpty())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").isNotEmpty())
    }

    @Test
    @DisplayName("로그인 - 없는 이메일 기입")
    @Throws(Exception::class)
    fun loginNonexistentEmail() {
        memberFixture.createMember(1)

        val requestBody = """
        {
            "email": "wrong@example.com",
            "password": "password123"
        }
        
        """.trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("해당 사용자를 찾을 수 없습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("로그인 - 맞지 않는 비밀번호 기입")
    @Throws(Exception::class)
    fun loginWrongPassword() {
        memberFixture.createMember(1)

        val requestBody = """
        {
            "email": "test1@example.com",
            "password": "WrongPassword"
        }
        
        """.trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("해당 사용자를 찾을 수 없습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 대소문자 구분 (대문자 입력 시도)")
    @Throws(Exception::class)
    fun loginFail_emailCaseSensitive() {
        memberFixture.createMember(1) // test1@example.com 으로 회원 생성됨

        val requestBody = """
    {
        "email": "TEST1@EXAMPLE.COM",
        "password": "password123"
    }
    
    """.trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("해당 사용자를 찾을 수 없습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 공백 입력")
    @Throws(Exception::class)
    fun loginFail_blankPassword() {
        memberFixture.createMember(1) // 회원 생성

        val requestBody = """
    {
        "email": "test1@example.com",
        "password": ""
    }
    
    """.trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest()) // 빈 비밀번호는 보통 400 Bad Request 처리 예상
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.message").value("password-NotBlank-비밀번호는 필수 입력값입니다.")
            ) // DTO @NotBlank 메시지와 일치하게 수정
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
    }


    @Test
    @DisplayName("로그아웃 - 정상 처리")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun logout() {
        memberFixture.createMember(1)

        val accessTokenCookie = loginAndGetAccessTokenCookie("test1@example.com", "password123")

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/members/auth/logout")
                .cookie(accessTokenCookie)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.cookie().maxAge("accessToken", 0)) // 쿠키 만료 확인
    }

    @Test
    @DisplayName("회원탈퇴 - 정상 처리")
    @Throws(Exception::class)
    fun withdrawMembership() {
        val member = memberFixture.createMember(1)

        val accessTokenCookie = loginAndGetAccessTokenCookie("test1@example.com", "password123")

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/members/me")
                .with(
                    SecurityMockMvcRequestPostProcessors.user(
                        SecurityUser(
                            member.id!!,
                            member.nickname,
                            member.tag!!,
                            member.memberType,
                            member.password!!,
                            mutableListOf<GrantedAuthority>()
                        )
                    )
                )
                .cookie(accessTokenCookie)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(member.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.tag").value(member.tag))
            .andExpect(MockMvcResultMatchers.cookie().maxAge("accessToken", 0)) // 쿠키 만료 확인

        val deletedMember = memberRepository.findById(member.id!!)
        AssertionsForClassTypes.assertThat<Member?>(deletedMember).isEmpty()
    }

    @Test
    @DisplayName("회원탈퇴 - 인증 없이 탈퇴 요청 시도")
    @Throws(Exception::class)
    fun withdrawMembership_Unauthenticated_Failure() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/members/me")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(401))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("로그인 후 이용해주세요."))
    }

    @Test
    @DisplayName("회원탈퇴 - 존재하지 않는 회원 ID로 탈퇴 시도 시 404 Not Found")
    @Throws(Exception::class)
    fun withdrawMembership_NonexistentUserId_Failure() {
        // 실제 DB에 없는 임의의 회원 ID 사용
        val fakeMemberId = 999999L

        val fakeUser = SecurityUser(
            fakeMemberId,
            "fakeNickname",
            "fakeTag",
            MemberType.MEMBER,
            "fakePassword",
            mutableListOf<GrantedAuthority>()
        )

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/members/me")
                .with(SecurityMockMvcRequestPostProcessors.user(fakeUser))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("회원 정보를 찾을 수 없습니다."))
    }


    @Test
    @DisplayName("access token 재발급 - 정상")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun reGenerateAccessToken_success() {
        //회원 생성
        val member = memberFixture.createMember(1)

        //로그인하여 액세스 토큰 쿠키 받기
        loginAndGetAccessTokenCookie("test1@example.com", "password123")

        //apiKey 멤버에서 가져오기
        val apiKey = member.getMemberInfo()!!.apiKey

        //액세스 토큰 재발급 요청 바디
        val requestBody = String.format(
            """
        {
            "refreshToken": "%s"
        }
        
        """.trimIndent(), apiKey
        )

        //재발급 api 호출 및 검증
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Access Token 재발급 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.apikey").value(apiKey))
            .andExpect(MockMvcResultMatchers.cookie().exists("accessToken"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("Access Token 재발급 실패 - 잘못된 또는 만료된 refreshToken")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(
        Exception::class
    )
    fun reGenerateAccessToken_fail_invalidOrExpiredRefreshToken() {
        val invalidRefreshToken = "invalid_or_expired_refresh_token"

        val requestBody = String.format(
            """
    {
        "refreshToken": "%s"
    }
    
    """.trimIndent(), invalidRefreshToken
        )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(401))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("유효하지 않은 API Key입니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("로그아웃 실패 - 잘못된 accessToken 으로 요청")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun logout_fail_invalidAccessToken() {
        val invalidAccessTokenCookie = Cookie("accessToken", "invalid_access_token_value")

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/members/auth/logout")
                .cookie(invalidAccessTokenCookie)
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(499))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("access token이 유효하지 않습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
    }


    @Test
    @DisplayName("내 정보 조회 - 정상")
    @Throws(Exception::class)
    fun getMyInfo_success() {
        val member = memberFixture.createMember(1)
        val memberInfo = member.getMemberInfo()

        val securityUser = SecurityUser(
            member.id!!,
            member.nickname,
            member.tag!!,
            member.memberType,
            member.password!!,
            mutableListOf<GrantedAuthority>()
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/members/me")
                .with(SecurityMockMvcRequestPostProcessors.user(securityUser))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("유저 정보 반환 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(member.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(memberInfo!!.email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.profileImage").value(memberInfo!!.profileImageUrl))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.bio").value(memberInfo.bio))
    }

    @Test
    @DisplayName("비밀번호 유효성 검사 - 정상")
    @Throws(Exception::class)
    fun checkPasswordValidity_success() {
        val member = memberFixture.createMember(1)
        val memberInfo = member.getMemberInfo()
        val rawPassword = "password123" //평문 비밀번호

        val requestBody = String.format(
            """
        {
            "password": "%s"
        }
        
        """.trimIndent(), rawPassword
        )

        val securityUser = SecurityUser(
            requireNotNull(member.id) { "member.id must not be null" },
            member.nickname,
            requireNotNull(member.tag) { "member.tag must not be null" },
            member.memberType,
            requireNotNull(member.password) { "member.password must not be null" },
            mutableListOf<GrantedAuthority>()
        )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .cookie(loginAndGetAccessTokenCookie(memberInfo!!.email, rawPassword))
                .with(SecurityMockMvcRequestPostProcessors.user(securityUser))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("비밀번호 유효성 반환 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.verified").value(true))
    }

    @Test
    @DisplayName("비밀번호 유효성 검사 - 잘못된 비밀번호")
    @Throws(Exception::class)
    fun checkPasswordValidity_wrongPassword() {
        val member = memberFixture.createMember(1)
        val memberInfo = member.getMemberInfo()
        val wrongPassword = "wrongPassword!" // 틀린 비밀번호

        val requestBody = String.format(
            """
    {
        "password": "%s"
    }
    
    """.trimIndent(), wrongPassword
        )

        val securityUser = SecurityUser(
            requireNotNull(member.id) { "member.id must not be null" },
            member.nickname,
            requireNotNull(member.tag) { "member.tag must not be null" },
            member.memberType,
            requireNotNull(member.password) { "member.password must not be null" },
            mutableListOf<GrantedAuthority>()
        )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .cookie(loginAndGetAccessTokenCookie(memberInfo!!.email, "password123")) // 실제 맞는 비밀번호로 로그인
                .with(SecurityMockMvcRequestPostProcessors.user(securityUser))
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.verified").value(false))
    }

    @Test
    @DisplayName("비밀번호 유효성 검사 - 인증되지 않은 사용자")
    @Throws(Exception::class)
    fun checkPasswordValidity_unauthorized() {
        val requestBody = """
    {
        "password": "password123"
    }
    
    """.trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(401))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("로그인 후 이용해주세요."))
    }

    @Test
    @DisplayName("비밀번호 유효성 검사 - 빈 비밀번호")
    @Throws(Exception::class)
    fun checkPasswordValidity_blankPassword() {
        val member = memberFixture.createMember(1)
        val memberInfo = member.getMemberInfo()

        val requestBody = """
    {
        "password": ""
    }
    
    """.trimIndent()

        val securityUser = SecurityUser(
            requireNotNull(member.id) { "member.id must not be null" },
            member.nickname,
            requireNotNull(member.tag) { "member.tag must not be null" },
            member.memberType,
            requireNotNull(member.password) { "member.password must not be null" },
            mutableListOf<GrantedAuthority>()
        )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .cookie(loginAndGetAccessTokenCookie(memberInfo!!.email, "password123"))
                .with(SecurityMockMvcRequestPostProcessors.user(securityUser))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest()) // @NotBlank 검증 실패 예상
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400)) // 상세 메시지는 DTO의 @NotBlank 메시지에 따라 다름
    }

    @Test
    @DisplayName("유저 정보 수정 - 성공")
    fun updateUserInfoTest_green() {
        // 1. Member 생성
        val member = memberFixture.createMember(1)

        val securityUser = SecurityUser(
            requireNotNull(member.id),
            member.nickname,
            requireNotNull(member.tag),
            member.memberType,
            requireNotNull(member.password),
            mutableListOf()
        )

        // 2. MultipartFile 준비 (프로필 이미지)
        val imagePart = MockMultipartFile(
            "profileImage",           // 컨트롤러 파라미터 이름과 일치
            "profileImage.jpg",       // 실제 파일명
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-content".toByteArray()
        )

        // 3. S3Service stub
        val path = "members/profile"
        whenever(s3Service.upload(imagePart, path))
            .thenReturn("http://s3.com/profile.jpg")

        // 4. JSON 데이터 MultipartFile
        val requestBody = """
        {
            "nickname": "개나리",
            "password": "newPassword",
            "bio": "노란색 개나리"
        }
    """.trimIndent()

        val dataPart = MockMultipartFile(
            "data", "data",
            MediaType.APPLICATION_JSON_VALUE,
            requestBody.toByteArray(StandardCharsets.UTF_8)
        )

        // 5. MockMvc 수행 (Multipart PUT)
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/members/me")
                .file(dataPart)
                .file(imagePart)
                .with(SecurityMockMvcRequestPostProcessors.user(securityUser))
                .with { request ->
                    request.method = "PUT" // multipart PUT 처리
                    request
                }
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value("개나리"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.bio").value("노란색 개나리"))
    }






    @Test
    @DisplayName("회원 정보 수정 - 잘못된 multipart 형식으로 요청 (data part 누락)")
    @Throws(Exception::class)
    fun updateUserInfo_invalidMultipartFormat() {
        val member = memberFixture.createMember(1)

        val securityUser = SecurityUser(
            requireNotNull(member.id) { "member.id must not be null" },
            member.nickname,
            requireNotNull(member.tag) { "member.tag must not be null" },
            member.memberType,
            requireNotNull(member.password) { "member.password must not be null" },
            mutableListOf<GrantedAuthority>()
        )

        // data part 없이 프로필 이미지 파일만 보냄 → 예외 발생 예상
        val imagePart = MockMultipartFile(
            "profileImage",
            "profile.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-content".toByteArray(StandardCharsets.UTF_8)
        )

        mockMvc.perform(
            MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/v1/members/me")
                .file(imagePart)
                .with(SecurityMockMvcRequestPostProcessors.user(securityUser))
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("필수 multipart 파트 'data'가 존재하지 않습니다."))
    }

    @Test
    @DisplayName("회원 정보 수정 - 허용되지 않은 이미지 포맷 업로드 시 400 Bad Request")
    @Throws(Exception::class)
    fun updateUserInfo_invalidImageFormat() {
        val member = memberFixture.createMember(1)

        val securityUser = SecurityUser(
            requireNotNull(member.id) { "member.id must not be null" },
            member.nickname,
            requireNotNull(member.tag) { "member.tag must not be null" },
            member.memberType,
            requireNotNull(member.password) { "member.password must not be null" },
            mutableListOf<GrantedAuthority>()
        )

        val requestBody = """
            {
                "nickname": "개나리",
                "password": "newPassword",
                "bio": "노란색 개나리"
            }
            
            """.trimIndent()

        val dataPart = MockMultipartFile(
            "data", "data",
            MediaType.APPLICATION_JSON_VALUE,
            requestBody.toByteArray(StandardCharsets.UTF_8)
        )

        // 허용되지 않는 파일 확장자 (예: .txt)
        val invalidImagePart = MockMultipartFile(
            "profileImage", "profileImage.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "some text content".toByteArray(StandardCharsets.UTF_8)
        )

        mockMvc.perform(
            MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/v1/members/me")
                .file(dataPart)
                .file(invalidImagePart)
                .with(SecurityMockMvcRequestPostProcessors.user(securityUser))
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
    }


    @Test
    @DisplayName("비회원 모임 등록 - 성공 및 DB 저장 확인")
    @Throws(Exception::class)
    fun GuestRegister_success_withDbCheck() {
        // 회원 생성 (기존 회원 fixture)
        val guest = memberFixture.createMember(1)

        // API 요청 바디 (요청하는 비회원 정보)
        val nickname = "guestUser"
        val rawPassword = "guestPassword123"
        val clubId = 1L
        val club = clubRepository.findById(clubId).orElseThrow()
        val requestBody = String.format(
            """
        {
            "nickname": "%s",
            "password": "%s",
            "clubId": %d
        }
        
        """.trimIndent(), nickname, rawPassword, clubId
        )

        // API 호출 및 응답 검증
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/guest-register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("비회원 모임 가입 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubId").value(clubId))
            .andExpect(MockMvcResultMatchers.cookie().exists("accessToken"))

        // DB에서 저장 여부 확인
        val savedGuestOpt = memberRepository.findByNickname(nickname)
        Assertions.assertTrue(savedGuestOpt != null, "비회원 게스트 회원이 멤버 DB에 저장되어야 합니다.")

        val savedGuest = savedGuestOpt
            ?: throw NoSuchElementException("회원이 존재하지 않습니다.")
        val savedClubGuestOpt = clubMemberRepository.findByClubAndMember(club, savedGuest)
        Assertions.assertTrue(savedClubGuestOpt != null, "비회원 게스트 회원이 클럽멤버 DB에 저장되어야 합니다.")

        Assertions.assertEquals(nickname, savedGuest.nickname)
        Assertions.assertEquals(MemberType.GUEST, savedGuest.memberType)

        // 비밀번호는 암호화되어 저장되었을 것이므로, 평문과 다를 것
        Assertions.assertNotEquals(rawPassword, savedGuest.password)

        // tag가 자동 생성되거나 세팅된다면, null이 아닌지 확인 가능
        Assertions.assertNotNull(savedGuest.tag)
    }

    @Test
    @DisplayName("비회원 임시 로그인 - 정상 처리")
    fun guestLogin_success() {
        val guest: Member = memberRepository.findByNickname("김암호")
            ?: throw ServiceException(400, "회원을 찾을 수 없습니다.")

        val club: Club = clubRepository.findAll()
            .firstOrNull { it.name == "친구 모임2" }
            ?: throw IllegalArgumentException("친구 모임2 클럽을 찾을 수 없습니다.")

        println("친구 모임2 ID = ${club.id}")

        val rawPassword = "password13"

        // Kotlin의 삼중 따옴표 문자열과 문자열 템플릿 사용으로 간결하게 변경
        val requestBody = """
        {
            "nickname": "${guest.nickname}",
            "password": "$rawPassword",
            "clubId": ${club.id}
        }
    """.trimIndent()

        // MockMvc 호출 부분의 불필요한 클래스 명 제거
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/guest-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("비회원 로그인 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(guest.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubId").value(club.id!!.toInt()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(MockMvcResultMatchers.cookie().exists("accessToken"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("비회원 닉네임 중복 확인 - 중복된 경우")
    fun guestNicknameDuplicate_shouldReturnTrue() {
        // given
        val guestDto = GuestDto("중복회원", "password", 5L)

        memberService.registerGuestMember(guestDto)

        val duplicateNickname = "중복회원"

        // Kotlin의 삼중 따옴표와 문자열 템플릿을 사용하여 requestBody를 더 간결하게 정의
        val requestBody = """
        {
            "nickname": "$duplicateNickname",
            "password": "password12",
            "clubId": 5
        }
    """.trimIndent()

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/guest-register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("이미 사용 중인 닉네임입니다."))
    }


    @Test
    @DisplayName("회원가입 - 이메일 중복 시 예외 발생")
    fun registerWithDuplicateEmailThrowsException() {
        val memberRegisterDto1 = MemberRegisterDto("1", "pw1", "user1", "안녕하세요")
        val memberRegisterDto2 = MemberRegisterDto("1", "pw1", "user2", "안녕하세요")

        memberService.registerMember(memberRegisterDto1)

        AssertionsForClassTypes.assertThatThrownBy(ThrowableAssert.ThrowingCallable {
            memberService.registerMember(memberRegisterDto2)
        }).isInstanceOf(ServiceException::class.java)
    }

    @Test
    @DisplayName("회원가입 - 비밀번호 해싱 성공")
    fun registerPasswordHashingAndMatching() {
        val rawPassword = "pw1"

        memberService.registerMember(MemberRegisterDto("1", rawPassword, "user1", "<>"))
        val savedMember = memberRepository.findByNickname("user1")
            ?: throw NoSuchElementException("회원이 존재하지 않습니다.")

        val savedHashedPassword = savedMember.password

        Assertions.assertNotEquals(rawPassword, savedHashedPassword)

        Assertions.assertTrue(passwordEncoder.matches(rawPassword, savedHashedPassword))

        Assertions.assertFalse(passwordEncoder.matches("wrongPassword", savedHashedPassword))
    }

    @Test
    @DisplayName("로그인 - 액세스토큰에 tag, memberType 포함 확인")
    @Throws(Exception::class)
    fun accessToken_containsTagAndMemberType() {
        memberFixture.createMember(1)

        val requestBody = """
        {
            "email": "test1@example.com",
            "password": "password123"
        }
    
    """.trimIndent()

        val result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").isNotEmpty())
            .andReturn()

        // 액세스토큰 문자열 추출
        val accessToken = JsonPath.read<String>(result.getResponse().getContentAsString(), "$.data.accessToken")

        // JWT 구조: header.payload.signature -> 우리는 payload만 디코딩
        val parts: Array<String?> = accessToken.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        Assertions.assertEquals(3, parts.size, "JWT 형식이 아님")

        // payload(base64) 디코딩 후 JSON 문자열로 변환
        val decodedBytes = Base64.getDecoder().decode(parts[1])
        val payloadJson = String(decodedBytes, StandardCharsets.UTF_8)

        // JSON 파싱 없이 문자열 포함 여부만 확인해도 기본적인 검증 가능
        Assertions.assertTrue(payloadJson.contains("\"tag\":\""), "토큰에 tag 포함 안됨")
        Assertions.assertTrue(payloadJson.contains("\"memberType\":\"MEMBER\""), "토큰에 memberType 포함 안됨")
    }


    @Throws(Exception::class)
    private fun loginAndGetAccessTokenCookie(email: String?, password: String?): Cookie? {
        val loginRequestBody = String.format(
            """
        {
            "email": "%s",
            "password": "%s"
        }
        
        """.trimIndent(), email, password
        )

        return mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/members/auth/login")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(loginRequestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.cookie().exists("accessToken"))
            .andDo(MockMvcResultHandlers.print())
            .andReturn()
            .getResponse()
            .getCookie("accessToken")
    }
}