package com.socialboost.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.socialboost.app.core.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

@Composable
fun HookLabScreen(api: SocialApi) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("instagram") }
    var result by remember { mutableStateOf<JSONObject?>(null) }
    var captionResult by remember { mutableStateOf<JSONObject?>(null) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    Headline("HOOK LAB", "Win the first seconds.", "The original SocialBoost Hook Lab uses transparent rules, not a trained retention prediction.")
    SbCard {
        SelectPlatform(platform) { platform = it }
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(text, { text = it.take(600) }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("Opening line / caption") })
        Button(onClick = {
            scope.launch {
                busy = true
                note = ""
                try {
                    result = api.request("/analysis/hook", "POST", JSONObject().put("hook_text", text).put("duration", 3)).asObject()
                    captionResult = api.request("/analysis/caption", "POST", JSONObject().put("caption", text).put("platform", platform).put("kind", "caption")).asObject()
                } catch (e: Exception) { note = e.message ?: "Analysis failed." }
                finally { busy = false }
            }
        }, enabled = text.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Measuring…" else "Analyze real text") }
        OutlinedButton(onClick = {
            scope.launch {
                busy = true
                note = ""
                try {
                    api.request("/drafts", "POST", JSONObject().put("platform", platform).put("caption", if (platform == "instagram") text else "").put("title", if (platform == "youtube") text.take(200) else ""))
                    note = "Saved as a real draft."
                } catch (e: Exception) { note = e.message ?: "Could not save." }
                finally { busy = false }
            }
        }, enabled = text.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) { Text("Save to drafts") }
        if (note.isNotBlank()) Text(note, color = MaterialTheme.colorScheme.primary)
    }
    Spacer(Modifier.height(12.dp))
    result?.let {
        SbCard {
            SectionEyebrow("HEURISTIC HOOK RESULTS")
            JsonEvidence(it)
        }
    }
    if (captionResult != null) {
        Spacer(Modifier.height(12.dp))
        SbCard {
            SectionEyebrow("CAPTION EVIDENCE")
            JsonEvidence(captionResult!!)
        }
    }
}

