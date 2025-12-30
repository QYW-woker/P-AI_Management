package com.lifemanager.app.feature.finance.transaction.billimport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.nio.charset.Charset

/**
 * 账单解析器
 *
 * 支持解析微信和支付宝的账单文件
 * 支持格式：CSV、Excel(.xlsx/.xls)、Word(.docx)
 */
class BillParser(private val context: Context) {

    companion object {
        // 微信账单关键字段
        private val WECHAT_HEADERS = listOf("交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)")

        // 支付宝账单关键字段
        private val ALIPAY_HEADERS = listOf("交易创建时间", "交易对方", "商品名称", "金额（元）", "收/支")

        // 需要跳过的交易状态
        private val SKIP_STATUS = listOf(
            "已退款", "退款成功", "交易关闭", "已关闭", "对方已退还",
            "已全额退款", "已转账到零钱", "朋友已收钱"
        )

        // 需要跳过的交易类型
        private val SKIP_TYPES = listOf(
            "零钱提现", "零钱通转出", "信用卡还款",
            "转入零钱通", "零钱通收益", "理财通"
        )

        // 支持的文件类型
        val SUPPORTED_MIME_TYPES = arrayOf(
            "text/csv",
            "text/comma-separated-values",
            "application/csv",
            "text/plain",
            "application/vnd.ms-excel",                                          // .xls
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "application/msword",                                                 // .doc
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // .docx
        )

        // 支持的文件扩展名
        val SUPPORTED_EXTENSIONS = listOf("csv", "xlsx", "xls", "docx")
    }

    /**
     * 解析账单文件（支持CSV、Excel、Word）
     * 使用IO线程处理，避免阻塞主线程
     */
    suspend fun parseFile(uri: Uri): BillParseResult = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(uri)
            val extension = fileName.substringAfterLast('.', "").lowercase()

