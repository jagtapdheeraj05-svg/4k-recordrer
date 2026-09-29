package com.example.data.repository

import com.example.data.db.VideoProjectDao
import com.example.data.model.VideoProject
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val dao: VideoProjectDao) {
    val allProjects: Flow<List<VideoProject>> = dao.getAllProjects()
    val favoriteProjects: Flow<List<VideoProject>> = dao.getFavoriteProjects()
    val countFlow: Flow<Int> = dao.getProjectCount()

    suspend fun getProjectById(id: Long): VideoProject? = dao.getProjectById(id)

    suspend fun saveProject(project: VideoProject): Long = dao.insertProject(project)

    suspend fun updateProject(project: VideoProject) = dao.updateProject(project)

    suspend fun deleteProject(project: VideoProject) = dao.deleteProject(project)

    suspend fun deleteProjectById(id: Long) = dao.deleteProjectById(id)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = dao.toggleFavorite(id, isFavorite)
}
