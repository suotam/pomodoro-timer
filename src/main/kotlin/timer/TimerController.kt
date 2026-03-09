package timer

import data.ProjectRepository
import data.SessionRepository
import data.Task
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import notification.NotificationService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class TimerPhase { IDLE, WORK, BREAK }
enum class TimerState { STOPPED, RUNNING, OVERTIME }

data class TimerUiState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val state: TimerState = TimerState.STOPPED,
    val elapsedSeconds: Long = 0L,
    val targetSeconds: Long = 0L,
    val selectedTask: Task? = null,
    val isOvertime: Boolean = false
)

class TimerController(private val scope: CoroutineScope) {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState

    private var timerJob: Job? = null
    private var sessionStart: LocalDateTime? = null

    var workDurationMinutes: Int = ProjectRepository.getSetting("work_duration_minutes").toIntOrNull() ?: 25
    var breakDurationMinutes: Int = ProjectRepository.getSetting("break_duration_minutes").toIntOrNull() ?: 5

    fun selectTask(task: Task) {
        if (_uiState.value.state == TimerState.RUNNING) return
        _uiState.value = _uiState.value.copy(selectedTask = task)
    }

    fun clearTask() {
        if (_uiState.value.state == TimerState.RUNNING) return
        _uiState.value = _uiState.value.copy(selectedTask = null)
    }

    fun startWork() {
        val task = _uiState.value.selectedTask ?: return
        startTimer(TimerPhase.WORK, workDurationMinutes * 60L)
    }

    fun startBreak() {
        startTimer(TimerPhase.BREAK, breakDurationMinutes * 60L)
    }

    private fun startTimer(phase: TimerPhase, targetSec: Long) {
        timerJob?.cancel()
        sessionStart = LocalDateTime.now()

        _uiState.value = _uiState.value.copy(
            phase = phase,
            state = TimerState.RUNNING,
            elapsedSeconds = 0L,
            targetSeconds = targetSec,
            isOvertime = false
        )

        timerJob = scope.launch {
            while (true) {
                delay(1000L)
                val elapsed = _uiState.value.elapsedSeconds + 1
                val justHitTarget = elapsed == targetSec

                if (justHitTarget) {
                    when (phase) {
                        TimerPhase.WORK -> {
                            val taskName = _uiState.value.selectedTask?.name ?: "Task"
                            NotificationService.notifyWorkDone(taskName, workDurationMinutes)
                        }
                        TimerPhase.BREAK -> {
                            NotificationService.notifyBreakDone(breakDurationMinutes)
                        }
                        else -> {}
                    }
                }

                _uiState.value = _uiState.value.copy(
                    elapsedSeconds = elapsed,
                    state = if (elapsed >= targetSec) TimerState.OVERTIME else TimerState.RUNNING,
                    isOvertime = elapsed >= targetSec
                )
            }
        }
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null

        val state = _uiState.value
        val start = sessionStart ?: return
        val end = LocalDateTime.now()

        if (state.phase != TimerPhase.IDLE && state.elapsedSeconds > 0) {
            val taskId = state.selectedTask?.id ?: return
            SessionRepository.insertSession(
                taskId = taskId,
                type = if (state.phase == TimerPhase.WORK) "work" else "break",
                startedAt = start.format(fmt),
                endedAt = end.format(fmt),
                durationSeconds = state.elapsedSeconds
            )
            if (state.phase == TimerPhase.WORK) {
                val taskName = state.selectedTask?.name ?: "Task"
                NotificationService.notifySessionSaved(taskName, state.elapsedSeconds)
            }
        }

        sessionStart = null
        _uiState.value = _uiState.value.copy(
            phase = TimerPhase.IDLE,
            state = TimerState.STOPPED,
            elapsedSeconds = 0L,
            isOvertime = false
        )
    }

    fun saveSettings(workMin: Int, breakMin: Int) {
        workDurationMinutes = workMin
        breakDurationMinutes = breakMin
        ProjectRepository.setSetting("work_duration_minutes", workMin.toString())
        ProjectRepository.setSetting("break_duration_minutes", breakMin.toString())
    }

    fun formatSeconds(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
        else "%02d:%02d".format(m, s)
    }
}