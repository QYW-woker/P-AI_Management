package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.AIAnalysisEntity
import kotlinx.coroutines.flow.Flow

/**
 * AI分析结果DAO
 */
@Dao
interface AIAnalysisDao {

    /**
     * 插入或更新分析结果
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(analysis: AIAnalysisEntity): Long

    /**
     * 批量插入或更新
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(analyses: List<AIAnalysisEntity>)

    /**
     * 获取指定模块的所有分析结果
     */
    @Query("SELECT * FROM ai_analysis WHERE module = :module ORDER BY lastUpdated DESC")
    fun getByModule(module: String): Flow<List<AIAnalysisEntity>>

    /**
     * 获取指定模块的所有分析结果（同步）
     */
    @Query("SELECT * FROM ai_analysis WHERE module = :module ORDER BY lastUpdated DESC")
    suspend fun getByModuleSync(module: String): List<AIAnalysisEntity>

    /**
     * 获取指定模块和类型的分析结果
     */
    @Query("SELECT * FROM ai_analysis WHERE module = :module AND analysisType = :type LIMIT 1")
    suspend fun getByModuleAndType(module: String, type: String): AIAnalysisEntity?

    /**
     * 获取指定模块和类型的分析结果（Flow）
     */
    @Query("SELECT * FROM ai_analysis WHERE module = :module AND analysisType = :type LIMIT 1")
    fun getByModuleAndTypeFlow(module: String, type: String): Flow<AIAnalysisEntity?>

    /**
     * 获取所有模块的最新分析摘要
     */
    @Query("""
        SELECT * FROM ai_analysis
        WHERE analysisType = 'WEEKLY_SUMMARY'
        ORDER BY lastUpdated DESC
    """)
    fun getAllWeeklySummaries(): Flow<List<AIAnalysisEntity>>

    /**
     * 获取需要更新的分析（超过指定时间未更新）
     */
    @Query("""
        SELECT * FROM ai_analysis
        WHERE lastUpdated < :threshold
        ORDER BY lastUpdated ASC
    """)
    suspend fun getStaleAnalyses(threshold: Long): List<AIAnalysisEntity>

    /**
     * 检查分析是否需要更新
     */
    @Query("""
        SELECT * FROM ai_analysis
        WHERE module = :module AND analysisType = :type
        AND (lastUpdated < :threshold OR dataHash != :currentHash)
        LIMIT 1
    """)
    suspend fun getNeedsUpdate(module: String, type: String, threshold: Long, currentHash: String): AIAnalysisEntity?

    /**
     * 获取最新的综合健康评分
     */
    @Query("""
        SELECT * FROM ai_analysis
        WHERE module = 'OVERALL' AND analysisType = 'HEALTH_SCORE'
        ORDER BY lastUpdated DESC LIMIT 1
    """)
    fun getOverallHealthScore(): Flow<AIAnalysisEntity?>

    /**
     * 删除指定模块的所有分析
     */
    @Query("DELETE FROM ai_analysis WHERE module = :module")
    suspend fun deleteByModule(module: String)

    /**
     * 删除过期的分析（超过30天）
     */
    @Query("DELETE FROM ai_analysis WHERE lastUpdated < :threshold")
    suspend fun deleteStale(threshold: Long)

    /**
     * 获取所有分析结果
     */
    @Query("SELECT * FROM ai_analysis ORDER BY lastUpdated DESC")
    fun getAll(): Flow<List<AIAnalysisEntity>>

    /**
     * 根据ID删除
     */
    @Query("DELETE FROM ai_analysis WHERE id = :id")
    suspend fun deleteById(id: Long)
}
