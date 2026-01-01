package com.lifemanager.app.ui.reading

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifemanager.app.core.database.entity.*

/**
 * 阅读主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    onNavigateToBookDetail: (Long) -> Unit = {},
    onNavigateToAddBook: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: ReadingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val overview by viewModel.overview.collectAsState()
    val yearlyStats by viewModel.yearlyStats.collectAsState()
    val readingBooks by viewModel.readingBooks.collectAsState()
    val allBooks by viewModel.allBooks.collectAsState()
    val currentGoal by viewModel.currentYearGoal.collectAsState()

    var showAddBookDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("阅读") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showGoalDialog = true }) {
                        Icon(Icons.Default.Flag, contentDescription = "阅读目标")
                    }
                    IconButton(onClick = { showAddBookDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加书籍")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 阅读概览卡片
            item {
                ReadingOverviewCard(
                    overview = overview,
                    yearlyStats = yearlyStats,
                    goal = currentGoal
                )
            }

            // 正在阅读
            if (readingBooks.isNotEmpty()) {
                item {
                    Text(
                        text = "正在阅读",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(readingBooks) { book ->
                            ReadingBookCard(
                                book = book,
                                onClick = { onNavigateToBookDetail(book.id) },
                                onUpdateProgress = { page ->
                                    viewModel.updateProgress(book.id, page)
                                }
                            )
                        }
                    }
                }
            }

            // 状态筛选标签
            item {
                StatusFilterTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            // 书籍列表
            val filteredBooks = when (selectedTab) {
                0 -> allBooks
                1 -> allBooks.filter { it.status == ReadingStatus.WISH }
                2 -> allBooks.filter { it.status == ReadingStatus.UNREAD }
                3 -> allBooks.filter { it.status == ReadingStatus.READING }
                4 -> allBooks.filter { it.status == ReadingStatus.FINISHED }
                5 -> allBooks.filter { it.status == ReadingStatus.ABANDONED }
                else -> allBooks
            }

            if (filteredBooks.isEmpty()) {
                item {
                    EmptyBooksPlaceholder(
                        selectedTab = selectedTab,
                        onAddBook = { showAddBookDialog = true }
                    )
                }
            } else {
                items(filteredBooks, key = { it.id }) { book ->
                    BookListItem(
                        book = book,
                        onClick = { onNavigateToBookDetail(book.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(book.id) }
                    )
                }
            }
        }
    }

    // 添加书籍对话框
    if (showAddBookDialog) {
        AddBookDialog(
            onDismiss = { showAddBookDialog = false },
            onConfirm = { title, author, pages, status ->
                viewModel.addBook(
                    title = title,
                    author = author,
                    totalPages = pages,
                    status = status
                )
                showAddBookDialog = false
            }
        )
    }

    // 阅读目标对话框
    if (showGoalDialog) {
        SetGoalDialog(
            currentGoal = currentGoal,
            onDismiss = { showGoalDialog = false },
            onConfirm = { books, pages, minutes ->
                viewModel.setYearlyGoal(books, pages, minutes)
                showGoalDialog = false
            }
        )
    }
}

/**
 * 阅读概览卡片
 */
@Composable
fun ReadingOverviewCard(
    overview: com.lifemanager.app.domain.usecase.ReadingOverview,
    yearlyStats: com.lifemanager.app.domain.usecase.YearlyReadingStats,
    goal: ReadingGoalEntity?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "阅读统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "总书籍",
                    value = "${overview.totalBooks}",
                    icon = Icons.Default.MenuBook
                )
                StatItem(
                    label = "已读完",
                    value = "${overview.finishedCount}",
                    icon = Icons.Default.CheckCircle
                )
                StatItem(
                    label = "在读",
                    value = "${overview.readingCount}",
                    icon = Icons.Default.AutoStories
                )
                StatItem(
                    label = "想读",
                    value = "${overview.wishCount}",
                    icon = Icons.Default.Bookmark
                )
            }

            // 年度进度
            if (goal != null && goal.targetBooks > 0) {
                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Text(
                    text = "年度目标: ${yearlyStats.booksFinished}/${goal.targetBooks} 本",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                LinearProgressIndicator(
                    progress = { yearlyStats.booksProgressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            }
        }
    }
}

/**
 * 统计项
 */
@Composable
fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

/**
 * 正在阅读书籍卡片
 */
