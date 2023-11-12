package ru.lazyhat.dbproducts.schemas

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ru.lazyhat.dbproducts.models.Local
import ru.lazyhat.utils.now

interface PricesService {
    suspend fun insert(priceCreate: Local.PriceCreate): Local.Price
    suspend fun all(): List<Local.Price>
    suspend fun findLatest(productId: ULong, shopId: ULong): Local.Price?
    suspend fun findById(id: ULong): Local.Price?
}

class PricesServiceImpl(private val database: Database) : PricesService {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }


    override suspend fun insert(priceCreate: Local.PriceCreate): Local.Price = dbQuery {
        PriceEntity.new { this.applyPriceCreate(priceCreate) }.toPrice()
    }

    override suspend fun all(): List<Local.Price> = dbQuery {
        PriceEntity.all().map { it.toPrice() }
    }

    override suspend fun findLatest(productId: ULong, shopId: ULong): Local.Price? = dbQuery {
        PriceEntity.find { PriceTable.productId eq productId and (PriceTable.shopId eq shopId) }
            .map(PriceEntity::toPrice)
    }.maxByOrNull { it.createdAt }

    override suspend fun findById(id: ULong): Local.Price? = dbQuery {
        PriceEntity.findById(id)?.toPrice()
    }
}

internal fun PriceEntity.toPrice() = Local.Price(
    id = id.value,
    productId = productId,
    shopId = shopId,
    value = value,
    createdAt = createdAt
)

internal fun PriceEntity.applyPriceCreate(priceCreate: Local.PriceCreate) {
    productId = priceCreate.productId
    shopId = priceCreate.shopId
    value = priceCreate.value
}

class PriceEntity(id: EntityID<ULong>) : Entity<ULong>(id) {
    companion object : EntityClass<ULong, PriceEntity>(PriceTable)

    var productId by PriceTable.productId
    var shopId by PriceTable.shopId
    var value by PriceTable.value
    val createdAt by PriceTable.createdAt
}

internal object PriceTable : IdTable<ULong>() {
    override val id: Column<EntityID<ULong>> = ulong("id").autoIncrement().uniqueIndex().entityId()
    val productId = ulong("product_id")
    val shopId = ulong("shop_id")
    val value = float("value")
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
}