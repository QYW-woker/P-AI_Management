package com.lifemanager.app.feature.finance.investment

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.lifemanager.app.domain.model.InvestmentFieldStats
import com.lifemanager.app.domain.model.InvestmentMonthlyStats
import com.lifemanager.app.domain.model.MonthlyInvestmentWithField
import java.io.File
import java.io.FileWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * 月度定投报表导出工具
 *
 * 支持导出为CSV格式，包含预算与实际投入对比
 */
object InvestmentExportUtil {

    private val numberFormat = NumberFormat.getNumberInstance(Locale.CHINA)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    /**
     * 导出月度定投报表为CSV
     */
    fun exportToCSV(
        context: Context,
        yearMonth: Int,
        stats: InvestmentMonthlyStats,
        records: List<MonthlyInvestmentWithField>,
        fieldStats: List<InvestmentFieldStats>
    ): Uri? {
        try {
            val year = yearMonth / 100
            val month = yearMonth % 100
            val fileName = "定投报表_${year}年${month}月_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)

            FileWriter(file).use { writer ->
                // 写入BOM以支持中文
                writer.write("\uFEFF")

                // 报表标题
                writer.write("月度定投统计报表\n")
                writer.write("报表月份,${year}年${month}月\n")
                writer.write("导出时间,${dateFormat.format(Date())}\n")
                writer.write("\n")

                // 定投概览
                writer.write("=== 定投概览 ===\n")
                writer.write("项目,金额(元)\n")
                writer.write("预算总额,${numberFormat.format(stats.totalBudget)}\n")
                writer.write("实际投入,${numberFormat.format(stats.totalActual)}\n")
                writer.write("预算差额,${numberFormat.format(stats.budgetDiff)}\n")
                writer.write("完成率,${String.format("%.2f", stats.completionRate)}%\n")
                writer.write("\n")

                // 预算完成情况提示
                writer.write("=== 预算完成情况 ===\n")
                val completionStatus = when {
                    stats.completionRate >= 100 -> "已完成"
                    stats.completionRate >= 80 -> "进度良好"
                    stats.completionRate >= 50 -> "进度一般"
                    else -> "进度较慢"
                }
                val completionTip = when {
                    stats.completionRate >= 100 -> "恭喜！本月定投目标已完成"
                    stats.completionRate >= 80 -> "定投进度良好，继续保持"
                    stats.completionRate >= 50 -> "定投进度一般，请及时补充投入"
                    else -> "定投进度较慢，请关注并调整计划"
                }
                writer.write("状态,$completionStatus\n")
                writer.write("建议,$completionTip\n")
                writer.write("\n")

                // 定投分类统计
                if (fieldStats.isNotEmpty()) {
                    writer.write("=== 定投分类统计 ===\n")
                    writer.write("类别,预算(元),实际(元),完成率,占比\n")
                    fieldStats.forEach { stat ->
                        val budget = numberFormat.format(stat.budgetAmount)
                        val actual = numberFormat.format(stat.actualAmount)
                        val completion = String.format("%.2f%%", stat.completionRate)
                        val percentage = String.format("%.2f%%", stat.percentage)
                        writer.write("${stat.fieldName},$budget,$actual,$completion,$percentage\n")
                    }
                    writer.write("\n")
                }

                // 定投明细
                if (records.isNotEmpty()) {
                    writer.write("=== 定投明细 ===\n")
                    writer.write("类别,预算(元),实际(元),完成率,备注\n")
                    records.forEach { record ->
                        val fieldName = record.field?.name ?: "未分类"
                        val budget = numberFormat.format(record.record.budgetAmount)
                        val actual = numberFormat.format(record.record.actualAmount)
                        val completion = if (record.record.budgetAmount > 0) {
                            String.format("%.2f%%", (record.record.actualAmount / record.record.budgetAmount) * 100)
                        } else {
                            "N/A"
                        }
                        val note = record.record.note.replace(",", "，") // 替换逗号避免CSV解析问题
                        writer.write("$fieldName,$budget,$actual,$completion,$note\n")
                    }
                    writer.write("合计,${numberFormat.format(stats.totalBudget)},${numberFormat.format(stats.totalActual)},${String.format("%.2f%%", stats.completionRate)},\n")
                    writer.write("\n")
                }

                // 报表备注
                writer.write("=== 报表说明 ===\n")
                writer.write("本报表由LifeManager生成\n")
                writer.write("完成率 = 实际投入 / 预算金额 × 100%\n")
                writer.write("占比 = 单项实际投入 / 总实际投入 × 100%\n")
                writer.write("建议每月定投金额保持稳定，长期持有获取复利收益\n")
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
        context.startActivity(Intent.createChooser(shareIntent, "分享定投报表"))
    }

    /**
     * 导出并分享
     */
    fun exportAndShare(
        context: Context,
        yearMonth: Int,
        stats: InvestmentMonthlyStats,
        records: List<MonthlyInvestmentWithField>,
        fieldStats: List<InvestmentFieldStats>
    ): Boolean {
        val uri = exportToCSV(context, yearMonth, stats, records, fieldStats)
        return if (uri != null) {
            shareFile(context, uri)
            true
        } else {
            false
        }
    }
}
