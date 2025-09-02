package com.back.global.globalExceptionHandler

import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import java.util.NoSuchElementException

/**
 * 글로벌 예외 핸들러 클래스
 * 각 예외에 대한 적절한 HTTP 상태 코드와 메시지를 포함한 응답 반환
 * 400: Bad Request
 * 404: Not Found
 * 500: Internal Server Error
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    // ServiceException: 서비스 계층에서 발생하는 커스텀 예외
    @ExceptionHandler(ServiceException::class)
    fun handle(ex: ServiceException): ResponseEntity<RsData<Void>> {
        val rsData = ex.getRsData()
        return ResponseEntity.status(rsData.code).body(rsData)
    }

    // NoSuchElementException: 데이터가 존재하지 않을 때 발생하는 예외
    @ExceptionHandler(NoSuchElementException::class)
    fun handle(ex: NoSuchElementException): ResponseEntity<RsData<Void>> {
        val errorMessage = ex.message ?: "해당 데이터가 존재하지 않습니다."
        return ResponseEntity(
            RsData.of(404, errorMessage),
            HttpStatus.NOT_FOUND
        )
    }

    // ConstraintViolationException: 제약 조건(@NotNull, @Size 등)을 어겼을 때 발생하는 예외
    @ExceptionHandler(ConstraintViolationException::class)
    fun handle(ex: ConstraintViolationException): ResponseEntity<RsData<Void>> {
        // 메시지 형식: <필드명>-<검증어노테이션명>-<검증실패메시지>
        val message = ex.constraintViolations
            .map { violation ->
                val path = violation.propertyPath.toString()
                val field = path.substringAfter('.', path)
                val messageTemplateBits = violation.messageTemplate.split('.')
                val code = messageTemplateBits.getOrNull(messageTemplateBits.size - 2) ?: "Unknown"
                val msg = violation.message
                "$field-$code-$msg"
            }
            .sorted()
            .joinToString("\n")

        return ResponseEntity(
            RsData.of(400, message),
            HttpStatus.BAD_REQUEST
        )
    }

    // MethodArgumentNotValidException: @Valid 유효성 검사 실패 시 발생하는 예외
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(ex: MethodArgumentNotValidException): ResponseEntity<RsData<Void>> {
        // 메시지 형식: <필드명>-<검증어노테이션명>-<검증실패메시지>
        val message = ex.bindingResult
            .allErrors
            .filterIsInstance<FieldError>()
            .map { "${it.field}-${it.code}-${it.defaultMessage}" }
            .sorted()
            .joinToString("\n")

        return ResponseEntity(
            RsData.of(400, message),
            HttpStatus.BAD_REQUEST
        )
    }

    // HttpMessageNotReadableException: 요청 본문이 올바르지 않을 때 발생하는 예외
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(ex: HttpMessageNotReadableException): ResponseEntity<RsData<Void>> =
        ResponseEntity(
            RsData.of(400, "요청 본문이 올바르지 않습니다."),
            HttpStatus.BAD_REQUEST
        )

    // MissingRequestHeaderException: 필수 요청 헤더가 누락되었을 때 발생하는 예외
    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handle(ex: MissingRequestHeaderException): ResponseEntity<RsData<Void>> {
        // 메시지 형식: <헤더명>-NotBlank-<에러메시지>
        val message = "${ex.headerName}-NotBlank-${ex.localizedMessage}"
        return ResponseEntity(
            RsData.of(400, message),
            HttpStatus.BAD_REQUEST
        )
    }

    // MethodArgumentTypeMismatchException: 요청 파라미터의 타입이 일치하지 않을 때 발생하는 예외
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMethodArgumentTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<RsData<Void>> {
        val requiredType = ex.requiredType?.simpleName ?: "알 수 없음"
        val message = "파라미터 '${ex.name}'의 타입이 올바르지 않습니다. 요구되는 타입: $requiredType"
        return ResponseEntity(
            RsData.of(400, message),
            HttpStatus.BAD_REQUEST
        )
    }

    //MissingServletRequestPartException: 요청된 multipart 요청에서 필수 파트가 누락되었을 때 발생하는 예외
    @ExceptionHandler(MissingServletRequestPartException::class)
    fun handle(ex: MissingServletRequestPartException): ResponseEntity<RsData<Void>> {
        val message = "필수 multipart 파트 '${ex.requestPartName}'가 존재하지 않습니다."
        return ResponseEntity(
            RsData.of(400, message),
            HttpStatus.BAD_REQUEST
        )
    }

    // @PreAuthorize 권한 에러
    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDenied(ex: AuthorizationDeniedException): ResponseEntity<RsData<Void>> =
        ResponseEntity(
            RsData.of(403, "권한이 없습니다."),
            HttpStatus.FORBIDDEN
        )

    // AccessDeniedException 에러 핸들러
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<RsData<Void>> =
        ResponseEntity(
            RsData.of(403, "권한이 없습니다."),
            HttpStatus.FORBIDDEN
        )

    // Fallback: 처리되지 않은 모든 예외
    @ExceptionHandler(Exception::class)
    fun handleUnhandled(ex: Exception): ResponseEntity<RsData<Void>> {
        return ResponseEntity(
            RsData.of(500, "서버 내부 오류가 발생했습니다."),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

}