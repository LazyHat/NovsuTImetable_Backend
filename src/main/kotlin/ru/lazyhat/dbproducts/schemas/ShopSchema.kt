package ru.lazyhat.dbproducts.schemas

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ru.lazyhat.dbproducts.models.Address
import ru.lazyhat.dbproducts.models.Local

interface ShopsService {
    suspend fun selectAll(): List<Local.Shop>
    suspend fun insert(shopCreate: Local.ShopCreate): Local.Shop
    suspend fun findById(id: ULong): Local.Shop?
}


class ShopsServiceImpl(private val database: Database) : ShopsService {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }

    override suspend fun selectAll(): List<Local.Shop> = dbQuery {
        ShopEntity.all().map { it.toShop() }
    }

    override suspend fun insert(shopCreate: Local.ShopCreate): Local.Shop = dbQuery {
        ShopEntity.new { applyShopCreate(shopCreate) }.toShop()
    }

    override suspend fun findById(id: ULong): Local.Shop? = dbQuery {
        ShopEntity.findById(id)?.toShop()
    }
}

fun ShopEntity.toShop(): Local.Shop = Local.Shop(
    id = id.value,
    name = name,
    address = Address(
        region = addressRegion,
        town = addressTown,
        street = addressStreet,
        house = addressHouse
    )
)

fun ShopEntity.applyShopCreate(shopCreate: Local.ShopCreate) {
    name = shopCreate.name
    addressRegion = shopCreate.address.region
    addressTown = shopCreate.address.town
    addressStreet = shopCreate.address.street
    addressHouse = shopCreate.address.house
}

class ShopEntity(id: EntityID<ULong>) : Entity<ULong>(id) {
    companion object : EntityClass<ULong, ShopEntity>(ShopTable)

    var name by ShopTable.name
    var addressRegion by ShopTable.addressRegion
    var addressTown by ShopTable.addressTown
    var addressStreet by ShopTable.addressStreet
    var addressHouse by ShopTable.addressHouse
}

internal object ShopTable : IdTable<ULong>() {
    override val id: Column<EntityID<ULong>> = ulong("id").autoIncrement().uniqueIndex().entityId()
    val name = text("name")
    val addressRegion = text("region")
    val addressTown = text("town")
    val addressStreet = text("street")
    val addressHouse = text("house")
}