package asana

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AsanaResponse<T>(
    val data: T
)

@Serializable
data class AsanaWorkspace(
    val gid: String,
    val name: String
)

@Serializable
data class AsanaProject(
    val gid: String,
    val name: String,
    val color: String? = null
)

@Serializable
data class AsanaTask(
    val gid: String,
    val name: String,
    val completed: Boolean = false,
    @SerialName("assignee_status")
    val assigneeStatus: String? = null
)