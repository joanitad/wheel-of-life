package com.joanita.wheeloflife

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joanita.wheeloflife.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Spin weighting:
 *  - the slider rating is a *baseline*; completing tasks in an area raises its
 *    effective satisfaction: +0.7 per task done in the last 7 days, +0.3 for
 *    ones done 8-30 days ago, capped at +3 and clamped to 10. Boosts fade as
 *    completions age, so the wheel drifts back if you stop tending an area.
 *  - base need = (11 - effective satisfaction)
 *  - each open task with a deadline adds urgency: overdue +6, due <=3 days +4,
 *    due <=7 days +2, further out +0.5
 */
object Weights {
    fun completionBoost(tasks: List<Task>, now: Long = System.currentTimeMillis()): Double {
        val day = 86_400_000L
        val boost = tasks.filter { it.done && it.completedAt != null }.sumOf {
            val age = (now - it.completedAt!!) / day
            when {
                age <= 7 -> 0.7
                age <= 30 -> 0.3
                else -> 0.0
            }
        }
        return minOf(boost, 3.0)
    }

    fun effectiveSatisfaction(cwt: CategoryWithTasks): Double =
        minOf(10.0, cwt.category.satisfaction + completionBoost(cwt.tasks))

    fun urgencyBoost(task: Task, today: LocalDate = LocalDate.now()): Double {
        if (task.done || task.deadline == null) return 0.0
        val due = java.time.Instant.ofEpochMilli(task.deadline)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val days = ChronoUnit.DAYS.between(today, due)
        return when {
            days < 0 -> 6.0
            days <= 3 -> 4.0
            days <= 7 -> 2.0
            else -> 0.5
        }
    }

    fun weight(cwt: CategoryWithTasks): Double =
        (11.0 - effectiveSatisfaction(cwt)) + cwt.tasks.sumOf { urgencyBoost(it) }
}

class WheelViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()

    val categories = dao.categoriesWithTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCategory(name: String, satisfaction: Int) = viewModelScope.launch {
        val used = categories.value.map { it.category.colorIndex }
        val colorIndex = (0 until 8).minByOrNull { idx -> used.count { it == idx } } ?: 0
        dao.insertCategory(Category(name = name, satisfaction = satisfaction, colorIndex = colorIndex))
    }

    fun updateCategory(c: Category) = viewModelScope.launch { dao.updateCategory(c) }
    fun deleteCategory(c: Category) = viewModelScope.launch { dao.deleteCategory(c) }

    fun addTask(categoryId: Long, title: String, deadline: Long?) = viewModelScope.launch {
        dao.insertTask(Task(categoryId = categoryId, title = title, deadline = deadline))
    }

    fun toggleTask(t: Task) = viewModelScope.launch {
        val nowDone = !t.done
        dao.updateTask(t.copy(done = nowDone, completedAt = if (nowDone) System.currentTimeMillis() else null))
    }
    fun deleteTask(t: Task) = viewModelScope.launch { dao.deleteTask(t) }

    /** Weighted random pick; returns the chosen category id. */
    fun pickWeighted(list: List<CategoryWithTasks>, random: Random = Random): Long {
        val weights = list.map { Weights.weight(it) }
        var r = random.nextDouble() * weights.sum()
        for ((i, w) in weights.withIndex()) {
            r -= w
            if (r <= 0) return list[i].category.id
        }
        return list.last().category.id
    }
}
