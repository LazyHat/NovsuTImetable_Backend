package ru.lazyhat.novsu.source.net

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.datetime.DayOfWeek
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.lazyhat.novsu.models.*

internal object Parsing {
    suspend fun parseTimeTable(group: ParsedGroup, client: HttpClient): List<LessonNetwork> {
        val doc = client.get("https://portal.novsu.ru" + group.refToTimetable).bodyAsText().let { Jsoup.parse(it) }
        val body = doc.body()
        val divNovsu = body.getElementsByClass("novsu").first()!!
        val divBody = divNovsu.getElementById("body")!!
        val divRow = divBody.getElementsByClass("row").first()!!
        val divRowEl = divRow.getElementsByClass("row_el").first()!!
        val divCol = divRowEl.getElementsByClass("col").first()!!
        val divColElement = divCol.getElementsByClass("col_element").first()!!
        val divBlockContent = divColElement.getElementsByClass("block_content").first()!!
        val tableScheduleTable = divBlockContent.getElementsByClass("shedultable").first() ?: return listOf()
        val tableRows = tableScheduleTable.getElementsByTag("tr").drop(1)
        val dows = tableRows.filter { it.getElementsByTag("td").count() == 1 }.map {
            it.text().parseDOW() to it.child(0).attr("rowspan").toInt()
        }
        val daysPairs = mutableListOf<Pair<DayOfWeek, List<Element>>>()
        var dowCount = 1
        dows.forEachIndexed { index, it ->
            daysPairs.add(dows[index].first to tableRows.subList(dowCount, dowCount + it.second - 1))
            dowCount += it.second
        }
        val days = daysPairs.map { pair -> pair.second.map { pair.first to it } }.flatten()
        val lessons = days.mapIndexed { index, pair ->
            val tableDatas = pair.second.getElementsByTag("td")
            val dow = pair.first
            val timeExists = tableDatas.count() == 6
            val time = (if (timeExists) tableDatas[0].textNodes() else days.subList(0, index).findLast {
                it.second.getElementsByTag("td").first()!!.hasAttr("rowspan")
            }!!.second.getElementsByTag("td").first()!!.textNodes()).let {
                val startHour =
                    it.first().text().substringBefore(":").trim().filter { it.isDigit() }.takeIf { it.isNotEmpty() }
                        ?.toUByte()
                val durationInHours = it.filter { !it.isBlank }.count().toUByte()
                startHour?.let { it to durationInHours }
            }
            val startIndex = if (timeExists) 1 else 0
            val subgroup =
                tableDatas[startIndex].text().filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toUByte() ?: 0U
            val type = tableDatas[startIndex + 1].getElementsByTag("b").first()?.text()?.parseType() ?: emptyList()
            val title = tableDatas[startIndex + 1].ownText()
            val teacher = tableDatas[startIndex + 2].text()
            val auditorium = tableDatas[startIndex + 3].text().trim().takeIf { it != "." }
            val week = tableDatas[startIndex + 4].text().let {
                weekLessonCodes.forEach { code ->
                    if (it.contains(code.key)) return@let code.toPair()
                }
                null to WeekLesson.All
            }
            val description = tableDatas[startIndex + 4].text().let {
                if (week.first != null) it.replace(week.first!!, "")
                else it
            }
            LessonNetwork(
                title,
                dow,
                week.second,
                subgroup,
                teacher,
                auditorium.orEmpty(),
                type,
                time?.first ?: 0u,
                time?.second ?: 0u,
                description
            )
        }
        return lessons
    }

    suspend fun parseOchnGroupsTable(client: HttpClient): List<ParsedGroup> {
        val doc = client.get("https://portal.novsu.ru/univer/timetable/ochn").bodyAsText().let { Jsoup.parse(it) }
        val body = doc.body()
        val divNovsu = body.getElementsByClass("novsu").first()!!
        val divBody = divNovsu.getElementById("body")!!
        val divRow = divBody.getElementsByClass("row").first()!!
        val divRowEl = divRow.getElementsByClass("row_el").first()!!
        val divCol = divRowEl.getElementsByClass("col").first()!!
        val divColElement = divCol.getElementsByClass("col_element").first()!!
        val divBlockContent = divColElement.getElementsByClass("block_content").first()!!
        val viewTables = divBlockContent.getElementsByClass("viewtable")
        val filteredViewTables = viewTables.filterIndexed { index, emenent -> index % 4 == 2 }
        val groups = filteredViewTables.map {
            it.getElementsByTag("tr").drop(1).first().getElementsByTag("td").mapIndexed { index, element ->
                val grade = Grade.entries[index]
                element.getElementsByTag("a").map {
                    ParsedGroup(grade, it.attr("href"))
                }
            }
        }
        return groups.flatten().flatten()
    }

    fun String.parseType(): List<LessonType> = split("/").mapNotNull { str ->
        typeCodes.forEach {
            if (str.contains(it.key)) return@mapNotNull it.value
        }
        null
    }

    suspend fun parseWeek(client: HttpClient): Week {
        val doc = client.get("https://portal.novsu.ru/study").bodyAsText().let { Jsoup.parse(it) }
        val body = doc.body()
        val divNovsu = body.getElementsByClass("novsu").first()!!
        val divBody = divNovsu.getElementById("body")!!
        val divRow = divBody.getElementsByClass("row").first()!!
        val divRowEl = divRow.getElementsByClass("row_el").first()!!
        val divCol = divRowEl.getElementsByClass("col").first()!!
        val divColElement = divCol.getElementsByClass("col_element").first()!!
        val divBlock3 = divColElement.getElementsByClass("block3").first()!!
        val divBlock_3padding = divBlock3.getElementsByClass("block_3padding").first()!!
        val weekText = divBlock_3padding.getElementsByTag("b").first()!!.text()
        return if (weekText.contains("верхняя"))
            Week.Upper
        else if (weekText.contains("нижняя"))
            Week.Lower
        else
            Week.Unknown
    }
}