package com.lifemanager.app.feature.finance.income

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.lifemanager.app.core.database.entity.IncomeExpenseType
import com.lifemanager.app.domain.model.FieldStats
import com.lifemanager.app.domain.model.IncomeExpenseMonthlyStats
import com.lifemanager.app.domain.model.MonthlyIncomeExpenseWithField
import java.io.File
import java.io.FileWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * 月度收支报表导出工具
 * 支持导出为CSV格式（可在Excel中打开）
 */
object IncomeExpenseExportUtil {

    private val numberFormat = NumberFormat.getNumberInstance(Locale.CHINA)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    /**
     * 导出月度收支报表为CSV
     */
    fun exportToCSV(
        context: Context,
        yearMonth: Int,
        stats: IncomeExpenseMonthlyStats,
        records: List<MonthlyIncomeExpenseWithField>,
        incomeFieldStats: List<FieldStats>,
        expenseFieldStats: List<FieldStats>
    ): Uri? {
        try {
            val year = yearMonth / 100
            val month = yearMonth % 100
            val fileName = "收支报表_${year}年${month}月_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)

            FileWriter(file).use { writer ->
                // 写入BOM以支持中文
                writer.write("\uFEFF")

                // 报表标题
                writer.write("月度收支统计报表\n")
                writer.write("报表月份,${year}年${month}月\n")
                writer.write("导出时间,${dateFormat.format(Date())}\n")
                writer.write("\n")

                // 收支概览
                writer.write("=== 收支概览 ===\n")
                writer.write("项目,金额(元)\n")
                writer.write("总收入,${numberFormat.format(stats.totalIncome)}\n")
                writer.write("总支出,${numberFormat.format(stats.totalExpense)}\n")
                writer.write("结余,${numberFormat.format(stats.netIncome)}\n")
                writer.write("储蓄率,${String.format("%.2f", stats.savingsRate)}%\n")

                val expenseRate = if (stats.totalIncome > 0) {
                    (stats.totalExpense / stats.totalIncome) * 100
                } else 0.0
                writer.write("开销率,${String.format("%.2f", expenseRate)}%\n")
                writer.write("\n")

                // 储蓄率健康提示
                writer.write("=== 财务健康状况 ===\n")
                val healthStatus = when {
                    stats.savingsRate >= 30 -> "健康"
                    stats.savingsRate >= 10 -> "一般"
                    else -> "需关注"
                }
                val healthTip = when {
                    stats.savingsRate >= 30 -> "储蓄率健康，继续保持！"
                    stats.savingsRate >= 10 -> "储蓄率一般，建议适当控制支出"
                    else -> "储蓄率较低，需要关注支出情况"
                }
                writer.write("状态,$healthStatus\n")
                writer.write("建议,$healthTip\n")
                writer.write("\n")

                // 收入分类统计
                if (incomeFieldStats.isNotEmpty()) {
                    writer.write("=== 收入分类统计 ===\n")
                    writer.write("类别,金额(元),占比\n")
                    incomeFieldStats.forEach { stat ->
                        val amount = numberFormat.format(stat.amount)
                        val percentage = String.format("%.2f%%", stat.percentage)
                        writer.write("${stat.fieldName},$amount,$percentage\n")
                    }
                    writer.write("\n")
                }

                // 支出分类统计
                if (expenseFieldStats.isNotEmpty()) {
                    writer.write("=== 支出分类统计 ===\n")
                    writer.write("类别,金额(元),占比\n")
                    expenseFieldStats.forEach { stat ->
                        val amount = numberFormat.format(stat.amount)
                        val percentage = String.format("%.2f%%", stat.percentage)
                        writer.write("${stat.fieldName},$amount,$percentage\n")
                    }
                    writer.write("\n")
                }

                // 收入明细
                val incomeRecords = records.filter { it.record.type == IncomeExpenseType.INCOME }
                if (incomeRecords.isNotEmpty()) {
                    writer.write("=== 收入明细 ===\n")
                    writer.write("类别,金额(元),备注\n")
                    incomeRecords.forEach { record ->
                        val fieldName = record.field?.name ?: "未分类"
                        val amount = numberFormat.format(record.record.amount)
                        val note = record.record.note.replace(",", "，") // 替换逗号避免CSV解析问题
                        writer.write("$fieldName,$amount,$note\n")
                    }
                    writer.write("收入合计,${numberFormat.format(stats.totalIncome)},\n")
                    writer.write("\n")
                }

                // 支出明细
                val expenseRecords = records.filter { it.record.type == IncomeExpenseType.EXPENSE }
                if (expenseRecords.isNotEmpty()) {
                    writer.write("=== 支出明细 ===\n")
                    writer.write("类别,金额(元),备注\n")
                    expenseRecords.forEach { record ->
                        val fieldName = record.field?.name ?: "未分类"
                        val amount = numberFormat.format(record.record.amount)
                        val note = record.record.note.replace(",", "，")
                        writer.write("$fieldName,$amount,$note\n")
                    }
                    writer.write("支出合计,${numberFormat.format(stats.totalExpense)},\n")
                    writer.write("\n")
                }

                // 报表备注
                writer.write("=== 报表说明 ===\n")
                writer.write("本报表由LifeManager生成\n")
                writer.write("储蓄率 = (收入 - 支出) / 收入 × 100%\n")
                writer.write("开销率 = 支出 / 收入 × 100%\n")
                writer.write("建议储蓄率保持在30%以上为健康水平\n")
            }

            // 返回文件URI
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * 分享导出的文件
     */
    fun shareFile(context: Context, uri: Uri, mimeType: String = "text/csv") {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享收支报表"))
    }

    /**
     * 导出并分享
     */
    fun exportAndShare(
        context: Context,
        yearMonth: Int,
        stats: IncomeExpenseMonthlyStats,
        records: List<MonthlyIncomeExpenseWithField>,
        incomeFieldStats: List<FieldStats>,
        expenseFieldStats: List<FieldStats>
    ): Boolean {
        val uri = exportToCSV(context, yearMonth, stats, records, incomeFieldStats, expenseFieldStats)
        return if (uri != null) {
            shareFile(context, uri)
            true
        } else {
            false
        }
    }
}