@Composable
fun PredictionsScreen(api: SocialApi) {
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var platform by remember { mutableStateOf("instagram") }
    var caption by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("25") }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var prediction by remember { mutableStateOf<JSONObject?>(null) }
    Headline("PREDICTIONS & GROWTH", "Estimates, not promises.", "The prediction model requires real posts with known view counts. The backend decides if enough history exists.")
    RemotePanel("prediction_status_" + revision, load = {
        JSONObject().put("model", api.request("/predictions/model"))
            .put("coach", api.request("/coach/status"))
            .put("history", api.request("/predictions?limit=8"))
    }) { data, refresh ->
        val root = data.asObject()
        SbCard {
            SectionEyebrow("MODEL READINESS")
            val model = root.optJSONObject("model") ?: JSONObject()
            val coach = root.optJSONObject("coach") ?: JSONObject()
            DataRow("Views model", if (model.optBoolean("available")) "Ready" else "Not trained")
            DataRow("Growth Coach", if (coach.optBoolean("available")) "Ready" else "Not trained")
            DataRow("Available training samples", coach.numberLabel("n_samples_available"))
            TextButton(onClick = {
                scope.launch {
                    busy = true
                    try {
                        val response = api.request("/predictions/train", "POST").asObject()
                        note = response.value("message", response.value("status"))
                        revision++
                    } catch (e: Exception) { note = e.message ?: "Training failed." }
                    finally { busy = false }
                }
            }, enabled = !busy) { Text("Train on real history") }
        }
        Spacer(Modifier.height(10.dp))
        val history = root.optJSONArray("history") ?: JSONArray()
        if (history.length() > 0) {
            SbCard {
                SectionEyebrow("RECENT PREDICTIONS")
                for (i in 0 until history.length()) {
                    val item = history.optJSONObject(i) ?: continue
                    DataRow(item.value("platform") + " · " + item.value("created_at"), item.value("category") + " · expected views: " + item.numberLabel("expected_views_low") + " – " + item.numberLabel("expected_views_high"))
                }
            }
        }
        TextButton(onClick = refresh) { Text("Reload model status") }
    }
    Spacer(Modifier.height(13.dp))
    SbCard {
        SectionEyebrow("PREDICT FROM CONTENT")
        SelectPlatform(platform) { platform = it }
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(caption, { caption = it }, label = { Text("Caption") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(duration, { duration = it }, label = { Text("Duration (seconds)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            scope.launch {
                busy = true
                note = ""
                try {
                    val seconds = duration.toDoubleOrNull() ?: throw IllegalArgumentException("Enter a number of seconds.")
                    prediction = api.request(
                        "/predictions/run", "POST", JSONObject()
                            .put("platform", platform).put("title", title).put("caption", caption)
                            .put("duration_seconds", seconds)
                    ).asObject()
                    revision++
                } catch (e: Exception) { note = e.message ?: "Prediction failed." }
                finally { busy = false }
            }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Run workspace model") }
        if (note.isNotBlank()) Text(note)
    }
    prediction?.let {
        Spacer(Modifier.height(11.dp))
        SbCard { SectionEyebrow("BACKEND PREDICTION"); JsonEvidence(it) }
    }
}

@Composable
fun PlatformsScreen(api: SocialApi) {
    Headline("PLATFORM COMPARISON", "One edit.\nTwo audiences.", "Compare actual post histories on Instagram and YouTube, without filling gaps with guesses.")
    RemotePanel("platform_comparison", load = { api.request("/analytics/summary") }) { data, refresh ->
        val root = data.asObject()
        val byPlatform = root.optJSONObject("by_platform") ?: JSONObject()
        listOf("instagram", "youtube").forEach { p ->
            val stats = byPlatform.optJSONObject(p) ?: JSONObject()
            SbCard {
                SectionEyebrow(p.uppercase())
                DataRow("Posts", stats.numberLabel("posts"))
                DataRow("Known views", stats.numberLabel("views"))
                DataRow("Average views", stats.numberLabel("avg_views"))
                DataRow("Shares", stats.numberLabel("shares"))
                DataRow("Saves", stats.numberLabel("saves"))
                DataRow("Reach", stats.numberLabel("reach"))
            }
            Spacer(Modifier.height(10.dp))
        }
        TextButton(onClick = refresh) { Text("Refresh comparison") }
    }
}

@Composable
fun YouTubeScreen(api: SocialApi) {
    var channel by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    Headline("YOUTUBE STUDIO", "Know the channel.", "Read public channel information through the SocialBoost server. The server needs YouTube API configuration or an authorized Google account.")
    SbCard {
        OutlinedTextField(channel, { channel = it.trim().take(250) }, modifier = Modifier.fillMaxWidth(), label = { Text("@handle, URL or channel ID") })
        Spacer(Modifier.height(8.dp))
        Button(onClick = { query = channel }, modifier = Modifier.fillMaxWidth()) { Text("Look up channel") }
    }
    Spacer(Modifier.height(13.dp))
    if (query.isNotBlank()) {
        RemotePanel("youtube_" + query, load = {
            api.request("/youtube/dashboard?channel=" + URLEncoder.encode(query, "UTF-8") + "&limit=10")
        }) { data, refresh ->
            SbCard { SectionEyebrow("LIVE YOUTUBE RESPONSE"); JsonEvidence(data.asObject()) }
            TextButton(onClick = refresh) { Text("Refresh results") }
        }
    } else MessagePanel("Enter a channel", "YouTube channel insights are retrieved only when you request them.")
}

@Composable
fun RecommendationsScreen(api: SocialApi, navigate: (Destination) -> Unit) {
    Headline("EVIDENCE-BASED ACTIONS", "Improve with context.", "Recommendations reflect what has actually been collected in your workspace.")
    RemotePanel("recommendations", load = {
        JSONObject().put("summary", api.request("/analytics/summary"))
            .put("model", api.request("/predictions/model"))
            .put("accounts", api.request("/social/accounts"))
    }) { data, refresh ->
        val root = data.asObject()
        val summary = root.optJSONObject("summary") ?: JSONObject()
        val model = root.optJSONObject("model") ?: JSONObject()
        val accounts = root.optJSONArray("accounts") ?: JSONArray()
        if (accounts.length() == 0) {
            MessagePanel("Connect a channel", "No linked provider profile exists yet. Connect Instagram or YouTube to bring real post history into SocialBoost.")
            TextButton(onClick = { navigate(Destination.ACCOUNTS) }) { Text("Open accounts") }
        }
        if (summary.optInt("historical_posts", 0) == 0) {
            Spacer(Modifier.height(10.dp))
            MessagePanel("Add post history", "Growth recommendations and performance models require actual historical post metrics. Import a CSV or sync a connected account.")
            TextButton(onClick = { navigate(Destination.DATASETS) }) { Text("Import your CSV") }
        }
        if (summary.optInt("videos", 0) == 0) {
            Spacer(Modifier.height(10.dp))
            MessagePanel("Analyze your opening", "Upload a real video to measure its visual and technical properties and receive concrete analysis results.")
            TextButton(onClick = { navigate(Destination.ANALYZE) }) { Text("Analyze a video") }
        }
        if (!model.optBoolean("available")) {
            Spacer(Modifier.height(10.dp))
            MessagePanel("Train your prediction model", "Your workspace does not currently have a usable trained view model. Predictions are not reliable without sufficient real examples.")
            TextButton(onClick = { navigate(Destination.PREDICTIONS) }) { Text("Open predictions") }
        }
        if (summary.optInt("videos", 0) > 0 && summary.optInt("historical_posts", 0) > 0) {
            Spacer(Modifier.height(10.dp))
            MessagePanel("Inspect available evidence", "Your workspace contains uploaded videos and real post history. Review their analysis reports and performance models for supported, specific recommendations.")
            TextButton(onClick = { navigate(Destination.INSIGHTS) }) { Text("Open insights") }
        }
        TextButton(onClick = refresh) { Text("Refresh recommendations") }
    }
}

@Composable
fun DatasetsScreen(api: SocialApi) {
    val scope = rememberCoroutineScope()
    var uploadResult by remember { mutableStateOf<JSONObject?>(null) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var file by remember { mutableStateOf<Uri?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { file = it }
    Headline("DATASETS", "Train on reality.", "Import your real CSV post history. Invalid rows and duplicates are reported by the server.")
    SbCard {
        Text("History CSV", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(7.dp))
        OutlinedButton(onClick = { pick.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Select CSV")
        }
        if (file != null) DataRow("Selected file", file.toString())
        Button(onClick = {
            val uri = file ?: return@Button
            busy = true
            scope.launch {
                try {
                    uploadResult = api.upload("/datasets/import", uri).asObject()
                    file = null
                    revision++
                    message = "Import completed; see exact row counts below."
                } catch (e: Exception) { message = e.message ?: "Import failed." }
                finally { busy = false }
            }
        }, enabled = !busy && file != null, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Importing…" else "Import historical posts") }
        if (message.isNotBlank()) Text(message)
    }
    uploadResult?.let { SbCard { SectionEyebrow("IMPORT REPORT"); JsonEvidence(it) } }
    Spacer(Modifier.height(12.dp))
    RemotePanel("imports_" + revision, load = { api.request("/datasets/imports") }) { data, refresh ->
        val rows = data.asRows()
        SectionEyebrow("PAST IMPORTS")
        if (rows.isEmpty()) MessagePanel("No imports", "Your account has no CSV import history yet.")
        rows.forEach { item ->
            Spacer(Modifier.height(8.dp))
            SbCard {
                Text(item.value("filename"), style = MaterialTheme.typography.titleMedium)
                DataRow("Accepted rows", item.numberLabel("rows_imported"))
                DataRow("Rejected rows", item.numberLabel("rows_rejected"))
                DataRow("Date", item.value("created_at"))
            }
        }
        TextButton(onClick = refresh) { Text("Refresh imports") }
    }
}

@Composable
fun JsonEvidence(result: JSONObject) {
    val keys = result.keys().asSequence().take(35).toList()
    if (keys.isEmpty()) Text("No data returned by the server.")
    keys.forEach { key ->
        val raw = result.opt(key)
        val detail = when (raw) {
            is JSONObject, is JSONArray -> raw.toString(2).take(1800)
            null, JSONObject.NULL -> "Unavailable"
            else -> raw.toString().take(1800)
        }
        DataRow(key.replace('_', ' ').replaceFirstChar { it.uppercase() }, detail)
    }
}
