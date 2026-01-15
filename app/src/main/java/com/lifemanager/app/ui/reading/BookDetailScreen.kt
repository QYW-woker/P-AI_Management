package com.lifemanager.app.ui.reading

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.*
import com.lifemanager.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * 书籍详情页面 - 简洁设计版本
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: Long,
    onNavigateBack: () -> Unit = {},
    viewModel: ReadingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedBook by viewModel.selectedBook.collectAsState()
    val bookNotes by viewModel.bookNotes.collectAsState()

    var showProgressDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // 阅读计时状态
    var isReading by remember { mutableStateOf(false) }
    var readingSeconds by remember { mutableLongStateOf(0L) }
    var startPage by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }

    // 加载书籍详情
    LaunchedEffect(bookId) {
        viewModel.loadBookDetail(bookId)
    }

    // 阅读计时器
    LaunchedEffect(isReading) {
        if (isReading) {
            startPage = selectedBook?.currentPage ?: 0
            while (isReading) {
                delay(1000)
                readingSeconds++
            }
        }
    }

    // 显示消息
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = CleanColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        selectedBook?.title ?: "书籍详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = CleanTypography.title,
                        color = CleanColors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = CleanColors.textSecondary
                        )
                    }
                },
                actions = {
                    selectedBook?.let { book ->
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "编辑",
                                tint = CleanColors.textSecondary
                            )
                        }
                        IconButton(onClick = { viewModel.toggleFavorite(book.id) }) {
                            Icon(
                                imageVector = if (book.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (book.isFavorite) "取消收藏" else "收藏",
                                tint = if (book.isFavorite) CleanColors.error else CleanColors.textSecondary
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "删除",
                                tint = CleanColors.textSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CleanColors.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showNoteDialog = true },
                    containerColor = CleanColors.primary,
                    contentColor = CleanColors.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加笔记")
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CleanColors.primary)
            }
        } else {
            selectedBook?.let { book ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // 标签页
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = CleanColors.background,
                        contentColor = CleanColors.primary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    "详情",
                                    color = if (selectedTab == 0) CleanColors.primary else CleanColors.textSecondary
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    "笔记 (${bookNotes.size})",
                                    color = if (selectedTab == 1) CleanColors.primary else CleanColors.textSecondary
                                )
                            }
                        )
                    }

                    when (selectedTab) {
                        0 -> CleanBookDetailContent(
                            book = book,
                            isReading = isReading,
                            readingSeconds = readingSeconds,
                            onUpdateProgress = { showProgressDialog = true },
                            onRateBook = { showRatingDialog = true },
                            onStartReading = {
                                if (book.status == ReadingStatus.UNREAD || book.status == ReadingStatus.WISH || book.status == ReadingStatus.ABANDONED) {
                                    viewModel.startReading(book.id)
                                }
                                isReading = true
                                readingSeconds = 0
                            },
                            onStopReading = { endPage ->
                                isReading = false
                                val duration = (readingSeconds / 60).toInt()
                                if (duration > 0 || endPage > startPage) {
                                    viewModel.recordReadingSession(
                                        bookId = book.id,
                                        startPage = startPage,
                                        endPage = endPage,
                                        duration = duration
                                    )
                                }
                                readingSeconds = 0
                            },
                            onFinishReading = { viewModel.finishReading(book.id) },
                            onAbandonReading = { viewModel.abandonReading(book.id) }
                        )
                        1 -> CleanBookNotesContent(
                            notes = bookNotes,
                            onToggleFavorite = { viewModel.toggleNoteFavorite(it) },
                            onDeleteNote = { viewModel.deleteNote(it, book.id) }
                        )
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "书籍不存在",
                        style = CleanTypography.body,
                        color = CleanColors.textTertiary
                    )
                }
            }
        }
    }

    // 更新进度对话框
    if (showProgressDialog) {
        selectedBook?.let { book ->
            UpdateProgressDialog(
                currentPage = book.currentPage,
                totalPages = book.totalPages,
                onDismiss = { showProgressDialog = false },
                onConfirm = { page ->
                    viewModel.updateProgress(book.id, page)
                    showProgressDialog = false
                }
            )
        }
    }

    // 添加笔记对话框
    if (showNoteDialog) {
        selectedBook?.let { book ->
            AddNoteDialog(
                onDismiss = { showNoteDialog = false },
                onConfirm = { noteType, content, excerpt, page, chapter ->
                    viewModel.addNote(
                        bookId = book.id,
                        noteType = noteType,
                        content = content,
                        excerpt = excerpt,
                        pageNumber = page,
                        chapter = chapter
                    )
                    showNoteDialog = false
                }
            )
        }
    }

    // 评分对话框
    if (showRatingDialog) {
        selectedBook?.let { book ->
            RatingDialog(
                currentRating = book.rating,
                currentReview = book.shortReview,
                onDismiss = { showRatingDialog = false },
                onConfirm = { rating, review ->
                    viewModel.finishReading(book.id, rating, review)
                    showRatingDialog = false
                }
            )
        }
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除", style = CleanTypography.title) },
            text = { Text("确定要删除这本书吗？相关的阅读记录和笔记也会被删除。", style = CleanTypography.body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedBook?.let { viewModel.deleteBook(it.id) }
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CleanColors.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消", color = CleanColors.textSecondary)
                }
            },
            containerColor = CleanColors.surface
        )
    }

    // 编辑书籍对话框
    if (showEditDialog) {
        selectedBook?.let { book ->
            EditBookDialog(
                book = book,
                onDismiss = { showEditDialog = false },
                onConfirm = { updatedBook ->
                    viewModel.updateBook(updatedBook)
                    viewModel.loadBookDetail(book.id)
                    showEditDialog = false
                }
            )
        }
    }
}

