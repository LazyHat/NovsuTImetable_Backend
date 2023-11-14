package ru.lazyhat.dbnovsu.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
enum class Institute(val code: String) {
    IEIS("815132"),
    ICEUS("868341"),
    INPO("868342"),
    IBHI("868343"),
    IGUM("868344"),
    IMO("868345"),
    IUR("1786977"),
    IPT("1798800")
}

@Serializable
enum class GroupQualifier { DO, DU, VO, VU, ZO, ZU }

@Serializable
enum class Grade(val number: Int) {
    First(1),
    Second(2),
    Third(3),
    Fourth(4),
    Fifth(5),
    Sixth(6)
}

@Serializable
data class Group(
    val id: UInt,
    val name: String,
    val institute: Institute,
    val grade: Grade,
    val qualifier: GroupQualifier,
    val entryYear: Short,
    val lastUpdated: LocalDateTime
)

@Serializable
data class GroupUpsert(
    val name: String,
    val institute: Institute,
    val grade: Grade,
    val qualifier: GroupQualifier,
    val entryYear: Short,
    val lastUpdated: LocalDateTime
)

data class ParsedGroup(
    val grade: Grade,
    val refToTimetable: String
)