package com.mroldl001.mimochat.data.repository

import android.content.Context
import com.google.gson.Gson
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.domain.model.AIModel
import com.mroldl001.mimochat.domain.model.ModelListResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cachedModels: List<AIModel>? = null

    fun getModels(): List<AIModel> {
        cachedModels?.let { return it }
        
        val inputStream = context.resources.openRawResource(R.raw.model_list)
        val json = inputStream.bufferedReader().use { it.readText() }
        val response = Gson().fromJson(json, ModelListResponse::class.java)
        
        cachedModels = response?.models?.filter { it.type == "llm" } ?: emptyList()
        return cachedModels ?: emptyList()
    }

    fun getModelById(modelId: String): AIModel? {
        return getModels().find { it.id == modelId }
    }
}