/**
 * 书籍详情内容 - 简洁设计
 */
@Composable
fun CleanBookDetailContent(
    book: BookEntity,
    isReading: Boolean,
    readingSeconds: Long,
    onUpdateProgress: () -> Unit,
    onRateBook: () -> Unit,
    onStartReading: () -> Unit,
    onStopReading: (Int) -> Unit,
    onFinishReading: () -> Unit,
    onAbandonReading: () -> Unit
) {
    var showStopReadingDialog by remember { mutableStateOf(false) }
    var endPageInput by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(Spacing.pageHorizontal, Spacing.pageVertical),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        // 书籍封面和基本信息
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                // 封面
                Box(
                    modifier = Modifier
                        .size(100.dp, 140.dp)
                        .background(CleanColors.surfaceVariant, RoundedCornerShape(Radius.md))
                        .clip(RoundedCornerShape(Radius.md)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = CleanColors.textTertiary
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = book.title,
                        style = CleanTypography.title,
                        color = CleanColors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (book.author.isNotBlank()) {
                        Text(
                            text = book.author,
                            style = CleanTypography.secondary,
                            color = CleanColors.textSecondary
                        )
                    }

                    CleanStatusChip(status = book.status)

                    // 评分
                    if (book.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { index ->
                                val filled = index < book.rating / 2
                                Icon(
                                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = CleanColors.warning
                                )
                            }
                            Spacer(Modifier.width(Spacing.xs))
                            Text(
                                text = "${book.rating / 2.0}",
                                style = CleanTypography.caption,
                                color = CleanColors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // 阅读计时卡片
        if (isReading) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CleanColors.primaryLight),
                    shape = RoundedCornerShape(Radius.md)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "正在阅读中",
                            style = CleanTypography.secondary,
                            color = CleanColors.primary
                        )

                        Spacer(Modifier.height(Spacing.sm))

                        Text(
                            text = formatTime(readingSeconds),
                            style = CleanTypography.amountLarge,
                            color = CleanColors.primary
                        )

                        Spacer(Modifier.height(Spacing.md))

                        Button(
                            onClick = {
                                endPageInput = book.currentPage.toString()
                                showStopReadingDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CleanColors.primary,
                                contentColor = CleanColors.onPrimary
                            ),
                            shape = RoundedCornerShape(Radius.sm)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Spacing.sm))
                            Text("结束阅读")
                        }
                    }
                }
            }
        }

        // 阅读进度
        if (book.totalPages > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CleanColors.surface),
                    shape = RoundedCornerShape(Radius.md),
                    elevation = CardDefaults.cardElevation(defaultElevation = Elevation.xs)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "阅读进度",
                                style = CleanTypography.secondary,
                                color = CleanColors.textSecondary
                            )
                            Text(
                                text = "${book.progressPercent}%",
                                style = CleanTypography.amountMedium,
                                color = CleanColors.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = book.progressPercent / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CleanColors.primary,
                            trackColor = CleanColors.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${book.currentPage} / ${book.totalPages} 页",
                                style = CleanTypography.caption,
                                color = CleanColors.textTertiary
                            )
                            if (book.actualReadingTime > 0) {
                                Text(
                                    text = "已读 ${book.actualReadingTime / 60}小时${book.actualReadingTime % 60}分钟",
                                    style = CleanTypography.caption,
                                    color = CleanColors.textTertiary
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onUpdateProgress,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Radius.sm),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CleanColors.primary
                            )
                        ) {
                            Text("更新进度")
                        }
                    }
                }
            }
        }

        // 操作按钮
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                when (book.status) {
                    ReadingStatus.WISH, ReadingStatus.UNREAD, ReadingStatus.ABANDONED -> {
                        if (!isReading) {
                            Button(
                                onClick = onStartReading,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CleanColors.primary,
                                    contentColor = CleanColors.onPrimary
                                ),
                                shape = RoundedCornerShape(Radius.sm)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(Spacing.sm))
                                Text(if (book.status == ReadingStatus.ABANDONED) "重新阅读" else "开始阅读")
                            }
                        }
                    }
                    ReadingStatus.READING -> {
                        if (!isReading) {
                            Button(
                                onClick = onStartReading,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CleanColors.primary,
                                    contentColor = CleanColors.onPrimary
                                ),
                                shape = RoundedCornerShape(Radius.sm)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(Spacing.sm))
                                Text("继续阅读")
                            }
                        }
                        OutlinedButton(
                            onClick = onFinishReading,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Radius.sm),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanColors.success)
                        ) {
                            Text("标记完成")
                        }
                    }
                    ReadingStatus.FINISHED -> {
                        OutlinedButton(
                            onClick = onRateBook,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Radius.sm),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanColors.warning)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Spacing.sm))
                            Text("评价")
                        }
                    }
                }
            }
        }

        // 书籍信息
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CleanColors.surface),
                shape = RoundedCornerShape(Radius.md),
                elevation = CardDefaults.cardElevation(defaultElevation = Elevation.xs)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Text(
                        text = "书籍信息",
                        style = CleanTypography.secondary,
                        color = CleanColors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )

                    if (book.translator.isNotBlank()) CleanInfoRow("译者", book.translator)
                    if (book.publisher.isNotBlank()) CleanInfoRow("出版社", book.publisher)
                    book.publishYear?.let { CleanInfoRow("出版年份", it.toString()) }
                    if (book.isbn.isNotBlank()) CleanInfoRow("ISBN", book.isbn)
                    CleanInfoRow("格式", BookFormat.getDisplayName(book.format))
                    CleanInfoRow("来源", BookSource.getDisplayName(book.source))
                    if (book.price > 0) CleanInfoRow("价格", "¥${book.price}")
                }
            }
        }

        // 一句话评价
        if (book.shortReview.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CleanColors.surface),
                    shape = RoundedCornerShape(Radius.md),
                    elevation = CardDefaults.cardElevation(defaultElevation = Elevation.xs)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Text(
                            text = "我的评价",
                            style = CleanTypography.secondary,
                            color = CleanColors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = book.shortReview,
                            style = CleanTypography.body,
                            color = CleanColors.textSecondary
                        )
                    }
                }
            }
        }
    }

    // 结束阅读对话框
    if (showStopReadingDialog) {
        AlertDialog(
            onDismissRequest = { showStopReadingDialog = false },
            title = { Text("结束阅读", style = CleanTypography.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        text = "本次阅读时长: ${formatTime(readingSeconds)}",
                        style = CleanTypography.body,
                        color = CleanColors.textSecondary
                    )
                    OutlinedTextField(
                        value = endPageInput,
                        onValueChange = { endPageInput = it.filter { c -> c.isDigit() } },
                        label = { Text("读到第几页") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onStopReading(endPageInput.toIntOrNull() ?: book.currentPage)
                        showStopReadingDialog = false
                    }
                ) {
                    Text("保存", color = CleanColors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopReadingDialog = false }) {
                    Text("取消", color = CleanColors.textSecondary)
                }
            },
            containerColor = CleanColors.surface
        )
    }
}

