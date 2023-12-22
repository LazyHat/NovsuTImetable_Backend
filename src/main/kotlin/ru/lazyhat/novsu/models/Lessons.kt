package ru.lazyhat.novsu.models

import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable


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
    val week: WeekLesson,
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
    val week: WeekLesson,
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
data class LessonNetwork(
    val title: String,
    val dow: DayOfWeek,
    val week: WeekLesson,
    val subgroup: UByte,
    val teacher: String,
    val auditorium: String,
    val type: List<LessonType>,
    val startHour: UByte,
    val durationInHours: UByte,
    val description: String
)