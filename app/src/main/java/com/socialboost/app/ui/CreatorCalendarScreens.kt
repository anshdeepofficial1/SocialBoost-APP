package com.socialboost.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.socialboost.app.core.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CreateScreen(api: SocialApi, navigate: (Destination) -> Unit) {
    val scope = rememberCoroutineScope()
    var topic by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("instagram") }
    var goal by remember { mutableStateOf("educate") }
    var caption by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var variants by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var modelNote by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    Headline("CREATE STUDIO · EARLY ACCESS", "Make every post\nintentional.", "Develop hooks and captions from your own trained creator voice. Server video rendering is not implemented in the current SocialBoost backend.")
    EditorialHero("CREATIVE WORKSPACE", "IDEA → EVIDENCE\n→ BETTER CONTENT.", "Use your account’s trained voice and save selected concepts, without pretending an MP4 already exists.")
    Spacer(Modifier.height(16.dp))
    SbCard {
        SectionEyebrow("01 / YOUR CREATIVE BRIEF")
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            topic, { topic = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Topic or idea") }, minLines = 2, maxLines = 4
        )
        Spacer(Modifier.height(8.dp))
        SelectPlatform(platform) { platform = it }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(goal, { goal = it }, label = { Text("Goal / creative direction") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(9.dp))
        Button(
            onClick = {
                busy = true
                modelNote = ""
                message = ""
                scope.launch {
                    try {
                        val res = api.request(
                            "/coach/voice/write", "POST",
                            JSONObject().put("topic", topic.trim() + ". Objective: " + goal.trim())
                                .put("platform", platform).put("kind", "caption").put("count", 3)
                        ).asObject()
                        variants = res.optJSONArray("variants")?.let { array ->
                            (0 until array.length()).mapNotNull { array.optJSONObject(it) }
                        } ?: emptyList()
                        modelNote = res.value("note", "")
                        if (!res.optBoolean("available", false)) {
                            message = "The trained voice model is not available for this workspace."
                        }
                    } catch (e: Exception) { message = e.message ?: "Could not generate variants." }
                    finally { busy = false }
                }
            },
            enabled = topic.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text(if (busy) "Working…" else "Request 3 real voice variants") }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(12.dp))
    if (variants.isNotEmpty()) {
        SectionEyebrow("02 / SELECT ONE VERSION")
        Spacer(Modifier.height(10.dp))
        variants.forEachIndexed { index, variant ->
            SbCard {
                SectionEyebrow("OPTION " + (index + 1))
                Text(variant.value("text"), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { caption = variant.optString("text"); title = topic.take(180) }) {
                    Text("Use this in my draft")
                }
            }
            Spacer(Modifier.height(9.dp))
        }
    }
    if (modelNote.isNotBlank()) {
        Text(modelNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f))
        Spacer(Modifier.height(10.dp))
    }
    SbCard {
        SectionEyebrow("03 / EDIT AND SAVE")
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(title, { title = it.take(200) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(caption, { caption = it.take(2200) }, label = { Text("Caption / script") }, minLines = 4, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Button(onClick = {
            busy = true
            message = ""
            scope.launch {
                try {
                    val body = JSONObject()
                        .put("platform", platform)
                        .put("title", title)
                        .put("caption", caption)
                        .put("description", "Creative goal: " + goal + "\nTopic: " + topic)
                    api.request("/drafts", "POST", body)
                    message = "Saved to your real SocialBoost workspace."
                } catch (e: Exception) { message = e.message ?: "Could not save draft." }
                finally { busy = false }
            }
        }, enabled = (title.isNotBlank() || caption.isNotBlank()) && !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Save actual draft")
        }
        TextButton(onClick = { navigate(Destination.DRAFTS) }) { Text("View saved drafts") }
    }
    Spacer(Modifier.height(11.dp))
    MessagePanel(
        "Video generation is not live",
        "The shared backend has no verified render-job endpoints or independent worker yet. SocialBoost will not run video generation on your phone, create fake MP4s, or charge a provider without your permission. Drafts and model-backed text creation are available now."
    )
}

@Composable
fun SelectPlatform(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("instagram" to "Instagram Reels", "youtube" to "YouTube Shorts").forEach { (id, label) ->
            FilterChip(selected = selected == id, onClick = { onSelect(id) }, label = { Text(label) })
        }
    }
}

@Composable
fun CalendarScreen(api: SocialApi) {
    val scope = rememberCoroutineScope()
    var platform by remember { mutableStateOf("instagram") }
    var whenLocal by remember {
        mutableStateOf(LocalDateTime.now().plusDays(1).withHour(19).withMinute(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")))
    }
    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<JSONObject?>(null) }
    Headline("CONTENT CALENDAR", "One clear timeline.", "Current backend scheduling is sandbox only. Nothing is described as published without a provider confirmation.")
    SbCard {
        SectionEyebrow(if (editItem == null) "NEW SCHEDULE" else "EDIT SCHEDULE")
        Spacer(Modifier.height(10.dp))
        SelectPlatform(platform) { platform = it }
        OutlinedTextField(
            whenLocal, { whenLocal = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Local date/time: YYYY-MM-DDTHH:MM") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(title, { title = it.take(200) }, modifier = Modifier.fillMaxWidth(), label = { Text("Title") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(caption, { caption = it.take(2200) }, modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("Caption") })
        Spacer(Modifier.height(7.dp))
        Text("Sandbox: this is a scheduled plan, not a real social-media post.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = {
            busy = true
            message = ""
            scope.launch {
                try {
                    val iso = LocalDateTime.parse(whenLocal).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()
                    val payload = JSONObject().put("platform", platform).put("scheduled_at", iso)
                        .put("caption", caption).put("title", title).put("is_sandbox", true)
                    if (editItem == null) api.request("/calendar/schedule", "POST", payload)
                    else api.request("/calendar/" + editItem!!.optString("id"), "PUT", payload)
                    message = "Sandbox schedule saved in your workspace."
                    editItem = null
                    revision++
                } catch (e: Exception) { message = e.message ?: "Could not schedule." }
                finally { busy = false }
            }
        }, enabled = !busy && whenLocal.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(if (editItem == null) "Save schedule" else "Save changes")
        }
        if (editItem != null) TextButton(onClick = { editItem = null }) { Text("Cancel edit") }
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(13.dp))
    RemotePanel("calendar_" + revision, load = { api.request("/calendar/events") }) { data, refresh ->
        val rows = data.asRows()
        SectionEyebrow("SCHEDULED ITEMS · " + rows.size)
        Spacer(Modifier.height(9.dp))
        if (rows.isEmpty()) MessagePanel("Nothing scheduled", "Add a sandbox release to see it in the calendar.")
        rows.forEach { event ->
            SbCard {
                Text(event.value("title", "Untitled release"), style = MaterialTheme.typography.titleMedium)
                DataRow("Platform", event.value("platform"))
                DataRow("Scheduled", event.value("scheduled_at"))
                DataRow("Actual status", event.value("status") + if (event.optBoolean("is_sandbox")) " · sandbox" else "")
                if (event.has("last_error") && !event.isNull("last_error")) DataRow("Failure reason", event.value("last_error"))
                Row {
                    if (event.value("status") == "scheduled") {
                        TextButton(onClick = {
                            editItem = event
                            platform = event.optString("platform", "instagram")
                            title = event.optString("title")
                            caption = event.optString("caption")
                            whenLocal = try {
                                LocalDateTime.parse(event.optString("scheduled_at").substring(0, 19)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                            } catch (_: Exception) { whenLocal }
                        }) { Text("Edit") }
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    api.request("/calendar/" + event.optString("id") + "/cancel", "POST")
                                    revision++
                                } catch (e: Exception) { message = e.message ?: "Cancel failed." }
                            }
                        }) { Text("Cancel") }
                    }
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                api.request("/calendar/" + event.optString("id"), "DELETE")
                                revision++
                            } catch (e: Exception) { message = e.message ?: "Delete failed." }
                        }
                    }) { Text("Delete") }
                }
            }
            Spacer(Modifier.height(9.dp))
        }
        TextButton(onClick = refresh) { Text("Refresh timeline") }
    }
}
