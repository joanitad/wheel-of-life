package com.joanita.wheeloflife.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // 1..10 — how satisfied you currently feel in this area. Lower = more spin weight.
    val satisfaction: Int = 5,
    // index into the colorblind-safe palette
    val colorIndex: Int = 0
)

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("categoryId")]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val title: String,
    // epoch millis at local midnight of the deadline day, or null for no deadline
    val deadline: Long? = null,
    val done: Boolean = false,
    // epoch millis when the task was marked done; drives the satisfaction boost
    val completedAt: Long? = null
)

data class CategoryWithTasks(
    @Embedded val category: Category,
    @Relation(parentColumn = "id", entityColumn = "categoryId")
    val tasks: List<Task>
)

@Dao
interface WheelDao {
    @Transaction
    @Query("SELECT * FROM categories ORDER BY id")
    fun categoriesWithTasks(): Flow<List<CategoryWithTasks>>

    @Insert suspend fun insertCategory(c: Category): Long
    @Update suspend fun updateCategory(c: Category)
    @Delete suspend fun deleteCategory(c: Category)

    @Insert suspend fun insertTask(t: Task): Long
    @Update suspend fun updateTask(t: Task)
    @Delete suspend fun deleteTask(t: Task)
}

@Database(entities = [Category::class, Task::class], version = 2)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): WheelDao

    companion object {
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER")
            }
        }

        @Volatile private var instance: AppDb? = null
        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "wheel.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }
    }
}
