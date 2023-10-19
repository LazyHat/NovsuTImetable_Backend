package ru.lazyhat.db.schemas

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.lazyhat.db.models.*

interface GroupsService {
    suspend fun insert(new: GroupUpsert): String
    suspend fun selectByName(name: String): Group?
    suspend fun selectWhere(query: ISqlExpressionBuilder.() -> Op<Boolean>): List<Group>
    suspend fun update(name: String, update: GroupUpsert): Boolean
    suspend fun deleteWhere(query: ISqlExpressionBuilder.() -> Op<Boolean>): Int
    suspend fun delete(id: UInt): Boolean
}

class GroupsServiceImpl(database: Database) : GroupsService {
    object Groups : IdTable<UInt>() {
        override val id: Column<EntityID<UInt>> = uinteger("id").autoIncrement().entityId()
        val name = varchar("name", 4)
        val institute = ubyteEnumeration("institute", Institute.entries)
        val grade = ubyteEnumeration("grade", Grade.entries)
        val qualifier = ubyteEnumeration("qualifier", GroupQualifier.entries)
        val entryYear = short("entry_year")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Groups)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }


    override suspend fun insert(new: GroupUpsert): String = dbQuery {
        Groups.insert {
            it.applyGroup(new)
        }[Groups.name]
    }

    override suspend fun selectByName(name: String): Group? = dbQuery {
        Groups.select { Groups.name eq name }.singleOrNull()?.toGroup()
    }

    override suspend fun selectWhere(query: ISqlExpressionBuilder.() -> Op<Boolean>): List<Group> = dbQuery {
        Groups.select(query).map { it.toGroup() }
    }

    override suspend fun update(name: String, update: GroupUpsert): Boolean = dbQuery {
        Groups.update({ Groups.name eq name }) { it.applyGroup(update) }
    } == 1

    override suspend fun deleteWhere(query: ISqlExpressionBuilder.() -> Op<Boolean>): Int = dbQuery {
        Groups.deleteWhere(op = { it.query() })
    }

    override suspend fun delete(id: UInt): Boolean = dbQuery {
        Groups.deleteWhere { name eq name }
    } == 1
}