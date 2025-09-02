package com.back.global.aspect

import com.back.global.rsData.RsData
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class ResponseAspect {
    companion object {
        private const val CUSTOM_AUTH_ERROR_CODE = 499 // Custom status code for specific cases
        private const val HTTP_UNAUTHORIZED = 401
    }

    @Around(
        """
        execution(public com.back.global.rsData.RsData *(..)) &&
        (
            within(@org.springframework.stereotype.Controller *) ||
            within(@org.springframework.web.bind.annotation.RestController *)
        ) &&
        (
            @annotation(org.springframework.web.bind.annotation.GetMapping) ||
            @annotation(org.springframework.web.bind.annotation.PostMapping) ||
            @annotation(org.springframework.web.bind.annotation.PutMapping) ||
            @annotation(org.springframework.web.bind.annotation.DeleteMapping) ||
            @annotation(org.springframework.web.bind.annotation.RequestMapping)
        )
        """
    )
    fun handleResponse(joinPoint: ProceedingJoinPoint): Any? {
        val proceed = joinPoint.proceed()

        if (proceed !is RsData<*>) {
            return proceed
        }

        val rsData = proceed

        try {
            val attributes = RequestContextHolder.currentRequestAttributes() as? ServletRequestAttributes
            val response = attributes?.response

            if (response != null) {
                if (rsData.code == CUSTOM_AUTH_ERROR_CODE) {
                    response.status = HTTP_UNAUTHORIZED // 인증 에러를 401로 변환
                } else {
                    response.status = rsData.code
                }
            }
        } catch (e: IllegalStateException) {
            // RequestContextHolder가 현재 스레드에 바인딩되지 않은 경우
            // 예를 들어, 비동기 작업에서 호출된 경우
            // 이 경우에는 아무런 처리를 하지 않고 proceed를 그대로 반환합니다.
            // 로그를 남기거나 다른 처리를 할 수도 있습니다.
        }

        return proceed
    }
}
