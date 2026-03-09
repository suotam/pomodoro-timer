package asana

import data.ProjectRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class AsanaClient(private val token: String) {

    private val BASE = "https://app.asana.com/api/1.0"

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.NONE
        }
    }

    private fun HttpRequestBuilder.auth() {
        header("Authorization", "Bearer $token")
    }

    suspend fun getWorkspaces(): List<AsanaWorkspace> {
        return http.get("$BASE/workspaces") { auth() }
            .body<AsanaResponse<List<AsanaWorkspace>>>()
            .data
    }

    suspend fun getProjects(workspaceGid: String): List<AsanaProject> {
        return http.get("$BASE/projects") {
            auth()
            parameter("workspace", workspaceGid)
            parameter("opt_fields", "gid,name,color")
            parameter("limit", 100)
        }.body<AsanaResponse<List<AsanaProject>>>().data
    }

    suspend fun getTasksForProject(projectGid: String): List<AsanaTask> {
        return http.get("$BASE/tasks") {
            auth()
            parameter("project", projectGid)
            parameter("opt_fields", "gid,name,completed,assignee_status")
            parameter("completed_since", "now") // jen nedokončené tasky
            parameter("limit", 100)
        }.body<AsanaResponse<List<AsanaTask>>>().data
    }

    fun close() = http.close()
}

/** Synchronizuje Asana projekty a jejich tasky do lokální DB */
suspend fun syncFromAsana(token: String, workspaceGid: String): Result<Int> {
    return runCatching {
        val client = AsanaClient(token)
        val projects = client.getProjects(workspaceGid)
        var taskCount = 0

        val colorMap = mapOf(
            "dark-pink" to "#E91E63", "dark-green" to "#388E3C",
            "dark-blue" to "#1565C0", "dark-red" to "#C62828",
            "dark-teal" to "#00695C", "dark-brown" to "#4E342E",
            "dark-orange" to "#E65100", "dark-purple" to "#6A1B9A",
            "light-pink" to "#F48FB1", "light-green" to "#A5D6A7",
            "light-blue" to "#90CAF9", "light-red" to "#EF9A9A",
            "light-teal" to "#80CBC4", "light-yellow" to "#FFF59D",
            "light-orange" to "#FFCC80", "light-purple" to "#CE93D8",
            "none" to "#4CAF50"
        )

        for (asanaProject in projects) {
            val color = colorMap[asanaProject.color] ?: "#4CAF50"
            val localProjectId = ProjectRepository.upsertFromAsana(
                name = asanaProject.name,
                asanaGid = asanaProject.gid,
                color = color
            )

            val tasks = client.getTasksForProject(asanaProject.gid)
            for (task in tasks) {
                if (task.name.isNotBlank()) {
                    ProjectRepository.upsertTaskFromAsana(
                        projectId = localProjectId,
                        name = task.name,
                        asanaGid = task.gid
                    )
                    taskCount++
                }
            }
        }

        client.close()
        taskCount
    }
}