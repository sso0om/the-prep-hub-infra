package com.back.global.exception

import com.back.global.rsData.RsData

/**
 * 서비스 예외를 나타내는 클래스
 * 서비스 계층에서 발생하는 오류를 처리하기 위해 사용
 * @param code 오류 코드
 * @param msg 오류 메시지
 */
class ServiceException(
    val code: Int,
    val msg: String,
) : RuntimeException("$code : $msg") { // 1. 주 생성자와 상속 처리

    // 2. 보조 생성자
    constructor(errorCode: ErrorCode) : this(errorCode.status, errorCode.message)

    // 3. 단일 표현식 함수
    fun getRsData(): RsData<Void> = RsData.of(code, msg)
}