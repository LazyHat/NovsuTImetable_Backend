package ru.lazyhat.novsu.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import ru.lazyhat.novsu.models.Week
import ru.lazyhat.novsu.source.net.NetworkSource
import ru.lazyhat.utils.now
import java.io.File
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private fun logWeekService(str: String) = println("WeekService: $str")

interface WeekService {
    val week: StateFlow<Week>
}

class WeekServiceImpl(private val networkSource: NetworkSource) : WeekService {
    private companion object {
        const val WEEK_FILENAME = "current_week.txt"
        val FILE_CHARSET = Charsets.UTF_8
    }

    private var _week = MutableStateFlow(Week.Unknown)

    override val week = _week.asStateFlow()

    init {
        logWeekService("Started")
        CoroutineScope(Dispatchers.IO).launch {
            updateWeek()
            while (LocalDateTime.now().date.dayOfWeek != DayOfWeek.MONDAY) {
                delay(6.hours)
            }
            while (true) {
                updateWeek()
                delay(7.days)
            }
        }
    }

    private suspend fun updateWeek() {
        logWeekService("updating week...")
        logWeekService("search in net")
        val netWeek = networkSource.getWeek()
        if (netWeek != Week.Unknown) {
            logWeekService("NetworkWeek founded")
            _week.value = netWeek
        } else {
            logWeekService("NetworkWeek Unknown, checking file...")
            _week.value = when (getWeekFromFile()) {
                Week.Upper -> Week.Lower
                Week.Lower -> Week.Upper
                Week.Unknown -> Week.Unknown
            }
            if (week.value != Week.Unknown)
                logWeekService("founded in file")
            else
                logWeekService("Nothing data of current week")
        }
        writeWeekToFile()
    }

    private fun getWeekFromFile(): Week =
        File(WEEK_FILENAME)
            .takeIf { it.exists() && it.canRead() }
            ?.readText(FILE_CHARSET)
            ?.let {
                Week.valueOf(it)
            } ?: Week.Unknown

    private fun writeWeekToFile(): Boolean {
        val file = File(WEEK_FILENAME)
        if (!file.exists())
            file.createNewFile()
        return if (file.canWrite()) {
            file.writeText(week.value.name, FILE_CHARSET)
            true
        } else
            false
    }
}