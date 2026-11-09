package com.socialboost.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.socialboost.app.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun DashboardScreen(api: SocialApi, account: JSONObject?, navigate: (Destination) -> Unit) {
    Headline("YOUR STUDIO", "Create smarter.\nGrow from evidence.", "Welcome " + (account?.optString("full_name")?.takeIf { it.isNotBlank() } ?: "creator") + ". Real channel numbers, real history, one workspace.")
    EditorialHero("CREATOR INTELLIGENCE", "READ THE SIGNAL.\nMAKE THE NEXT MOVE.", "One clear view of your own content performance. No synthetic numbers.")
    Spacer(Modifier.height(16.dp))
    RemotePanel("home", load = {
        JSONObject()
            .put("summary", api.request("/analytics/summary").asObject())
            .put("accounts", org.json.JSONArray().apply {
                api.request("/social/accounts").asRows().forEach { put(it) }
            })
    }) { data, refresh ->
        val root = data.asObject()
        val sum = root.optJSONObject("summary") ?: JSONObject()
        val a = root.optJSONArray("accounts")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { Metric("Uploaded videos", sum.numberLabel("videos")) }
            Box(Modifier.weight(1f)) { Metric("History posts", sum.numberLabel("historical_posts")) }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { Metric("Total views", sum.numberLabel("total_views")) }
            Box(Modifier.weight(1f)) { Metric("Analyses", sum.numberLabel("analyses_completed")) }
        }
        Spacer(Modifier.height(18.dp))
        SbCard {
            SectionEyebrow("CONNECTED CHANNELS")
            Spacer(Modifier.height(10.dp))
            if (a == null || a.length() == 0) {
                Text("No channels linked yet. Connect a real account or import your own history.", style = MaterialTheme.typography.bodyMedium)
            } else {
                for (i in 0 until a.length()) {
                    val profile = a.optJSONObject(i) ?: continue
                    DataRow(profile.value("platform").uppercase() + " · @" + profile.value("handle"), profile.value("mode") + " / " + profile.value("status"))
                }
            }
            TextButton(onClick = { navigate(Destination.ACCOUNTS) }) { Text("Manage accounts") }
        }
        Spacer(Modifier.height(13.dp))
        OutlinedButton(onClick = refresh, modifier = Modifier.fillMaxWidth()) { Text("Refresh real data") }
    }
    Spacer(Modifier.height(17.dp))
    SectionEyebrow("QUICK ACTIONS")
    Spacer(Modifier.height(9.dp))
    Button(
        onClick = { navigate(Destination.ANALYZE) },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp)
    ) { Text("Analyze a video") }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { navigate(Destination.CREATE) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text("Open creator workspace")
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { navigate(Destination.RECOMMENDATIONS) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text("Review growth recommendations")
    }
}

@Composable
fun InsightsScreen(api: SocialApi, navigate: (Destination) -> Unit) {
    Headline("REAL ANALYTICS", "Know your numbers.", "Everything below comes from actual imported or provider-synced posts. Missing metrics remain unavailable.")
    RemotePanel("insights", load = {
        JSONObject().put("summary", api.request("/analytics/summary").asObject())
            .put("posts", org.json.JSONArray().apply { api.request("/analytics/posts?limit=25").asRows().forEach { put(it) } })
    }) { value, refresh ->
        val root = value.asObject()
        val summary = root.optJSONObject("summary") ?: JSONObject()
        val byPlatform = summary.optJSONObject("by_platform") ?: JSONObject()
        Metric("All-time views", summary.numberLabel("total_views"), summary.value("source", "No source data"))
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { Metric("Likes", summary.numberLabel("total_likes")) }
            Box(Modifier.weight(1f)) { Metric("Reach", summary.numberLabel("total_reach")) }
        }
        Spacer(Modifier.height(14.dp))
        listOf("instagram", "youtube").forEach { platform ->
            val p = byPlatform.optJSONObject(platform) ?: JSONObject()
            SbCard {
                SectionEyebrow(platform.uppercase())
                DataRow("Posts", p.numberLabel("posts"))
                DataRow("Views", p.numberLabel("views"))
                DataRow("Engagement rate", if (p.isNull("engagement_rate") || !p.has("engagement_rate")) "Unavailable"
                    else "%.2f%%".format(java.util.Locale.US, p.optDouble("engagement_rate") * 100.0))
                DataRow("Average views", p.numberLabel("avg_views"))
            }
            Spacer(Modifier.height(10.dp))
        }
        val posts = root.optJSONArray("posts")
        SbCard {
            SectionEyebrow("RECENT POSTS")
            if (posts == null || posts.length() == 0) Text("No actual post history yet. Connect an account or import your own CSV.")
            else for (i in 0 until posts.length()) {
                val post = posts.optJSONObject(i) ?: continue
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(post.value("title", post.value("caption", "Untitled post")).take(90), fontWeight = FontWeight.SemiBold)
                DataRow(post.value("platform"), "Views " + post.numberLabel("views") + " · " + post.value("published_at"))
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { navigate(Destination.PREDICTIONS) }, modifier = Modifier.fillMaxWidth()) { Text("Train and inspect prediction models") }
        TextButton(onClick = refresh) { Text("Refresh insights") }
    }
}

