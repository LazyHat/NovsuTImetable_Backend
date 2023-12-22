package ru.lazyhat.novsu.repo

import kotlinx.datetime.LocalDateTime
import ru.lazyhat.novsu.models.*
import ru.lazyhat.novsu.service.WeekService
import ru.lazyhat.novsu.source.db.schemas.GroupsService
import ru.lazyhat.novsu.source.db.schemas.LessonsService
import ru.lazyhat.novsu.source.db.schemas.LessonsServiceImpl
import ru.lazyhat.novsu.source.net.NetworkSource
import ru.lazyhat.utils.now

interface NovsuRepository {
    suspend fun getAllGroups(): List<Group>
    suspend fun getLessons(groupId: UInt): List<Lesson>
    suspend fun getGroupByIdAndUpdate(id: UInt): Group?
    suspend fun getWeek(): Week
}

class NovsuRepositoryImpl(
    private val groupsService: GroupsService,
    private val lessonsService: LessonsService,
    private val weekService: WeekService,
    private val networkSource: NetworkSource
) : NovsuRepository {
    override suspend fun getAllGroups(): List<Group> = groupsService.selectAll()

    override suspend fun getLessons(groupId: UInt): List<Lesson> =
        lessonsService.selectByGroup(groupId).takeIf { it.isNotEmpty() } ?: groupsService.selectById(groupId)?.let {
            it.updateLessons()
            lessonsService.selectByGroup(groupId)
        } ?: listOf()

    override suspend fun getGroupByIdAndUpdate(id: UInt): Group? = groupsService.selectById(id)?.let {
        groupsService.update(id, it.toGroupUpsert().copy(lastUpdated = LocalDateTime.now()))
        it.updateLessons()
        groupsService.selectById(id)
    }

    override suspend fun getWeek(): Week = weekService.week.value
    private suspend fun Group.updateLessons() {
        lessonsService.deleteWhere { LessonsServiceImpl.Lessons.group eq this@updateLessons.id }
        networkSource.getTimetable(this).forEach {
            lessonsService.insert(it.toLessonUpsert(this.id))
        }
    }
}