package com.back.domain.preset.preset.service

import com.back.domain.preset.preset.dto.PresetDto
import com.back.domain.preset.preset.dto.PresetWriteReqDto
import com.back.domain.preset.preset.entity.Preset
import com.back.domain.preset.preset.entity.PresetItem
import com.back.domain.preset.preset.repository.PresetRepository
import com.back.global.enums.ClubCategory
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException

@Service
@Transactional(readOnly = true)
class PresetService(
    private val presetRepository: PresetRepository,
    private val rq: Rq,
    private val objectMapper: ObjectMapper // ObjectMapper를 주입받아 사용
) {
    // lateinit: 프로퍼티가 나중에 초기화될 것을 명시
    private lateinit var presetsByCategory: Map<ClubCategory, List<PresetDto>>

    /**
     * 플랫폼 프리셋 세팅
     */
    @PostConstruct
    fun init() {
        val typeReference = object : TypeReference<Map<String, List<PresetDto>>>() {}
        val tempMap: Map<String, List<PresetDto>> =
            try {
                // 'use' 함수를 사용하여 InputStream을 안전하게 자동 종료
                Thread.currentThread().contextClassLoader.getResourceAsStream("presets/preset-data.json")?.use {
                    objectMapper.readValue(it, typeReference)
                } ?: throw IOException("preset-data.json 파일을 찾을 수 없습니다.")
            } catch (e: IOException) {
                throw ServiceException(500, "플랫폼의 프리셋 불러오기에 실패했습니다.")
            }

        // mapKeys를 사용해 간결하게 Map의 키를 String에서 ClubCategory Enum으로 변환
        presetsByCategory = tempMap.mapKeys { ClubCategory.fromString(it.key) }
    }

    /**
     * 모임 카테고리별 프리셋 목록
     */
    fun getPresetsByCategory(category: ClubCategory): RsData<List<PresetDto>> {
        val presetDtos = presetsByCategory.getOrDefault(category, emptyList())
        return RsData.of(200, "프리셋 목록 조회 성공", presetDtos)
    }

    /**
     * 프리셋 생성
     */
    @Transactional
    fun write(presetWriteReqDto: PresetWriteReqDto): RsData<PresetDto> {
        val member = rq.actor ?: throw ServiceException(404, "멤버를 찾을 수 없습니다")

        // 코틀린의 map 확장 함수를 사용하여 DTO를 엔티티로 변환
        val presetItems = presetWriteReqDto.presetItems.map { req ->
            PresetItem(
                content = req.content,
                category = req.category,
                sequence = req.sequence
            )
        }

        // Lombok의 @Builder 대신 주 생성자를 사용하여 객체 생성
        val newPreset = Preset(
            owner = member,
            name = presetWriteReqDto.name,
            presetItems = presetItems
        )

        val savedPreset = presetRepository.save(newPreset)
        return RsData.of(201, "프리셋 생성 성공", PresetDto(savedPreset))
    }

    /**
     * 내 프리셋 단건 조회
     */
    fun getPreset(presetId: Long): RsData<PresetDto> {
        val member = rq.actor ?: throw ServiceException(404, "멤버를 찾을 수 없습니다")

        // orElse(null)과 엘비스 연산자(?:)를 사용하여 Optional 처리를 간소화
        val preset = presetRepository.findById(presetId).orElse(null)
            ?: return RsData.of(404, "프리셋을 찾을 수 없습니다")

        if (preset.owner.id != member.id) {
            return RsData.of(403, "권한 없는 프리셋")
        }

        return RsData.of(200, "프리셋 조회 성공", PresetDto(preset))
    }

    /**
     * 내 프리셋 목록 조회
     */
    fun getPresetList(): RsData<List<PresetDto>> {
        val member = rq.actor ?: throw ServiceException(404, "멤버를 찾을 수 없습니다")
        val presets = presetRepository.findByOwner(member)

        // map을 사용하여 엔티티 리스트를 DTO 리스트로 변환
        val presetDtos = presets.map { PresetDto(it) }

        return RsData.of(200, "프리셋 목록 조회 성공", presetDtos)
    }

    /**
     * 프리셋 삭제
     */
    @Transactional
    fun deletePreset(presetId: Long): RsData<Void> {
        val member = rq.actor ?: throw ServiceException(404, "멤버를 찾을 수 없습니다")

        val preset = presetRepository.findById(presetId).orElse(null)
            ?: return RsData.of(404, "프리셋을 찾을 수 없습니다")

        if (preset.owner.id != member.id) {
            return RsData.of(403, "권한 없는 프리셋")
        }

        presetRepository.delete(preset)
        return RsData.of(200, "프리셋 삭제 성공")
    }

    /**
     * 프리셋 수정
     */
    @Transactional
    fun updatePreset(presetId: Long, presetWriteReqDto: PresetWriteReqDto): RsData<PresetDto> {
        val member = rq.actor ?: throw ServiceException(404, "멤버를 찾을 수 없습니다")

        val preset = presetRepository.findById(presetId).orElse(null)
            ?: return RsData.of(404, "프리셋을 찾을 수 없습니다")

        if (preset.owner.id != member.id) {
            return RsData.of(403, "권한 없는 프리셋")
        }

        val newPresetItems = presetWriteReqDto.presetItems.map { req ->
            PresetItem(
                content = req.content,
                category = req.category,
                sequence = req.sequence
            )
        }

        preset.updateName(presetWriteReqDto.name)
        preset.updatePresetItems(newPresetItems)

        // JPA의 변경 감지(dirty checking)에 의해 save 호출은 필수는 아니지만, 명시적으로 유지
        val modifiedPreset = presetRepository.save(preset)
        return RsData.of(200, "프리셋 수정 성공", PresetDto(modifiedPreset))
    }
}
