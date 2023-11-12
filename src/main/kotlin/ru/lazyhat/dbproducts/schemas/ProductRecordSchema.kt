package ru.lazyhat.dbproducts.schemas

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ru.lazyhat.dbproducts.models.Local
import ru.lazyhat.utils.eqOrTrue

interface ProductRecordsService {
    suspend fun insert(productRecordCreate: Local.ProductRecordCreate): Local.ProductRecord
    suspend fun find(productRecordFind: Local.ProductRecordFind): List<Local.ProductRecord>
}

class ProductRecordsServiceImpl(private val database: Database) : ProductRecordsService {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }

    override suspend fun insert(productRecordCreate: Local.ProductRecordCreate): Local.ProductRecord = dbQuery {
        ProductRecordEntity.new {
            applyProductRecordCreate(productRecordCreate)
        }.toProductRecord()
    }

    override suspend fun find(productRecordFind: Local.ProductRecordFind): List<Local.ProductRecord> = dbQuery {
        ProductRecordEntity.find {
            ProductRecordTable.productId eqOrTrue productRecordFind.productId and
                    (ProductRecordTable.cartId eqOrTrue productRecordFind.cartId) and
                    (ProductRecordTable.priceId eqOrTrue productRecordFind.priceId) and
                    (ProductRecordTable.amount eqOrTrue productRecordFind.amount)
        }.map(ProductRecordEntity::toProductRecord)
    }
}

internal fun ProductRecordEntity.toProductRecord() = Local.ProductRecord(
    id = id.value,
    productId = productId,
    cartId = cartId,
    priceId = priceId,
    amount = amount
)

internal fun ProductRecordEntity.applyProductRecordCreate(productRecordCreate: Local.ProductRecordCreate) {
    productId = productRecordCreate.productId
    cartId = productRecordCreate.cartId
    priceId = productRecordCreate.priceId
    amount = productRecordCreate.amount
}


class ProductRecordEntity(id: EntityID<ULong>) : Entity<ULong>(id) {
    companion object : EntityClass<ULong, ProductRecordEntity>(ProductRecordTable)

    var productId by ProductRecordTable.productId
    var cartId by ProductRecordTable.cartId
    var priceId by ProductRecordTable.priceId
    var amount by ProductRecordTable.amount
}

object ProductRecordTable : IdTable<ULong>() {
    override val id: Column<EntityID<ULong>> = ulong("id").autoIncrement().uniqueIndex().entityId()
    val productId = ulong("product_id")
    val cartId = ulong("cart_id")
    val priceId = ulong("price_td")
    val amount = float("amount")
}