/**
 * 格式化时间
 */
private fun formatTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

/**
 * 简洁信息行
 */
@Composable
fun CleanInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = CleanTypography.secondary,
            color = CleanColors.textTertiary
        )
        Text(
            text = value,
            style = CleanTypography.secondary,
            color = CleanColors.textSecondary
        )
    }
}

/**
 * 简洁状态标签
 */
@Composable
fun CleanStatusChip(status: String) {
    val (color, text) = when (status) {
        ReadingStatus.WISH -> CleanColors.primary to "想读"
        ReadingStatus.UNREAD -> CleanColors.textTertiary to "未读"
        ReadingStatus.READING -> CleanColors.warning to "在读"
        ReadingStatus.FINISHED -> CleanColors.success to "已读"
        ReadingStatus.ABANDONED -> CleanColors.error to "弃读"
        else -> CleanColors.textTertiary to "未知"
    }

    Surface(
        shape = RoundedCornerShape(Radius.sm),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            style = CleanTypography.caption,
            color = color
        )
    }
}

/**
 * 书籍笔记内容 - 简洁设计
 */
@Composable
fun CleanBookNotesContent(
    notes: List<ReadingNoteEntity>,
    onToggleFavorite: (Long) -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    if (notes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.xxl),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = CleanColors.textPlaceholder
                )
                Text(
                    text = "还没有读书笔记",
                    style = CleanTypography.body,
                    color = CleanColors.textTertiary
                )
                Text(
                    text = "点击右下角按钮添加",
                    style = CleanTypography.caption,
                    color = CleanColors.textPlaceholder
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(Spacing.pageHorizontal, Spacing.pageVertical),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            items(notes, key = { it.id }) { note ->
                CleanNoteCard(
                    note = note,
                    onToggleFavorite = { onToggleFavorite(note.id) },
                    onDelete = { onDeleteNote(note.id) }
                )
            }
        }
    }
}

