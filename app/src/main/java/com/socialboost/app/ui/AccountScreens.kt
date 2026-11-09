package com.socialboost.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.socialboost.app.core.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun AccountsScreen(api: SocialApi) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    var handle by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("instagram") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var disconnecting by remember { mutableStateOf<JSONObject?>(null) }
    Headline("CONNECTED ACCOUNTS", "Your channels.\nYour control.", "Real OAuth accounts can sync permitted statistics. Manual accounts are clearly marked sandbox and never claimed as live.")
    RemotePanel("accounts_" + revision, load = {
        JSONObject().put("accounts", api.request("/social/accounts"))
            .put("providers", api.request("/social/providers"))
    }) { data, refresh ->
        val root = data.asObject()
        val providers = root.optJSONArray("providers") ?: JSONArray()
        SbCard {
            SectionEyebrow("CONNECT A REAL ACCOUNT")
            for (i in 0 until providers.length()) {
                val provider = providers.optJSONObject(i) ?: continue
                val p = provider.optString("platform")
                val configured = provider.optBoolean("configured", false)
                DataRow(provider.value("label"), if (configured) "OAuth is configured on the server" else "OAuth is not configured; manual profile only")
                OutlinedButton(onClick = {
                    scope.launch {
                        busy = true
                        message = ""
                        try {
                            val auth = api.request("/social/oauth/" + p + "/start").asObject()
                            val url = auth.optString("authorize_url")
                            if (!url.startsWith("https://")) throw IllegalStateException("Provider did not supply an HTTPS consent link.")
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            message = "Finish consent in the browser, then return here and refresh."
                        } catch (e: Exception) { message = e.message ?: "Could not start OAuth." }
                        finally { busy = false }
                    }
                }, enabled = configured && !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Connect " + provider.value("label") + " via official OAuth")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        SbCard {
            SectionEyebrow("MANUAL / SANDBOX PROFILE")
            SelectPlatform(platform) { platform = it }
            OutlinedTextField(handle, { handle = it }, label = { Text("Account handle") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(displayName, { displayName = it }, label = { Text("Display name (optional)") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        message = ""
                        try {
                            api.request("/social/accounts", "POST", JSONObject().put("platform", platform)
                                .put("handle", handle.trim()).put("display_name", displayName.trim()).put("mode", "sandbox"))
                            message = "Manual profile saved. It is not a live OAuth connection."
                            handle = ""
                            revision++
                        } catch (e: Exception) { message = e.message ?: "Could not save profile." }
                        finally { busy = false }
                    }
                },
                enabled = handle.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()
            ) { Text("Add manual profile") }
        }
        Spacer(Modifier.height(12.dp))
        SectionEyebrow("YOUR ACCOUNT CONNECTIONS")
        val accounts = root.optJSONArray("accounts") ?: JSONArray()
        if (accounts.length() == 0) MessagePanel("No linked accounts", "Sign in with Instagram/YouTube or add a sandbox profile. We do not invent engagement statistics.")
        for (i in 0 until accounts.length()) {
            val acc = accounts.optJSONObject(i) ?: continue
            Spacer(Modifier.height(9.dp))
            SbCard {
                Text("@" + acc.value("handle"), style = MaterialTheme.typography.titleMedium)
                DataRow("Platform", acc.value("platform"))
                DataRow("Access", acc.value("mode") + " · " + acc.value("status"))
                DataRow("Followers", if (acc.optString("mode") == "oauth") acc.numberLabel("live_followers") else "No live count · manual")
                DataRow("Last sync", acc.value("last_synced_at"))
                DataRow("Evidence", acc.value("evidence"))
                Row {
                    if (acc.optString("mode") == "oauth") {
                        TextButton(onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    val updated = api.request("/social/accounts/" + acc.optString("id") + "/sync", "POST").asObject()
                                    message = updated.value("message", "Sync request completed. Review actual timestamps and metrics.")
                                    revision++
                                } catch (e: Exception) { message = e.message ?: "Sync failed." }
                                finally { busy = false }
                            }
                        }, enabled = !busy) { Text("Sync") }
                    }
                    TextButton(onClick = { disconnecting = acc }) { Text("Disconnect") }
                }
            }
        }
        TextButton(onClick = refresh) { Text("Refresh connection statuses") }
    }
    if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    disconnecting?.let { selected ->
        AlertDialog(
            onDismissRequest = { disconnecting = null },
            title = { Text("Disconnect this channel?") },
            text = { Text("Historical posts may remain in your SocialBoost workspace. This does not delete your social account.") },
            confirmButton = {
                TextButton(onClick = {
                    disconnecting = null
                    scope.launch {
                        try {
                            api.request("/social/accounts/" + selected.optString("id"), "DELETE")
                            revision++
                        } catch (e: Exception) { message = e.message ?: "Disconnect failed." }
                    }
                }) { Text("Disconnect") }
            },
            dismissButton = { TextButton(onClick = { disconnecting = null }) { Text("Keep account") } }
        )
    }
}

