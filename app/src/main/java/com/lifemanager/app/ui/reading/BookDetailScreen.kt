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

/**
 * 书籍详情页面
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
    var selectedTab by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }

    // 加载书籍详情
    LaunchedEffect(bookId) {
        viewModel.loadBookDetail(bookId)
    }

    // 显示消息
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedBook?.title ?: "书籍详情", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    selectedBook?.let { book ->
                        IconButton(onClick = { viewModel.toggleFavorite(book.id) }) {
                            Icon(
                                imageVector = if (book.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (book.isFavorite) "取消收藏" else "收藏",
                                tint = if (book.isFavorite) Color.Red else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = { showNoteDialog = true }) {
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
                CircularProgressIndicator()
            }
        } else {
            selectedBook?.let { book ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // 标签页
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("详情") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("笔记 (${bookNotes.size})") }
                        )
                    }

                    when (selectedTab) {
                        0 -> BookDetailContent(
                            book = book,
                            onUpdateProgress = { showProgressDialog = true },
                            onRateBook = { showRatingDialog = true },
                            onStartReading = { viewModel.startReading(book.id) },
                            onFinishReading = { viewModel.finishReading(book.id) },
                            onAbandonReading = { viewModel.abandonReading(book.id) }
                        )
                        1 -> BookNotesContent(
                            notes = bookNotes,
                            onToggleFavorite = { viewModel.toggleNoteFavorite(it) },
                            onDeleteNote = { viewModel.deleteNote(it) }
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
                    Text("书籍不存在")
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
            title = { Text("确认删除") },
            text = { Text("确定要删除这本书吗？相关的阅读记录和笔记也会被删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedBook?.let { viewModel.deleteBook(it.id) }
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 书籍详情内容
 */
@Composable
fun BookDetailContent(
    book: BookEntity,
    onUpdateProgress: () -> Unit,
    onRateBook: () -> Unit,
    onStartReading: () -> Unit,
    onFinishReading: () -> Unit,
    onAbandonReading: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 书籍封面和基本信息
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 封面占位
                Box(
                    modifier = Modifier
                        .size(120.dp, 160.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (book.author.isNotBlank()) {
                        Text(
                            text = "作者: ${book.author}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (book.translator.isNotBlank()) {
                        Text(
                            text = "译者: ${book.translator}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (book.publisher.isNotBlank()) {
                        Text(
                            text = "出版社: ${book.publisher}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    StatusChip(status = book.status)

                    // 评分显示
                    if (book.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { index ->
                                val filled = index < book.rating / 2
                                val halfFilled = index == book.rating / 2 && book.rating % 2 == 1
                                Icon(
                                    imageVector = when {
                                        filled -> Icons.Filled.Star
                                        halfFilled -> Icons.Filled.StarHalf
                                        else -> Icons.Outlined.StarBorder
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFFFC107)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${book.rating / 2.0}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // 阅读进度卡片
        if (book.totalPages > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "阅读进度",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${book.progressPercent}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { book.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Text(
                            text = "${book.currentPage} / ${book.totalPages} 页",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Button(
                            onClick = onUpdateProgress,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (book.status) {
                    ReadingStatus.WISH, ReadingStatus.UNREAD -> {
                        Button(
                            onClick = onStartReading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("开始阅读")
                        }
                    }
                    ReadingStatus.READING -> {
                        OutlinedButton(
                            onClick = onFinishReading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("标记完成")
                        }
                        OutlinedButton(
                            onClick = onAbandonReading,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("放弃阅读")
                        }
                    }
                    ReadingStatus.FINISHED -> {
                        OutlinedButton(
                            onClick = onRateBook,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("评价")
                        }
                    }
                    ReadingStatus.ABANDONED -> {
                        Button(
                            onClick = onStartReading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("重新阅读")
                        }
                    }
                }
            }
        }

        // 一句话评价
        if (book.shortReview.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "我的评价",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = book.shortReview,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 书籍详细信息
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "书籍信息",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    if (book.isbn.isNotBlank()) {
                        InfoRow("ISBN", book.isbn)
                    }
                    book.publishYear?.let { InfoRow("出版年份", it.toString()) }
                    InfoRow("格式", BookFormat.getDisplayName(book.format))
                    InfoRow("来源", BookSource.getDisplayName(book.source))
                    if (book.price > 0) {
                        InfoRow("价格", "¥${book.price}")
                    }
                    if (book.actualReadingTime > 0) {
                        InfoRow("阅读时长", "${book.actualReadingTime / 60}小时${book.actualReadingTime % 60}分钟")
                    }
                }
            }
        }

        // 备注
        if (book.notes.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "备注",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = book.notes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 信息行
 */
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 书籍笔记内容
 */
@Composable
fun BookNotesContent(
    notes: List<ReadingNoteEntity>,
    onToggleFavorite: (Long) -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    if (notes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "还没有读书笔记",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "点击右下角按钮添加笔记",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onToggleFavorite = { onToggleFavorite(note.id) },
                    onDelete = { onDeleteNote(note.id) }
                )
            }
        }
    }
}

/**
 * 笔记卡片
 */
@Composable
fun NoteCard(
    note: ReadingNoteEntity,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NoteTypeChip(noteType = note.noteType)
                    note.pageNumber?.let {
                        Text(
                            text = "第${it}页",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (note.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 摘录内容
            if (note.excerpt.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "「${note.excerpt}」",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 笔记内容
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium
            )

            // 章节信息
            if (note.chapter.isNotBlank()) {
                Text(
                    text = "章节: ${note.chapter}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条笔记吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 笔记类型标签
 */
@Composable
fun NoteTypeChip(noteType: String) {
    val (color, text) = when (noteType) {
        NoteType.EXCERPT -> MaterialTheme.colorScheme.secondary to "摘录"
        NoteType.THOUGHT -> MaterialTheme.colorScheme.primary to "想法"
        NoteType.SUMMARY -> MaterialTheme.colorScheme.tertiary to "总结"
        NoteType.QUESTION -> Color(0xFFFF9800) to "疑问"
        NoteType.VOCABULARY -> Color(0xFF9C27B0) to "生词"
        else -> MaterialTheme.colorScheme.outline to "其他"
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

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
        title = { Text("更新阅读进度") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                    val progress = (page.toIntOrNull() ?: 0) * 100 / totalPages
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "进度: $progress%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
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
        title = { Text("添加笔记") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 笔记类型
                Text("笔记类型", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        NoteType.THOUGHT to "想法",
                        NoteType.EXCERPT to "摘录",
                        NoteType.SUMMARY to "总结"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = noteType == type,
                            onClick = { noteType = type },
                            label = { Text(label) }
                        )
                    }
                }

                // 摘录内容（仅摘录类型显示）
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

                // 笔记内容
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        onConfirm(
                            noteType,
                            content.trim(),
                            excerpt.trim(),
                            page.toIntOrNull(),
                            chapter.trim()
                        )
                    }
                },
                enabled = content.isNotBlank() || (noteType == NoteType.EXCERPT && excerpt.isNotBlank())
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
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
        title = { Text("评价书籍") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 星级评分
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) { index ->
                        val starValue = (index + 1) * 2
                        IconButton(
                            onClick = { rating = starValue }
                        ) {
                            Icon(
                                imageVector = if (rating >= starValue) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = Color(0xFFFFC107)
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 一句话评价
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
            TextButton(
                onClick = { onConfirm(rating, review.trim()) }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
