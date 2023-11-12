package ru.lazyhat.parsing

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.application.*
import kotlinx.coroutines.*
import kotlinx.datetime.DayOfWeek
import org.jetbrains.exposed.sql.Op
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.koin.ktor.ext.inject
import ru.lazyhat.dbnovsu.models.*
import ru.lazyhat.dbnovsu.schemas.GroupsService
import ru.lazyhat.dbnovsu.schemas.LessonsService
import ru.lazyhat.dbnovsu.schemas.LessonsServiceImpl
import kotlin.time.Duration.Companion.minutes

val client = HttpClient(OkHttp)

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
val parsingContext = newSingleThreadContext("parsing")

fun Application.configureParsing() {
    val groupService by inject<GroupsService>()
    val lessonsService by inject<LessonsService>()
    val scope = CoroutineScope(parsingContext)
    scope.launch {
        val novsuGroups = parseOchnGroupTable().also { println("${it.count()} groups in novsu") }
        while (true) {
            val databaseGroups =
                groupService.selectAll().also { println("${it.count()} groups in database") }
            if (novsuGroups.count() != databaseGroups.count()) {
                println("are not equal")
                println("deleting all")
                groupService.deleteWhere { Op.TRUE }
                println("inserting from novsu")
                novsuGroups.forEach {
                    groupService.insert(it.toGroupUpsert())
                }
            } else println("are equal, do nothing")
            println("set up updater timetable")
            while (true) {
                databaseGroups.forEach {
                    println("checking group: id: ${it.id}, name: ${it.name}, q:${it.qualifier}, ${it.institute}")
                    val novsuTimetable = it.parseTimeTable()
                    val dbTimeTable = lessonsService.selectByGroup(it.id)
                    if (dbTimeTable.map { it.toUpsert() } == novsuTimetable) {
                        println("check passed")
                    } else {
                        println("check not passed")
                        lessonsService.deleteWhere { LessonsServiceImpl.Lessons.group eq it.id }
                        novsuTimetable.forEach {
                            lessonsService.insert(it)
                        }
                    }
                    delay(4.minutes)
                }
            }
        }
    }
}

fun String.parseType(): List<LessonType> = split("/").mapNotNull { str ->
    typeCodes.forEach {
        if (str.contains(it.key)) return@mapNotNull it.value
    }
    null
}

fun String.parseDOW(): DayOfWeek = dowCodes[this]!!
fun String.parseInstitute(): Institute = Institute.entries.find { it.code == this }!!
fun String.parseGroupQualifiers(): GroupQualifier = qualifiersCodes[this]!!

suspend fun parseOchnGroupTable(): List<ParsedGroup> {
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


suspend fun Group.parseTimeTable(): List<LessonUpsert> {
    val parsed = this.toParsedGroup()
    val doc = client.get("https://portal.novsu.ru" + parsed.refToTimetable).bodyAsText().let { Jsoup.parse(it) }
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
        val subgroup = tableDatas[startIndex].text().filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toUByte() ?: 0U
        val type = tableDatas[startIndex + 1].getElementsByTag("b").first()?.text()?.parseType() ?: emptyList()
        val title = tableDatas[startIndex + 1].ownText()
        val teacher = tableDatas[startIndex + 2].text()
        val auditorium = tableDatas[startIndex + 3].text().trim().takeIf { it != "." }
        val week = tableDatas[startIndex + 4].text().let {
            weekCodes.forEach { code ->
                if (it.contains(code.key)) return@let code.toPair()
            }
            null to Week.All
        }
        val description = tableDatas[startIndex + 4].text().let {
            if (week.first != null) it.replace(week.first!!, "")
            else it
        }
        LessonUpsert(
            title,
            dow,
            week.second,
            this.id,
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