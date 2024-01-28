package ru.lazyhat.novsu.models

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import ru.lazyhat.novsu.source.db.schemas.GroupsServiceImpl
import ru.lazyhat.novsu.source.db.schemas.LessonsServiceImpl
import ru.lazyhat.utils.now

fun Group.toParsedGroup(): ParsedGroup = ParsedGroup(
    grade,
    "/univer/timetable/ochn/i.1103357/" +
            "?page=EditViewGroup&" +
            "instId=${Institute.entries.find { it == institute }!!.code}&" +
            "name=$name&" +
            "type=${qualifiersCodes.entries.find { it.value == qualifier }!!.key}&" +
            "year=$entryYear"
)

//fun ParsedGroup.toGroup(id: UInt): Group = toGroupUpsert().let {
//    Group(
//        id,
//        it.name,
//        it.institute,
//        it.grade,
//        it.qualifier,
//        it.entryYear,
//        it.lastUpdated
//    )
//}

fun LessonNetwork.toLessonUpsert(groupId: UInt): LessonUpsert = LessonUpsert(
    title,
    dow,
    week,
    groupId,
    subgroup,
    teacher,
    auditorium,
    type,
    startHour,
    durationInHours,
    description
)

fun Lesson.toUpsert() =
    LessonUpsert(
        title,
        dow,
        week,
        group,
        subgroup,
        teacher,
        auditorium,
        type,
        startHour,
        durationInHours,
        description
    )

fun ParsedGroup.toGroupUpsert(): GroupUpsert =
    refToTimetable
        .substringAfter("?")
        .split("&")
        .associate {
            it.substringBefore("=") to it.substringAfter("=")
        }.let {
            GroupUpsert(
                it["name"]!!,
                it["instId"]!!.parseInstitute(),
                grade,
                it["type"]!!.parseGroupQualifiers(),
                it["year"]!!.toShort(),
                LocalDateTime.now()
            )
        }

fun String.parseInstitute(): Institute = Institute.entries.find { it.code == this }!!
fun String.parseGroupQualifiers(): GroupQualifier = qualifiersCodes[this]!!
fun String.parseDOW(): DayOfWeek = dowCodes[this]!!

fun UpdateBuilder<Int>.applyGroup(upsert: GroupUpsert) {
    this[GroupsServiceImpl.Groups.name] = upsert.name
    this[GroupsServiceImpl.Groups.institute] = upsert.institute
    this[GroupsServiceImpl.Groups.grade] = upsert.grade
    this[GroupsServiceImpl.Groups.qualifier] = upsert.qualifier
    this[GroupsServiceImpl.Groups.entryYear] = upsert.entryYear
    this[GroupsServiceImpl.Groups.lastUpdated] = LocalDateTime.now()
}

fun ResultRow.toGroup() = Group(
    id = this[GroupsServiceImpl.Groups.id].value,
    name = this[GroupsServiceImpl.Groups.name],
    institute = this[GroupsServiceImpl.Groups.institute],
    grade = this[GroupsServiceImpl.Groups.grade],
    qualifier = this[GroupsServiceImpl.Groups.qualifier],
    entryYear = this[GroupsServiceImpl.Groups.entryYear],
    lastUpdated = this[GroupsServiceImpl.Groups.lastUpdated]
)

fun UpdateBuilder<Int>.applyLesson(update: LessonUpsert) {
    this[LessonsServiceImpl.Lessons.title] = update.title
    this[LessonsServiceImpl.Lessons.dow] = update.dow
    this[LessonsServiceImpl.Lessons.week] = update.week
    this[LessonsServiceImpl.Lessons.group] = update.group
    this[LessonsServiceImpl.Lessons.subgroup] = update.subgroup
    this[LessonsServiceImpl.Lessons.teacher] = update.teacher
    this[LessonsServiceImpl.Lessons.auditorium] = update.auditorium
    this[LessonsServiceImpl.Lessons.type] = update.type
    this[LessonsServiceImpl.Lessons.startHour] = update.startHour
    this[LessonsServiceImpl.Lessons.durationInHours] = update.durationInHours
    this[LessonsServiceImpl.Lessons.description] = update.description
}

fun ResultRow.toLesson() = Lesson(
    id = this[LessonsServiceImpl.Lessons.id].value,
    title = this[LessonsServiceImpl.Lessons.title],
    dow = this[LessonsServiceImpl.Lessons.dow],
    week = this[LessonsServiceImpl.Lessons.week],
    group = this[LessonsServiceImpl.Lessons.group].value,
    subgroup = this[LessonsServiceImpl.Lessons.subgroup],
    teacher = this[LessonsServiceImpl.Lessons.teacher],
    auditorium = this[LessonsServiceImpl.Lessons.auditorium],
    type = this[LessonsServiceImpl.Lessons.type],
    startHour = this[LessonsServiceImpl.Lessons.startHour],
    durationInHours = this[LessonsServiceImpl.Lessons.durationInHours],
    description = this[LessonsServiceImpl.Lessons.description]
)

fun Group.toGroupUpsert() = GroupUpsert(
    name, institute, grade, qualifier, entryYear, lastUpdated
)

val qualifiersCodes = mapOf(
    "ДО" to GroupQualifier.DO,
    "ДУ" to GroupQualifier.DU,
    "ВО" to GroupQualifier.VO,
    "ВУ" to GroupQualifier.VU,
    "ЗО" to GroupQualifier.ZO,
    "ЗУ" to GroupQualifier.ZU
)

val dowCodes = mapOf(
    "Пн" to DayOfWeek.MONDAY,
    "Вт" to DayOfWeek.TUESDAY,
    "Ср" to DayOfWeek.WEDNESDAY,
    "Чт" to DayOfWeek.THURSDAY,
    "Пт" to DayOfWeek.FRIDAY,
    "Сб" to DayOfWeek.SATURDAY,
    "Вс" to DayOfWeek.SUNDAY
)

val typeCodes = mapOf(
    "пр" to LessonType.Practice,
    "лек" to LessonType.Lecture,
    "лаб" to LessonType.Lab,
    "конс" to LessonType.Consultation
)

val weekLessonCodes = mapOf(
    "по верх. неделе" to WeekLesson.Upper,
    "по нижн. неделе" to WeekLesson.Lower,
    "по верх.неделе" to WeekLesson.Upper,
    "по нижн.неделе" to WeekLesson.Lower,
    "по верхней неделе" to WeekLesson.Upper,
    "по нижней неделе" to WeekLesson.Lower
)