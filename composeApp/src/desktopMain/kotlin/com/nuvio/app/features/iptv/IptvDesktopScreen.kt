package com.nuvio.app.features.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.player.PlatformPlayerSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Live TV for the desktop app. Uses the existing Nuvio macOS player. */
@Composable
internal fun IptvDesktopScreen(onBack: () -> Unit) {
    val sources = remember { mutableStateListOf<IptvSource>().apply { addAll(IptvSourceStore.load()) } }
    val scope = rememberCoroutineScope()
    var guide by remember { mutableStateOf(IptvGuide()) }
    var loading by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf("All") }
    var selectedKey by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var showAddSource by remember { mutableStateOf(false) }
    var playbackUrl by remember { mutableStateOf<String?>(null) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    fun refresh() {
        if (loading) return
        scope.launch {
            loading = true
            guide = IptvLoader.load(sources.toList())
            if (selectedKey !in guide.channels.map { it.key }) {
                selectedKey = guide.channels.firstOrNull()?.key
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { if (sources.isNotEmpty()) refresh() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }
    val selected = guide.channels.firstOrNull { it.key == selectedKey }
    val programs = selected?.let { guide.programs[it.key] }.orEmpty()
    val currentProgram = programs.firstOrNull { it.isLive(now) }
    val groups = remember(guide.channels) { listOf("All") + guide.channels.map { it.group }.distinct().sorted() }
    val filtered = remember(guide.channels, selectedGroup, search) {
        guide.channels.filter { channel ->
            (selectedGroup == "All" || channel.group == selectedGroup) &&
                (search.isBlank() || channel.name.contains(search, ignoreCase = true))
        }
    }

    Surface(color = Color(0xFF101014), contentColor = Color.White) {
      Row(Modifier.fillMaxSize()) {
        Column(Modifier.width(330.dp).fillMaxHeight().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) { Text("← Nuvio") }
                Text("Live TV", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { refresh() }, enabled = !loading && sources.isNotEmpty()) { Text(if (loading) "Loading…" else "Refresh") }
            }
            if (sources.isEmpty()) {
                Text("Add an M3U playlist or Xtream account to see your channels and guide.", color = Color.LightGray)
            }
            Button(onClick = { showAddSource = true }, modifier = Modifier.fillMaxWidth()) { Text("Add source") }
            if (sources.isNotEmpty()) {
                sources.forEach { source ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(source.name, modifier = Modifier.weight(1f), maxLines = 1)
                        TextButton(onClick = {
                            sources.remove(source)
                            IptvSourceStore.save(sources)
                            refresh()
                        }) { Text("Remove") }
                    }
                }
            }
            if (guide.errors.isNotEmpty()) {
                Text(guide.errors.joinToString("\n"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Find channel") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                groups.forEach { group ->
                    FilterChip(selected = selectedGroup == group, onClick = { selectedGroup = group }, label = { Text(group) })
                }
            }
            Text("${filtered.size} channels", style = MaterialTheme.typography.labelMedium, color = Color.LightGray)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.key }) { channel ->
                    val live = guide.programs[channel.key]?.firstOrNull { it.isLive(now) }
                    Column(
                        Modifier.fillMaxWidth()
                            .background(if (selectedKey == channel.key) Color(0xFF2A2A38) else Color.Transparent)
                            .clickable {
                                selectedKey = channel.key
                                playbackUrl = channel.url
                                playbackError = null
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text("${channel.number}. ${channel.name}", maxLines = 1, fontWeight = if (selectedKey == channel.key) FontWeight.Bold else FontWeight.Normal)
                        Text(live?.title ?: channel.group, style = MaterialTheme.typography.bodySmall, color = Color.LightGray, maxLines = 1)
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(selected?.name ?: "Select a channel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(390.dp).background(Color.Black), contentAlignment = Alignment.Center) {
                val url = playbackUrl
                if (url != null && selected != null) {
                    key(url) {
                        PlatformPlayerSurface(
                            sourceUrl = url,
                            sourceHeaders = selected.headers,
                            modifier = Modifier.fillMaxSize(),
                            useNativeController = true,
                            onControllerReady = {},
                            onSnapshot = {},
                            onError = { playbackError = it },
                        )
                    }
                } else {
                    Text("Choose a channel to play", color = Color.LightGray)
                }
            }
            playbackError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (selected?.drm != null) {
                Text("This channel uses DRM; support depends on the macOS player.", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (selected != null) {
                    OutlinedButton(onClick = { playbackUrl = selected.url }) { Text("Watch live") }
                }
                if (selected != null && currentProgram != null && CatchupUrlBuilder.isAvailable(selected.catchup, currentProgram.startMs, now)) {
                    OutlinedButton(onClick = {
                        playbackUrl = CatchupUrlBuilder.build(
                            selected.url, selected.catchup!!, currentProgram.startMs, currentProgram.stopMs,
                        ) ?: selected.url
                    }) { Text("Watch from start") }
                }
                Text(currentProgram?.title ?: "No guide data", maxLines = 1)
            }
            HorizontalDivider()
            Text("Guide", style = MaterialTheme.typography.titleLarge)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val visiblePrograms = programs.filter { it.stopMs > now - 86_400_000 && it.startMs < now + 2 * 86_400_000 }
                items(visiblePrograms) { program ->
                    val archiveUrl = selected?.catchup?.takeIf { CatchupUrlBuilder.isAvailable(it, program.startMs, now) }
                        ?.let { catchup -> CatchupUrlBuilder.build(selected.url, catchup, program.startMs, program.stopMs) }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable(enabled = archiveUrl != null) { playbackUrl = archiveUrl }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(time(program.startMs), modifier = Modifier.width(88.dp), color = Color.LightGray)
                        Column(Modifier.weight(1f)) {
                            Text(program.title, fontWeight = if (program.isLive(now)) FontWeight.Bold else FontWeight.Normal)
                            program.description?.takeIf { it.isNotBlank() }?.let { Text(it, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = Color.LightGray) }
                        }
                        if (archiveUrl != null) Text("Replay", style = MaterialTheme.typography.labelSmall)
                    }
                    HorizontalDivider()
                }
            }
        }
      }
    }

    if (showAddSource) {
        AddSourceDialog(
            onDismiss = { showAddSource = false },
            onAdd = { source ->
                sources += source
                IptvSourceStore.save(sources)
                showAddSource = false
                refresh()
            },
        )
    }
}

@Composable
private fun AddSourceDialog(onDismiss: () -> Unit, onAdd: (IptvSource) -> Unit) {
    var kind by remember { mutableStateOf("m3u") }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val valid = url.startsWith("https://") || url.startsWith("http://") || (kind == "m3u" && url.startsWith("file://"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add IPTV source") },
        text = {
            Column(Modifier.width(470.dp).height(400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kind == "m3u", onClick = { kind = "m3u" }, label = { Text("M3U playlist") })
                    FilterChip(selected = kind == "xtream", onClick = { kind = "xtream" }, label = { Text("Xtream") })
                }
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(url, { url = it }, label = { Text(if (kind == "xtream") "Server URL" else "Playlist URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (kind == "xtream") {
                    OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(epgUrl, { epgUrl = it }, label = { Text("XMLTV guide URL (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Sources are saved privately on this Mac. Add only streams you can access.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(IptvSource(UUID.randomUUID().toString(), name.ifBlank { "Playlist" }, url.trim(), epgUrl.trim(), kind, username.trim(), password))
                },
                enabled = valid && (kind != "xtream" || (username.isNotBlank() && password.isNotBlank())),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun time(timestamp: Long): String = SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(timestamp))
