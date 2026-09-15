package com.joanita.wheeloflife.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joanita.wheeloflife.WheelViewModel
import com.joanita.wheeloflife.Weights
import com.joanita.wheeloflife.data.Category
import kotlin.math.roundToInt

@Composable
fun CategoriesScreen(vm: WheelViewModel, onOpenCategory: (Long) -> Unit) {
    val cats by vm.categories.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add area")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("Life areas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Rate how satisfied you feel in each area. Lower ratings get more wheel weight.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cats, key = { it.category.id }) { cwt ->
                    val open = cwt.tasks.count { !it.done }
                    Card(Modifier.fillMaxWidth().clickable { onOpenCategory(cwt.category.id) }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(20.dp).clip(CircleShape)
                                    .background(WheelPalette[cwt.category.colorIndex % WheelPalette.size])
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cwt.category.name, style = MaterialTheme.typography.titleMedium)
                                val eff = Weights.effectiveSatisfaction(cwt)
                                val satText = if (eff > cwt.category.satisfaction)
                                    "Satisfaction ${cwt.category.satisfaction} ↗ ${"%.1f".format(eff)}/10"
                                else "Satisfaction ${cwt.category.satisfaction}/10"
                                Text(
                                    "$satText · $open open task${if (open == 1) "" else "s"} · weight ${"%.1f".format(Weights.weight(cwt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            TextButton(onClick = { editing = cwt.category }) { Text("Edit") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        CategoryDialog(
            title = "New life area",
            initialName = "",
            initialSatisfaction = 5,
            onConfirm = { name, sat -> vm.addCategory(name, sat); showAdd = false },
            onDismiss = { showAdd = false }
        )
    }

    editing?.let { cat ->
        CategoryDialog(
            title = "Edit ${cat.name}",
            initialName = cat.name,
            initialSatisfaction = cat.satisfaction,
            onConfirm = { name, sat ->
                vm.updateCategory(cat.copy(name = name, satisfaction = sat)); editing = null
            },
            onDismiss = { editing = null },
            onDelete = { vm.deleteCategory(cat); editing = null }
        )
    }
}

@Composable
private fun CategoryDialog(
    title: String,
    initialName: String,
    initialSatisfaction: Int,
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var sat by remember { mutableFloatStateOf(initialSatisfaction.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name (e.g. Health, Career, Friends)") },
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Text("Current satisfaction: ${sat.roundToInt()}/10")
                Slider(value = sat, onValueChange = { sat = it }, valueRange = 1f..10f, steps = 8)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim(), sat.roundToInt()) }
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
