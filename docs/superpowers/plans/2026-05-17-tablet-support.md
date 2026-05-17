# 平板适配实现计划

**目标：** 在完全不改变手机现有设计的情况下，让软件支持平板：手机上保持原有交互（侧边栏抽屉式），平板上侧边栏常驻与聊天页二分屏显示。

**架构方案：**
- 使用 Jetpack Compose 的 `WindowSizeClass` 检测设备类型
- 平板（Expanded）：使用 `PermanentNavigationDrawer` 实现常驻侧边栏布局
- 手机（Compact/Medium）：保持现有的 `ModalNavigationDrawer` 抽屉式交互
- 所有现有手机界面完全不变

**技术栈：** Jetpack Compose Material3, WindowSizeClass

---

## 任务 1: 添加 WindowSizeClass 依赖

**文件：**
- 修改: `d:\code\MIMOChat\app\build.gradle.kts`

- [ ] **Step 1: 添加 material3-window-size-class 依赖**

在 `dependencies` 块中添加：
```kotlin
implementation("androidx.compose.material3:material3-window-size-class")
```

---

## 任务 2: 创建自适应布局组件

**文件：**
- 创建: `d:\code\MIMOChat\app\src\main\java\com\mroldl001\mimochat\ui\chat\components\AdaptiveChatLayout.kt`

- [ ] **Step 1: 创建 AdaptiveChatLayout.kt**

```kotlin
package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.domain.model.Chat
import com.mroldl001.mimochat.ui.chat.viewmodel.ChatUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveChatLayout(
    windowWidthSizeClass: WindowWidthSizeClass,
    uiState: ChatUiState,
    messages: List<com.mroldl001.mimochat.domain.model.Message>,
    streamingContent: String,
    streamingReasoning: String,
    isStreaming: Boolean,
    isThinkingMode: Boolean,
    onSendMessage: (String) -> Unit,
    onStopGenerating: () -> Unit,
    onCreateNewChat: () -> Unit,
    onDeleteChat: (Chat) -> Unit,
    onSelectChat: (Chat) -> Unit,
    onThemeChanged: (com.mroldl001.mimochat.ui.theme.ThemeColor, com.mroldl001.mimochat.ui.theme.ThemeMode) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (windowWidthSizeClass) {
        WindowWidthSizeClass.Expanded -> {
            // 平板：常驻侧边栏 + 聊天页面 二分屏
            PermanentSplitView(
                uiState = uiState,
                messages = messages,
                streamingContent = streamingContent,
                streamingReasoning = streamingReasoning,
                isStreaming = isStreaming,
                isThinkingMode = isThinkingMode,
                onSendMessage = onSendMessage,
                onStopGenerating = onStopGenerating,
                onCreateNewChat = onCreateNewChat,
                onDeleteChat = onDeleteChat,
                onSelectChat = onSelectChat,
                onThemeChanged = onThemeChanged,
                onNavigateToSearch = onNavigateToSearch,
                modifier = modifier
            )
        }
        else -> {
            // 手机：保持原有 ModalNavigationDrawer 模式
            // 返回 null 让 ChatScreen 使用原有逻辑
            Box(modifier = modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermanentSplitView(
    uiState: ChatUiState,
    messages: List<com.mroldl001.mimochat.domain.model.Message>,
    streamingContent: String,
    streamingReasoning: String,
    isStreaming: Boolean,
    isThinkingMode: Boolean,
    onSendMessage: (String) -> Unit,
    onStopGenerating: () -> Unit,
    onCreateNewChat: () -> Unit,
    onDeleteChat: (Chat) -> Unit,
    onSelectChat: (Chat) -> Unit,
    onThemeChanged: (com.mroldl001.mimochat.ui.theme.ThemeColor, com.mroldl001.mimochat.ui.theme.ThemeMode) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                ChatListContent(
                    chats = uiState.chats,
                    selectedChatId = uiState.currentChat?.id,
                    onChatSelected = onSelectChat,
                    onCreateNewChat = onCreateNewChat,
                    onDeleteChat = onDeleteChat,
                    onNavigateToSearch = onNavigateToSearch,
                    onSettingsClick = { /* TODO: Settings */ }
                )
            }
        },
        modifier = modifier
    ) {
        ChatContentArea(
            uiState = uiState,
            messages = messages,
            streamingContent = streamingContent,
            streamingReasoning = streamingReasoning,
            isStreaming = isStreaming,
            isThinkingMode = isThinkingMode,
            onSendMessage = onSendMessage,
            onStopGenerating = onStopGenerating
        )
    }
}

@Composable
private fun ChatListContent(
    chats: List<Chat>,
    selectedChatId: Long?,
    onChatSelected: (Chat) -> Unit,
    onCreateNewChat: () -> Unit,
    onDeleteChat: (Chat) -> Unit,
    onNavigateToSearch: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "对话历史",
                style = MaterialTheme.typography.headlineSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onNavigateToSearch) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                        contentDescription = "设置"
                    )
                }
            }
        }

        HorizontalDivider()

        if (chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无对话记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(chats) { chat ->
                    ChatHistoryItem(
                        chat = chat,
                        isSelected = chat.id == selectedChatId,
                        onClick = { onChatSelected(chat) },
                        onDelete = { onDeleteChat(chat) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onCreateNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("新建对话")
        }
    }
}

@Composable
private fun ChatHistoryItem(
    chat: Chat,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = formatTimestamp(chat.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
private fun ChatContentArea(
    uiState: ChatUiState,
    messages: List<com.mroldl001.mimochat.domain.model.Message>,
    streamingContent: String,
    streamingReasoning: String,
    isStreaming: Boolean,
    isThinkingMode: Boolean,
    onSendMessage: (String) -> Unit,
    onStopGenerating: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ModelSelector(
                        currentModel = uiState.selectedModel,
                        models = uiState.availableModels,
                        onModelSelected = { /* TODO */ }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            ) {
                // 技能开关区域
                SkillSwitchRow(
                    isThinkingMode = isThinkingMode,
                    onThinkingModeChanged = { },
                    activeSkill = uiState.activeSkill,
                    isGenerating = uiState.isLoading || isStreaming,
                    onSkillSelected = { }
                )

                InputBar(
                    onSendMessage = onSendMessage,
                    onStopGenerating = onStopGenerating,
                    isGenerating = uiState.isLoading
                )
            }
        }
    ) { paddingValues ->
        // 消息列表内容
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (messages.isEmpty() && !isStreaming) {
                EmptyStateContent(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.size) { index ->
                        MessageBubble(message = messages[index])
                    }
                    if (isStreaming) {
                        item {
                            StreamingMessageBubble(
                                content = streamingContent,
                                reasoningContent = streamingReasoning
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MIMO在这里，今天你要做什么？",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SkillSwitchRow(
    isThinkingMode: Boolean,
    onThinkingModeChanged: (Boolean) -> Unit,
    activeSkill: com.mroldl001.mimochat.ui.chat.viewmodel.SkillType?,
    isGenerating: Boolean,
    onSkillSelected: (com.mroldl001.mimochat.ui.chat.viewmodel.SkillType?) -> Unit
) {
    // 从 ChatScreen 复制的技能开关逻辑
    // 保持与手机界面完全一致
}
```

