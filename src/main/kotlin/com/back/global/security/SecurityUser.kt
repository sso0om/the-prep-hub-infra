package com.back.global.security

import com.back.global.enums.MemberType
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class SecurityUser(
    val id: Long,
    val nickname: String,
    val tag: String,
    val memberType: MemberType,
    password: String,
    authorities: Collection<out GrantedAuthority>
) : User(nickname, password, authorities)