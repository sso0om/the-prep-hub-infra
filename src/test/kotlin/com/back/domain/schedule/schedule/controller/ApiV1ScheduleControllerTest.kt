package com.back.domain.schedule.schedule.controller

import com.back.domain.schedule.schedule.dto.response.ScheduleDetailDto
import com.back.domain.schedule.schedule.dto.response.ScheduleDto
import com.back.domain.schedule.schedule.dto.response.ScheduleWithClubDto
import com.back.domain.schedule.schedule.service.ScheduleService
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
internal class ApiV1ScheduleControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var scheduleService: ScheduleService

    @Test
    @DisplayName("일정 목록 조회 - 날짜 파라미터 없는 경우 현재 달 기준")
    @WithUserDetails(value = "hgd222@test.com")
    fun trl1() {
        // given
        val clubId = 1L
        val schedules = scheduleService.getClubSchedules(clubId, null, null)

        // when
        val resultActions = mockMvc
            .perform(get("/api/v1/schedules/clubs/$clubId"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("getClubSchedules"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("일정 목록이 조회되었습니다."))
            .andExpect(jsonPath("$.data.length()").value(schedules.size))
            .andExpectScheduleList(schedules)
    }

    @Test
    @DisplayName("일정 목록 조회 - 날짜 파라미터 있는 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun trl2() {
        // given
        val clubId = 1L
        val startDateStr = "2025-08-01"
        val endDateStr = "2025-08-31"
        val schedules = scheduleService.getClubSchedules(clubId, LocalDate.parse(startDateStr), LocalDate.parse(endDateStr))

        // when
        val resultActions = mockMvc
            .perform(get("/api/v1/schedules/clubs/$clubId")
                .param("startDate", startDateStr)
                .param("endDate", endDateStr)
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("getClubSchedules"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("일정 목록이 조회되었습니다."))
            .andExpect(jsonPath("$.data.length()").value(schedules.size))
            .andExpectScheduleList(schedules)
    }

    @Test
    @DisplayName("일정 목록 조회 - 시작일 파라미터만 있는 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun trl3() {
        // given
        val clubId = 1L
        val startDateStr = "2025-07-15"
        // Service 로직에 따라 endDate가 null이면 해당 월의 마지막 날로 처리되므로, 테스트 데이터도 동일하게 조회
        val schedules = scheduleService.getClubSchedules(clubId, LocalDate.parse(startDateStr), null)

        // when
        val resultActions = mockMvc
            .perform(
            get("/api/v1/schedules/clubs/$clubId")
                .param("startDate", startDateStr)
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("getClubSchedules"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("일정 목록이 조회되었습니다."))
            .andExpect(jsonPath("$.data.length()").value(schedules.size))
            .andExpectScheduleList(schedules)
    }

    @Test
    @DisplayName("일정 목록 조회 - 모임의 참여자가 아닌 경우 예외 처리")
    @WithUserDetails(value = "cjw5@test.com")
    fun trl4() {
        // given
        val clubId = 1L

        // when
        val resultActions = mockMvc
            .perform(get("/api/v1/schedules/clubs/$clubId"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isForbidden)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("getClubSchedules"))
    }

    @Test
    @DisplayName("일정 조회")
    @WithUserDetails(value = "hgd222@test.com")
    fun tr1() {
        // given
        val scheduleId = 1L
        val schedule = scheduleService.getActiveScheduleById(scheduleId)

        // when
        val resultActions = mockMvc
            .perform(get("/api/v1/schedules/$scheduleId"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("getSchedule"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("일정이 조회되었습니다."))
            .andExpectScheduleDetails(schedule)
    }

    @Test
    @DisplayName("일정 조회 - 모임 참여자가 아닌데 모임 조회 시 예외 발생")
    @WithUserDetails(value = "cjw5@test.com")
    fun tr2() {
        // given
        val scheduleId = 1L

        // when
        val resultActions = mockMvc
            .perform(get("/api/v1/schedules/$scheduleId"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isForbidden)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("getSchedule"))
    }

    @Test
    @DisplayName("일정 생성")
    @WithUserDetails(value = "hgd222@test.com")
    fun tc1() {
        // given
        val clubId = 1L

        // when
        val resultActions = mockMvc
            .perform(
            post("/api/v1/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                            "clubId" : $clubId,
                            "title" : "제 4회 걷기 일정",
                            "content" : "공원에서 함께 걷습니다",
                            "spot" : "서울시 서초동",
                            "startDate" : "2025-08-02T10:00:00",
                            "endDate" : "2025-08-02T15:00:00"
                        }
                    """.trimIndent()
                )
            )
            .andDo(print())

        // then
        val schedule = scheduleService.getLatestClubSchedule(clubId)
        resultActions
            .andExpect(status().isCreated)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("createSchedule"))
            .andExpect(jsonPath("$.code").value(201))
            .andExpect(jsonPath("$.message").value("일정이 생성되었습니다."))
            .andExpectScheduleDetails(schedule)
    }

    @Test
    @DisplayName("일정 생성 - 모임 참여자가 아닌 경우 예외 처리")
    @WithUserDetails(value = "cjw5@test.com")
    fun tc2() {
        // given
        val clubId = 1L

        // when
        val resultActions = mockMvc
            .perform(
            post("/api/v1/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                            "clubId" : $clubId,
                            "title" : "제 4회 걷기 일정",
                            "content" : "공원에서 함께 걷습니다",
                            "spot" : "서울시 서초동",
                            "startDate" : "2025-08-02T10:00:00",
                            "endDate" : "2025-08-02T15:00:00"
                        }
                    """.trimIndent()
                )
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isForbidden)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("createSchedule"))
    }

    @Test
    @DisplayName("일정 생성 - 모임장/매니저가 아닌 참여자인 경우 예외 처리")
    @WithUserDetails(value = "lyh3@test.com")
    fun tc3() {
        // given
        val clubId = 1L

        // when
        val resultActions = mockMvc
            .perform(
            post("/api/v1/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                            "clubId" : $clubId,
                            "title" : "제 4회 걷기 일정",
                            "content" : "공원에서 함께 걷습니다",
                            "spot" : "서울시 서초동",
                            "startDate" : "2025-08-02T10:00:00",
                            "endDate" : "2025-08-02T15:00:00"
                        }
                    """.trimIndent()
                )
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isForbidden)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("createSchedule"))
    }

    @Test
    @DisplayName("일정 수정 - 모임장")
    @WithUserDetails(value = "hgd222@test.com")
    fun tu1() {
        // given
        val scheduleId = 4L

        // when
        val resultActions = mockMvc
            .perform(
            put("/api/v1/schedules/$scheduleId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                            "title" : "속초 여행",
                            "content" : "1박 2일 속초 여행",
                            "spot" : "속초",
                            "startDate" : "2025-07-22T10:00:00",
                            "endDate" : "2025-07-24T15:00:00"
                        }
                    """.trimIndent()
                )
            )
            .andDo(print())

        // then
        val schedule = scheduleService.getActiveScheduleById(scheduleId)
        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("modifySchedule"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("일정이 수정되었습니다."))
            .andExpectScheduleDetails(schedule)
    }

    @Test
    @DisplayName("일정 수정 - 모임 매니저")
    @WithUserDetails(value = "chs4s@test.com")
    fun tu2() {
        // given
        val scheduleId = 4L

        // when
        val resultActions = mockMvc
            .perform(
            put("/api/v1/schedules/$scheduleId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                            "title" : "속초 여행",
                            "content" : "1박 2일 속초 여행",
                            "spot" : "속초",
                            "startDate" : "2025-07-22T10:00:00",
                            "endDate" : "2025-07-24T15:00:00"
                        }
                    """.trimIndent()
                )
            )
            .andDo(print())

        // then
        val schedule = scheduleService.getActiveScheduleById(scheduleId)
        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("modifySchedule"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("일정이 수정되었습니다."))
            .andExpectScheduleDetails(schedule)
    }

    @Test
    @DisplayName("일정 수정 - 시작일 종료일 보다 늦은 경우 예외 발생")
    @WithUserDetails(value = "hgd222@test.com")
    fun tu3() {
        // given
        val scheduleId = 6L

        // when
        val resultActions = mockMvc.perform(
            put("/api/v1/schedules/$scheduleId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                            "title" : "속초 여행",
                            "content" : "1박 2일 속초 여행",
                            "spot" : "속초",
                            "startDate" : "2025-07-22T15:00:00",
                            "endDate" : "2025-07-22T10:00:00"
                        }
                    """.trimIndent()
                )
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isBadRequest)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("modifySchedule"))
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("시작일은 종료일보다 이전이어야 합니다."))
    }

    @Test
    @DisplayName("일정 수정 - 모임 일반 참여자인 경우 예외 처리")
    @WithUserDetails(value = "lyh3@test.com")
    fun tu4() {
        // given
        val scheduleId = 4L

        // when
        val resultActions = mockMvc
            .perform(
            put("/api/v1/schedules/$scheduleId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                            "title" : "속초 여행",
                            "content" : "1박 2일 속초 여행",
                            "spot" : "속초",
                            "startDate" : "2025-07-22T10:00:00",
                            "endDate" : "2025-07-24T15:00:00"
                        }
                    """.trimIndent()
                )
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isForbidden)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("modifySchedule"))
    }

    @Test
    @DisplayName("일정 수정 - 모임 참여자가 아닌 경우 예외 처리")
    @WithUserDetails(value = "cjw5@test.com")
    fun tu5() {
        // given
        val scheduleId = 4L

        // when
        val resultActions = mockMvc
            .perform(
            put("/api/v1/schedules/$scheduleId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                            "title" : "속초 여행",
                            "content" : "1박 2일 속초 여행",
                            "spot" : "속초",
                            "startDate" : "2025-07-22T10:00:00",
                            "endDate" : "2025-07-24T15:00:00"
                        }
                    """.trimIndent()
                )
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isForbidden)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("modifySchedule"))
    }

    @Test
    @DisplayName("일정 삭제 - 체크리스트가 없는 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun td1() {
        // given
        val scheduleId = 6L
        val schedule = scheduleService.getActiveScheduleEntityById(scheduleId)
        val clubId = schedule.club.id!!
        val preCnt = scheduleService.countClubSchedules(clubId)

        // when
        val resultActions = mockMvc
            .perform(delete("/api/v1/schedules/$scheduleId"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("deleteSchedule"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("일정이 삭제되었습니다."))

        val afterCnt = scheduleService.countClubSchedules(clubId)
        Assertions.assertThat(afterCnt).isEqualTo(preCnt - 1)
    }

    @Test
    @DisplayName("일정 삭제(비활성화) - 체크리스트가 있는 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun td2() {
        // given
        val scheduleId = 5L
        val schedule = scheduleService.getActiveScheduleEntityById(scheduleId)
        val clubId = schedule.club.id!!
        val preCnt = scheduleService.countClubSchedules(clubId)

        // when
        val resultActions = mockMvc
            .perform(delete("/api/v1/schedules/$scheduleId"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("deleteSchedule"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("일정이 삭제되었습니다."))

        // 일정이 삭제가 아닌 비활성화 되었는지 확인
        val afterCnt = scheduleService.countClubSchedules(clubId)
        Assertions.assertThat(afterCnt).isEqualTo(preCnt)

        val updatedSchedule = scheduleService.getScheduleEntityById(scheduleId)
        Assertions.assertThat(updatedSchedule.isActive).isFalse()

        // 체크리스트가 비활성화 되었는지 확인
        if (updatedSchedule.checkList != null) {
            Assertions.assertThat(updatedSchedule.checkList!!.isActive).isFalse()
        }
    }

    @Test
    @DisplayName("일정 삭제 - 모임장/매니저가 아닌 경우 예외 처리")
    @WithUserDetails(value = "pms4@test.com")
    fun td3() {
        // given
        val scheduleId = 6L

        // when
        val resultActions = mockMvc
            .perform(delete("/api/v1/schedules/$scheduleId"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isForbidden)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("deleteSchedule"))
    }

    @Test
    @DisplayName("일정 삭제 - 모임 참여자가 아닌 경우 예외 처리")
    @WithUserDetails(value = "chs4s@test.com")
    fun td4() {
        // given
        val scheduleId = 6L

        // when
        val resultActions = mockMvc
            .perform(delete("/api/v1/schedules/$scheduleId"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isForbidden)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("deleteSchedule"))
    }

    @Test
    @DisplayName("나의 일정 목록 조회")
    @WithUserDetails(value = "hgd222@test.com")
    fun trm1() {
        // given
        val startDateStr = "2025-07-01"
        val endDateStr = "2025-07-31"
        val schedules = scheduleService.getMySchedules(1L, LocalDate.parse(startDateStr), LocalDate.parse(endDateStr))

        // when
        val resultActions = mockMvc
            .perform(
            get("/api/v1/schedules/me")
                .param("startDate", startDateStr)
                .param("endDate", endDateStr)
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ApiV1ScheduleController::class.java))
            .andExpect(handler().methodName("getMySchedules"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("나의 일정 목록이 조회되었습니다."))
            .andExpect(jsonPath("$.data.length()").value(schedules.size))
            .andExpectMyScheduleList(schedules)
    }


    // ========== 확장 함수 ==========

    /**
     * 응답 본문의 data 필드가 ScheduleDetailDto와 일치하는지 검증하는 확장 함수
     */
    private fun ResultActions.andExpectScheduleDetails(schedule: ScheduleDetailDto): ResultActions {
        // `tu1`, `tu2` 테스트에서 `startDate`와 `endDate`가 초(second) 정보 없이 비교되어야 하므로,
        // 기존 코드와 동일하게 `startsWith`와 `toString()`을 사용합니다.
        val startDatePrefix = schedule.startDate.toString().let { if (it.length > 16) it.substring(0, 16) else it }
        val endDatePrefix = schedule.endDate.toString().let { if (it.length > 16) it.substring(0, 16) else it }

        return andExpect(jsonPath("$.data.id").value(schedule.id))
            .andExpect(jsonPath("$.data.title").value(schedule.title))
            .andExpect(jsonPath("$.data.content").value(schedule.content))
            .andExpect(jsonPath("$.data.startDate").value(startsWith(startDatePrefix)))
            .andExpect(jsonPath("$.data.endDate").value(startsWith(endDatePrefix)))
            .andExpect(jsonPath("$.data.spot").value(schedule.spot))
            .andExpect(jsonPath("$.data.clubId").value(schedule.clubId))
            .andExpect(jsonPath("$.data.checkListId").value(schedule.checkListId))
    }

    /**
     * 응답 본문의 data 필드가 ScheduleDto 리스트와 일치하는지 검증하는 확장 함수
     */
    private fun ResultActions.andExpectScheduleList(schedules: List<ScheduleDto>): ResultActions {
        schedules.forEachIndexed { i, schedule ->
            andExpect(jsonPath("$.data[$i].id").value(schedule.id))
            andExpect(jsonPath("$.data[$i].title").value(schedule.title))
            andExpect(jsonPath("$.data[$i].startDate").value(startsWith(schedule.startDate.toString().substring(0, 16))))
            andExpect(jsonPath("$.data[$i].endDate").value(startsWith(schedule.endDate.toString().substring(0, 16))))
            andExpect(jsonPath("$.data[$i].clubId").value(schedule.clubId))
        }
        return this
    }

    /**
     * 응답 본문의 data 필드가 ScheduleWithClubDto 리스트와 일치하는지 검증하는 확장 함수
     */
    private fun ResultActions.andExpectMyScheduleList(schedules: List<ScheduleWithClubDto>): ResultActions {
        schedules.forEachIndexed { i, schedule ->
            andExpect(jsonPath("$.data[$i].id").value(schedule.id))
            andExpect(jsonPath("$.data[$i].title").value(schedule.title))
            andExpect(jsonPath("$.data[$i].startDate").value(startsWith(schedule.startDate.toString().substring(0, 16))))
            andExpect(jsonPath("$.data[$i].endDate").value(startsWith(schedule.endDate.toString().substring(0, 16))))
            andExpect(jsonPath("$.data[$i].clubId").value(schedule.clubId))
            andExpect(jsonPath("$.data[$i].clubName").value(schedule.clubName))
        }
        return this
    }
}