@Composable
fun DraftsScreen(api: SocialApi) {
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var platform by remember { mutableStateOf("instagram") }
    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var message by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<JSONObject?>(null) }
    Headline("DRAFT LIBRARY", "Made to improve.", "Save and edit drafts against the same workspace used by the website.")
    SbCard {
        SectionEyebrow(if (selected == null) "NEW DRAFT" else "EDITING DRAFT")
        SelectPlatform(platform) { platform = it }
        OutlinedTextField(title, { title = it.take(200) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(caption, { caption = it.take(2200) }, label = { Text("Caption") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(description, { description = it.take(5000) }, label = { Text("Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            scope.launch {
                try {
                    val body = JSONObject().put("platform", platform).put("title", title)
                        .put("caption", caption).put("description", description)
                    if (selected == null) api.request("/drafts", "POST", body)
                    else api.request("/drafts/" + selected!!.optString("id"), "PUT", body)
                    selected = null
                    title = ""; caption = ""; description = ""
                    message = "Draft saved to SocialBoost."
                    revision++
                } catch (e: Exception) { message = e.message ?: "Saving failed." }
            }
        }, enabled = title.isNotBlank() || caption.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (selected == null) "Save draft" else "Update draft") }
        if (selected != null) TextButton(onClick = { selected = null; title = ""; caption = ""; description = "" }) { Text("Cancel editing") }
        if (message.isNotBlank()) Text(message)
    }
    Spacer(Modifier.height(13.dp))
    RemotePanel("drafts_" + revision, load = { api.request("/drafts") }) { data, refresh ->
        val drafts = data.asRows()
        if (drafts.isEmpty()) MessagePanel("No drafts saved", "Create a hook or draft, then refine it here.")
        drafts.forEach { item ->
            SbCard {
                SectionEyebrow(item.value("platform").uppercase())
                Text(item.value("title", "Untitled"), style = MaterialTheme.typography.titleMedium)
                DataRow("Caption", item.value("caption"))
                DataRow("Updated", item.value("updated_at"))
                Row {
                    TextButton(onClick = {
                        selected = item
                        platform = item.optString("platform", "instagram")
                        title = item.optString("title")
                        caption = item.optString("caption")
                        description = item.optString("description")
                    }) { Text("Edit") }
                    TextButton(onClick = { deleting = item }) { Text("Delete") }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        TextButton(onClick = refresh) { Text("Refresh drafts") }
    }
    deleting?.let { item ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete draft?") },
            text = { Text("This will permanently delete this draft from your shared workspace.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch {
                        try { api.request("/drafts/" + item.optString("id"), "DELETE"); revision++ }
                        catch (e: Exception) { message = e.message ?: "Delete failed." }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
}

@Composable
fun SettingsScreen(api: SocialApi, onThemeChanged: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var preferences by remember { mutableStateOf<JSONObject?>(null) }
    var audience by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf("Asia/Kolkata") }
    var dark by remember { mutableStateOf(false) }
    var instagram by remember { mutableStateOf(true) }
    var youtube by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf("") }
    Headline("STUDIO SETTINGS", "Your workspace.", "Preferences save to the existing SocialBoost account and apply to the website.")
    RemotePanel("settings_" + revision, load = { api.request("/settings/preferences") }) { data, refresh ->
        val p = data.asObject()
        LaunchedEffect(revision) {
            if (preferences == null || preferences != p) {
                preferences = p
                audience = p.optString("target_audience")
                timezone = p.optString("timezone", "Asia/Kolkata")
                dark = p.optString("theme") == "dark"
                instagram = p.optString("platforms", "instagram,youtube").contains("instagram")
                youtube = p.optString("platforms", "instagram,youtube").contains("youtube")
            }
        }
        SbCard {
            SectionEyebrow("PREFERENCES")
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(audience, { audience = it.take(300) }, label = { Text("Target audience") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(timezone, { timezone = it }, label = { Text("IANA timezone") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row { Checkbox(checked = instagram, onCheckedChange = { instagram = it }); Text("Instagram", modifier = Modifier.padding(top = 12.dp)) }
            Row { Checkbox(checked = youtube, onCheckedChange = { youtube = it }); Text("YouTube", modifier = Modifier.padding(top = 12.dp)) }
            Row { Checkbox(checked = dark, onCheckedChange = { dark = it }); Text("Prefer dark theme on website", modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(9.dp))
            Button(onClick = {
                scope.launch {
                    try {
                        api.request("/settings/preferences", "PUT", JSONObject()
                            .put("timezone", timezone.trim()).put("target_audience", audience)
                            .put("theme", if (dark) "dark" else "light")
                            .put("platforms", listOfNotNull("instagram".takeIf { instagram }, "youtube".takeIf { youtube }).joinToString(",")))
                        notice = "Preferences saved."
                        onThemeChanged(dark)
                        preferences = null
                        revision++
                    } catch (e: Exception) { notice = e.message ?: "Could not save." }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save preferences") }
            if (notice.isNotBlank()) Text(notice)
        }
        TextButton(onClick = refresh) { Text("Reload preferences") }
    }
}

@Composable
fun AutopilotScreen(api: SocialApi) {
    Headline("AUTOPILOT · ROADMAP", "Automation,\nwith control.", "The existing SocialBoost API currently has sandbox scheduling, not live automated publishing or a server render worker.")
    MessagePanel(
        "Availability: not implemented in shared backend",
        "Autopilot modes, real post approvals, remote video generation and background publishing require new verified backend endpoints, durable jobs and official platform authorization. There is no fake toggle or hidden paid API."
    )
    Spacer(Modifier.height(12.dp))
    SbCard {
        SectionEyebrow("APPROVAL MODEL")
        DataRow("Default", "Draft and manual review")
        DataRow("Scheduling today", "Sandbox; not posted to Instagram or YouTube")
        DataRow("Paid generation", "Disabled until funded, licensed and explicitly enabled")
        DataRow("Device role", "Control and review only; never render on Android")
    }
}