/**
 * 简洁笔记卡片
 */
@Composable
fun CleanNoteCard(
    note: ReadingNoteEntity,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CleanColors.surface),
        shape = RoundedCornerShape(Radius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.xs)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CleanNoteTypeChip(noteType = note.noteType)
                    note.pageNumber?.let {
                        Text(
                            text = "P$it",
                            style = CleanTypography.caption,
                            color = CleanColors.textTertiary
                        )
                    }
                }

                Row {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (note.isFavorite) CleanColors.error else CleanColors.textTertiary
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = CleanColors.textTertiary
                        )
                    }
                }
            }

            // 摘录
            if (note.excerpt.isNotBlank()) {
                Surface(
                    color = CleanColors.surfaceVariant,
                    shape = RoundedCornerShape(Radius.sm)
                ) {
                    Text(
                        text = "「${note.excerpt}」",
                        modifier = Modifier.padding(Spacing.md),
                        style = CleanTypography.secondary,
                        color = CleanColors.textSecondary
                    )
                }
            }

            Text(
                text = note.content,
                style = CleanTypography.body,
                color = CleanColors.textPrimary
            )

            if (note.chapter.isNotBlank()) {
                Text(
                    text = note.chapter,
                    style = CleanTypography.caption,
                    color = CleanColors.textTertiary
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除", style = CleanTypography.title) },
            text = { Text("确定要删除这条笔记吗？", style = CleanTypography.body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CleanColors.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消", color = CleanColors.textSecondary)
                }
            },
            containerColor = CleanColors.surface
        )
    }
}

/**
 * 简洁笔记类型标签
 */
@Composable
fun CleanNoteTypeChip(noteType: String) {
    val (color, text) = when (noteType) {
        NoteType.EXCERPT -> CleanColors.primary to "摘录"
        NoteType.THOUGHT -> CleanColors.success to "想法"
        NoteType.SUMMARY -> CleanColors.warning to "总结"
        NoteType.QUESTION -> CleanColors.error to "疑问"
        NoteType.VOCABULARY -> Color(0xFF9C27B0) to "生词"
        else -> CleanColors.textTertiary to "其他"
    }

    Surface(
        shape = RoundedCornerShape(Radius.sm),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
            style = CleanTypography.caption,
            color = color
        )
    }
}

