package ru.lazyhat.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.lazyhat.dbnovsu.WeekState
import ru.lazyhat.dbnovsu.WeekStateImpl
import ru.lazyhat.dbnovsu.repo.NovsuRepository
import ru.lazyhat.dbnovsu.repo.NovsuRepositoryImpl
import ru.lazyhat.dbnovsu.schemas.GroupsService
import ru.lazyhat.dbnovsu.schemas.GroupsServiceImpl
import ru.lazyhat.dbnovsu.schemas.LessonsService
import ru.lazyhat.dbnovsu.schemas.LessonsServiceImpl
import ru.lazyhat.dbproducts.repo.ProductsRepository
import ru.lazyhat.dbproducts.repo.ProductsRepositoryImpl
import ru.lazyhat.dbproducts.schemas.*

fun Application.configureDatabaseModule(): Module {
    val driver = environment.config.property("storage.driverClassName").getString()
    val novsuURL = environment.config.property("storage.jdbcURLNovsu").getString()
    val productsURL = environment.config.property("storage.jdbcURLProducts").getString()
    val user = environment.config.property("storage.user").getString()
    val pass = environment.config.property("storage.pass").getString()

    return module {
        this.single<Database>(qualifier = named("novsu")) {
            Database.connect(
                url = novsuURL,
                driver = driver,
                user = user,
                password = pass
            )
        }
        this.single<LessonsService> { LessonsServiceImpl(this.get(named("novsu"))) }
        this.single<GroupsService> { GroupsServiceImpl(this.get(named("novsu"))) }
        this.single<WeekState> { WeekStateImpl() }
        this.single<NovsuRepository> { NovsuRepositoryImpl(get(), get(), get()) }


        this.single<Database>(named("products")) {
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

        this.single<ShopsService> { ShopsServiceImpl(this.get(named("products"))) }
        this.single<PricesService> { PricesServiceImpl(this.get(named("products"))) }
        this.single<ProductRecordsService> { ProductRecordsServiceImpl(this.get(named("products"))) }
        this.single<ProductsService> { ProductsServiceImpl(this.get(named("products"))) }
        this.single<CartsService> { CartsServiceImpl(this.get(named("products"))) }
        this.single<ProductsRepository> {
            ProductsRepositoryImpl(
                this.get(),
                this.get(),
                this.get(),
                this.get(),
                this.get()
            )
        }
    }
}
