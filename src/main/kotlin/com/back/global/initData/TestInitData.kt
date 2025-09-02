package com.back.global.initData

import com.back.domain.checkList.checkList.entity.CheckList
import com.back.domain.checkList.checkList.entity.CheckListItem
import com.back.domain.checkList.checkList.repository.CheckListItemRepository
import com.back.domain.checkList.checkList.repository.CheckListRepository
import com.back.domain.checkList.itemAssign.entity.ItemAssign
import com.back.domain.checkList.itemAssign.repository.ItemAssignRepository
import com.back.domain.club.club.entity.Club
import com.back.domain.club.club.repository.ClubRepository
import com.back.domain.club.clubMember.entity.ClubMember
import com.back.domain.club.clubMember.repository.ClubMemberRepository
import com.back.domain.member.friend.entity.Friend
import com.back.domain.member.friend.entity.FriendStatus
import com.back.domain.member.friend.repository.FriendRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.entity.Member.Companion.createGuest
import com.back.domain.member.member.entity.MemberInfo
import com.back.domain.member.member.repository.MemberInfoRepository
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.schedule.schedule.entity.Schedule
import com.back.domain.schedule.schedule.repository.ScheduleRepository
import com.back.global.enums.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

/**
 * 테스트 환경의 초기 데이터 설정
 */
