package com.back.domain.preset

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.preset.preset.entity.Preset
import com.back.domain.preset.preset.entity.PresetItem
import com.back.domain.preset.preset.repository.PresetRepository
import com.back.global.enums.CheckListItemCategory
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@Transactional
class ApiV1PresetControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var presetRepository: PresetRepository

    private lateinit var member: Member
    private lateinit var preset: Preset

    @BeforeEach
    fun setUp() {
        member = memberRepository.findByMemberInfo_Email("hgd222@test.com")
        ?: throw IllegalStateException("테스트용 멤버(email: hgd222@test.com)가 존재하지 않습니다")

        val presetItems = mutableListOf(
            PresetItem(
                "테스트 아이템 내용 1",
                CheckListItemCategory.RESERVATION,
                1
            )
        )

        val presetBuilder = Preset(
            "테스트 프리셋 1",
            member,
            presetItems
        )

        preset = presetRepository.save(presetBuilder)
    }

    fun presetCreate(): Long {
        val requestBody = """
        {
          "presetItems": [
            { "content": "아이템 1", "category": "${CheckListItemCategory.PREPARATION.name}", "sequence":1 },
            { "content": "아이템 2", "category": "${CheckListItemCategory.ETC.name}", "sequence":2 }
          ],
          "name": "My Custom Preset"
        }
        """.trimIndent()

        val result = mockMvc.perform(
            post("/api/v1/presets")
                .contentType("application/json")
                .content(requestBody)
        ).andReturn()

        val responseContent = result.response.contentAsString
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(responseContent)
        return jsonNode.get("data").get("id").asLong()
    }

    @Test
    @DisplayName("프리셋 생성 테스트")
    @WithUserDetails(value = "hgd222@test.com")
    fun t1() {
        val requestBody = """
        {
          "presetItems": [
            { "content": "아이템 1", "category": "${CheckListItemCategory.PREPARATION.name}", "sequence":1 },
            { "content": "아이템 2", "category": "${CheckListItemCategory.ETC.name}", "sequence":2 }
          ],
          "name": "My Custom Preset"
        }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/presets")
                .contentType("application/json")
                .content(requestBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value(201))
            .andExpect(jsonPath("$.message").value("프리셋 생성 성공"))
            .andExpect(jsonPath("$.data.name").value("My Custom Preset"))
            .andExpect(jsonPath("$.data.presetItems[0].content").value("아이템 1"))
            .andExpect(jsonPath("$.data.presetItems[0].category").value(CheckListItemCategory.PREPARATION.name))
            .andExpect(jsonPath("$.data.presetItems[1].content").value("아이템 2"))
            .andExpect(jsonPath("$.data.presetItems[1].category").value(CheckListItemCategory.ETC.name))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 생성 실패 - 멤버를 찾을 수 없음")
    @WithMockUser(username = "notfound@user.com")
    fun t4() {
        val requestBody = """
        {
          "presetItems": [
            { "content": "아이템 1", "category": "${CheckListItemCategory.PREPARATION.name}", "sequence":1 },
            { "content": "아이템 2", "category": "${CheckListItemCategory.ETC.name}", "sequence":2 }
          ],
          "name": "My Custom Preset"
        }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/presets")
                .contentType("application/json")
                .content(requestBody)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("멤버를 찾을 수 없습니다"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 생성 실패 - 잘못된 요청 형식")
    @WithUserDetails(value = "hgd222@test.com")
    fun t5() {
        val requestBody = """
        {
          "presetItems": [
            { "content": "아이템 1", "category": "${CheckListItemCategory.PREPARATION.name}", "sequence":1 },
            { "content": "아이템 2", "category": "${CheckListItemCategory.ETC.name}", "sequence":2 }
          ],
          "name": ""
        }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/presets")
                .contentType("application/json")
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("name-NotBlank-프리셋 이름은 필수입니다"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 생성 실패 - 잘못된 카테고리")
    @WithUserDetails(value = "hgd222@test.com")
    fun t6() {
        val requestBody = """
        {
          "presetItems": [
            { "content": "아이템 1", "category": "INVALID_CATEGORY", "sequence":1 },
            { "content": "아이템 2", "category": "${CheckListItemCategory.ETC.name}", "sequence":2 }
          ],
          "name": "My Custom Preset"
        }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/presets")
                .contentType("application/json")
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("요청 본문이 올바르지 않습니다."))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 세부 정보 조회")
    @WithUserDetails(value = "hgd222@test.com")
    fun t7() {
        // 먼저 프리셋을 생성합니다.
        val presetId = presetCreate()

        // 생성된 프리셋의 ID를 사용하여 세부 정보를 조회합니다.
        mockMvc.perform(
            get("/api/v1/presets/$presetId")
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("프리셋 조회 성공"))
            .andExpect(jsonPath("$.data.name").value("My Custom Preset"))
            .andExpect(jsonPath("$.data.presetItems[0].content").value("아이템 1"))
            .andExpect(jsonPath("$.data.presetItems[0].category").value(CheckListItemCategory.PREPARATION.name))
            .andExpect(jsonPath("$.data.presetItems[1].content").value("아이템 2"))
            .andExpect(jsonPath("$.data.presetItems[1].category").value(CheckListItemCategory.ETC.name))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 세부 정보 조회 실패 - 프리셋이 존재하지 않음")
    @WithUserDetails(value = "hgd222@test.com")
    fun t8() {
        // 먼저 프리셋을 생성합니다.
        presetCreate()

        // 존재하지 않는 프리셋 ID로 조회 시도
        mockMvc.perform(
            get("/api/v1/presets/9999") // 존재하지 않는 ID
                .contentType("application/json")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("프리셋을 찾을 수 없습니다"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 세부 정보 조회 실패 - 권한 없는 프리셋")
    @WithUserDetails(value = "chs4s@test.com")
    fun t9() {
        // 먼저 프리셋을 생성합니다.
        val presetId = preset.id!!

        // 생성된 프리셋의 ID를 사용하여 세부 정보를 조회합니다.
        mockMvc.perform(
            get("/api/v1/presets/$presetId")
                .contentType("application/json")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("권한 없는 프리셋"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 세부 정보 조회 실패 - 잘못된 요청 형식")
    @WithUserDetails(value = "hgd222@test.com")
    fun t12() {
        // 먼저 프리셋을 생성합니다.
        presetCreate()

        // 잘못된 요청 형식으로 조회 시도
        mockMvc.perform(
            get("/api/v1/presets/invalid-id") // 잘못된 ID 형식
                .contentType("application/json")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("파라미터 'presetId'의 타입이 올바르지 않습니다. 요구되는 타입: long"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 목록 조회")
    @WithUserDetails(value = "hgd222@test.com")
    fun t13() {
        val presetId1 = preset.id!!

        // 추가 프리셋 생성
        val requestBody2 = """
        {
          "presetItems": [
            { "content": "아이템 A", "category": "${CheckListItemCategory.PREPARATION.name}", "sequence":1 },
            { "content": "아이템 B", "category": "${CheckListItemCategory.ETC.name}", "sequence":2 }
          ],
          "name": "테스트 프리셋 2"
        }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/presets")
                .contentType("application/json")
                .content(requestBody2)
        ).andExpect(status().isCreated)

        // 프리셋 목록 조회
        mockMvc.perform(
            get("/api/v1/presets")
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("프리셋 목록 조회 성공"))
            .andExpect(jsonPath("$.data.length()").value(2)) // 두 개의 프리셋이 있어야 함
            .andExpect(jsonPath("$.data[0].id").value(presetId1)) // 첫 번째 프리셋 ID 확인
            .andExpect(jsonPath("$.data[0].name").value("테스트 프리셋 1"))
            .andExpect(jsonPath("$.data[1].name").value("테스트 프리셋 2"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 목록 조회 - 빈 리스트 인 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun t14() {
        // 프리셋 목록 조회
        presetRepository.deleteAll()
        mockMvc.perform(
            get("/api/v1/presets")
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("프리셋 목록 조회 성공"))
            .andExpect(jsonPath("$.data.length()").value(0)) // 빈 리스트 확인
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 목록 조회 실패 - 멤버를 찾을 수 없음")
    @WithMockUser(username = "notfound@user.com")
    fun t17() {
        // 프리셋 목록 조회 시도
        mockMvc.perform(
            get("/api/v1/presets")
                .contentType("application/json")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("멤버를 찾을 수 없습니다"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 삭제")
    @WithUserDetails(value = "hgd222@test.com")
    fun t18() {
        // 먼저 프리셋을 생성합니다.
        val presetId = presetCreate()

        // 생성된 프리셋의 ID를 사용하여 삭제 요청을 보냅니다.
        mockMvc.perform(
            delete("/api/v1/presets/$presetId")
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("프리셋 삭제 성공"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 삭제 실패 - 프리셋이 존재하지 않음")
    @WithUserDetails(value = "hgd222@test.com")
    fun t19() {
        // 존재하지 않는 프리셋 ID로 삭제 요청을 보냅니다.
        mockMvc.perform(
            delete("/api/v1/presets/9999") // 존재하지 않는 ID
                .contentType("application/json")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("프리셋을 찾을 수 없습니다"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 삭제 실패 - 권한 없는 프리셋")
    @WithUserDetails(value = "chs4s@test.com")
    fun t20() {
        // 먼저 프리셋을 생성합니다.
        val presetId = preset.id!!

        // 생성된 프리셋의 ID를 사용하여 삭제 요청을 보냅니다.
        mockMvc.perform(
            delete("/api/v1/presets/$presetId")
                .contentType("application/json")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("권한 없는 프리셋"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 삭제 실패 - 잘못된 요청 형식")
    @WithUserDetails(value = "hgd222@test.com")
    fun t23() {
        // 먼저 프리셋을 생성합니다.
        presetCreate()

        // 잘못된 요청 형식으로 삭제 시도
        mockMvc.perform(
            delete("/api/v1/presets/invalid-id") // 잘못된 ID 형식
                .contentType("application/json")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("파라미터 'presetId'의 타입이 올바르지 않습니다. 요구되는 타입: long"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 수정")
    @WithUserDetails(value = "hgd222@test.com")
    fun t24() {
        // 먼저 프리셋을 생성합니다.
        val presetId = presetCreate()

        val requestBody = """
        {
          "presetItems": [
            { "content": "수정된 아이템 1", "category": "${CheckListItemCategory.PREPARATION.name}", "sequence":1 },
            { "content": "수정된 아이템 2", "category": "${CheckListItemCategory.ETC.name}", "sequence":2 }
          ],
          "name": "수정된 프리셋 이름"
        }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/presets/$presetId")
                .contentType("application/json")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("프리셋 수정 성공"))
            .andExpect(jsonPath("$.data.name").value("수정된 프리셋 이름"))
            .andExpect(jsonPath("$.data.presetItems[0].content").value("수정된 아이템 1"))
            .andExpect(jsonPath("$.data.presetItems[0].category").value(CheckListItemCategory.PREPARATION.name))
            .andExpect(jsonPath("$.data.presetItems[1].content").value("수정된 아이템 2"))
            .andExpect(jsonPath("$.data.presetItems[1].category").value(CheckListItemCategory.ETC.name))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 수정 실패 - 프리셋이 존재하지 않음")
    @WithUserDetails(value = "hgd222@test.com")
    fun t25() {
        val requestBody = """
        {
          "presetItems": [
            { "content": "수정된 아이템 1", "category": "${CheckListItemCategory.PREPARATION.name}", "sequence":1 },
            { "content": "수정된 아이템 2", "category": "${CheckListItemCategory.ETC.name}", "sequence":2 }
          ],
          "name": "수정된 프리셋 이름"
        }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/presets/9999") // 존재하지 않는 ID
                .contentType("application/json")
                .content(requestBody)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("프리셋을 찾을 수 없습니다"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 수정 실패 - 권한 없는 프리셋")
    @WithUserDetails(value = "chs4s@test.com")
    fun t26() {
        val presetId = preset.id!!
        val requestBody = """
        {
          "presetItems": [
            { "content": "수정된 아이템 1", "category": "${CheckListItemCategory.PREPARATION.name}", "sequence":1 },
            { "content": "수정된 아이템 2", "category": "${CheckListItemCategory.ETC.name}", "sequence":2 }
          ],
          "name": "수정된 프리셋 이름"
        }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/presets/$presetId")
                .contentType("application/json")
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("권한 없는 프리셋"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 수정 실패 - 잘못된 요청 형식")
    @WithUserDetails(value = "hgd222@test.com")
    fun t29() {
        // 먼저 프리셋을 생성합니다.
        val presetId = presetCreate()

        val requestBody = """
        {
          "presetItems": [
            { "content": "수정된 아이템 1", "category": "${CheckListItemCategory.PREPARATION.name}", "sequence":1 },
            { "content": "수정된 아이템 2", "category": "${CheckListItemCategory.ETC.name}", "sequence":2 }
          ],
          "name": ""
        }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/presets/$presetId")
                .contentType("application/json")
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("name-NotBlank-프리셋 이름은 필수입니다"))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 수정 실패 - 잘못된 카테고리")
    @WithUserDetails(value = "hgd222@test.com")
    fun t30() {
        // 먼저 프리셋을 생성합니다.
        val presetId = presetCreate()

        val requestBody = """
        {
          "presetItems": [
            { "content": "수정된 아이템 1", "category": "INVALID_CATEGORY", "sequence":1 },
            { "content": "수정된 아이템 2", "category": "${CheckListItemCategory.ETC.name}", "sequence":2 }
          ],
          "name": "수정된 프리셋 이름"
        }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/presets/$presetId")
                .contentType("application/json")
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("요청 본문이 올바르지 않습니다."))
            .andDo(print())
    }

    @Test
    @DisplayName("프리셋 수정 성공 - 프리셋 아이템이 비어있을 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun t31() {
        // 먼저 프리셋을 생성합니다.
        val presetId = presetCreate()

        val requestBody = """
        {
          "presetItems": [],
          "name": "빈 아이템 프리셋"
        }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/presets/$presetId")
                .contentType("application/json")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("프리셋 수정 성공"))
            .andExpect(jsonPath("$.data.name").value("빈 아이템 프리셋"))
            .andExpect(jsonPath("$.data.presetItems.length()").value(0)) // 빈 아이템 리스트 확인
            .andDo(print())
    }
}