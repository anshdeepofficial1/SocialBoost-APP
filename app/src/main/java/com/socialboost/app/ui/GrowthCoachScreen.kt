package com.socialboost.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.socialboost.app.core.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun GrowthCoachScreen(api: SocialApi) {
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var platform by remember { mutableStateOf("instagram") }
    var topic by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    var result by remember { mutableStateOf<JSONObject?>(null) }
    var hookResult by remember { mutableStateOf<JSONObject?>(null) }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Headline("GROWTH COACH", "Learn from what\nactually worked.", "The existing SocialBoost ML and creator-voice models train only on your real workspace history.")
    RemotePanel("coach_" + revision, load = {
        JSONObject()
            .put("status", api.request("/coach/status"))
            .put("winners", api.request("/coach/winners"))
            .put("voice", api.request("/coach/voice/status"))
    }) { data, refresh ->
        val root = data.asObject()
        val model = root.optJSONObject("status") ?: JSONObject()
        val voice = root.optJSONObject("voice") ?: JSONObject()
        SbCard {
            SectionEyebrow("MODEL STATUS")
            DataRow("Growth model", if (model.optBoolean("available")) "Trained" else "Unavailable")
            DataRow("Historical posts with views", model.numberLabel("n_samples_available"))
            DataRow("Voice writer", if (voice.optBoolean("available")) "Trained" else "Unavailable")
            TextButton(enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    try {
                        val response = api.request("/coach/train", "POST").asObject()
                        notice = response.value("message", response.value("status"))
                        revision++
                    } catch (e: Exception) { notice = e.message ?: "Training failed." }
                    finally { busy = false }
                }
            }) { Text("Train Growth Coach") }
            OutlinedButton(enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    try {
                        val response = api.request("/coach/voice/train", "POST").asObject()
                        notice = response.value("message", response.value("status"))
                        revision++
                    } catch (e: Exception) { notice = e.message ?: "Voice training failed." }
                    finally { busy = false }
                }
            }) { Text("Train my creator voice") }
        }
        Spacer(Modifier.height(10.dp))
        val winners = root.optJSONObject("winners") ?: JSONObject()
        if (winners.optBoolean("available")) {
            SbCard {
                SectionEyebrow("TOP CONTENT EVIDENCE")
                JsonEvidence(winners)
            }
        }
        TextButton(onClick = refresh) { Text("Refresh model evidence") }
    }
    Spacer(Modifier.height(12.dp))
    SbCard {
        SectionEyebrow("TEST A CONTENT IDEA")
        SelectPlatform(platform) { platform = it }
        OutlinedTextField(topic, { topic = it }, label = { Text("Category / topic") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(caption, { caption = it }, label = { Text("Caption or opening") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(duration, { duration = it }, label = { Text("Video length (seconds)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            scope.launch {
                busy = true
                try {
                    result = api.request("/coach/score", "POST", JSONObject()
                        .put("platform", platform)
                        .put("content_category", topic)
                        .put("title", title)
                        .put("caption", caption)
                        .put("duration_seconds", duration.toDoubleOrNull() ?: 30.0)).asObject()
                } catch (e: Exception) { notice = e.message ?: "Could not score." }
                finally { busy = false }
            }
        }, enabled = !busy && (title.isNotBlank() || caption.isNotBlank()), modifier = Modifier.fillMaxWidth()) {
            Text("Score with my real model")
        }
        OutlinedButton(onClick = {
            scope.launch {
                busy = true
                try {
                    hookResult = api.request("/coach/voice/hooks", "POST", JSONObject()
                        .put("topic", topic).put("count", 5)).asObject()
                } catch (e: Exception) { notice = e.message ?: "Hooks unavailable." }
                finally { busy = false }
            }
        }, enabled = !busy && topic.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("Get hooks in my own style")
        }
        if (notice.isNotBlank()) Text(notice, style = MaterialTheme.typography.bodyMedium)
    }
    result?.let {
        Spacer(Modifier.height(12.dp))
        SbCard { SectionEyebrow("CONTENT SCORING RESULT"); JsonEvidence(it) }
    }
    hookResult?.let {
        Spacer(Modifier.height(12.dp))
        SbCard { SectionEyebrow("ACTUAL VOICE-MODEL HOOKS"); JsonEvidence(it) }
    }
}
