package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mroldl001.mimochat.domain.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val hasThinking = !isUser && !message.reasoningContent.isNullOrBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (isUser) {
            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                if (hasThinking) {
                    ThinkingCard(
                        reasoningContent = message.reasoningContent!!,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (message.content.isNotBlank()) {
                    MixedMarkdownLatex(
                        text = message.content,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (message.isStreaming && message.content.isNotBlank()) {
                    StreamingIndicator(
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Text(
            text = formatTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
