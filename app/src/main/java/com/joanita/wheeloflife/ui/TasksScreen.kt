package com.joanita.wheeloflife.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.joanita.wheeloflife.WheelViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(vm: WheelViewModel, categoryId: Long, onBack: () -> Unit) {
    val cats by vm.categories.collectAsState()
    val cwt = cats.firstOrNull { it.category.id == categoryId } ?: return
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cwt.category.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }
    ) { pad ->
        val tasks = cwt.tasks.sortedWith(
            compareBy({ it.done }, { it.deadline ?: Long.MAX_VALUE })
        )
        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("No tasks yet. Tap + to add one.")
            }
        } else {
            LazyColumn(
                Modifier.padding(pad).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = task.done, onCheckedChange = { vm.toggleTask(task) })
                            Column(Modifier.weight(1f)) {
                                Text(
                                    task.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (task.done) TextDecoration.LineThrough else null
                                )
                                task.deadline?.let { millis ->
                                    val due = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault()).toLocalDate()
                                    val overdue = !task.done && due.isBefore(LocalDate.now())
                                    Text(
                                        (if (overdue) "Overdue — " else "Due ") +
                                            due.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (overdue) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            IconButton(onClick = { vm.deleteTask(task) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete task")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddTaskDialog(
            onConfirm = { title, deadline ->
                vm.addTask(categoryId, title, deadline); showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(onConfirm: (String, Long?) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var hasDeadline by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("What needs doing?") }, singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = hasDeadline, onCheckedChange = {
                        hasDeadline = it
                        if (it) showPicker = true
                    })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (hasDeadline && dateState.selectedDateMillis != null)
                            "Due " + Instant.ofEpochMilli(dateState.selectedDateMillis!!)
                                .atZone(ZoneId.of("UTC")).toLocalDate()
                                .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                        else "No deadline"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val deadline = if (hasDeadline) dateState.selectedDateMillis?.let { utc ->
                        // date picker returns UTC midnight; store local midnight of same date
                        Instant.ofEpochMilli(utc).atZone(ZoneId.of("UTC")).toLocalDate()
                            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } else null
                    onConfirm(title.trim(), deadline)
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false; if (dateState.selectedDateMillis == null) hasDeadline = false },
            confirmButton = { TextButton(onClick = { showPicker = false }) { Text("OK") } },
            dismissButton = {
                TextButton(onClick = {
                    showPicker = false; hasDeadline = false
                }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}
