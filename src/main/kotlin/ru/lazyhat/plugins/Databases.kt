package ru.lazyhat.plugins

import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module
import ru.lazyhat.db.schemas.GroupsService
import ru.lazyhat.db.schemas.GroupsServiceImpl
import ru.lazyhat.db.schemas.LessonsService
import ru.lazyhat.db.schemas.LessonsServiceImpl

fun configureDatabaseModule() =
    module {
        single<Database> {
            Database.connect(
                url = "jdbc:h2:file:./build/db",
                user = "root",
                driver = "org.h2.Driver",
                password = ""
            )
        }
        single<LessonsService> { LessonsServiceImpl(get()) }
        single<GroupsService> { GroupsServiceImpl(get()) }
    }