// ==================== 保留原有对话框组件 ====================

/**
 * 更新进度对话框
 */
@Composable
fun UpdateProgressDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var page by remember { mutableStateOf(currentPage.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更新阅读进度", style = CleanTypography.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = page,
                    onValueChange = { page = it.filter { c -> c.isDigit() } },
                    label = { Text("当前页码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("/ $totalPages") }
                )

                if (totalPages > 0) {
                    val progressValue = (page.toIntOrNull() ?: 0) * 100 / totalPages
                    LinearProgressIndicator(
                        progress = progressValue / 100f,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = CleanColors.primary,
                        trackColor = CleanColors.surfaceVariant
                    )
                    Text(
                        text = "进度: $progressValue%",
                        style = CleanTypography.caption,
                        color = CleanColors.textTertiary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newPage = page.toIntOrNull() ?: currentPage
                    onConfirm(newPage.coerceIn(0, totalPages.coerceAtLeast(0)))
                }
            ) {
                Text("保存", color = CleanColors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = CleanColors.textSecondary)
            }
        },
        containerColor = CleanColors.surface
    )
}

/**
 * 添加笔记对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (noteType: String, content: String, excerpt: String, page: Int?, chapter: String) -> Unit
) {
    var noteType by remember { mutableStateOf(NoteType.THOUGHT) }
    var content by remember { mutableStateOf("") }
    var excerpt by remember { mutableStateOf("") }
    var page by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加笔记", style = CleanTypography.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text("笔记类型", style = CleanTypography.caption, color = CleanColors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    listOf(
                        NoteType.THOUGHT to "想法",
                        NoteType.EXCERPT to "摘录",
                        NoteType.SUMMARY to "总结"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = noteType == type,
                            onClick = { noteType = type },
                            label = { Text(label, style = CleanTypography.caption) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CleanColors.primaryLight,
                                selectedLabelColor = CleanColors.primary
                            )
                        )
                    }
                }

                if (noteType == NoteType.EXCERPT) {
                    OutlinedTextField(
                        value = excerpt,
                        onValueChange = { excerpt = it },
                        label = { Text("原文摘录") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (noteType == NoteType.EXCERPT) "我的感想" else "笔记内容 *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = page,
                        onValueChange = { page = it.filter { c -> c.isDigit() } },
                        label = { Text("页码") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = chapter,
                        onValueChange = { chapter = it },
                        label = { Text("章节") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (content.isNotBlank() || (noteType == NoteType.EXCERPT && excerpt.isNotBlank())) {
                        onConfirm(noteType, content.trim(), excerpt.trim(), page.toIntOrNull(), chapter.trim())
                    }
                },
                enabled = content.isNotBlank() || (noteType == NoteType.EXCERPT && excerpt.isNotBlank())
            ) {
                Text("保存", color = CleanColors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = CleanColors.textSecondary)
            }
        },
        containerColor = CleanColors.surface
    )
}

/**
 * 评分对话框
 */
@Composable
fun RatingDialog(
    currentRating: Int,
    currentReview: String,
    onDismiss: () -> Unit,
    onConfirm: (rating: Int, review: String) -> Unit
) {
    var rating by remember { mutableIntStateOf(currentRating) }
    var review by remember { mutableStateOf(currentReview) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("评价书籍", style = CleanTypography.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) { index ->
                        val starValue = (index + 1) * 2
                        IconButton(onClick = { rating = starValue }) {
                            Icon(
                                imageVector = if (rating >= starValue) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = CleanColors.warning
                            )
                        }
                    }
                }

                Text(
                    text = when {
                        rating == 0 -> "点击星星评分"
                        rating <= 2 -> "不推荐"
                        rating <= 4 -> "一般"
                        rating <= 6 -> "还不错"
                        rating <= 8 -> "推荐"
                        else -> "强烈推荐"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = CleanTypography.secondary,
                    color = CleanColors.textTertiary
                )

                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("一句话评价 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(rating, review.trim()) }) {
                Text("保存", color = CleanColors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = CleanColors.textSecondary)
            }
        },
        containerColor = CleanColors.surface
    )
}

