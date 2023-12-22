package ru.lazyhat.plugins

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.lazyhat.dbproducts.repo.ProductsRepository
import ru.lazyhat.dbproducts.repo.ProductsRepositoryImpl
import ru.lazyhat.dbproducts.schemas.*
import ru.lazyhat.novsu.repo.NovsuRepository
import ru.lazyhat.novsu.repo.NovsuRepositoryImpl
import ru.lazyhat.novsu.service.WeekService
import ru.lazyhat.novsu.service.WeekServiceImpl
import ru.lazyhat.novsu.source.db.schemas.GroupsService
import ru.lazyhat.novsu.source.db.schemas.GroupsServiceImpl
import ru.lazyhat.novsu.source.db.schemas.LessonsService
import ru.lazyhat.novsu.source.db.schemas.LessonsServiceImpl
import ru.lazyhat.novsu.source.net.NetworkSource
import ru.lazyhat.novsu.source.net.NetworkSourceImpl

fun Application.configureDatabaseModule(): Module {
    val driver = environment.config.property("storage.driverClassName").getString()
    val novsuURL = environment.config.property("storage.jdbcURLNovsu").getString()
    val productsURL = environment.config.property("storage.jdbcURLProducts").getString()
    val user = environment.config.property("storage.user").getString()
    val pass = environment.config.property("storage.pass").getString()

    return module(createdAtStart = true) {
        single<Database>(qualifier = named("novsu")) {
            Database.connect(
                url = novsuURL,
                driver = driver,
                user = user,
                password = pass
            )
        }
        single<HttpClient> { HttpClient(OkHttp) }
        single<LessonsService> { LessonsServiceImpl(get(named("novsu"))) }
        single<GroupsService> { GroupsServiceImpl(get(named("novsu"))) }
        single<NetworkSource> { NetworkSourceImpl(get()) }
        single<NovsuRepository> { NovsuRepositoryImpl(get(), get(), get(), get()) }

        single<WeekService> { WeekServiceImpl(get()) }


        single<Database>(named("products")) {
            Database.connect(
                url = productsURL,
                driver = driver,
                user = user,
                password = pass
            ).also {
                transaction(it) {
                    SchemaUtils.create(
                        ShopTable,
                        ProductTable,
                        CartTable,
                        PriceTable,
                        ProductRecordTable
                    )
                }
            }
        }

        single<ShopsService> { ShopsServiceImpl(get(named("products"))) }
        single<PricesService> { PricesServiceImpl(get(named("products"))) }
        single<ProductRecordsService> { ProductRecordsServiceImpl(get(named("products"))) }
        single<ProductsService> { ProductsServiceImpl(get(named("products"))) }
        single<CartsService> { CartsServiceImpl(get(named("products"))) }
        single<ProductsRepository> {
            ProductsRepositoryImpl(
                get(),
                get(),
                get(),
                get(),
                get()
            )
        }
    }
}
