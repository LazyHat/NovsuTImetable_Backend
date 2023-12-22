package ru.lazyhat.novsu.source.db.schemas

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.lazyhat.novsu.models.*
import ru.lazyhat.utils.now
import ru.lazyhat.utils.ubyteEnumeration

interface GroupsService {
    suspend fun insert(new: GroupUpsert): String
    suspend fun selectAll(): List<Group>
    suspend fun selectById(id: UInt): Group?
    suspend fun selectByName(name: String): Group?
    suspend fun update(id: UInt, update: GroupUpsert): Boolean
    suspend fun deleteWhere(query: ISqlExpressionBuilder.() -> Op<Boolean>): Int
    suspend fun delete(id: UInt): Boolean
}

class GroupsServiceImpl(private val database: Database) : GroupsService {
    object Groups : IdTable<UInt>() {
        override val id: Column<EntityID<UInt>> = uinteger("id").autoIncrement().entityId()
        val name = varchar("name", 4)
        val institute = ubyteEnumeration("institute", Institute.entries)
        val grade = ubyteEnumeration("grade", Grade.entries)
        val qualifier = ubyteEnumeration("qualifier", GroupQualifier.entries)
        val entryYear = short("entry_year")
        val lastUpdated = datetime("last_updated").clientDefault { LocalDateTime.now() }

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Groups)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }


    override suspend fun insert(new: GroupUpsert): String = dbQuery {
        Groups.insert {
            it.applyGroup(new)
        }[Groups.name]
    }

    override suspend fun selectAll(): List<Group> = dbQuery {
        Groups.selectAll().map(ResultRow::toGroup)
    }

    override suspend fun selectById(id: UInt): Group? = dbQuery {
        Groups.select { Groups.id eq id }.singleOrNull()?.toGroup()
    }

    override suspend fun selectByName(name: String): Group? = dbQuery {
        Groups.select { Groups.name eq name }.singleOrNull()?.toGroup()
    }

    override suspend fun update(id: UInt, update: GroupUpsert): Boolean = dbQuery {
        Groups.update({ Groups.id eq id }) { it.applyGroup(update) }
    } == 1

    override suspend fun deleteWhere(query: ISqlExpressionBuilder.() -> Op<Boolean>): Int = dbQuery {
        Groups.deleteWhere(op = { it.query() })
    }

    override suspend fun delete(id: UInt): Boolean = dbQuery {
        Groups.deleteWhere { name eq name }
    } == 1
}