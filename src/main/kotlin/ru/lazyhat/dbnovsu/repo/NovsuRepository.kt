package ru.lazyhat.dbnovsu.repo

import kotlinx.datetime.LocalDateTime
import ru.lazyhat.dbnovsu.models.Group
import ru.lazyhat.dbnovsu.models.Lesson
import ru.lazyhat.dbnovsu.models.toGroupUpsert
import ru.lazyhat.dbnovsu.schemas.GroupsService
import ru.lazyhat.dbnovsu.schemas.LessonsService
import ru.lazyhat.dbnovsu.schemas.LessonsServiceImpl
import ru.lazyhat.parsing.parseTimeTable
import ru.lazyhat.utils.now

interface NovsuRepository {
    suspend fun getAllGroups(): List<Group>
    suspend fun getLessons(groupId: UInt): List<Lesson>
    suspend fun getGroupByIdAndUpdate(id: UInt): Group?
}

class NovsuRepositoryImpl(
    private val groupsService: GroupsService,
    private val lessonsService: LessonsService
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

    private suspend fun Group.updateLessons() {
        lessonsService.deleteWhere { LessonsServiceImpl.Lessons.group eq this@updateLessons.id }
        parseTimeTable().forEach {
            lessonsService.insert(it)
        }
    }
}