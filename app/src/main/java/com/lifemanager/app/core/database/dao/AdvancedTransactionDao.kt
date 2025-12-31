package com.lifemanager.app.core.database.dao

import androidx.room.*
import com.lifemanager.app.core.database.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * 拆分交易DAO
 */
@Dao
interface SplitTransactionDao {

    @Query("SELECT * FROM split_transactions WHERE parentTransactionId = :transactionId")
    fun getByTransactionId(transactionId: Long): Flow<List<SplitTransactionEntity>>

    @Query("SELECT * FROM split_transactions WHERE parentTransactionId = :transactionId")
    suspend fun getByTransactionIdSync(transactionId: Long): List<SplitTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(split: SplitTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(splits: List<SplitTransactionEntity>)

    @Update
    suspend fun update(split: SplitTransactionEntity)

    @Query("DELETE FROM split_transactions WHERE parentTransactionId = :transactionId")
    suspend fun deleteByTransactionId(transactionId: Long)

    @Query("DELETE FROM split_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 检查交易是否有拆分
     */
    @Query("SELECT COUNT(*) FROM split_transactions WHERE parentTransactionId = :transactionId")
    suspend fun hasSplits(transactionId: Long): Int
}

/**
 * 商家DAO
 */
@Dao
interface MerchantDao {

    @Query("SELECT * FROM merchants ORDER BY transactionCount DESC")
    fun getAllMerchants(): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE isFavorite = 1 ORDER BY transactionCount DESC")
    fun getFavoriteMerchants(): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE type = :type ORDER BY name")
    fun getMerchantsByType(type: String): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE id = :id")
    suspend fun getById(id: Long): MerchantEntity?

    @Query("SELECT * FROM merchants WHERE name LIKE '%' || :keyword || '%' OR aliases LIKE '%' || :keyword || '%' LIMIT 10")
    suspend fun searchByKeyword(keyword: String): List<MerchantEntity>

    /**
     * 根据关键词自动匹配商家
     */
    @Query("""
        SELECT * FROM merchants
        WHERE name = :exactName
        OR aliases LIKE '%' || :keyword || '%'
        LIMIT 1
    """)
    suspend fun findByKeyword(exactName: String, keyword: String): MerchantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(merchant: MerchantEntity): Long

    @Update
    suspend fun update(merchant: MerchantEntity)

    @Query("DELETE FROM merchants WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 更新商家统计数据
     */
    @Query("""
        UPDATE merchants
        SET transactionCount = transactionCount + 1,
            totalAmount = totalAmount + :amount,
            lastTransactionDate = :date,
            updatedAt = :now
        WHERE id = :merchantId
    """)
    suspend fun updateStats(merchantId: Long, amount: Double, date: Int, now: Long = System.currentTimeMillis())

    /**
     * 获取消费最高的商家
     */
    @Query("SELECT * FROM merchants ORDER BY totalAmount DESC LIMIT :limit")
    suspend fun getTopMerchants(limit: Int = 10): List<MerchantEntity>

    /**
     * 获取最常光顾的商家
     */
    @Query("SELECT * FROM merchants ORDER BY transactionCount DESC LIMIT :limit")
    suspend fun getMostFrequentMerchants(limit: Int = 10): List<MerchantEntity>
}

/**
 * 账单DAO
 */
@Dao
interface BillDao {

    @Query("SELECT * FROM bills ORDER BY dueDate ASC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE status = :status ORDER BY dueDate ASC")
    fun getBillsByStatus(status: String): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE status = 'PENDING' AND dueDate <= :date ORDER BY dueDate ASC")
    fun getUpcomingBills(date: Int): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE status = 'PENDING' AND dueDate < :today")
    fun getOverdueBills(today: Int): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getById(id: Long): BillEntity?

    @Query("""
        SELECT * FROM bills
        WHERE status = 'PENDING'
        AND reminderEnabled = 1
        AND dueDate <= :targetDate
        ORDER BY dueDate ASC
    """)
    suspend fun getBillsToRemind(targetDate: Int): List<BillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: BillEntity): Long

    @Update
    suspend fun update(bill: BillEntity)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 标记账单已支付
     */
    @Query("""
        UPDATE bills
        SET status = 'PAID',
            paidDate = :paidDate,
            paidAmount = :paidAmount,
            transactionId = :transactionId,
            updatedAt = :now
        WHERE id = :billId
    """)
    suspend fun markPaid(billId: Long, paidDate: Int, paidAmount: Double, transactionId: Long?, now: Long = System.currentTimeMillis())

    /**
     * 更新逾期账单状态
     */
    @Query("""
        UPDATE bills
        SET status = 'OVERDUE', updatedAt = :now
        WHERE status = 'PENDING' AND dueDate < :today
    """)
    suspend fun updateOverdueBills(today: Int, now: Long = System.currentTimeMillis())

    /**
     * 统计本月账单
     */
    @Query("""
        SELECT
            COUNT(*) as total,
            SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) as paid,
            SUM(CASE WHEN status = 'PENDING' THEN amount ELSE 0 END) as pendingAmount,
            SUM(CASE WHEN status = 'PAID' THEN paidAmount ELSE 0 END) as paidAmount
        FROM bills
        WHERE dueDate BETWEEN :monthStart AND :monthEnd
    """)
    suspend fun getMonthlyStats(monthStart: Int, monthEnd: Int): BillMonthlyStats
}

data class BillMonthlyStats(
    val total: Int,
    val paid: Int,
    val pendingAmount: Double,
    val paidAmount: Double
)

/**
 * 退款DAO
 */
@Dao
interface RefundDao {

    @Query("SELECT * FROM refunds ORDER BY applyDate DESC")
    fun getAllRefunds(): Flow<List<RefundEntity>>

    @Query("SELECT * FROM refunds WHERE status = :status ORDER BY applyDate DESC")
    fun getRefundsByStatus(status: String): Flow<List<RefundEntity>>

    @Query("SELECT * FROM refunds WHERE originalTransactionId = :transactionId")
    suspend fun getByTransactionId(transactionId: Long): RefundEntity?

    @Query("SELECT * FROM refunds WHERE id = :id")
    suspend fun getById(id: Long): RefundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(refund: RefundEntity): Long

    @Update
    suspend fun update(refund: RefundEntity)

    @Query("DELETE FROM refunds WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 完成退款
     */
    @Query("""
        UPDATE refunds
        SET status = 'COMPLETED',
            completedDate = :completedDate,
            refundTransactionId = :refundTransactionId,
            updatedAt = :now
        WHERE id = :refundId
    """)
    suspend fun completeRefund(refundId: Long, completedDate: Int, refundTransactionId: Long?, now: Long = System.currentTimeMillis())

    /**
     * 统计待处理退款
     */
    @Query("SELECT COUNT(*) FROM refunds WHERE status IN ('PENDING', 'PROCESSING')")
    suspend fun countPendingRefunds(): Int

    /**
     * 统计退款总额
     */
    @Query("SELECT SUM(amount) FROM refunds WHERE status = 'COMPLETED' AND completedDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalRefundAmount(startDate: Int, endDate: Int): Double?
}

/**
 * 分期计划DAO
 */
@Dao
interface InstallmentPlanDao {

    @Query("SELECT * FROM installment_plans ORDER BY startDate DESC")
    fun getAllPlans(): Flow<List<InstallmentPlanEntity>>

    @Query("SELECT * FROM installment_plans WHERE status = :status ORDER BY startDate DESC")
    fun getPlansByStatus(status: String): Flow<List<InstallmentPlanEntity>>

    @Query("SELECT * FROM installment_plans WHERE status = 'ACTIVE' ORDER BY startDate ASC")
    fun getActivePlans(): Flow<List<InstallmentPlanEntity>>

    @Query("SELECT * FROM installment_plans WHERE id = :id")
    suspend fun getById(id: Long): InstallmentPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: InstallmentPlanEntity): Long

    @Update
    suspend fun update(plan: InstallmentPlanEntity)

    @Query("DELETE FROM installment_plans WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 更新已还期数
     */
    @Query("""
        UPDATE installment_plans
        SET paidPeriods = paidPeriods + 1,
            status = CASE WHEN paidPeriods + 1 >= totalPeriods THEN 'COMPLETED' ELSE status END,
            updatedAt = :now
        WHERE id = :planId
    """)
    suspend fun incrementPaidPeriods(planId: Long, now: Long = System.currentTimeMillis())

    /**
     * 统计活跃分期总额
     */
    @Query("""
        SELECT
            COUNT(*) as count,
            SUM((totalPeriods - paidPeriods) * (periodAmount + periodFee)) as remainingAmount,
            SUM(totalAmount) as totalAmount
        FROM installment_plans
        WHERE status = 'ACTIVE'
    """)
    suspend fun getActiveStats(): InstallmentStats
}

data class InstallmentStats(
    val count: Int,
    val remainingAmount: Double?,
    val totalAmount: Double?
)

/**
 * 分期还款记录DAO
 */
@Dao
interface InstallmentPaymentDao {

    @Query("SELECT * FROM installment_payments WHERE planId = :planId ORDER BY periodNumber")
    fun getByPlanId(planId: Long): Flow<List<InstallmentPaymentEntity>>

    @Query("SELECT * FROM installment_payments WHERE planId = :planId ORDER BY periodNumber")
    suspend fun getByPlanIdSync(planId: Long): List<InstallmentPaymentEntity>

    @Query("SELECT * FROM installment_payments WHERE id = :id")
    suspend fun getById(id: Long): InstallmentPaymentEntity?

    @Query("""
        SELECT * FROM installment_payments
        WHERE status = 'PENDING' AND dueDate <= :date
        ORDER BY dueDate ASC
    """)
    suspend fun getUpcomingPayments(date: Int): List<InstallmentPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: InstallmentPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<InstallmentPaymentEntity>)

    @Update
    suspend fun update(payment: InstallmentPaymentEntity)

    /**
     * 标记已还款
     */
    @Query("""
        UPDATE installment_payments
        SET status = 'PAID',
            paidDate = :paidDate,
            transactionId = :transactionId
        WHERE id = :paymentId
    """)
    suspend fun markPaid(paymentId: Long, paidDate: Int, transactionId: Long?)

    /**
     * 更新逾期状态
     */
    @Query("""
        UPDATE installment_payments
        SET status = 'OVERDUE'
        WHERE status = 'PENDING' AND dueDate < :today
    """)
    suspend fun updateOverduePayments(today: Int)
}

/**
 * 交易模板DAO
 */
@Dao
interface TransactionTemplateDao {

    @Query("SELECT * FROM transaction_templates WHERE isEnabled = 1 ORDER BY usageCount DESC, sortOrder")
    fun getActiveTemplates(): Flow<List<TransactionTemplateEntity>>

    @Query("SELECT * FROM transaction_templates ORDER BY sortOrder")
    fun getAllTemplates(): Flow<List<TransactionTemplateEntity>>

    @Query("SELECT * FROM transaction_templates WHERE type = :type AND isEnabled = 1 ORDER BY usageCount DESC")
    fun getTemplatesByType(type: String): Flow<List<TransactionTemplateEntity>>

    @Query("SELECT * FROM transaction_templates WHERE id = :id")
    suspend fun getById(id: Long): TransactionTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TransactionTemplateEntity): Long

    @Update
    suspend fun update(template: TransactionTemplateEntity)

    @Query("DELETE FROM transaction_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 更新使用次数
     */
    @Query("""
        UPDATE transaction_templates
        SET usageCount = usageCount + 1,
            lastUsedAt = :now
        WHERE id = :templateId
    """)
    suspend fun incrementUsage(templateId: Long, now: Long = System.currentTimeMillis())

    /**
     * 获取最常用模板
     */
    @Query("SELECT * FROM transaction_templates WHERE isEnabled = 1 ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getMostUsedTemplates(limit: Int = 5): List<TransactionTemplateEntity>
}

/**
 * 搜索预设DAO
 */
@Dao
interface SearchPresetDao {

    @Query("SELECT * FROM search_presets ORDER BY usageCount DESC")
    fun getAllPresets(): Flow<List<SearchPresetEntity>>

    @Query("SELECT * FROM search_presets WHERE id = :id")
    suspend fun getById(id: Long): SearchPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: SearchPresetEntity): Long

    @Update
    suspend fun update(preset: SearchPresetEntity)

    @Query("DELETE FROM search_presets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        UPDATE search_presets
        SET usageCount = usageCount + 1
        WHERE id = :presetId
    """)
    suspend fun incrementUsage(presetId: Long)
}