@Composable
fun LibraryScreen(api: SocialApi, navigate: (Destination) -> Unit) {
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<JSONObject?>(null) }
    Headline("MEDIA LIBRARY", "Your uploads.", "Only files from your authenticated SocialBoost workspace appear here.")
    if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.primary)
    RemotePanel("library_" + revision, load = { api.request("/videos") }) { data, refresh ->
        val rows = data.asRows()
        if (rows.isEmpty()) MessagePanel("No videos yet", "Upload a video to run an evidence-based analysis. New uploads stay in your web and Android workspace.")
        rows.forEach { item ->
            SbCard {
                Text(item.value("original_name"), style = MaterialTheme.typography.titleMedium)
                DataRow("Format", item.value("aspect_ratio") + " · " + item.value("duration_seconds") + " sec")
                DataRow("Uploaded", item.value("created_at"))
                Row {
                    TextButton(onClick = { navigate(Destination.ANALYZE) }) { Text("View analysis") }
                    TextButton(onClick = { deleting = item }) { Text("Delete") }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        TextButton(onClick = refresh) { Text("Refresh library") }
    }
    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this video?") },
            text = { Text("The video and its analysis will be deleted from SocialBoost. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = item.optString("id")
                    deleting = null
                    scope.launch {
                        try {
                            api.request("/videos/" + id, "DELETE")
                            notice = "Video deleted."
                            revision++
                        } catch (e: Exception) { notice = e.message ?: "Could not delete." }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AnalyzeScreen(api: SocialApi, navigate: (Destination) -> Unit) {
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedUri = uri
    }
    AnalyzeContent(
        api = api,
        selectedUri = selectedUri,
        onSelectedClear = { selectedUri = null },
        openPicker = { picker.launch(arrayOf("video/*")) },
        navigate = navigate
    )
}

@Composable
private fun AnalyzeContent(
    api: SocialApi,
    selectedUri: android.net.Uri?,
    onSelectedClear: () -> Unit,
    openPicker: () -> Unit,
    navigate: (Destination) -> Unit
) {
    val scope = rememberCoroutineScope()
    var uploadJobId by remember { mutableStateOf<String?>(null) }
    var activeVideo by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    Headline("VIDEO ANALYZER", "See the signal.", "Upload MP4, MOV, WebM or MKV. The existing server performs real technical, hook and metadata analysis.")
    SbCard {
        Text("Upload video", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = openPicker, modifier = Modifier.fillMaxWidth()) { Text("Choose a video") }
        if (selectedUri != null) Text("Selected: " + selectedUri.toString().substringAfterLast('/').take(75), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val uri = selectedUri ?: return@Button
                busy = true
                error = ""
                scope.launch {
                    try {
                        val result = api.upload("/videos/upload", uri).asObject()
                        activeVideo = result.optString("video_id")
                        uploadJobId = result.optString("job_id")
                        revision++
                        onSelectedClear()
                    } catch (e: Exception) { error = e.message ?: "Upload failed." }
                    finally { busy = false }
                }
            },
            enabled = selectedUri != null && !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Uploading…" else "Upload and analyze") }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(13.dp))
    LaunchedEffect(uploadJobId) {
        val id = uploadJobId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        while (true) {
            try {
                val result = api.request("/analysis/job/" + id).asObject()
                job = result
                if (result.optString("status") in listOf("done", "failed", "cancelled")) break
                delay(2500)
            } catch (e: Exception) {
                error = e.message ?: "Unable to read analysis progress."
                break
            }
        }
    }
    job?.let { result ->
        SbCard {
            SectionEyebrow("SERVER ANALYSIS")
            Text("Status: " + result.value("status"), style = MaterialTheme.typography.titleMedium)
            val progress = result.optInt("progress", 0).coerceIn(0, 100)
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
            DataRow("Current step", result.value("current_step"))
            if (result.optString("status") == "failed") DataRow("Error", result.value("error"))
            if (result.optString("status") == "done") {
                Text("Report", style = MaterialTheme.typography.titleMedium)
                ReportDetails(result.optJSONObject("result"))
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    SectionEyebrow("PREVIOUS UPLOADS")
    Spacer(Modifier.height(8.dp))
    RemotePanel("analyze_library_" + revision, load = { api.request("/videos") }) { data, refresh ->
        val rows = data.asRows()
        if (rows.isEmpty()) Text("No uploads yet.")
        rows.forEach { video ->
            SbCard {
                Text(video.value("original_name"), style = MaterialTheme.typography.titleMedium)
                DataRow("Duration", video.value("duration_seconds") + " seconds")
                TextButton(onClick = {
                    activeVideo = video.optString("id")
                    uploadJobId = null
                    scope.launch {
                        try {
                            job = api.request("/analysis/video/" + activeVideo).asObject()
                            error = ""
                        } catch (e: Exception) { error = e.message ?: "Report not available." }
                    }
                }) { Text("Open report") }
            }
            Spacer(Modifier.height(8.dp))
        }
        TextButton(onClick = refresh) { Text("Refresh uploads") }
    }
}

@Composable
private fun ReportDetails(result: JSONObject?) {
    if (result == null) {
        Text("No completed result was returned.")
        return
    }
    listOf("technical", "hook", "metadata_analysis", "recommendations").forEach { key ->
        Text(key.replace('_', ' ').uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(result.opt(key)?.toString()?.take(2500) ?: "Unavailable", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(9.dp))
    }
}
