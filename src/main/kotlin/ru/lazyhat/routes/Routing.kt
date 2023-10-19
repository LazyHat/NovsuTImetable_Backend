package ru.lazyhat.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Op
import org.koin.ktor.ext.inject
import ru.lazyhat.db.schemas.GroupsService
import ru.lazyhat.db.schemas.LessonsService
import ru.lazyhat.db.schemas.LessonsServiceImpl

fun Application.configureRouting() {
    val groupsService by inject<GroupsService>()
    val lessonsService by inject<LessonsService>()
    routing {
        get {
            call.respondText("Hello World!")
        }
        route("groups") {
            get{
                call.respond(
                    groupsService.selectWhere { Op.TRUE }
                )
            }
            get("{id}") {
                val id = call.parameters["id"]!!.toUInt()
                call.respond(
                    lessonsService.selectWhere { LessonsServiceImpl.Lessons.group eq id }
                )
            }
        }
    }
}
