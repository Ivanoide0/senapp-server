package com.senapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Token(
    val signId: String,
    val tStart: Long,
    val tEnd: Long,
    val conf: Float = 0.7f
)

@Serializable
data class InterpretRequest(
    val tokens: List<Token>,
    val locale: String = "es-MX"
)

@Serializable
data class InterpretResponse(
    val text: String,
    val alt: List<String> = emptyList(),
    val confidence: Float = 0.7f
)
