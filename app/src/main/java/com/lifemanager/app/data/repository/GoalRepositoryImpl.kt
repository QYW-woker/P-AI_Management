package com.lifemanager.app.data.repository

import com.lifemanager.app.core.database.dao.GoalDao
import com.lifemanager.app.core.database.dao.GoalRecordDao
import com.lifemanager.app.core.database.entity.GoalEntity
import com.lifemanager.app.core.database.entity.GoalRecordEntity
import com.lifemanager.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 目标仓库实现
 */
@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val goalRecordDao: GoalRecordDao
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

    // ============ 多级目标相关 ============

    override fun getTopLevelGoals(): Flow<List<GoalEntity>> {
        return goalDao.getTopLevelGoals()
    }

    override fun getAllTopLevelGoals(): Flow<List<GoalEntity>> {
        return goalDao.getAllTopLevelGoals()
    }

    override fun getChildGoals(parentId: Long): Flow<List<GoalEntity>> {
        return goalDao.getChildGoals(parentId)
    }

    override suspend fun getChildGoalsSync(parentId: Long): List<GoalEntity> {
        return goalDao.getChildGoalsSync(parentId)
    }

    override suspend fun countChildGoals(parentId: Long): Int {
        return goalDao.countChildGoals(parentId)
    }

    override suspend fun countCompletedChildGoals(parentId: Long): Int {
        return goalDao.countCompletedChildGoals(parentId)
    }

    override suspend fun updateMultiLevelFlag(id: Long, isMultiLevel: Boolean) {
        goalDao.updateMultiLevelFlag(id, isMultiLevel)
    }

    override suspend fun deleteChildGoals(parentId: Long) {
        goalDao.deleteChildGoals(parentId)
    }

    override suspend fun deleteWithChildren(id: Long) {
        goalDao.deleteWithChildren(id)
    }

    // ============ 进度记录相关 ============

    override suspend fun getProgressRecords(goalId: Long): List<GoalRecordEntity> {
        return goalRecordDao.getRecordsByGoalIdSync(goalId)
    }

    override suspend fun insertProgressRecord(record: GoalRecordEntity): Long {
        return goalRecordDao.insert(record)
    }
}