@Composable
fun ReadingBookCard(
    book: BookEntity,
    onClick: () -> Unit,
    onUpdateProgress: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 书籍图标占位
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (book.author.isNotBlank()) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 进度条
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${book.currentPage}/${book.totalPages}页",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${book.progressPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { book.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

/**
 * 状态筛选标签
 */
@Composable
fun StatusFilterTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("全部", "想读", "未读", "在读", "已读", "弃读")

    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        modifier = Modifier.fillMaxWidth(),
        edgePadding = 0.dp,
        divider = {}
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = { Text(title) }
            )
        }
    }
}

/**
 * 书籍列表项
 */
@Composable
fun BookListItem(
    book: BookEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 书籍图标
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // 书籍信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (book.author.isNotBlank()) {
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 状态标签
                    StatusChip(status = book.status)

                    // 进度或评分
                    if (book.status == ReadingStatus.READING && book.totalPages > 0) {
                        Text(
                            text = "${book.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (book.status == ReadingStatus.FINISHED && book.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFC107)
                            )
                            Text(
                                text = "${book.rating / 2.0}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 收藏按钮
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (book.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (book.isFavorite) "取消收藏" else "收藏",
                    tint = if (book.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 状态标签
 */
@Composable
fun StatusChip(status: String) {
    val (color, text) = when (status) {
        ReadingStatus.WISH -> MaterialTheme.colorScheme.tertiary to "想读"
        ReadingStatus.UNREAD -> MaterialTheme.colorScheme.outline to "未读"
        ReadingStatus.READING -> MaterialTheme.colorScheme.primary to "在读"
        ReadingStatus.FINISHED -> Color(0xFF4CAF50) to "已读"
        ReadingStatus.ABANDONED -> MaterialTheme.colorScheme.error to "弃读"
        else -> MaterialTheme.colorScheme.outline to "未知"
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
 * 空书籍占位符
 */
@Composable
fun EmptyBooksPlaceholder(
    selectedTab: Int,
    onAddBook: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        val message = when (selectedTab) {
            0 -> "还没有添加任何书籍"
            1 -> "还没有想读的书"
            2 -> "没有未读的书籍"
            3 -> "当前没有在读的书"
            4 -> "还没有读完任何书"
            5 -> "没有弃读的书籍"
            else -> "暂无书籍"
        }

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(onClick = onAddBook) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("添加书籍")
        }
    }
}

/**
 * 添加书籍对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, author: String, pages: Int, status: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var pages by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(ReadingStatus.UNREAD) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加书籍") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("书名 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("作者") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pages,
                    onValueChange = { pages = it.filter { c -> c.isDigit() } },
                    label = { Text("总页数") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "阅读状态",
                    style = MaterialTheme.typography.labelMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        ReadingStatus.WISH to "想读",
                        ReadingStatus.UNREAD to "未读",
                        ReadingStatus.READING to "在读"
                    ).forEach { (status, label) ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            title.trim(),
                            author.trim(),
                            pages.toIntOrNull() ?: 0,
                            selectedStatus
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("添加")
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
 * 设置目标对话框
 */
@Composable
fun SetGoalDialog(
    currentGoal: ReadingGoalEntity?,
    onDismiss: () -> Unit,
    onConfirm: (books: Int, pages: Int, minutes: Int) -> Unit
) {
    var targetBooks by remember { mutableStateOf((currentGoal?.targetBooks ?: 12).toString()) }
    var targetPages by remember { mutableStateOf((currentGoal?.targetPages ?: 0).toString()) }
    var targetMinutes by remember { mutableStateOf((currentGoal?.targetMinutes ?: 0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置年度阅读目标") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = targetBooks,
                    onValueChange = { targetBooks = it.filter { c -> c.isDigit() } },
                    label = { Text("目标书籍数量") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("本") }
                )

                OutlinedTextField(
                    value = targetPages,
                    onValueChange = { targetPages = it.filter { c -> c.isDigit() } },
                    label = { Text("目标页数 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("页") }
                )

                OutlinedTextField(
                    value = targetMinutes,
                    onValueChange = { targetMinutes = it.filter { c -> c.isDigit() } },
                    label = { Text("目标时长 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("分钟") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        targetBooks.toIntOrNull() ?: 12,
                        targetPages.toIntOrNull() ?: 0,
                        targetMinutes.toIntOrNull() ?: 0
                    )
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
