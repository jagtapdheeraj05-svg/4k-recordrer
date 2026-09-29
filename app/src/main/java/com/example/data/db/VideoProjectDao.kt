package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VideoProject
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoProjectDao {
    @Query("SELECT * FROM video_projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<VideoProject>>

    @Query("SELECT * FROM video_projects WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteProjects(): Flow<List<VideoProject>>

    @Query("SELECT * FROM video_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): VideoProject?

    @Query("SELECT * FROM video_projects WHERE is4k = 1 ORDER BY createdAt DESC")
    fun get4KProjects(): Flow<List<VideoProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProject): Long

    @Update
    suspend fun updateProject(project: VideoProject)

    @Delete
    suspend fun deleteProject(project: VideoProject)

    @Query("DELETE FROM video_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("UPDATE video_projects SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM video_projects")
    fun getProjectCount(): Flow<Int>
}
