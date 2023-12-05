package ru.lazyhat.dbnovsu

import ru.lazyhat.dbnovsu.models.Week

interface WeekState {
    fun set(new: Week)
    fun get(): Week
}

class WeekStateImpl : WeekState {
    private var week = Week.All

    override fun set(new: Week) {
        week = new
    }

    override fun get(): Week = week
}