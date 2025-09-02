package com.back.domain.member.member.entity

import com.back.domain.club.clubMember.entity.ClubMember
import com.back.domain.member.friend.entity.Friend
import com.back.domain.preset.preset.entity.Preset
import com.back.global.enums.MemberType
import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

@Entity
@Table(name = "member")
class Member(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    var id: Long? = null,

    @Column(length = 50, nullable = false)
    var nickname: String,

    var password: String? = null,

    @Enumerated(EnumType.STRING)
    var memberType: MemberType,

    var tag: String? = null,

    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, mappedBy = "member")
    private var memberInfo: MemberInfo? = null, // <- backing property

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, mappedBy = "owner")
    var presets: MutableList<Preset> = mutableListOf(),

    @OneToMany(mappedBy = "member1", cascade = [CascadeType.ALL], orphanRemoval = true)
    var friendshipsAsMember1: MutableSet<Friend> = mutableSetOf(),

    @OneToMany(mappedBy = "member2", cascade = [CascadeType.ALL], orphanRemoval = true)
    var friendshipsAsMember2: MutableSet<Friend> = mutableSetOf(),

    @OneToMany(mappedBy = "member", cascade = [CascadeType.ALL], orphanRemoval = true)
    var clubMembers: MutableList<ClubMember> = mutableListOf()
) {

    //==========================보조 생성자==========================
    constructor(id: Long?, nickname: String, memberType: MemberType, tag: String?) :
            this(id, nickname, null, memberType, tag)

    //==========================컴패니언 객체==========================
    companion object {
        fun createGuest(nickname: String, password: String, tag: String?): Member {
            return Member(
                nickname = nickname,
                password = password,
                memberType = MemberType.GUEST,
                tag = tag
            )
        }

        fun createMember(nickname: String, password: String, tag: String?): Member {
            return Member(
                nickname = nickname,
                password = password,
                memberType = MemberType.MEMBER,
                tag = tag
            )
        }
    }

    //===========================커스텀 메소드=========================
    fun getMemberInfo(): MemberInfo? = memberInfo // 읽기 전용 getter

    fun updateInfo(nickname: String?, tag: String?, password: String?) {
        if (nickname != null) this.nickname = nickname
        if (tag != null) this.tag = tag
        if (password != null) this.password = password
    }

    fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_USER"))

    fun getEmail(): String? = memberInfo?.email

    @JvmName("setMemberInfo")
    fun setMemberInfo(memberInfo: MemberInfo?) {
        this.memberInfo = memberInfo
        memberInfo?.setMember(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Member) return false
        return this.id != null && this.id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)
}