---

## 任务 3: 修改 MainActivity 传递 WindowSizeClass

**文件：**
- 修改: `d:\code\MIMOChat\app\src\main\java\com\mroldl001\mimochat\MainActivity.kt`

- [ ] **Step 1: 修改 MainActivity 添加 calculateWindowSizeClass**

```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    // 添加 WindowSizeClass 计算
    val configuration = LocalConfiguration.current
    val windowSizeClass = remember(configuration) {
        WindowSizeClass.getFromConfiguration(configuration)
    }
    
    // ... 其余保持不变
}
```

- [ ] **Step 2: 添加必要的 imports**

```kotlin
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
```

---

## 任务 4: 修改 ChatScreen 集成自适应布局

**文件：**
- 修改: `d:\code\MIMOChat\app\src\main\java\com\mroldl001\mimochat\ui\chat\ChatScreen.kt`

- [ ] **Step 1: 添加 WindowSizeClass 参数**

在 ChatScreen 函数签名中添加：
```kotlin
@Composable
fun ChatScreen(
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    // ... 其他参数保持不变
)
```

- [ ] **Step 2: 在平板模式下使用 AdaptiveChatLayout**

在 ChatScreen 函数的开始处添加：
```kotlin
// 检测是否为平板展开模式
if (windowWidthSizeClass == WindowWidthSizeClass.Expanded) {
    AdaptiveChatLayout(
        windowWidthSizeClass = windowWidthSizeClass,
        uiState = uiState,
        messages = messages,
        streamingContent = streamingContent,
        streamingReasoning = streamingReasoning,
        isStreaming = isStreaming,
        isThinkingMode = isThinkingMode,
        onSendMessage = { ... },
        onStopGenerating = { viewModel.stopGenerating() },
        onCreateNewChat = { viewModel.createNewChat() },
        onDeleteChat = { viewModel.deleteChat(it) },
        onSelectChat = { viewModel.selectChat(it) },
        onThemeChanged = onThemeChanged,
        onNavigateToSearch = onNavigateToSearch
    )
    return
}
```

---

## 任务 5: 验证编译

**文件：**
- 测试编译: `d:\code\MIMOChat`

- [ ] **Step 1: 运行 assembleDebug 构建**

```bash
cd d:\code\MIMOChat
./gradlew assembleDebug
```

预期结果：编译成功，无错误

- [ ] **Step 2: 在平板模拟器/设备上测试**

验证：
1. 平板上侧边栏常驻显示
2. 手机上保持原有抽屉式交互
3. 所有功能正常工作

---

## 注意事项

1. **不改变手机界面**：所有手机上的 UI 逻辑保持 100% 不变
2. **平板布局比例**：侧边栏宽度 280dp，聊天区域自适应填充
3. **代码复用**：尽量复用现有组件（ModelSelector, InputBar, MessageBubble 等）
4. **主题一致性**：平板和手机使用相同的主题和配色方案