@Configuration
@Profile("test")
class TestInitData(
    private val memberRepository: MemberRepository,
    private val memberInfoRepository: MemberInfoRepository,
    private val friendRepository: FriendRepository,
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val scheduleRepository: ScheduleRepository,
    private val checkListRepository: CheckListRepository,
    private val checkListItemRepository: CheckListItemRepository,
    private val itemAssignRepository: ItemAssignRepository,
) {
    @Autowired
    @Lazy
    private lateinit var self: TestInitData

    private val members: MutableMap<String, Member> = mutableMapOf()
    private val clubs: MutableMap<String, Club> = mutableMapOf()

    @Bean
    fun testInitDataApplicationRunner(): ApplicationRunner {
        return ApplicationRunner {
            self.initMemberTestData()
            self.initFriendTestData()
            self.initGroupTestData()
            self.initGroupMemberTestData()
            self.initScheduleTestData()
            self.initCheckListTestData()
            self.initCheckListItemTestData()
            self.initItemAssignTestData()
        }
    }

    /**
     * 회원, 회원 정보 초기 데이터 설정
     */
    @Transactional
    fun initMemberTestData() {
        // 회원
        val member1 = createMember("홍길동", "password1", "hgd222@test.com", "안녕하세요. 홍길동입니다.")
        members[member1.nickname] = member1

        val member2 = createMember("김철수", "password2", "chs4s@test.com", "안녕하세요. 김철수입니다.")
        members[member2.nickname] = member2

        val member3 = createMember("이영희", "password3", "lyh3@test.com", "안녕하세요. 이영희입니다.")
        members[member3.nickname] = member3

        val member4 = createMember("최지우", "password4", "cjw5@test.com", "안녕하세요. 최지우입니다.")
        members[member4.nickname] = member4

        val member5 = createMember("박민수", "password5", "pms4@test.com", "안녕하세요. 박민수입니다.")
        members[member5.nickname] = member5

        val member6 = createMember("유나영", "password6", "uny@test.com", "안녕하세요, 유나영입니다.") //가입 신청 테스트용
        members[member6.nickname] = member6

        val member7 = createMember("이채원", "password7", "lcw@test.com", "안녕하세요, 이채원입니다.") //가입 신청 테스트용
        members[member7.nickname] = member7

        val member8 = createMember("호윤호", "password8", "hyh@test.com", "안녕하세요, 호윤호입니다.") //가입 신청 테스트용
        members[member8.nickname] = member8

        // 비회원
        val guest1 = createMember("이덕혜", "password11", null, null)
        members[guest1.nickname] = guest1

        val guest2 = createMember("레베카", "password12", null, null)
        members[guest2.nickname] = guest2

        val guest3 = createEncodedMember("김암호", "password13", null, null)
        members[guest3.nickname] = guest3
    }


    /**
     * 친구 초기 데이터 설정
     */
    @Transactional
    fun initFriendTestData() {
        val requester = members["홍길동"]!!

        // 친구 요청을 보낸 회원
        val responder1 = members["이영희"]!!
        val friend1 = Friend(
            requester,
            requester,
            responder1,
            FriendStatus.PENDING
        )
        friendRepository.save(friend1)

        // 친구 요청을 수락한 회원
        val responder2 = members["최지우"]!!
        val friend2 = Friend(
            requester,
            requester,
            responder2,
            FriendStatus.ACCEPTED
        )
        friendRepository.save(friend2)

        // 친구 요청을 거절한 회원
        val responder3 = members["박민수"]!!
        val friend3 = Friend(
            requester,
            requester,
            responder3,
            FriendStatus.REJECTED
        )
        friendRepository.save(friend3)
    }

    /**
     * 모임 초기 데이터 설정
     */
    @Transactional
    fun initGroupTestData() {
        val leader1 = members["홍길동"]!!

        // 장기 공개 모임 - 모집 중
        val club1 = Club(
            null,
            "산책 모임",
            null,
            ClubCategory.SPORTS,
            "서울",
            25,
            true,
            EventType.LONG_TERM,
            LocalDate.parse("2025-07-05"),
            LocalDate.parse("2026-08-30"),
            null,
            true,
            leader1.id,
            true
        )
        clubRepository.save(club1)
        clubs[club1.name] = club1

        val clubMember1 = ClubMember(leader1, ClubMemberRole.HOST, ClubMemberState.JOINING)
        club1.addClubMember(clubMember1)
        clubMemberRepository.save(clubMember1)

        // 장기 비공개 모임 - 모집 마감
        val club2 = Club(
            null,
            "친구 모임",
            null,
            ClubCategory.TRAVEL,
            "강원도",
            4,
            false,
            EventType.LONG_TERM,
            LocalDate.parse("2025-05-01"),
            LocalDate.parse("2026-12-31"),
            null,
            false,
            leader1.id,
            true
        )
        clubRepository.save(club2)
        clubs[club2.name] = club2

        val clubMember2 = ClubMember(leader1, ClubMemberRole.HOST, ClubMemberState.JOINING)
        club2.addClubMember(clubMember2)
        clubMemberRepository.save(clubMember2)

        // 단기 비공개 모임 - 모집중
        val club3 = Club(
            null,
            "친구 모임2",
            null,
            ClubCategory.TRAVEL,
            "제주도",
            5,
            true,
            EventType.SHORT_TERM,
            LocalDate.parse("2025-07-01"),
            LocalDate.parse("2025-12-31"),
            null,
            false,
            leader1.id,
            true
        )
        clubRepository.save(club3)
        clubs[club3.name] = club3

        val clubMember3 = ClubMember(leader1, ClubMemberRole.HOST, ClubMemberState.JOINING)
        club3.addClubMember(clubMember3)
        clubMemberRepository.save(clubMember3)

        val leader2 = members["최지우"]!!

        // 일회성 공개 모임 - 모집 중
        val club4 = Club(
            null,
            "A도시 러닝 대회",
            null,
            ClubCategory.SPORTS,
            "서울",
            50,
            true,
            EventType.ONE_TIME,
            LocalDate.parse("2025-08-10"),
            LocalDate.parse("2026-08-10"),
            null,
            true,
            leader2.id,
            true
        )
        clubRepository.save(club4)
        clubs[club4.name] = club4

        val clubMember4 = ClubMember(leader2, ClubMemberRole.HOST, ClubMemberState.JOINING)
        club4.addClubMember(clubMember4)
        clubMemberRepository.save(clubMember4)

        // 종료일 지난 모임
        val nClub1 = Club(
            null,
            "독서 모임",
            null,
            ClubCategory.STUDY,
            "부산",
            10,
            true,
            EventType.SHORT_TERM,
            LocalDate.parse("2025-07-12"),
            LocalDate.parse("2026-07-12"),
            "img3",
            false,
            leader2.id,
            true
        )
        clubRepository.save(nClub1)
        clubs[nClub1.name] = nClub1

        val nClubMember1 = ClubMember(leader2, ClubMemberRole.HOST, ClubMemberState.JOINING)
        nClub1.addClubMember(nClubMember1)
        clubMemberRepository.save(nClubMember1)

        // 삭제된 모임
        val nClub2 = Club(
            null,
            "테니스 모임",
            null,
            ClubCategory.SPORTS,
            "충청도 A 테니스장",
            2,
            false,
            EventType.SHORT_TERM,
            LocalDate.parse("2025-07-05"),
            LocalDate.parse("2026-08-11"),
            "img4",
            false,
            leader2.id,
            false
        )
        clubRepository.save(nClub2)
        clubs[nClub2.name] = nClub2

        val nClubMember2 = ClubMember(leader2, ClubMemberRole.HOST, ClubMemberState.JOINING)
        nClub2.addClubMember(nClubMember2)
        clubMemberRepository.save(nClubMember2)
    }

    /**
     * 모임 맴버 헬퍼 dto
     */
    private data class GroupMemberData(
        val clubName: String,
        val memberNickname: String,
        val role: ClubMemberRole
    )

    /**
     * 모임 맴버 초기 데이터 설정
     */
    @Transactional
    fun initGroupMemberTestData() {
        val groupMembers = listOf(
            GroupMemberData("산책 모임", "김철수", ClubMemberRole.MANAGER),
            GroupMemberData("산책 모임", "이영희", ClubMemberRole.PARTICIPANT),
            GroupMemberData("친구 모임", "박민수", ClubMemberRole.PARTICIPANT),
            GroupMemberData("친구 모임", "이영희", ClubMemberRole.PARTICIPANT),
            GroupMemberData("친구 모임2", "이덕혜", ClubMemberRole.PARTICIPANT),
            GroupMemberData("독서 모임", "레베카", ClubMemberRole.PARTICIPANT),
            GroupMemberData("친구 모임2", "김암호", ClubMemberRole.PARTICIPANT) //암호화 테스트용 데이터
        )

        for (gm in groupMembers) {
            val club = clubs[gm.clubName]!!
            val member = members[gm.memberNickname]!!

            val clubMember = ClubMember(
                member,
                gm.role,
                ClubMemberState.JOINING
            )
            clubMember.setClub(club)

            clubMemberRepository.save(clubMember)
        }
    }


    /**
     * 모임 일정 초기 데이터 설정
     */
    @Transactional
    fun initScheduleTestData() {
        // 모임 1의 일정 초기 데이터
        val club1 = clubs["산책 모임"]!!

        for (i in 1..4) {
            val schedule = Schedule(
                "제 ${i}회 걷기 일정",
                "서울에서 함께 산책합니다",
                LocalDateTime.parse("2025-07-05T10:00:00").plusDays((i * 7).toLong()),
                LocalDateTime.parse("2025-07-05T15:00:00").plusDays((i * 7).toLong()),
                "서울시 서초동",
                club1
            )
            scheduleRepository.save(schedule)
        }

        // 모임 2의 일정 초기 데이터
        val club2 = clubs["친구 모임"]!!

        val schedule2 = Schedule(
            "맛집 탐방",
            "시장 맛집 탐방",
            LocalDateTime.parse("2025-05-07T18:00:00"),
            LocalDateTime.parse("2025-05-07T21:30:00"),
            "단양시장",
            club2
        )
        scheduleRepository.save(schedule2)

        val schedule3 = Schedule(
            "강릉 여행",
            "1박 2일 강릉 여행",
            LocalDateTime.parse("2025-07-23T08:10:00"),
            LocalDateTime.parse("2025-07-24T15:00:00"),
            "강릉",
            club2
        )
        scheduleRepository.save(schedule3)

        // 모임 3의 일정 초기 데이터
        val club3 = clubs["친구 모임2"]!!
        val schedule4 = Schedule(
            "제주도 여행",
            "제주도에서 함께 여행해요",
            LocalDateTime.parse("2025-07-01T09:00:00"),
            LocalDateTime.parse("2025-07-05T18:00:00"),
            "제주도",
            club3
        )
        scheduleRepository.save(schedule4)

        // 모임 3의 일정 초기 데이터 - 비활성화된 일정
        val schedule5 = Schedule(
            "제주도 여행 (비활성화)",
            "제주도에서 함께 여행해요",
            LocalDateTime.parse("2025-10-01T09:00:00"),
            LocalDateTime.parse("2025-10-05T18:00:00"),
            "제주도",
            club3
        )
        scheduleRepository.save(schedule5)
        schedule5.deactivate()

        // 모임 4의 일정 초기 데이터
        val club4 = clubs["A도시 러닝 대회"]!!
        val schedule6 = Schedule(
            "A도시 러닝 대회",
            "A도시에서 열리는 러닝 대회에 참여해요",
            LocalDateTime.parse("2025-08-10T07:00:00"),
            LocalDateTime.parse("2025-08-10T12:00:00"),
            "서울 A도시",
            club4
        )
        scheduleRepository.save(schedule6)

        // 종료된 모임 일정
        val nClub1 = clubs["독서 모임"]!!
        val nSchedule1 = Schedule(
            "독서 모임 일정",
            "부산에서 함께 독서해요",
            LocalDateTime.parse("2025-07-12T10:00:00"),
            LocalDateTime.parse("2025-07-12T15:00:00"),
            "부산",
            nClub1
        )
        scheduleRepository.save(nSchedule1)
    }

    /**
     * 모임의 체크리스트 초기 데이터 설정
     */
    @Transactional
    fun initCheckListTestData() {
        val clubNames = listOf("산책 모임", "친구 모임", "친구 모임2", "A도시 러닝 대회")

        for (clubName in clubNames) {
            val club = clubs[clubName] ?: continue

            val clubSchedules = scheduleRepository.findByClubIdOrderByStartDate(club.id!!)

            for (schedule in clubSchedules) {
                if (schedule.title == "강릉 여행") continue  // 체크리스트 없는 일정(테스트용)

                val checkList = CheckList(true, schedule, mutableListOf())
                checkListRepository.save(checkList)
            }
        }
    }

    /**
     * 체크리스트 항목 초기 데이터 설정
     */
    @Transactional
    fun initCheckListItemTestData() {
        val allCheckLists = checkListRepository.findAll()

        for (checkList in allCheckLists) {
            // 각 체크리스트에 3개의 체크리스트 항목 생성
            for (i in 1..3) {
                val item = CheckListItem("체크리스트 항목 $i", CheckListItemCategory.ETC, i, false, mutableListOf())
                item.checkList = checkList
                checkListItemRepository.save(item)
            }
        }
    }

    /**
     * 체크리스트 항목에 모임 맴버를 랜덤으로 할당
     */
    @Transactional
    fun initItemAssignTestData() {
        val allItems = checkListItemRepository.findAll()

        for (item in allItems) {
            val clubId = item.checkList.schedule.club.id ?: throw IllegalStateException("체크리스트 항목의 모임 정보가 없습니다.")

            // 모임의 맴버들만 할당 대상
            val clubMembers = clubMemberRepository.findAllByClubId(clubId)
            if (clubMembers.isEmpty()) {
                continue
            }

            // 1명 또는 2명을 랜덤으로 할당
            val assignCount = Random.nextInt(1, 3).coerceAtMost(clubMembers.size)

            // 중복되지 않도록 할당
            val assignedMembers = mutableSetOf<ClubMember>()
            for (i in 0..<assignCount) {
                // 중복되지 않는 멤버를 랜덤으로 선택
                val assignee = clubMembers.filterNot { it in assignedMembers }.random()
                assignedMembers.add(assignee)

                // 아이템 할당 생성
                val assign = ItemAssign(assignee, false)
                assign.checkListItem = item

                assignee.addItemAssign(assign)
                itemAssignRepository.save(assign)
            }
        }
    }

    /**
     * 회원 생성 메서드
     */
    private fun createMember(nickname: String, password: String?, email: String?, bio: String?): Member {
        val member = Member(
            null,  // id
            nickname,
            password,
            MemberType.MEMBER,
            UUID.randomUUID().toString().substring(0, 8),
            null,  // memberInfo
            mutableListOf(),  // presets
            mutableSetOf(),  // friendshipsAsMember1
            mutableSetOf(),  // friendshipsAsMember2
            mutableListOf() // clubMembers
        )
        memberRepository.save(member)

        if (email == null) return member

        val info = MemberInfo(
            null,  // id
            email,
            bio,
            "",  // profileImageUrl
            null,  //apiKey
            member // Member 객체
        )

        memberInfoRepository.save(info)

        member.setMemberInfo(info)
        return member
    }

    /**
     * 회원 생성 메서드 2 - 비밀번호 암호화 테스트용
     */
    private fun createEncodedMember(nickname: String, password: String?, email: String?, bio: String?): Member {
        val passwordEncoder = BCryptPasswordEncoder()
        val member = createGuest(nickname, passwordEncoder.encode(password), "2344")
        memberRepository.save(member)

        if (email == null) return member

        val info = MemberInfo(
            null,  // id
            email,
            bio,
            null,  // profileImageUrl
            null,  // apiKey
            member // _member
        )
        memberInfoRepository.save(info)

        member.setMemberInfo(info)
        return member
    }
}