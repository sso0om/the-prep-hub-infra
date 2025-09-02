package com.back.global.initData

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional

/**
 * 개발 환경의 초기 데이터 설정
 */
@Configuration
@Profile("dev")
class DevInitData {
    @Autowired
    @Lazy
    private lateinit var self: DevInitData

    @Bean
    fun devInitDataApplicationRunner() = ApplicationRunner {
        self.work1()
        self.work2()
        self.work3()
    }

    @Transactional
    fun work1() {
        // 여기에 데이터 삽입 로직 작성
    }


    @Transactional
    fun work2() {
    }

    @Transactional
    fun work3() {
    }
}
