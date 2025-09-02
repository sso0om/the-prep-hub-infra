package com.back.global.aws

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.IOException

@Service
class S3Service(
    private val s3Client: S3Client
) {
    @Value("\${spring.cloud.aws.s3.bucket}")
    private lateinit var bucket: String

    /**
     * S3에 파일 업로드 메서드
     * @param multipartFile 업로드할 파일
     * @param fileName S3에 저장될 파일 이름
     * @return 업로드된 파일의 URL 주소
     * @throws IOException 파일 처리 중 발생할 수 있는 예외
     */
    @Throws(IOException::class)
    fun upload(multipartFile: MultipartFile, fileName: String): String {
        // 1. PutObjectRequest 객체 생성 (빌더 패턴 사용)
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(fileName)
            .contentType(multipartFile.contentType ?: "application/octet-stream")
            .build()

        // 2. S3에 파일 업로드 (InputStream을 직접 사용)
        multipartFile.inputStream.use { inputStream ->
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, multipartFile.size))
        }

        // 3. 업로드된 파일의 URL 주소 반환
        return s3Client.utilities().getUrl { it.bucket(bucket).key(fileName) }.toString()
    }
}
