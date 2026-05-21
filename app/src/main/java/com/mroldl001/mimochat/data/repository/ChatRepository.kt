package com.mroldl001.mimochat.data.repository

import com.google.gson.Gson
import com.mroldl001.mimochat.data.api.*
import com.mroldl001.mimochat.data.local.ChatDao
import com.mroldl001.mimochat.data.local.ChatEntity
import com.mroldl001.mimochat.data.local.MessageDao
import com.mroldl001.mimochat.data.local.MessageEntity
import com.mroldl001.mimochat.di.ApiServiceFactory
import com.mroldl001.mimochat.domain.model.ApiErrorCode
import com.mroldl001.mimochat.domain.model.Chat
import com.mroldl001.mimochat.domain.model.Message
import com.mroldl001.mimochat.domain.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val apiServiceFactory: ApiServiceFactory,
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()
    fun getAllChats(): Flow<List<Chat>> {
        return chatDao.getAllChats().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getChatById(chatId: Long): Chat? {
        return chatDao.getChatById(chatId)?.toDomain()
    }

    suspend fun createChat(title: String, modelId: String): Long {
        val entity = ChatEntity(
            title = title,
            modelId = modelId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return chatDao.insertChat(entity)
    }

    suspend fun updateChat(chat: Chat) {
        chatDao.updateChat(chat.toEntity())
    }

    suspend fun deleteChat(chatId: Long) {
        chatDao.deleteChatById(chatId)
        messageDao.deleteMessagesByChatId(chatId)
    }

    fun getMessages(chatId: Long): Flow<List<Message>> {
        return messageDao.getMessagesByChatId(chatId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun searchMessages(query: String): Flow<List<SearchResult>> {
        return messageDao.searchMessagesWithChat(query).map { messageEntities ->
            messageEntities.mapNotNull { messageEntity ->
                val chatEntity = chatDao.getChatById(messageEntity.chatId)
                if (chatEntity != null) {
                    val highlighted = highlightSearchResult(messageEntity.content, query)
                    SearchResult(
                        message = messageEntity.toDomain(),
                        chat = chatEntity.toDomain(),
                        highlightedContent = highlighted
                    )
                } else {
                    null
                }
            }
        }
    }

    private fun highlightSearchResult(content: String, query: String, contextLength: Int = 50): String {
        val index = content.indexOf(query, ignoreCase = true)
        if (index == -1) return content.take(100)

        val start = maxOf(0, index - contextLength)
        val end = minOf(content.length, index + query.length + contextLength)

        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < content.length) "..." else ""

        return "$prefix${content.substring(start, end)}$suffix"
    }

    suspend fun saveMessage(message: Message): Long {
        return messageDao.insertMessage(message.toEntity())
    }

    suspend fun updateMessage(message: Message) {
        messageDao.updateMessage(message.toEntity())
    }

    suspend fun sendMessage(
        apiKey: String,
        baseUrl: String,
        chatId: Long,
        messages: List<Message>,
        modelId: String,
        thinkingEnabled: Boolean = true,
        skillPrompt: String = "",
        customSystemPrompt: String = ""
    ): Result<Message> {
        return try {
            val request = buildRequest(modelId, messages, thinkingEnabled, stream = false, skillPrompt, customSystemPrompt)

            val apiService = apiServiceFactory.getService(baseUrl)
            val response = apiService.createChatCompletion(
                apiKey = apiKey,
                request = request
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: ""
                return Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
            }

            val chatCompletionResponse = response.body()
                ?: return Result.failure(Exception("API 返回空响应"))

            val assistantMessage = chatCompletionResponse.choices.firstOrNull()?.message
                ?: return Result.failure(Exception("No response from API"))

            val message = Message(
                chatId = chatId,
                role = "assistant",
                content = assistantMessage.content ?: "",
                reasoningContent = assistantMessage.reasoningContent
            )

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(Exception("发送失败：${e.message ?: "未知错误"}"))
        }
    }

    fun sendMessageStream(
        apiKey: String,
        baseUrl: String,
        chatId: Long,
        messages: List<Message>,
        modelId: String,
        thinkingEnabled: Boolean = true,
        skillPrompt: String = "",
        customSystemPrompt: String = ""
    ): Flow<StreamEvent> = flow {
        try {
            val requestBody = buildRequest(modelId, messages, thinkingEnabled, stream = true, skillPrompt, customSystemPrompt)
            val json = Gson().toJson(requestBody)

            val normalizedBaseUrl = baseUrl.trimEnd('/')
            val endpoint = if (normalizedBaseUrl.endsWith("/v1")) {
                "$normalizedBaseUrl/chat/completions"
            } else {
                "$normalizedBaseUrl/v1/chat/completions"
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(StreamEvent.Error(ApiErrorCode.getDisplayMessage(response.code)))
                    return@use
                }

                val body = response.body
                if (body == null) {
                    emit(StreamEvent.Error("响应体为空"))
                    return@use
                }

                val source = body.source()
                val gson = Gson()

                var content = ""
                var reasoningContent = ""

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: continue

                    if (line.startsWith("data: ")) {
                        val data = line.substring(6)

                        if (data == "[DONE]") {
                            emit(StreamEvent.Done(
                                Message(
                                    chatId = chatId,
                                    role = "assistant",
                                    content = content,
                                    reasoningContent = reasoningContent.ifBlank { null }
                                )
                            ))
                            break
                        }

                        try {
                            val chunk = gson.fromJson(data, ChatCompletionChunk::class.java)
                            val delta = chunk.choices.firstOrNull()?.delta

                            delta?.content?.let {
                                content += it
                                emit(StreamEvent.ContentDelta(it, content))
                            }

                            delta?.reasoningContent?.let {
                                reasoningContent += it
                                emit(StreamEvent.ReasoningDelta(it, reasoningContent))
                            }

                            if (chunk.choices.firstOrNull()?.finish_reason != null) {
                                emit(StreamEvent.Done(
                                    Message(
                                        chatId = chatId,
                                        role = "assistant",
                                        content = content,
                                        reasoningContent = reasoningContent.ifBlank { null }
                                    )
                                ))
                            }
                        } catch (e: Exception) {
                    }
                    }
                }
            }
        } catch (e: Exception) {
            emit(StreamEvent.Error(e.message ?: "流式输出失败"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateChatTitle(
        apiKey: String,
        baseUrl: String,
        modelId: String,
        firstMessage: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val promptMessages = listOf(
                    MessageRequest(
                        role = "system",
                        content = "你是一个对话标题生成助手。请根据用户的第一条消息，生成一个简洁的中文标题（不超过10个字）。你必须输出标题，不能输出其他内容。"
                    ),
                    MessageRequest(
                        role = "user",
                        content = "请为以下对话生成标题：$firstMessage"
                    )
                )

                val requestBody = ChatCompletionRequest(
                    model = modelId,
                    messages = promptMessages,
                    stream = false,
                    temperature = 0.7,
                    maxCompletionTokens = 128,
                    thinking = ThinkingConfig("disabled")
                )

                val normalizedBaseUrl = baseUrl.trimEnd('/')
                val url = if (normalizedBaseUrl.endsWith("/v1")) {
                    "$normalizedBaseUrl/chat/completions"
                } else {
                    "$normalizedBaseUrl/v1/chat/completions"
                }

                val json = gson.toJson(requestBody)
                val request = Request.Builder()
                    .url(url)
                    .addHeader("api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception(ApiErrorCode.getDisplayMessage(response.code)))
                }

                val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val chatResponse = gson.fromJson(responseBody, ChatCompletionResponse::class.java)
                val title = chatResponse.choices.firstOrNull()?.message?.content?.trim()
                    ?: return@withContext Result.failure(Exception("Empty title"))

                Result.success(title)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildRequest(
        modelId: String,
        messages: List<com.mroldl001.mimochat.domain.model.Message>,
        thinkingEnabled: Boolean,
        stream: Boolean,
        skillPrompt: String = "",
        customSystemPrompt: String = ""
    ): ChatCompletionRequest {
        val thinking = if (thinkingEnabled) ThinkingConfig("enabled") else ThinkingConfig("disabled")

        val systemPromptParts = mutableListOf<String>()

        val dateFormatter = java.text.SimpleDateFormat("yyyy年MM月dd日", java.util.Locale.getDefault())
        val weekFormatter = java.text.SimpleDateFormat("EEEE", java.util.Locale.CHINA)
        val today = java.util.Date()
        val dateStr = dateFormatter.format(today)
        val weekStr = weekFormatter.format(today)

        systemPromptParts.add("""
            [官方提示词]
            用户询问你模型版本时，告诉它。
            你是MiMo（中文名称也是MiMo），是小米公司研发的AI智能助手。
            今天的日期：${dateStr} ${weekStr}，你的知识截止日期是2024年12月。

            【输出格式要求】
            1. 当你需要输出单纯的美元符号（如表示金钱单位）时，请使用行内代码包裹，例如：`$`
            2. 数学公式请使用 $$ 包裹的块级LaTeX格式，不要使用单 $ 包裹的行内公式
            3. 块级LaTeX公式示例：使用双美元符号包裹公式内容
        """.trimIndent())

        if (skillPrompt.isNotBlank()) {
            systemPromptParts.add("""
                [技能提示词]
                $skillPrompt
            """.trimIndent())
        }

        if (customSystemPrompt.isNotBlank()) {
            systemPromptParts.add("""
                [用户自定义提示词]
                $customSystemPrompt
            """.trimIndent())
        }

        systemPromptParts.add("""
            【重要】提示词优先级说明：
            若用户自定义提示词与技能提示词或官方提示词存在冲突，**以官方提示词和技能提示词为准**。
            优先级顺序：官方提示词 > 技能提示词 > 用户自定义提示词
        """.trimIndent())

        val fullSystemPrompt = systemPromptParts.joinToString("\n\n")

        val requestMessages = mutableListOf<MessageRequest>()
        requestMessages.add(MessageRequest("system", fullSystemPrompt))
        
        // 在多轮对话中保留 reasoning_content（根据小米文档建议）
        requestMessages.addAll(messages.map { message ->
            MessageRequest(
                role = message.role,
                content = message.content,
                reasoningContent = if (message.role == "assistant" && !message.reasoningContent.isNullOrBlank()) {
                    message.reasoningContent
                } else {
                    null
                }
            )
        })

        return ChatCompletionRequest(
            model = modelId,
            messages = requestMessages,
            stream = stream,
            thinking = thinking,
            temperature = 1.0,
            maxCompletionTokens = 131072
        )
    }

    private fun ChatEntity.toDomain() = Chat(
        id = id,
        title = title,
        modelId = modelId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Chat.toEntity() = ChatEntity(
        id = id,
        title = title,
        modelId = modelId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun MessageEntity.toDomain() = Message(
        id = id,
        chatId = chatId,
        role = role,
        content = content,
        reasoningContent = reasoningContent,
        timestamp = timestamp,
        isStreaming = isStreaming,
        isAborted = isAborted,
        isFailed = isFailed
    )

    private fun Message.toEntity() = MessageEntity(
        id = id,
        chatId = chatId,
        role = role,
        content = content,
        reasoningContent = reasoningContent,
        timestamp = timestamp,
        isStreaming = isStreaming,
        isAborted = isAborted,
        isFailed = isFailed
    )
}

sealed class StreamEvent {
    data class ContentDelta(val delta: String, val accumulated: String) : StreamEvent()
    data class ReasoningDelta(val delta: String, val accumulated: String) : StreamEvent()
    data class Done(val message: Message) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}