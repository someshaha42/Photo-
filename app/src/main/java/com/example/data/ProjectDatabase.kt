package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.ProjectDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.io.File

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val thumbnailPath: String?,
    val projectJson: String,
    val aspectLabel: String,
    val layerCount: Int,
    val hasVideo: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAtMillis DESC")
    fun getAllProjectsDesc(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class ProjectDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: ProjectDatabase? = null

        fun getInstance(context: Context): ProjectDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ProjectDatabase::class.java,
                    "pixelforge_projects.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class ProjectRepository(
    private val context: Context,
    private val dao: ProjectDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "layerType"
    }

    val projectsFlow: Flow<List<ProjectEntity>> = dao.getAllProjectsDesc()

    suspend fun loadDocument(projectId: String): ProjectDocument? {
        val entity = dao.getProjectById(projectId) ?: return null
        return runCatching { json.decodeFromString<ProjectDocument>(entity.projectJson) }.getOrNull()
    }

    suspend fun saveDocument(
        doc: ProjectDocument,
        thumbnailPath: String?,
        createdAtMillis: Long? = null
    ) {
        val existing = dao.getProjectById(doc.id)
        val now = System.currentTimeMillis()
        val encoded = json.encodeToString(ProjectDocument.serializer(), doc)
        val entity = ProjectEntity(
            id = doc.id,
            title = doc.title.ifBlank { "Untitled Project" },
            thumbnailPath = thumbnailPath ?: existing?.thumbnailPath,
            projectJson = encoded,
            aspectLabel = doc.aspectRatio.label,
            layerCount = doc.layers.size,
            hasVideo = doc.layers.any { it is com.example.model.VideoLayer },
            createdAtMillis = createdAtMillis ?: existing?.createdAtMillis ?: now,
            updatedAtMillis = now
        )
        dao.upsertProject(entity)
    }

    suspend fun deleteProject(projectId: String) {
        val existing = dao.getProjectById(projectId)
        existing?.thumbnailPath?.let { path ->
            runCatching { File(path).delete() }
        }
        val mediaDir = File(context.filesDir, "projects_media/$projectId")
        if (mediaDir.exists()) {
            runCatching { mediaDir.deleteRecursively() }
        }
        dao.deleteProjectById(projectId)
    }
}
