package com.lifemanager.app.data.repository

import com.lifemanager.app.core.database.dao.GoalDao
import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 目标仓库实现
 */
@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {

    override fun getActiveGoals(): Flow<List<GoalEntity>> {
        return goalDao.getActiveGoals()
    }

    override fun getAllGoals(): Flow<List<GoalEntity>> {
        return goalDao.getAllGoals()
    }

    override fun getGoalsByType(type: String): Flow<List<GoalEntity>> {
        return goalDao.getGoalsByType(type)
    }

    override fun getGoalsByCategory(category: String): Flow<List<GoalEntity>> {
        return goalDao.getGoalsByCategory(category)
    }

    override suspend fun getGoalById(id: Long): GoalEntity? {
        return goalDao.getGoalById(id)
    }

    override suspend fun insert(goal: GoalEntity): Long {
        return goalDao.insert(goal)
    }

    override suspend fun update(goal: GoalEntity) {
        goalDao.update(goal)
    }

    override suspend fun updateProgress(id: Long, value: Double) {
        goalDao.updateProgress(id, value)
    }

    override suspend fun updateStatus(id: Long, status: String) {
        goalDao.updateStatus(id, status)
    }

    override suspend fun delete(id: Long) {
        goalDao.delete(id)
    }

    override suspend fun countActiveGoals(): Int {
        return goalDao.countActiveGoals()
    }
}
