package com.mroldl001.mimochat.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface MiMoApiService {

    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("api-key") apiKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>

    @Streaming
    @POST("v1/chat/completions")
    suspend fun createStreamingChatCompletion(
        @Header("api-key") apiKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatCompletionRequest
    ): okhttp3.ResponseBody
}