// 保留旧的组件名称以兼容
@Composable
fun BookDetailContent(
    book: BookEntity,
    onUpdateProgress: () -> Unit,
    onRateBook: () -> Unit,
    onStartReading: () -> Unit,
    onFinishReading: () -> Unit,
    onAbandonReading: () -> Unit
) {
    CleanBookDetailContent(
        book = book,
        isReading = false,
        readingSeconds = 0,
        onUpdateProgress = onUpdateProgress,
        onRateBook = onRateBook,
        onStartReading = onStartReading,
        onStopReading = {},
        onFinishReading = onFinishReading,
        onAbandonReading = onAbandonReading
    )
}

@Composable
fun BookNotesContent(
    notes: List<ReadingNoteEntity>,
    onToggleFavorite: (Long) -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    CleanBookNotesContent(notes, onToggleFavorite, onDeleteNote)
}

/**
 * 编辑书籍对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookDialog(
    book: BookEntity,
    onDismiss: () -> Unit,
    onConfirm: (BookEntity) -> Unit
) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }
    var translator by remember { mutableStateOf(book.translator) }
    var publisher by remember { mutableStateOf(book.publisher) }
    var publishYear by remember { mutableStateOf(book.publishYear?.toString() ?: "") }
    var isbn by remember { mutableStateOf(book.isbn) }
    var totalPages by remember { mutableStateOf(book.totalPages.toString()) }
    var price by remember { mutableStateOf(if (book.price > 0) book.price.toString() else "") }
    var source by remember { mutableStateOf(book.source) }
    var format by remember { mutableStateOf(book.format) }
    var notes by remember { mutableStateOf(book.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑书籍", style = CleanTypography.title) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("书名 *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("作者") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = translator,
                        onValueChange = { translator = it },
                        label = { Text("译者") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = publisher,
                        onValueChange = { publisher = it },
                        label = { Text("出版社") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        OutlinedTextField(
                            value = publishYear,
                            onValueChange = { publishYear = it.filter { c -> c.isDigit() } },
                            label = { Text("出版年份") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = totalPages,
                            onValueChange = { totalPages = it.filter { c -> c.isDigit() } },
                            label = { Text("总页数") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = isbn,
                        onValueChange = { isbn = it },
                        label = { Text("ISBN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("价格") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("¥") }
                    )
                }

                item {
                    Text("书籍格式", style = CleanTypography.caption, color = CleanColors.textSecondary)
                    Spacer(Modifier.height(Spacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        listOf(
                            BookFormat.PAPER to "纸质书",
                            BookFormat.EBOOK to "电子书",
                            BookFormat.AUDIOBOOK to "有声书"
                        ).forEach { (formatValue, label) ->
                            FilterChip(
                                selected = format == formatValue,
                                onClick = { format = formatValue },
                                label = { Text(label, style = CleanTypography.caption) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CleanColors.primaryLight,
                                    selectedLabelColor = CleanColors.primary
                                )
                            )
                        }
                    }
                }

                item {
                    Text("书籍来源", style = CleanTypography.caption, color = CleanColors.textSecondary)
                    Spacer(Modifier.height(Spacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        listOf(
                            BookSource.BOUGHT to "购买",
                            BookSource.BORROWED to "借阅",
                            BookSource.GIFT to "赠送"
                        ).forEach { (sourceValue, label) ->
                            FilterChip(
                                selected = source == sourceValue,
                                onClick = { source = sourceValue },
                                label = { Text(label, style = CleanTypography.caption) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CleanColors.primaryLight,
                                    selectedLabelColor = CleanColors.primary
                                )
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("备注") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            book.copy(
                                title = title.trim(),
                                author = author.trim(),
                                translator = translator.trim(),
                                publisher = publisher.trim(),
                                publishYear = publishYear.toIntOrNull(),
                                isbn = isbn.trim(),
                                totalPages = totalPages.toIntOrNull() ?: 0,
                                price = price.toDoubleOrNull() ?: 0.0,
                                source = source,
                                format = format,
                                notes = notes.trim(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("保存", color = CleanColors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = CleanColors.textSecondary)
            }
        },
        containerColor = CleanColors.surface
    )
}