            when (extension) {
                "csv", "txt" -> parseCSVFile(uri)
                "xlsx" -> parseExcelFile(uri, isXlsx = true)
                "xls" -> parseExcelFile(uri, isXlsx = false)
                "docx" -> parseWordFile(uri)
                "doc" -> BillParseResult.Error("暂不支持旧版Word(.doc)格式，请将文件另存为.docx格式")
                else -> {
                    // 尝试根据MIME类型判断
                    val mimeType = context.contentResolver.getType(uri)
                    when {
                        mimeType?.contains("csv") == true ||
                        mimeType?.contains("comma-separated") == true ||
                        mimeType == "text/plain" -> parseCSVFile(uri)
                        mimeType?.contains("spreadsheet") == true ||
                        mimeType?.contains("excel") == true ||
                        mimeType == "application/vnd.ms-excel" -> {
                            parseExcelFile(uri, isXlsx = mimeType?.contains("openxml") == true)
                        }
                        mimeType?.contains("word") == true -> {
                            if (mimeType.contains("openxml")) parseWordFile(uri)
                            else BillParseResult.Error("暂不支持旧版Word(.doc)格式，请转换为.docx格式")
                        }
                        else -> parseCSVFile(uri) // 默认尝试CSV解析
                    }
                }
            }
        } catch (e: Exception) {
            BillParseResult.Error("解析失败: ${e.message}")
        }
    }

    /**
     * 获取文件名
     */
    private fun getFileName(uri: Uri): String {
        var fileName = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex) ?: "unknown"
                }
            }
        }
        return fileName
    }

    /**
     * 解析CSV文件
     */
    private fun parseCSVFile(uri: Uri): BillParseResult {
        val content = readFileContent(uri)
        if (content.isBlank()) {
            return BillParseResult.Error("文件内容为空")
        }

        // 检测账单类型
        val source = detectBillSource(content)

        return when (source) {
            BillSource.WECHAT -> parseWechatBill(content)
            BillSource.ALIPAY -> parseAlipayBill(content)
            BillSource.UNKNOWN -> BillParseResult.Error("无法识别账单格式，请确保是微信或支付宝导出的账单")
        }
    }

    /**
     * 解析Excel文件 (.xlsx / .xls)
     */
    private fun parseExcelFile(uri: Uri, isXlsx: Boolean): BillParseResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return BillParseResult.Error("无法打开文件")

            val workbook: Workbook = if (isXlsx) {
                XSSFWorkbook(inputStream)
            } else {
                HSSFWorkbook(inputStream)
            }

            val sheet = workbook.getSheetAt(0)
            if (sheet == null || sheet.physicalNumberOfRows == 0) {
                workbook.close()
                inputStream.close()
                return BillParseResult.Error("Excel文件为空或无法读取")
            }

            // 将Excel转换为CSV格式的内容
            val csvContent = excelSheetToCSV(sheet)
            workbook.close()
            inputStream.close()

            if (csvContent.isBlank()) {
                return BillParseResult.Error("Excel文件内容为空")
            }

            // 检测账单类型并解析
            val source = detectBillSource(csvContent)
            when (source) {
                BillSource.WECHAT -> parseWechatBill(csvContent)
                BillSource.ALIPAY -> parseAlipayBill(csvContent)
                BillSource.UNKNOWN -> BillParseResult.Error("无法识别Excel账单格式，请确保是微信或支付宝导出的账单")
            }
        } catch (e: Exception) {
            BillParseResult.Error("Excel解析失败: ${e.message}")
        }
    }

    /**
     * 将Excel工作表转换为CSV格式字符串
     * 限制最大行数防止处理过大的文件
     */
    private fun excelSheetToCSV(sheet: Sheet): String {
        val maxRows = 10000 // 限制最大处理行数
        val sb = StringBuilder(1024 * 16) // 预分配16KB
        val formatter = DataFormatter()
        var rowCount = 0

        for (row in sheet) {
            if (rowCount++ >= maxRows) break

            val lastCellNum = row.lastCellNum.toInt().coerceAtLeast(0)
            if (lastCellNum == 0) continue

            val cells = Array(lastCellNum) { i ->
                val cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                var cellValue = formatter.formatCellValue(cell)
                // 处理包含逗号或换行的单元格
                if (cellValue.contains(",") || cellValue.contains("\n") || cellValue.contains("\"")) {
                    cellValue = "\"${cellValue.replace("\"", "\"\"")}\""
                }
                cellValue
            }

            if (cells.any { it.isNotBlank() }) {
                sb.append(cells.joinToString(",")).append("\n")
            }
        }

        return sb.toString()
    }

    /**
     * 解析Word文件 (.docx)
     */
    private fun parseWordFile(uri: Uri): BillParseResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return BillParseResult.Error("无法打开文件")

            val document = XWPFDocument(inputStream)

            // 提取Word文档中的所有文本和表格
            val content = StringBuilder()

            // 提取段落文本
            for (paragraph in document.paragraphs) {
                val text = paragraph.text.trim()
                if (text.isNotBlank()) {
                    content.append(text).append("\n")
                }
            }

            // 提取表格内容（账单通常在表格中）
            for (table in document.tables) {
                for (row in table.rows) {
                    val cells = row.tableCells.map { cell ->
                        val cellText = cell.text.trim()
                        if (cellText.contains(",") || cellText.contains("\n") || cellText.contains("\"")) {
                            "\"${cellText.replace("\"", "\"\"")}\""
                        } else {
                            cellText
                        }
                    }
                    if (cells.any { it.isNotBlank() }) {
                        content.append(cells.joinToString(",")).append("\n")
                    }
                }
            }

            document.close()
            inputStream.close()

            val csvContent = content.toString()
            if (csvContent.isBlank()) {
                return BillParseResult.Error("Word文件内容为空")
            }

            // 检测账单类型并解析
            val source = detectBillSource(csvContent)
            when (source) {
                BillSource.WECHAT -> parseWechatBill(csvContent)
                BillSource.ALIPAY -> parseAlipayBill(csvContent)
                BillSource.UNKNOWN -> BillParseResult.Error("无法识别Word账单格式，请确保文件包含有效的账单数据")
            }
        } catch (e: Exception) {
            BillParseResult.Error("Word解析失败: ${e.message}")
        }
    }

    /**
     * 读取文件内容，自动检测编码
     * 限制最大文件大小为10MB防止OOM
     */
    private fun readFileContent(uri: Uri): String {
        val maxFileSize = 10 * 1024 * 1024 // 10MB

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("无法打开文件")

        // 尝试不同编码
        val charsets = listOf(
            Charset.forName("UTF-8"),
            Charset.forName("GBK"),
            Charset.forName("GB2312"),
            Charset.forName("GB18030")
        )

        val bytes = inputStream.use { stream ->
            val buffer = ByteArray(maxFileSize)
            var totalRead = 0
            var bytesRead: Int

            while (stream.read(buffer, totalRead, buffer.size - totalRead).also { bytesRead = it } != -1) {
                totalRead += bytesRead
                if (totalRead >= maxFileSize) {
                    break // 达到最大限制
                }
            }

            buffer.copyOf(totalRead)
        }

        // 优先尝试UTF-8
        for (charset in charsets) {
            try {
                val content = String(bytes, charset)
                // 检查是否包含中文，如果是乱码则尝试下一个编码
                if (content.contains("交易") || content.contains("时间") || content.contains("金额")) {
                    return content
                }
            } catch (e: Exception) {
                continue
            }
        }

        // 默认使用GBK（微信/支付宝常用）
        return String(bytes, Charset.forName("GBK"))
    }

    /**
     * 检测账单来源
     */
    private fun detectBillSource(content: String): BillSource {
        val firstLines = content.lines().take(30).joinToString("\n")

        return when {
            firstLines.contains("微信支付账单") ||
            WECHAT_HEADERS.all { header -> firstLines.contains(header) } -> BillSource.WECHAT

            firstLines.contains("支付宝") ||
            firstLines.contains("账单明细") ||
            ALIPAY_HEADERS.count { header -> firstLines.contains(header) } >= 3 -> BillSource.ALIPAY

            else -> BillSource.UNKNOWN
        }
    }

    /**
     * 解析微信账单
     *
     * 微信账单格式：
     * 交易时间,交易类型,交易对方,商品,收/支,金额(元),支付方式,当前状态,交易单号,商户单号,备注
     */
    private fun parseWechatBill(content: String): BillParseResult {
        val lines = content.lines()

        // 找到数据开始行（跳过账单统计信息）
        var headerIndex = -1
        for (i in lines.indices) {
            if (lines[i].startsWith("交易时间") && lines[i].contains("金额")) {
                headerIndex = i
                break
            }
        }

        if (headerIndex == -1) {
            return BillParseResult.Error("无法找到微信账单数据表头")
        }

        val records = mutableListOf<ParsedBillRecord>()
        var totalIncome = 0.0
        var totalExpense = 0.0
        var skippedCount = 0

        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue

            val fields = parseCSVLine(line)
            if (fields.size < 8) continue

            val datetime = fields.getOrNull(0)?.trim() ?: continue
            val transactionType = fields.getOrNull(1)?.trim() ?: ""
            val counterparty = fields.getOrNull(2)?.trim() ?: ""
            val goods = fields.getOrNull(3)?.trim() ?: ""
            val incomeExpense = fields.getOrNull(4)?.trim() ?: ""
            val amountStr = fields.getOrNull(5)?.trim()?.replace("¥", "")?.replace(",", "") ?: continue
            val paymentMethod = fields.getOrNull(6)?.trim() ?: ""
            val status = fields.getOrNull(7)?.trim() ?: ""
            val orderNo = fields.getOrNull(8)?.trim() ?: ""
            val merchantNo = fields.getOrNull(9)?.trim() ?: ""
            val note = fields.getOrNull(10)?.trim() ?: ""

            // 跳过不需要的交易
            if (shouldSkipTransaction(status, transactionType)) {
                skippedCount++
                continue
            }

            val amount = amountStr.toDoubleOrNull() ?: continue
            if (amount <= 0) continue

            val type = when {
                incomeExpense.contains("收入") -> "收入"
                incomeExpense.contains("支出") -> "支出"
                else -> continue  // 跳过不收不支的记录
            }

            if (type == "收入") totalIncome += amount else totalExpense += amount

            records.add(ParsedBillRecord(
                datetime = datetime,
                type = type,
                counterparty = counterparty,
                goods = goods.ifBlank { transactionType },
                amount = amount,
                paymentMethod = paymentMethod,
                status = status,
                orderNo = orderNo,
                merchantNo = merchantNo,
                note = note,
                source = BillSource.WECHAT
            ))
        }

        if (records.isEmpty()) {
            return BillParseResult.Error("未找到有效的交易记录")
        }

        return BillParseResult.Success(
            records = records,
            source = BillSource.WECHAT,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            skippedCount = skippedCount
        )
    }

    /**
     * 解析支付宝账单
     *
     * 支付宝账单格式：
     * 交易创建时间,交易来源,交易类型,交易对方,商品名称,金额（元）,收/支,交易状态,...
     */
    private fun parseAlipayBill(content: String): BillParseResult {
        val lines = content.lines()

        // 找到数据开始行
        var headerIndex = -1
        for (i in lines.indices) {
            val line = lines[i]
            if ((line.contains("交易创建时间") || line.contains("交易时间")) &&
                line.contains("金额")) {
                headerIndex = i
                break
            }
        }

        if (headerIndex == -1) {
            return BillParseResult.Error("无法找到支付宝账单数据表头")
        }

        // 解析表头，确定各字段位置
        val headerFields = parseCSVLine(lines[headerIndex])
        val timeIndex = headerFields.indexOfFirst { it.contains("时间") }
        val counterpartyIndex = headerFields.indexOfFirst { it.contains("交易对方") || it.contains("对方") }
        val goodsIndex = headerFields.indexOfFirst { it.contains("商品") || it.contains("名称") }
        val amountIndex = headerFields.indexOfFirst { it.contains("金额") }
        val typeIndex = headerFields.indexOfFirst { it == "收/支" || it.contains("收/支") }
        val statusIndex = headerFields.indexOfFirst { it.contains("状态") }
        val orderIndex = headerFields.indexOfFirst { it.contains("订单号") || it.contains("交易号") }

        val records = mutableListOf<ParsedBillRecord>()
        var totalIncome = 0.0
        var totalExpense = 0.0
        var skippedCount = 0

        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank() || line.startsWith("-") || line.startsWith("=")) continue

            val fields = parseCSVLine(line)
            if (fields.size < 5) continue

            val datetime = fields.getOrNull(timeIndex)?.trim() ?: continue
            val counterparty = fields.getOrNull(counterpartyIndex)?.trim() ?: ""
            val goods = fields.getOrNull(goodsIndex)?.trim() ?: ""
            val amountStr = fields.getOrNull(amountIndex)?.trim()
                ?.replace("¥", "")?.replace(",", "")?.replace(" ", "") ?: continue
            val incomeExpense = fields.getOrNull(typeIndex)?.trim() ?: ""
            val status = if (statusIndex >= 0) fields.getOrNull(statusIndex)?.trim() ?: "" else ""
            val orderNo = if (orderIndex >= 0) fields.getOrNull(orderIndex)?.trim() ?: "" else ""

            // 跳过不需要的交易
            if (shouldSkipTransaction(status, goods)) {
                skippedCount++
                continue
            }

            val amount = amountStr.toDoubleOrNull() ?: continue
            if (amount <= 0) continue

            val type = when {
                incomeExpense.contains("收入") -> "收入"
                incomeExpense.contains("支出") -> "支出"
                incomeExpense.isBlank() && status.contains("退款") -> continue  // 跳过退款
                else -> continue
            }

            if (type == "收入") totalIncome += amount else totalExpense += amount

            records.add(ParsedBillRecord(
                datetime = datetime,
                type = type,
                counterparty = counterparty,
                goods = goods,
                amount = amount,
                paymentMethod = "",
                status = status,
                orderNo = orderNo,
                source = BillSource.ALIPAY
            ))
        }

        if (records.isEmpty()) {
            return BillParseResult.Error("未找到有效的交易记录")
        }

        return BillParseResult.Success(
            records = records,
            source = BillSource.ALIPAY,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            skippedCount = skippedCount
        )
    }

    /**
     * 解析CSV行（处理引号包裹的逗号）
     */
    private fun parseCSVLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())

        return result
    }

    /**
     * 判断是否应该跳过该交易
     */
    private fun shouldSkipTransaction(status: String, type: String): Boolean {
        return SKIP_STATUS.any { status.contains(it) } ||
               SKIP_TYPES.any { type.contains(it) }
    }
}
