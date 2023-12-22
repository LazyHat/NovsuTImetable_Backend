package ru.lazyhat.novsu.source.net

import io.ktor.client.*
import ru.lazyhat.novsu.models.*

interface NetworkSource {
    suspend fun getTimetable(group: Group): List<LessonNetwork>
    suspend fun getOchnGroupsTable(): List<GroupNetwork>
    suspend fun getWeek(): Week
}

class NetworkSourceImpl(private val client: HttpClient) : NetworkSource {
    override suspend fun getTimetable(group: Group): List<LessonNetwork> =
        Parsing.parseTimeTable(group.toParsedGroup(), client)

    override suspend fun getOchnGroupsTable(): List<GroupNetwork> =
        Parsing.parseOchnGroupsTable(client).map { it.toGroupUpsert() }

    override suspend fun getWeek(): Week = Parsing.parseWeek(client)
}