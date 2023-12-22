package ru.lazyhat.parsing

import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Op
import org.koin.ktor.ext.inject
import ru.lazyhat.novsu.models.toLessonUpsert
import ru.lazyhat.novsu.models.toUpsert
import ru.lazyhat.novsu.source.db.schemas.GroupsService
import ru.lazyhat.novsu.source.db.schemas.LessonsService
import ru.lazyhat.novsu.source.db.schemas.LessonsServiceImpl
import ru.lazyhat.novsu.source.net.NetworkSource
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
val parsingContext = newSingleThreadContext("parsing")
private fun logParsing(string: String) = println("Parsing: $string")

fun Application.configureParsing() {
    val networkSource by inject<NetworkSource>()
    val groupService by inject<GroupsService>()
    val lessonsService by inject<LessonsService>()

    val scope = CoroutineScope(parsingContext)

    scope.launch {
        val novsuGroups = networkSource.getOchnGroupsTable().also { logParsing("${it.count()} groups in novsu") }
        while (true) {
            val databaseGroups =
                groupService.selectAll().also { logParsing("${it.count()} groups in database") }
            if (novsuGroups.count() != databaseGroups.count()) {
                logParsing("are not equal")
                logParsing("deleting all")
                groupService.deleteWhere { Op.TRUE }
                logParsing("inserting from novsu")
                novsuGroups.also { logParsing("novsu groups count: ${it.count()}") }.forEach {
                    groupService.insert(it)
                }
            } else logParsing("are equal, do nothing")
            delay(10.minutes)
        }
    }

    logParsing("set up updater timetable")

    scope.launch {
        while (true) {
            val groups = groupService.selectAll()
            if (groups.isNotEmpty()) {
                groups.forEach { group ->
                    logParsing("checking group: id: ${group.id}, name: ${group.name}, q:${group.qualifier}, ${group.institute}")
                    val novsuTimetable = networkSource.getTimetable(group)
                    val dbTimeTable = lessonsService.selectByGroup(group.id)
                    if (dbTimeTable.map { it.toUpsert() } == novsuTimetable) {
                        logParsing("check passed")
                    } else {
                        logParsing("check not passed")
                        lessonsService.deleteWhere { LessonsServiceImpl.Lessons.group eq group.id }
                        novsuTimetable.forEach {
                            lessonsService.insert(it.toLessonUpsert(group.id))
                        }
                    }
                    logParsing("updated")
                    delay(1.minutes)
                }
                logParsing("All Groups Checked")
            } else
                logParsing("Groups are empty")
            delay(1.minutes)
        }
    }
}