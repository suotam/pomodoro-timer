package data

data class Project(
    val id: Long,
    val name: String,
    val asanaGid: String?,
    val color: String,
    val createdAt: String
)

data class Task(
    val id: Long,
    val projectId: Long,
    val name: String,
    val asanaGid: String?,
    val createdAt: String
)

object ProjectRepository {

    // ── Projects ──────────────────────────────────────────────────────────────

    fun getAllProjects(): List<Project> {
        Database.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(
                    "SELECT id, name, asana_gid, color, created_at FROM projects ORDER BY name"
                )
                val result = mutableListOf<Project>()
                while (rs.next()) {
                    result += Project(
                        id = rs.getLong("id"),
                        name = rs.getString("name"),
                        asanaGid = rs.getString("asana_gid"),
                        color = rs.getString("color"),
                        createdAt = rs.getString("created_at")
                    )
                }
                return result
            }
        }
    }

    fun insertProject(name: String, asanaGid: String? = null, color: String = "#4CAF50"): Long {
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                "INSERT INTO projects (name, asana_gid, color) VALUES (?, ?, ?)"
            ).use { ps ->
                ps.setString(1, name)
                ps.setString(2, asanaGid)
                ps.setString(3, color)
                ps.executeUpdate()
            }
            return conn.createStatement().executeQuery("SELECT last_insert_rowid()").getLong(1)
        }
    }

    fun updateProject(id: Long, name: String, color: String) {
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                "UPDATE projects SET name = ?, color = ? WHERE id = ?"
            ).use { ps ->
                ps.setString(1, name)
                ps.setString(2, color)
                ps.setLong(3, id)
                ps.executeUpdate()
            }
        }
    }

    fun deleteProject(id: Long) {
        Database.getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM projects WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    fun upsertFromAsana(name: String, asanaGid: String, color: String = "#2196F3"): Long {
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO projects (name, asana_gid, color)
                VALUES (?, ?, ?)
                ON CONFLICT(asana_gid) DO UPDATE SET name = excluded.name
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, name)
                ps.setString(2, asanaGid)
                ps.setString(3, color)
                ps.executeUpdate()
            }
            return conn.prepareStatement(
                "SELECT id FROM projects WHERE asana_gid = ?"
            ).use { ps ->
                ps.setString(1, asanaGid)
                ps.executeQuery().getLong("id")
            }
        }
    }

    // ── Tasks ─────────────────────────────────────────────────────────────────

    fun getTasksForProject(projectId: Long): List<Task> {
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT id, project_id, name, asana_gid, created_at FROM tasks WHERE project_id = ? ORDER BY name"
            ).use { ps ->
                ps.setLong(1, projectId)
                val rs = ps.executeQuery()
                val result = mutableListOf<Task>()
                while (rs.next()) {
                    result += Task(
                        id = rs.getLong("id"),
                        projectId = rs.getLong("project_id"),
                        name = rs.getString("name"),
                        asanaGid = rs.getString("asana_gid"),
                        createdAt = rs.getString("created_at")
                    )
                }
                return result
            }
        }
    }

    fun insertTask(projectId: Long, name: String, asanaGid: String? = null): Long {
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                "INSERT INTO tasks (project_id, name, asana_gid) VALUES (?, ?, ?)"
            ).use { ps ->
                ps.setLong(1, projectId)
                ps.setString(2, name)
                ps.setString(3, asanaGid)
                ps.executeUpdate()
            }
            return conn.createStatement().executeQuery("SELECT last_insert_rowid()").getLong(1)
        }
    }

    fun deleteTask(id: Long) {
        Database.getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM tasks WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    fun upsertTaskFromAsana(projectId: Long, name: String, asanaGid: String): Long {
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO tasks (project_id, name, asana_gid)
                VALUES (?, ?, ?)
                ON CONFLICT(asana_gid) DO UPDATE SET name = excluded.name, project_id = excluded.project_id
                """.trimIndent()
            ).use { ps ->
                ps.setLong(1, projectId)
                ps.setString(2, name)
                ps.setString(3, asanaGid)
                ps.executeUpdate()
            }
            return conn.prepareStatement(
                "SELECT id FROM tasks WHERE asana_gid = ?"
            ).use { ps ->
                ps.setString(1, asanaGid)
                ps.executeQuery().getLong("id")
            }
        }
    }


    fun getSetting(key: String): String {
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT value FROM settings WHERE key = ?").use { ps ->
                ps.setString(1, key)
                val rs = ps.executeQuery()
                return if (rs.next()) rs.getString("value") else ""
            }
        }
    }

    fun setSetting(key: String, value: String) {
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)"
            ).use { ps ->
                ps.setString(1, key)
                ps.setString(2, value)
                ps.executeUpdate()
            }
        }
    }
}