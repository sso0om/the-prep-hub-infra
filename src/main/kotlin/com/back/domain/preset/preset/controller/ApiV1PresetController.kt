package com.back.domain.preset.preset.controller

import com.back.domain.preset.preset.dto.PresetDto
import com.back.domain.preset.preset.dto.PresetWriteReqDto
import com.back.domain.preset.preset.service.PresetService
import com.back.global.enums.ClubCategory
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * RsData를 ResponseEntity로 변환하는 확장 함수
 * 컨트롤러의 반복적인 코드를 줄여줍니다.
 */
private fun <T> RsData<T>.toResponseEntity(): ResponseEntity<RsData<T>> =
    ResponseEntity
        .status(this.code)
        .body(this)

@RestController
@RequestMapping("/api/v1/presets")
@Tag(name = "ApiV1PresetController", description = "프리셋 API V1 컨트롤러")
class ApiV1PresetController(
    private val presetService: PresetService // 1. 생성자 주입
) {

    @PostMapping
    @Operation(summary = "프리셋 생성")
    fun write(
        @Valid @RequestBody presetWriteReqDto: PresetWriteReqDto
    ): ResponseEntity<RsData<PresetDto>> {
        val rsData = presetService.write(presetWriteReqDto)
        return rsData.toResponseEntity() // 2. 확장 함수 사용
    }

    @GetMapping("/{presetId}")
    @Operation(summary = "프리셋 조회")
    fun getPreset(@PathVariable presetId: Long): ResponseEntity<RsData<PresetDto>> =
        presetService.getPreset(presetId).toResponseEntity() // 3. 단일 표현식 함수

    @GetMapping
    @Operation(summary = "프리셋 목록 조회")
    fun getPresetList(): ResponseEntity<RsData<List<PresetDto>>> =
        presetService.getPresetList().toResponseEntity()

    @GetMapping("/platform")
    @Operation(summary = "특정 모임 카테고리의 프리셋 목록")
    fun getPresetListByCategory(
        @RequestParam category: ClubCategory
    ): ResponseEntity<RsData<List<PresetDto>>> =
        presetService.getPresetsByCategory(category).toResponseEntity()

    @DeleteMapping("/{presetId}")
    @Operation(summary = "프리셋 삭제")
    fun deletePreset(@PathVariable presetId: Long): ResponseEntity<RsData<Void>> =
        presetService.deletePreset(presetId).toResponseEntity()

    @PutMapping("/{presetId}")
    @Operation(summary = "프리셋 수정")
    fun updatePreset(
        @PathVariable presetId: Long,
        @Valid @RequestBody presetWriteReqDto: PresetWriteReqDto
    ): ResponseEntity<RsData<PresetDto>> =
        presetService.updatePreset(presetId, presetWriteReqDto).toResponseEntity()
}