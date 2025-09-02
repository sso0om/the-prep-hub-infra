package com.back.global.security

import com.back.global.rsData.RsData.Companion.of
import com.back.standard.util.Ut
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.Optional

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val customAuthenticationFilter: CustomAuthenticationFilter,
    private val mockAuthFilterForSpecificApi: Optional<MockAuthFilterForSpecificApi>
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(HttpMethod.GET, "/api/v1/clubs/invitations/**").permitAll()
                auth.requestMatchers("/favicon.ico", "/h2-console/**").permitAll()
                auth.requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/swagger-resources",
                    "/webjars/**"
                ).permitAll()
                auth.requestMatchers(
                    "/api/v1/members/auth/register",
                    "/api/v1/members/auth/login",
                    "/api/v1/members/auth/guest-register",
                    "/api/v1/members/auth/guest-login"
                ).permitAll()
                auth.requestMatchers(
                    "/api/v1/clubs/{clubId:[0-9]+}",
                    "/api/v1/clubs/public"
                ).permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/v1/clubs/invitations/{token}/apply").authenticated()
                auth.anyRequest().authenticated()
            }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.disable() }
            .headers { it.frameOptions { fo -> fo.sameOrigin() } }
            .exceptionHandling { eh ->
                eh.authenticationEntryPoint { _, response, _ ->
                    response.contentType = "application/json;charset=UTF-8"
                    response.status = 401
                    response.writer.write(Ut.json.toString(of<Void>(401, "로그인 후 이용해주세요.")))
                }
                eh.accessDeniedHandler { _, response, _ ->
                    response.contentType = "application/json;charset=UTF-8"
                    response.status = 403
                    response.writer.write(Ut.json.toString(of<Void>(403, "권한이 없습니다.")))
                }
            }

        http.addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        mockAuthFilterForSpecificApi.ifPresent { filter ->
            http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter::class.java)
        }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:3000", "http://localhost:8080", "http://127.0.0.1:3000")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", configuration)
        }
    }
}
