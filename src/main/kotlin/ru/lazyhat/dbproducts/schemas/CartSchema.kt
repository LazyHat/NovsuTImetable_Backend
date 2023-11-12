package ru.lazyhat.dbproducts.schemas

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ru.lazyhat.dbproducts.models.Local
import ru.lazyhat.utils.now

interface CartsService {
    suspend fun insert(create: Local.CartCreate): Local.Cart
    suspend fun selectAll(): List<Local.Cart>
    suspend fun findById(id: ULong): Local.Cart?
}

internal class CartsServiceImpl(private val database: Database) : CartsService {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }

    override suspend fun insert(create: Local.CartCreate): Local.Cart = dbQuery {
        CartEntity.new {
            applyCartCreate(create)
        }.toCart()
    }

    override suspend fun selectAll(): List<Local.Cart> = dbQuery {
        CartEntity.all().map { it.toCart() }
    }

    override suspend fun findById(id: ULong): Local.Cart? = dbQuery {
        CartEntity.findById(id)?.toCart()
    }
}

internal fun CartEntity.applyCartCreate(create: Local.CartCreate) {
    shopId = create.shopId
}

internal fun CartEntity.toCart() = Local.Cart(
    id = id.value,
    shopId = shopId,
    buyDate = buyDate,
)

internal class CartEntity(id: EntityID<ULong>) : Entity<ULong>(id) {
    companion object : EntityClass<ULong, CartEntity>(CartTable)

    var shopId by CartTable.shopId
    var buyDate by CartTable.buyDate
}

internal object CartTable : IdTable<ULong>() {
    override val id: Column<EntityID<ULong>> = ulong("id").autoIncrement().uniqueIndex().entityId()
    val shopId = ulong("shop_id")
    val buyDate = datetime("buy_date").clientDefault { LocalDateTime.now() }
}