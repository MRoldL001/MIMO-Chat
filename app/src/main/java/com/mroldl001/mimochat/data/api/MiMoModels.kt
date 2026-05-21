package com.mroldl001.mimochat.data.api

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    val model: String,
    val messages: List<MessageRequest>,
    @SerializedName("max_completion_tokens")
    val maxCompletionTokens: Int = 1024,
    val temperature: Double = 1.0,
    @SerializedName("top_p")
    val topP: Double = 0.95,
    val stream: Boolean = false,
    val stop: Any? = null,
    @SerializedName("frequency_penalty")
    val frequencyPenalty: Double = 0.0,
    @SerializedName("presence_penalty")
    val presencePenalty: Double = 0.0,
    val thinking: ThinkingConfig? = null
)

data class MessageRequest(
    val role: String,
    val content: String,
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null
)

data class ThinkingConfig(
    val type: String = "enabled"
)

// Non-streaming response
data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>,
    val created: Long,
    val model: String,
    val usage: Usage?
)

data class Choice(
    val finish_reason: String?,
    val message: AssistantMessage
)

data class AssistantMessage(
    val content: String?,
    @SerializedName("reasoning_content")
    val reasoningContent: String?
)

// Streaming response chunk
data class ChatCompletionChunk(
    val id: String,
    val choices: List<ChunkChoice>,
    val created: Long,
    val model: String,
    val `object`: String
)

data class ChunkChoice(
    val index: Int,
    val delta: DeltaMessage,
    val finish_reason: String?
)

data class DeltaMessage(
    val role: String? = null,
    val content: String? = null,
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null
)

data class Usage(
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)
