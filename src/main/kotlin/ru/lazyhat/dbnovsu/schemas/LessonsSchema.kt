package ru.lazyhat.dbnovsu.schemas

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.DayOfWeek
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.lazyhat.dbnovsu.models.*
import ru.lazyhat.utils.listEnumeration
import ru.lazyhat.utils.ubyteEnumeration

interface LessonsService {
    suspend fun insert(new: LessonUpsert): UInt
    suspend fun selectById(id: UInt): Lesson?
    suspend fun selectByGroup(id: UInt): List<Lesson>
    suspend fun update(id: UInt, update: LessonUpsert): Boolean
    suspend fun deleteWhere(query: ISqlExpressionBuilder.() -> Op<Boolean>): Int
    suspend fun delete(id: UInt): Boolean
}

class LessonsServiceImpl(private val database: Database) : LessonsService {
    object Lessons : IdTable<UInt>() {
        override val id: Column<EntityID<UInt>> = uinteger("id").autoIncrement().entityId()
        val title = text("title")
        val dow = ubyteEnumeration("dow", DayOfWeek.entries)
        val week = ubyteEnumeration("week", Week.entries)
        val group = reference("group", GroupsServiceImpl.Groups)
        val subgroup = ubyte("subgroup")
        val teacher = varchar("teacher", 60)
        val auditorium = varchar("auditorium", 20)
        val type = listEnumeration("type", 7, LessonType.entries)
        val startHour = ubyte("start_hour")
        val durationInHours = ubyte("duration_hours")
        val description = text("description")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Lessons)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }


    override suspend fun insert(new: LessonUpsert): UInt = dbQuery {
        Lessons.insert {
            it.applyLesson(new)
        }[Lessons.id].value
    }

    override suspend fun selectById(id: UInt): Lesson? = dbQuery {
        Lessons.select(Lessons.id eq id).singleOrNull()?.toLesson()
    }

    override suspend fun selectByGroup(id: UInt): List<Lesson> = dbQuery {
        Lessons.select { Lessons.group eq id }.map(ResultRow::toLesson)
    }

    override suspend fun update(id: UInt, update: LessonUpsert): Boolean = dbQuery {
        Lessons.update({ Lessons.id eq id }) { it.applyLesson(update) }
    } == 1

    override suspend fun deleteWhere(query: ISqlExpressionBuilder.() -> Op<Boolean>): Int = dbQuery {
        Lessons.deleteWhere(op = { it.query() })
    }

    override suspend fun delete(id: UInt): Boolean = dbQuery {
        Lessons.deleteWhere { Lessons.id eq id }
    } == 1
}