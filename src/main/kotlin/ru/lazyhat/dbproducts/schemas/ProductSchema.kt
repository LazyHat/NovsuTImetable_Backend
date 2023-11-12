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
import ru.lazyhat.dbproducts.models.ValueUnit
import ru.lazyhat.utils.eqOrTrue
import ru.lazyhat.utils.ubyteEnumeration

interface ProductsService {
    suspend fun selectAll(): List<Local.Product>
    suspend fun searchByName(name: String): List<Local.Product>
    suspend fun find(productFind: Local.ProductFind): List<Local.Product>
    suspend fun insert(productCreate: Local.ProductCreate): Local.Product
    suspend fun findById(id: ULong): Local.Product?
}

class ProductsServiceImpl(private val database: Database) : ProductsService {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }

    override suspend fun selectAll(): List<Local.Product> = dbQuery {
        ProductEntity.all().map { it.toProduct() }
    }

    override suspend fun searchByName(name: String): List<Local.Product> = dbQuery {
        ProductEntity.find { ProductTable.name regexp name }.map { it.toProduct() }
    }

    override suspend fun find(productFind: Local.ProductFind): List<Local.Product> = dbQuery {
        ProductEntity.find {
            ProductTable.barcode eqOrTrue productFind.barcode and (ProductTable.name eqOrTrue productFind.name) and (ProductTable.valueInPiece eqOrTrue productFind.valueInPiece) and (ProductTable.valueUnit eqOrTrue productFind.valueUnit)

        }.map { it.toProduct() }
    }

    override suspend fun insert(productCreate: Local.ProductCreate): Local.Product = dbQuery {
        ProductEntity.new { applyProductCreate(productCreate) }
    }.toProduct()

    override suspend fun findById(id: ULong): Local.Product? = dbQuery {
        ProductEntity.findById(id)
    }?.toProduct()
}

internal fun ProductEntity.toProduct(): Local.Product = if (barcode != 0UL) Local.Product.Barcode(
    id = id.value,
    barcode = barcode,
    name = name,
    valueInPiece = valueInPiece,
    valueUnit = valueUnit,
    isLatest = isLatest
) else Local.Product.NonBarcode(
    id = id.value,
    name = name,
    valueInPiece = valueInPiece,
    valueUnit = valueUnit,
    isLatest = isLatest
)

internal fun ProductEntity.applyProductCreate(productCreate: Local.ProductCreate) {
    barcode = productCreate.barcode
    name = productCreate.name
    valueInPiece = productCreate.valueInPiece
    valueUnit = productCreate.valueUnit
    isLatest = true
}

class ProductEntity(id: EntityID<ULong>) : Entity<ULong>(id) {
    companion object : EntityClass<ULong, ProductEntity>(ProductTable)

    var barcode by ProductTable.barcode
    var name by ProductTable.name
    var valueInPiece by ProductTable.valueInPiece
    var valueUnit by ProductTable.valueUnit
    var isLatest by ProductTable.isLatest
}

internal object ProductTable : IdTable<ULong>() {
    override val id: Column<EntityID<ULong>> = ulong("id").autoIncrement().uniqueIndex().entityId()
    val barcode = ulong("barcode")
    val name = text("name")
    val valueInPiece = float("value_in_piece")
    val valueUnit = ubyteEnumeration("value_unit", ValueUnit.entries)
    val isLatest = bool("is_latest")
}