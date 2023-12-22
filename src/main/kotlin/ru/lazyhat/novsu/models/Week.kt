package ru.lazyhat.novsu.models

import kotlinx.serialization.Serializable

@Serializable
enum class WeekLesson {
    Upper,
    Lower,
    All
}

@Serializable
enum class Week{
    Upper,
    Lower,
    Unknown
}