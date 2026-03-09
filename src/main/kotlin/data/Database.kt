package data

import java.sql.Connection
import java.sql.DriverManager

object Database {

    private const val DB_PATH = "focusflow.db"

    fun getConnection(): Connection {
        val conn = DriverManager.getConnection("jdbc:sqlite:$DB_PATH")
        conn.autoCommit = true
        return conn
    }

    fun initialize() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS projects (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        asana_gid TEXT UNIQUE,
                        color TEXT DEFAULT '#4CAF50',
                        created_at TEXT NOT NULL DEFAULT (datetime('now'))
                    )
                """.trimIndent())

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        project_id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        asana_gid TEXT UNIQUE,
                        created_at TEXT NOT NULL DEFAULT (datetime('now')),
                        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        task_id INTEGER NOT NULL,
                        type TEXT NOT NULL CHECK(type IN ('work', 'break')),
                        started_at TEXT NOT NULL,
                        ended_at TEXT NOT NULL,
                        duration_seconds INTEGER NOT NULL,
                        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS settings (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                """.trimIndent())

                stmt.executeUpdate("""
                    INSERT OR IGNORE INTO settings (key, value) VALUES
                        ('work_duration_minutes', '25'),
                        ('break_duration_minutes', '5'),
                        ('asana_token', ''),
                        ('asana_workspace_gid', '')
                """.trimIndent())
            }
        }
    }
}