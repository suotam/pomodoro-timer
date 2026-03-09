package data

data class Session(
    val id: Long,
    val taskId: Long,
    val taskName: String,
    val projectId: Long,
    val projectName: String,
    val type: String,
    val startedAt: String,
    val endedAt: String,
    val durationSeconds: Long
)

data class ExportRow(
    val date: String,
    val projectName: String,
    val taskName: String,
    val type: String,
    val durationSeconds: Long
)

data class DayStats(
    val date: String,       // "YYYY-MM-DD"
    val workSeconds: Long,
    val breakSeconds: Long,
    val sessionCount: Int
)

object SessionRepository {

    fun insertSession(
        taskId: Long,
        type: String,
        startedAt: String,
        endedAt: String,
        durationSeconds: Long
    ): Long {
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                "INSERT INTO sessions (task_id, type, started_at, ended_at, duration_seconds) VALUES (?, ?, ?, ?, ?)"
            ).use { ps ->
                ps.setLong(1, taskId)
                ps.setString(2, type)
                ps.setString(3, startedAt)
                ps.setString(4, endedAt)
                ps.setLong(5, durationSeconds)
                ps.executeUpdate()
            }
            return conn.createStatement().executeQuery("SELECT last_insert_rowid()").getLong(1)
        }
    }

    fun getRecentSessions(limitDays: Int = 7): List<Session> {
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                """
                SELECT s.id, s.task_id, t.name as task_name, p.id as project_id,
                       p.name as project_name, s.type, s.started_at, s.ended_at, s.duration_seconds
                FROM sessions s
                JOIN tasks t ON t.id = s.task_id
                JOIN projects p ON p.id = t.project_id
                WHERE date(s.started_at) >= date('now', '-$limitDays days')
                ORDER BY s.started_at DESC
                """.trimIndent()
            ).use { ps ->
                val rs = ps.executeQuery()
                val result = mutableListOf<Session>()
                while (rs.next()) {
                    result += Session(
                        id = rs.getLong("id"),
                        taskId = rs.getLong("task_id"),
                        taskName = rs.getString("task_name"),
                        projectId = rs.getLong("project_id"),
                        projectName = rs.getString("project_name"),
                        type = rs.getString("type"),
                        startedAt = rs.getString("started_at"),
                        endedAt = rs.getString("ended_at"),
                        durationSeconds = rs.getLong("duration_seconds")
                    )
                }
                return result
            }
        }
    }

    fun getDayStats(limitDays: Int = 14): List<DayStats> {
        Database.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(
                    """
                    SELECT
                        date(started_at) as day,
                        SUM(CASE WHEN type = 'work' THEN duration_seconds ELSE 0 END) as work_sec,
                        SUM(CASE WHEN type = 'break' THEN duration_seconds ELSE 0 END) as break_sec,
                        COUNT(CASE WHEN type = 'work' THEN 1 END) as session_count
                    FROM sessions
                    WHERE date(started_at) >= date('now', '-$limitDays days')
                    GROUP BY date(started_at)
                    ORDER BY day ASC
                    """.trimIndent()
                )
                val result = mutableListOf<DayStats>()
                while (rs.next()) {
                    result += DayStats(
                        date = rs.getString("day"),
                        workSeconds = rs.getLong("work_sec"),
                        breakSeconds = rs.getLong("break_sec"),
                        sessionCount = rs.getInt("session_count")
                    )
                }
                return result
            }
        }
    }

    fun getSessionsForExport(
        projectId: Long? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): List<ExportRow> {
        Database.getConnection().use { conn ->
            val conditions = mutableListOf<String>()
            if (projectId != null) conditions += "p.id = $projectId"
            if (dateFrom != null) conditions += "date(s.started_at) >= '$dateFrom'"
            if (dateTo != null) conditions += "date(s.started_at) <= '$dateTo'"
            val where = if (conditions.isEmpty()) "" else "WHERE " + conditions.joinToString(" AND ")

            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(
                    """
                    SELECT date(s.started_at) as session_date,
                           p.name as project_name, t.name as task_name,
                           s.type, s.duration_seconds
                    FROM sessions s
                    JOIN tasks t ON t.id = s.task_id
                    JOIN projects p ON p.id = t.project_id
                    $where
                    ORDER BY s.started_at
                    """.trimIndent()
                )
                val result = mutableListOf<ExportRow>()
                while (rs.next()) {
                    result += ExportRow(
                        date = rs.getString("session_date"),
                        projectName = rs.getString("project_name"),
                        taskName = rs.getString("task_name"),
                        type = rs.getString("type"),
                        durationSeconds = rs.getLong("duration_seconds")
                    )
                }
                return result
            }
        }
    }

    fun getTotalSecondsForTask(taskId: Long): Long {
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT COALESCE(SUM(duration_seconds), 0) FROM sessions WHERE task_id = ? AND type = 'work'"
            ).use { ps ->
                ps.setLong(1, taskId)
                return ps.executeQuery().getLong(1)
            }
        }
    }

    fun getTotalSecondsForProject(projectId: Long): Long {
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                """
                SELECT COALESCE(SUM(s.duration_seconds), 0)
                FROM sessions s
                JOIN tasks t ON t.id = s.task_id
                WHERE t.project_id = ? AND s.type = 'work'
                """.trimIndent()
            ).use { ps ->
                ps.setLong(1, projectId)
                return ps.executeQuery().getLong(1)
            }
        }
    }

    fun deleteSession(id: Long) {
        Database.getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM sessions WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }
}