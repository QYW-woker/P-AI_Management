package com.lifemanager.app.domain.model

import com.lifemanager.app.core.database.entity.CustomFieldEntity
import com.lifemanager.app.core.database.entity.MonthlyAssetEntity

/**
 * 月度资产记录模型（带字段详情）
 */
data class MonthlyAssetWithField(
    val record: MonthlyAssetEntity,
    val field: CustomFieldEntity?
)

/**
 * 月度资产统计模型
 */
data class AssetStats(
    val yearMonth: Int,
    val totalAssets: Double,
    val totalLiabilities: Double
) {
    /**
     * 净资产 = 资产 - 负债
     */
    val netWorth: Double get() = totalAssets - totalLiabilities

    /**
     * 负债率 = 负债 / 资产 * 100
     */
    val debtRatio: Double get() = if (totalAssets > 0) {
        (totalLiabilities / totalAssets) * 100
    } else {
        0.0
    }
}

/**
 * 资产字段统计
 */
data class AssetFieldStats(
    val fieldId: Long,
    val fieldName: String,
    val fieldColor: String,
    val fieldIcon: String,
    val amount: Double,
    val percentage: Double,
    val isAsset: Boolean
)

/**
 * 净资产趋势数据点
 */
data class NetWorthTrendPoint(
    val yearMonth: Int,
    val netWorth: Double
) {
    val year: Int get() = yearMonth / 100
    val month: Int get() = yearMonth % 100
    fun formatMonth(): String = "${month}月"
}

/**
 * 资产UI状态
 */
sealed class AssetUiState {
    data object Loading : AssetUiState()

    data class Success(
        val records: List<MonthlyAssetWithField>,
        val stats: AssetStats,
        val assetsByField: List<AssetFieldStats>,
        val liabilitiesByField: List<AssetFieldStats>
    ) : AssetUiState()

    data class Error(val message: String) : AssetUiState()
}

/**
 * 添加/编辑资产记录状态
 */
data class EditAssetState(
    val id: Long = 0,
    val yearMonth: Int = 0,
    val isAsset: Boolean = true,  // true=资产, false=负债
    val fieldId: Long = 0,
    val amount: Double = 0.0,
    val note: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)
