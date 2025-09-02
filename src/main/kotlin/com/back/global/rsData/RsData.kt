package com.back.global.rsData

@JvmRecord
data class RsData<T>(
    val code: Int,
    val message: String?,
    val data: T?
) {
    companion object {
        @JvmStatic
        fun <T> of(code: Int, message: String, data: T?): RsData<T> =
            RsData(code, message, data)

        @JvmStatic
        fun <T> of(code: Int, message: String?): RsData<T> =
            RsData(code, message, null)

        // 성공 편의 메소드
        @JvmStatic
        fun <T> successOf(data: T?): RsData<T> =
            of(200, "success", data)

        // 실패 편의 메소드
        @JvmStatic
        fun <T> failOf(data: T?): RsData<T> =
            of(500, "fail", data)
    }
}