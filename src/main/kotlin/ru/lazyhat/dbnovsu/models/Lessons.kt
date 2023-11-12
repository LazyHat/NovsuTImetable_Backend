package ru.lazyhat.dbnovsu.models

import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable

@Serializable
enum class Week {
    Upper,
    Lower,
    All
}

@Serializable
enum class LessonType {
    Consultation,
    Lecture,
    Practice,
    Lab
}

@Serializable
data class Lesson(
    val id: UInt,
    val title: String,
    val dow: DayOfWeek,
    val week: Week,
    val group: UInt,
    val subgroup: UByte,
    val teacher: String,
    val auditorium: String,
    val type: List<LessonType>,
    val startHour: UByte,
    val durationInHours: UByte,
    val description: String
)

@Serializable
data class LessonUpsert(
    val title: String,
    val dow: DayOfWeek,
    val week: Week,
    val group: UInt,
    val subgroup: UByte,
    val teacher: String,
    val auditorium: String,
    val type: List<LessonType>,
    val startHour: UByte,
    val durationInHours: UByte,
    val description: String
)

