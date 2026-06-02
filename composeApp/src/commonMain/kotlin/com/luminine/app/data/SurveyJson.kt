package com.luminine.app.data

import com.luminine.app.model.SurveyResponse
import kotlinx.serialization.json.Json

// Central Json config for survey persistence (used by SurveyRepository impls and tests).
// ignoreUnknownKeys = true -> forward-compatible when a newer schemaVersion adds fields.
// encodeDefaults = false -> compact JSON; enums serialize by name (stable across declaration order).
val SurveyJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    isLenient = false
}

fun SurveyResponse.encodeToJson(): String =
    SurveyJson.encodeToString(SurveyResponse.serializer(), this)

fun decodeSurvey(json: String): SurveyResponse =
    SurveyJson.decodeFromString(SurveyResponse.serializer(), json)
