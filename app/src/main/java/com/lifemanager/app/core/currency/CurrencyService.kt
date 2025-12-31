package com.lifemanager.app.core.currency

import com.lifemanager.app.core.database.dao.CurrencyRateDao
import com.lifemanager.app.core.database.dao.UserCurrencySettingDao
import com.lifemanager.app.core.database.entity.CurrencyRateEntity
import com.lifemanager.app.core.database.entity.UserCurrencySettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 货币服务
 *
 * 管理多币种转换和汇率
 */
@Singleton
class CurrencyService @Inject constructor(
    private val currencyRateDao: CurrencyRateDao,
    private val userCurrencySettingDao: UserCurrencySettingDao
) {

    companion object {
        // 默认货币列表
        val DEFAULT_CURRENCIES = listOf(
            CurrencyInfo("CNY", "人民币", "¥", "中国"),
            CurrencyInfo("USD", "美元", "$", "美国"),
            CurrencyInfo("EUR", "欧元", "€", "欧元区"),
            CurrencyInfo("GBP", "英镑", "£", "英国"),
            CurrencyInfo("JPY", "日元", "¥", "日本"),
            CurrencyInfo("HKD", "港币", "HK$", "香港"),
            CurrencyInfo("TWD", "新台币", "NT$", "台湾"),
            CurrencyInfo("KRW", "韩元", "₩", "韩国"),
            CurrencyInfo("SGD", "新加坡元", "S$", "新加坡"),
            CurrencyInfo("AUD", "澳元", "A$", "澳大利亚"),
            CurrencyInfo("CAD", "加元", "C$", "加拿大"),
            CurrencyInfo("CHF", "瑞士法郎", "CHF", "瑞士"),
            CurrencyInfo("THB", "泰铢", "฿", "泰国"),
            CurrencyInfo("MYR", "马来西亚林吉特", "RM", "马来西亚"),
            CurrencyInfo("RUB", "俄罗斯卢布", "₽", "俄罗斯"),
            CurrencyInfo("INR", "印度卢比", "₹", "印度")
        )

        // 默认汇率（以CNY为基准）
        val DEFAULT_RATES = mapOf(
            "CNY" to 1.0,
            "USD" to 0.14,
            "EUR" to 0.13,
            "GBP" to 0.11,
            "JPY" to 21.0,
            "HKD" to 1.09,
            "TWD" to 4.45,
            "KRW" to 186.0,
            "SGD" to 0.19,
            "AUD" to 0.21,
            "CAD" to 0.19,
            "CHF" to 0.12,
            "THB" to 4.9,
            "MYR" to 0.65,
            "RUB" to 12.8,
            "INR" to 11.7
        )

        const val RATE_UPDATE_INTERVAL = 24 * 60 * 60 * 1000L // 24小时
    }

    /**
     * 获取所有汇率
     */
    fun getAllRates(): Flow<List<CurrencyRateEntity>> = currencyRateDao.getAllRates()

    /**
     * 获取用户货币设置
     */
    fun getUserSettings(): Flow<UserCurrencySettingEntity?> = userCurrencySettingDao.getSettings()

    /**
     * 初始化默认汇率
     */
    suspend fun initDefaultRates() {
        val existing = currencyRateDao.getAllRates().first()
        if (existing.isEmpty()) {
            val rates = DEFAULT_CURRENCIES.map { currency ->
                CurrencyRateEntity(
                    currencyCode = currency.code,
                    currencyName = currency.name,
                    symbol = currency.symbol,
                    rateToBase = DEFAULT_RATES[currency.code] ?: 1.0,
                    country = currency.country
                )
            }
            currencyRateDao.insertAll(rates)
        }
    }

    /**
     * 初始化用户货币设置
     */
    suspend fun initUserSettings() {
        if (userCurrencySettingDao.getSettingsSync() == null) {
            userCurrencySettingDao.insert(
                UserCurrencySettingEntity(
                    id = 1,
                    baseCurrency = "CNY",
                    displayCurrencies = "CNY,USD,EUR,JPY",
                    autoConvert = true,
                    showOriginalAmount = true
                )
            )
        }
    }

    /**
     * 转换金额
     */
    suspend fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): Double {
        if (fromCurrency == toCurrency) return amount

        val fromRate = currencyRateDao.getByCode(fromCurrency)?.rateToBase ?: 1.0
        val toRate = currencyRateDao.getByCode(toCurrency)?.rateToBase ?: 1.0

        // 先转换为基准货币，再转换为目标货币
        val baseAmount = amount / fromRate
        return baseAmount * toRate
    }

    /**
     * 批量转换金额
     */
    suspend fun convertBatch(
        amounts: List<Pair<Double, String>>,
        toCurrency: String
    ): List<Double> {
        return amounts.map { (amount, fromCurrency) ->
            convert(amount, fromCurrency, toCurrency)
        }
    }

    /**
     * 获取汇率
     */
    suspend fun getRate(fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return 1.0

        val fromRate = currencyRateDao.getByCode(fromCurrency)?.rateToBase ?: 1.0
        val toRate = currencyRateDao.getByCode(toCurrency)?.rateToBase ?: 1.0

        return toRate / fromRate
    }

    /**
     * 格式化金额
     */
    suspend fun formatAmount(
        amount: Double,
        currencyCode: String,
        showSymbol: Boolean = true
    ): String {
        val currency = currencyRateDao.getByCode(currencyCode)
        val symbol = if (showSymbol) currency?.symbol ?: "" else ""

        return when (currencyCode) {
            "JPY", "KRW" -> "$symbol${amount.toLong()}"
            else -> "$symbol%.2f".format(amount)
        }
    }

    /**
     * 更新汇率
     */
    suspend fun updateRates(rates: Map<String, Double>) {
        rates.forEach { (code, rate) ->
            val existing = currencyRateDao.getByCode(code)
            if (existing != null) {
                currencyRateDao.insert(
                    existing.copy(
                        rateToBase = rate,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * 检查是否需要更新汇率
     */
    suspend fun needsRateUpdate(): Boolean {
        val lastUpdate = currencyRateDao.getLastUpdateTime() ?: 0
        return System.currentTimeMillis() - lastUpdate > RATE_UPDATE_INTERVAL
    }

    /**
     * 更新用户货币设置
     */
    suspend fun updateSettings(settings: UserCurrencySettingEntity) {
        userCurrencySettingDao.update(settings)
    }

    /**
     * 设置基准货币
     */
    suspend fun setBaseCurrency(currencyCode: String) {
        val settings = userCurrencySettingDao.getSettingsSync()
        if (settings != null) {
            userCurrencySettingDao.update(
                settings.copy(
                    baseCurrency = currencyCode,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 添加显示货币
     */
    suspend fun addDisplayCurrency(currencyCode: String) {
        val settings = userCurrencySettingDao.getSettingsSync() ?: return
        val currencies = settings.displayCurrencies.split(",").toMutableList()
        if (!currencies.contains(currencyCode)) {
            currencies.add(currencyCode)
            userCurrencySettingDao.update(
                settings.copy(
                    displayCurrencies = currencies.joinToString(","),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 移除显示货币
     */
    suspend fun removeDisplayCurrency(currencyCode: String) {
        val settings = userCurrencySettingDao.getSettingsSync() ?: return
        val currencies = settings.displayCurrencies.split(",").toMutableList()
        if (currencies.remove(currencyCode)) {
            userCurrencySettingDao.update(
                settings.copy(
                    displayCurrencies = currencies.joinToString(","),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 获取货币信息
     */
    fun getCurrencyInfo(code: String): CurrencyInfo? {
        return DEFAULT_CURRENCIES.find { it.code == code }
    }

    /**
     * 获取所有支持的货币
     */
    fun getSupportedCurrencies(): List<CurrencyInfo> = DEFAULT_CURRENCIES
}

/**
 * 货币信息
 */
data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val country: String
)

/**
 * 货币转换结果
 */
data class ConversionResult(
    val originalAmount: Double,
    val originalCurrency: String,
    val convertedAmount: Double,
    val targetCurrency: String,
    val rate: Double,
    val formattedOriginal: String,
    val formattedConverted: String
)
