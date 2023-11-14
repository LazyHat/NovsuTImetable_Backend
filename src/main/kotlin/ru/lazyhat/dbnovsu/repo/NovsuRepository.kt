package ru.lazyhat.dbnovsu.repo

import ru.lazyhat.dbnovsu.models.Group
import ru.lazyhat.dbnovsu.models.Lesson
import ru.lazyhat.dbnovsu.schemas.GroupsService
import ru.lazyhat.dbnovsu.schemas.LessonsService
import ru.lazyhat.dbnovsu.schemas.LessonsServiceImpl
import ru.lazyhat.parsing.parseTimeTable

interface NovsuRepository {
    suspend fun getAllGroups(): List<Group>
    suspend fun getLessons(groupId: UInt): List<Lesson>
    suspend fun getGroupById(id: UInt): Group?
}

class NovsuRepositoryImpl(
    private val groupsService: GroupsService,
    private val lessonsService: LessonsService
) : NovsuRepository {
    override suspend fun getAllGroups(): List<Group> = groupsService.selectAll()

    override suspend fun getLessons(groupId: UInt): List<Lesson> =
        lessonsService.selectByGroup(groupId).takeIf { it.isEmpty() } ?: groupsService.selectById(groupId)?.let {
           it.updateLessons()
            lessonsService.selectByGroup(groupId)
        } ?: listOf()

    override suspend fun getGroupById(id: UInt): Group? = groupsService.selectById(id)?.apply {
        updateLessons()
    }

    private suspend fun Group.updateLessons() {
        lessonsService.deleteWhere { LessonsServiceImpl.Lessons.group eq this@updateLessons.id }
        parseTimeTable().forEach {
            lessonsService.insert(it)
        }
    }
}