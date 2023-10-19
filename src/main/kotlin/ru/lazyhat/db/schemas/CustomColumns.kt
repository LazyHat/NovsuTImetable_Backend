package ru.lazyhat.db.schemas

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.UByteColumnType
import org.jetbrains.exposed.sql.VarCharColumnType
import kotlin.enums.EnumEntries

class UByteEnumerationColumnType<T : Enum<T>>(
    private val enumEntries: EnumEntries<T>,
    private val uByteColumnType: UByteColumnType = UByteColumnType()
) :
    IColumnType by uByteColumnType {
    override fun nonNullValueToString(value: Any): String = uByteColumnType.nonNullValueToString(valueUnwrap(value))

    override fun notNullValueToDB(value: Any): Any = uByteColumnType.notNullValueToDB(valueUnwrap(value))

    override fun valueToDB(value: Any?): Any? = uByteColumnType.valueToDB(value?.let { valueUnwrap(it) })

    override fun valueFromDB(value: Any): Any = uByteColumnType.valueFromDB(value).toInt().let { enumEntries[it] }

    private fun valueUnwrap(value: Any) = (value as Enum<*>).ordinal.toUByte()
}

fun <T : Enum<T>> Table.ubyteEnumeration(name: String, enumEntries: EnumEntries<T>) =
    registerColumn<T>(name, UByteEnumerationColumnType(enumEntries))

class CustomEnumerationListColumn<T : Enum<T>>(
    val length: Int = 255,
    val enumEntries: EnumEntries<T>,
    private val varCharColumnType: VarCharColumnType = VarCharColumnType(length)
) : IColumnType by varCharColumnType {
    override fun nonNullValueToString(value: Any): String =
        varCharColumnType.nonNullValueToString(
            Json.encodeToString(valueUnwrap(value))
        )

    override fun notNullValueToDB(value: Any): Any =
        varCharColumnType.notNullValueToDB(Json.encodeToString(valueUnwrap(value)))

    override fun valueToDB(value: Any?): Any? =
        varCharColumnType.valueToDB(value?.let { valueUnwrap(it) }?.let { Json.encodeToString(it) })

    override fun valueFromDB(value: Any): Any =
        Json.decodeFromString<List<Int>>(varCharColumnType.valueFromDB(value).toString()).map { enumEntries[it] }


    private fun valueUnwrap(value: Any) = (value as List<*>).map { (it as Enum<*>).ordinal }
}

fun <T : Enum<T>> Table.listEnumeration(name: String, length: Int, enumEntries: EnumEntries<T>) =
    registerColumn<List<T>>(name, CustomEnumerationListColumn(length, enumEntries))