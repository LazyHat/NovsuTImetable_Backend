package ru.lazyhat

import io.ktor.server.application.*
import org.koin.core.context.startKoin
import ru.lazyhat.parsing.configureParsing
import ru.lazyhat.plugins.*
import ru.lazyhat.routes.configureRouting

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    startKoin {
        modules(configureDatabaseModule())
    }
    configureSerialization()
    configureTemplating()
    configureMonitoring()
    configureSecurity()
    configureRouting()
    configureParsing()
}
