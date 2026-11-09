package com.socialboost.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialboost.app.R
import com.socialboost.app.core.*
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class Destination(val label: String) {
    HOME("Overview"), CREATE("Create"), CALENDAR("Calendar"),
    INSIGHTS("Insights"), LIBRARY("Library"), ANALYZE("Analyze"),
    HOOK("Hook Lab"), PREDICTIONS("Predictions"), GROWTH("Growth Coach"), PLATFORMS("Platforms"),
    YOUTUBE("YouTube Studio"), RECOMMENDATIONS("Recommendations"),
    DATASETS("Datasets"), ACCOUNTS("Accounts"), DRAFTS("Drafts"),
    SETTINGS("Settings"), AUTOPILOT("Autopilot")
}

@Composable
fun SocialBoostApp() {
    val context = LocalContext.current
    val session = remember { SecureSession(context) }
    val api = remember { SocialApi(context, session) }
    var loggedIn by remember { mutableStateOf(session.token() != null) }
    var account by remember { mutableStateOf<JSONObject?>(null) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var check by remember { mutableIntStateOf(0) }
    LaunchedEffect(loggedIn, check) {
        if (loggedIn) {
            try {
                account = api.request("/auth/me").asObject()
                connectionError = null
            } catch (e: ApiException) {
                if (e.code == 401) {
                    session.clear()
                    loggedIn = false
                    account = null
                } else connectionError = e.message
            } catch (e: Exception) {
                connectionError = e.message ?: "Could not reach SocialBoost."
            }
        }
    }
    if (!loggedIn) {
        Authentication(api, session) {
            account = null
            loggedIn = true
            check++
        }
    } else {
        StudioShell(api, account, connectionError, onRetry = { check++ }) {
            session.clear()
            account = null
            loggedIn = false
        }
    }
}

@Composable
private fun Authentication(api: SocialApi, session: SecureSession, onSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.Center
    ) {
        BrandHeader()
        Spacer(Modifier.height(45.dp))
        Text(if (register) "Start creating.\nThink bigger." else "Welcome\nback.", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(12.dp))
        Text("Your creator analytics, content workflow and real account insights. One workspace, on web and Android.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(26.dp))
        SbCard {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                if (register) {
                    OutlinedTextField(fullName, { fullName = it }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Button(
                    enabled = !busy && email.isNotBlank() && password.length >= 6,
                    onClick = {
                        scope.launch {
                            busy = true
                            error = ""
                            try {
                                val payload = JSONObject().put("email", email.trim())
                                    .put("password", password)
                                val route = if (register) "/auth/signup" else "/auth/login"
                                if (register) payload.put("full_name", fullName.trim())
                                val response = api.request(route, "POST", payload, withAuth = false).asObject()
                                val token = response.optString("access_token")
                                if (token.isBlank()) error = "Server did not return a valid session."
                                else {
                                    session.saveToken(token)
                                    onSuccess()
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Authentication failed."
                            } finally { busy = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(51.dp),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text(if (register) "Create your workspace" else "Sign in to studio")
                }
                TextButton(onClick = { register = !register; error = "" }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (register) "Already have an account? Sign in" else "New to SocialBoost? Create account")
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("No demo metrics. No paid model calls. Powered by your existing SocialBoost account.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f), fontSize = 11.sp)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StudioShell(
    api: SocialApi,
    account: JSONObject?,
    connectionError: String?,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
) {
    var page by remember { mutableStateOf(Destination.HOME) }
    var more by remember { mutableStateOf(false) }
    val tabs = listOf(Destination.HOME, Destination.CREATE, Destination.CALENDAR, Destination.INSIGHTS, Destination.LIBRARY)
    val context = LocalContext.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Row(
                Modifier.fillMaxWidth().height(62.dp).background(MaterialTheme.colorScheme.surface).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (page in tabs) BrandHeader()
                else Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { page = Destination.HOME }) { Icon(Icons.Default.ArrowBack, "Back to home") }
                    Text(page.label, style = MaterialTheme.typography.titleMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRetry) { Icon(Icons.Default.Refresh, "Refresh connection") }
                    IconButton(onClick = { more = true }) { Icon(Icons.Default.Menu, "More studio tools") }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val icons = listOf(Icons.Default.Home, Icons.Default.Edit, Icons.Default.DateRange, Icons.Default.BarChart, Icons.Default.VideoLibrary)
                tabs.forEachIndexed { i, item ->
                    NavigationBarItem(
                        selected = page == item,
                        onClick = { page = item },
                        icon = { Icon(icons[i], item.label) },
                        label = { Text(if (item == Destination.HOME) "Home" else item.label, fontSize = 10.sp) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp)) {
            if (connectionError != null) {
                SbCard {
                    Text("Backend unavailable: " + connectionError, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Try again") }
                }
                Spacer(Modifier.height(12.dp))
            }
            when (page) {
                Destination.HOME -> DashboardScreen(api, account) { page = it }
                Destination.CREATE -> CreateScreen(api) { page = it }
                Destination.CALENDAR -> CalendarScreen(api)
                Destination.INSIGHTS -> InsightsScreen(api) { page = it }
                Destination.LIBRARY -> LibraryScreen(api) { page = it }
                Destination.ANALYZE -> AnalyzeScreen(api) { page = it }
                Destination.HOOK -> HookLabScreen(api)
                Destination.PREDICTIONS -> PredictionsScreen(api)
                Destination.GROWTH -> GrowthCoachScreen(api)
                Destination.PLATFORMS -> PlatformsScreen(api)
                Destination.YOUTUBE -> YouTubeScreen(api)
                Destination.RECOMMENDATIONS -> RecommendationsScreen(api) { page = it }
                Destination.DATASETS -> DatasetsScreen(api)
                Destination.ACCOUNTS -> AccountsScreen(api)
                Destination.DRAFTS -> DraftsScreen(api)
                Destination.SETTINGS -> SettingsScreen(api)
                Destination.AUTOPILOT -> AutopilotScreen(api)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (more) {
        ModalBottomSheet(onDismissRequest = { more = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                SectionEyebrow("STUDIO TOOLS")
                Text(account?.optString("email") ?: "SocialBoost", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                val destinations = listOf(
                    Destination.ANALYZE, Destination.HOOK, Destination.PREDICTIONS, Destination.GROWTH,
                    Destination.PLATFORMS, Destination.YOUTUBE, Destination.RECOMMENDATIONS,
                    Destination.DATASETS, Destination.ACCOUNTS, Destination.DRAFTS,
                    Destination.AUTOPILOT, Destination.SETTINGS
                )
                destinations.forEach { dest ->
                    Row(
                        Modifier.fillMaxWidth().clickable { page = dest; more = false }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dest.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
                HorizontalDivider()
                TextButton(onClick = { more = false; onLogout() }) {
                    Icon(Icons.Default.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out")
                }
                TextButton(onClick = {
                    more = false
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://socialboostweb.vercel.app")))
                }) { Text("Open SocialBoost website") }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Image(painterResource(R.drawable.ic_brand_mark), contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(7.dp)))
        Text("SocialBoost", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
        Box(Modifier.clip(RoundedCornerShape(5.dp)).background(Signal.copy(alpha = .13f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text("AI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Signal)
        }
    }
}

@Composable
fun SectionEyebrow(label: String) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = Signal, letterSpacing = 1.2.sp)
}

@Composable
fun Headline(eyebrow: String, heading: String, detail: String? = null) {
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        SectionEyebrow(eyebrow)
        Spacer(Modifier.height(9.dp))
        Text(heading, style = MaterialTheme.typography.headlineMedium)
        if (detail != null) {
            Spacer(Modifier.height(9.dp))
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f))
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun SbCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(17.dp),
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(17.dp), content = content)
    }
}

@Composable
fun Metric(label: String, amount: String, hint: String? = null) {
    SbCard {
        SectionEyebrow(label.uppercase())
        Spacer(Modifier.height(9.dp))
        Text(amount, fontWeight = FontWeight.Bold, fontSize = 27.sp)
        if (hint != null) Text(hint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
    }
}

@Composable
fun DataRow(label: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun MessagePanel(title: String, content: String) {
    SbCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(7.dp))
        Text(content, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .75f), style = MaterialTheme.typography.bodyMedium)
    }
}

/** On-screen status reflects the API result, never a hard-coded success. */
@Composable
fun RemotePanel(
    key: String,
    load: suspend () -> Any?,
    content: @Composable (Any?, refresh: () -> Unit) -> Unit
) {
    var result by remember(key) { mutableStateOf<Any?>(null) }
    var loading by remember(key) { mutableStateOf(true) }
    var error by remember(key) { mutableStateOf<String?>(null) }
    var revision by remember(key) { mutableIntStateOf(0) }
    LaunchedEffect(key, revision) {
        loading = true
        try {
            result = load()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Could not load data."
        } finally { loading = false }
    }
    if (loading) {
        Row(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator(Modifier.size(25.dp))
            Spacer(Modifier.width(14.dp))
            Text("Reading your workspace…")
        }
    } else if (error != null) {
        MessagePanel("Couldn't load", error.orEmpty())
        TextButton(onClick = { revision++ }) { Text("Retry") }
    } else {
        content(result) { revision++ }
    }
}
