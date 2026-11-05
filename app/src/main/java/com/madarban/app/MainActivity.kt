package com.madarban.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max

private val Ink = Color(0xFF102322)
private val Green = Color(0xFF17B890)
private val Mint = Color(0xFFE8F7F1)
private val Muted = Color(0xFF71817D)
private val Surface = Color(0xFFF7F9F7)

/** Local-only Electruck starter: no account, cloud, OCR, or AI connection. */
data class VehicleProject(
    val id: String = UUID.randomUUID().toString(),
    val car: String,
    val brand: String,
    val year: String,
    val system: String,
    val notes: String = "",
    val mapUri: String? = null,
    val mapMime: String? = null,
    val verified: Boolean = false,
    val codes: List<String> = emptyList()
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ElectruckApp(this) }
    }
}

private fun loadProjects(activity: ComponentActivity): List<VehicleProject> = runCatching {
    val raw = activity.getSharedPreferences("electruck_local", 0).getString("projects", "[]") ?: "[]"
    val array = JSONArray(raw)
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        VehicleProject(
            id = o.optString("id", UUID.randomUUID().toString()),
            car = o.optString("car"), brand = o.optString("brand"), year = o.optString("year"),
            system = o.optString("system"), notes = o.optString("notes"),
            mapUri = o.optString("mapUri").takeIf { it.isNotBlank() },
            mapMime = o.optString("mapMime").takeIf { it.isNotBlank() },
            verified = o.optBoolean("verified"),
            codes = o.optJSONArray("codes")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()
        )
    }
}.getOrDefault(emptyList())

private fun saveProjects(activity: ComponentActivity, projects: List<VehicleProject>) {
    val array = JSONArray()
    projects.forEach { p ->
        val codes = JSONArray().also { a -> p.codes.forEach { code -> a.put(code) } }
        array.put(JSONObject().apply {
            put("id", p.id); put("car", p.car); put("brand", p.brand); put("year", p.year)
            put("system", p.system); put("notes", p.notes); put("mapUri", p.mapUri ?: "")
            put("mapMime", p.mapMime ?: ""); put("verified", p.verified); put("codes", codes)
        })
    }
    activity.getSharedPreferences("electruck_local", 0).edit().putString("projects", array.toString()).apply()
}

@Composable
fun ElectruckApp(activity: ComponentActivity) {
    val projects = remember { mutableStateListOf<VehicleProject>().apply { addAll(loadProjects(activity)) } }
    var page by remember { mutableIntStateOf(0) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var dark by remember { mutableStateOf(activity.getSharedPreferences("electruck_local", 0).getBoolean("dark", false)) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun persist() = saveProjects(activity, projects)
    fun updateProject(updated: VehicleProject) {
        val index = projects.indexOfFirst { it.id == updated.id }
        if (index >= 0) projects[index] = updated
        persist()
    }

    val mapPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null && selectedId != null) {
            runCatching { activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val mime = activity.contentResolver.getType(uri) ?: "application/octet-stream"
            projects.firstOrNull { it.id == selectedId }?.let { updateProject(it.copy(mapUri = uri.toString(), mapMime = mime, verified = false)) }
            notice = "نقشه به‌صورت محلی به پروژه پیوست شد؛ هنوز تأیید نشده است."
        }
    }

    val scheme = if (dark) darkColorScheme(primary = Green, background = Color(0xFF101917), surface = Color.White, onSurface = Ink)
    else lightColorScheme(primary = Green, background = Surface, surface = Color.White)

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    val labels = listOf("خانه", "پروژه‌ها", "نقشهٔ پروژه", "تنظیمات")
                    val icons = listOf(Icons.Default.Home, Icons.Default.FolderOpen, Icons.Default.Hub, Icons.Default.Settings)
                    labels.forEachIndexed { i, label ->
                        NavigationBarItem(selected = page == i, onClick = { page = i }, icon = { Icon(icons[i], null) },
                            label = { Text(label, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Green, selectedTextColor = Green, indicatorColor = Mint))
                    }
                }
            },
            snackbarHost = { SnackbarHost(remember { SnackbarHostState() }) }
        ) { pad ->
            when (page) {
                0 -> HomeScreen(Modifier.padding(pad), projects, onNew = { showNew = true }, onProjects = { page = 1 }, onOpen = { id -> selectedId = id; page = 2 })
                1 -> ProjectsScreen(Modifier.padding(pad), projects, onNew = { showNew = true }, onOpen = { id -> selectedId = id; page = 2 }, onDelete = { id -> projects.removeAll { it.id == id }; persist() })
                2 -> AnalysisScreen(Modifier.padding(pad), projects.firstOrNull { it.id == selectedId }, onPickMap = { id -> selectedId = id; mapPicker.launch(arrayOf("image/*", "application/pdf")) }, onUpdate = ::updateProject, onNotice = { notice = it })
                else -> SettingsScreen(Modifier.padding(pad), dark = dark, onDark = { dark = it; activity.getSharedPreferences("electruck_local", 0).edit().putBoolean("dark", it).apply() })
            }
            if (showNew) NewProjectDialog(onDismiss = { showNew = false }, onSave = { project -> projects.add(0, project); persist(); selectedId = project.id; showNew = false; page = 2; notice = "پروژه روی همین گوشی ذخیره شد." })
            notice?.let { message -> AlertDialog(onDismissRequest = { notice = null }, text = { Text(message) }, confirmButton = { TextButton(onClick = { notice = null }) { Text("باشه") } }) }
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier, projects: List<VehicleProject>, onNew: () -> Unit, onProjects: () -> Unit, onOpen: (String) -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column { Text("فضای خصوصی متخصص", color = Muted, fontSize = 13.sp); Text("کارگاه Electruck", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                Box(Modifier.size(46.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) { Text("E", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
        }
        item { HeroCard(onNew) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("پروژه‌های محلی", projects.size.toString(), "روی همین گوشی", Icons.Default.Folder, Modifier.weight(1f))
                StatCard("نقشه‌های پیوست", projects.count { it.mapUri != null }.toString(), "نیازمند بازبینی", Icons.Default.Bolt, Modifier.weight(1f))
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("پروژه‌های اخیر", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("همه ←", color = Green, modifier = Modifier.clickable { onProjects() }) } }
        if (projects.isEmpty()) item { EmptyProjects(onNew) }
        items(projects.take(3), key = { it.id }) { project -> ProjectCard(project, onClick = { onOpen(project.id) }) }
        item { LocalOnlyNotice() }
    }
}

@Composable
private fun HeroCard(onNew: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF102D2B)), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            CircuitArt(Modifier.align(Alignment.CenterEnd).width(150.dp).height(145.dp))
            Column(Modifier.padding(20.dp).fillMaxWidth(.82f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = Color(0x3322C99A), shape = RoundedCornerShape(50)) { Text("آفلاین · فقط برای شما", color = Color(0xFF8BE3C4), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) }
                Text("آرشیو نقشه‌های\nقابل اعتماد خودت را بساز", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 29.sp)
                Text("پروژه بساز، نقشه را اضافه کن و اطلاعات تأییدشده را ثبت کن.", fontSize = 12.sp, color = Color(0xFFC6D7D1), lineHeight = 18.sp)
                Button(onClick = onNew, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Ink), shape = RoundedCornerShape(13.dp)) { Icon(Icons.Default.Add, null); Text("ساخت پروژه", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun CircuitArt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Color(0xFF1FC99A).copy(alpha = .42f); val x = size.width * .2f; val y = size.height * .2f
        drawLine(c, Offset(x, y), Offset(x + 65, y), 3f, cap = StrokeCap.Round)
        drawLine(c, Offset(x + 65, y), Offset(x + 65, y + 43), 3f)
        drawLine(c, Offset(x + 65, y + 43), Offset(x + 125, y + 43), 3f)
        drawLine(c, Offset(x + 65, y + 43), Offset(x + 65, y + 92), 3f)
        listOf(Offset(x, y), Offset(x + 65, y), Offset(x + 65, y + 43), Offset(x + 125, y + 43), Offset(x + 65, y + 92)).forEach { drawCircle(c, 6f, it) }
    }
}

@Composable
private fun StatCard(title: String, value: String, foot: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, color = Muted, fontSize = 11.sp); Icon(icon, null, tint = Green, modifier = Modifier.size(18.dp)) }
            Text(value, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(foot, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ProjectCard(project: VehicleProject, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Mint), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = Green, modifier = Modifier.size(25.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(project.car, fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
                Text("${project.brand} · ${project.year} · ${project.system}", color = Muted, fontSize = 10.sp, maxLines = 1)
                Text(if (project.mapUri != null) "نقشه پیوست شده${if (project.verified) " · تأییدشده" else " · بررسی‌نشده"}" else "هنوز نقشه‌ای پیوست نشده", color = if (project.verified) Green else Muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = Muted)
        }
    }
}

@Composable
private fun EmptyProjects(onNew: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Default.FolderOpen, null, tint = Green, modifier = Modifier.size(34.dp)); Text("هنوز پروژه‌ای نساخته‌ای", color = Ink, fontWeight = FontWeight.Bold)
            Text("اولین خودرو و مدار را به آرشیوت اضافه کن.", color = Muted, fontSize = 12.sp)
            TextButton(onClick = onNew) { Text("ساخت اولین پروژه") }
        }
    }
}

@Composable
private fun LocalOnlyNotice() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Mint).padding(13.dp)) {
        Icon(Icons.Default.Lock, null, tint = Green, modifier = Modifier.size(19.dp))
        Text("اطلاعات فقط روی این گوشی ذخیره می‌شود؛ فضای ابری و کاربران فعلاً غیرفعال‌اند.", fontSize = 11.sp, color = Ink)
    }
}

@Composable
private fun ProjectsScreen(modifier: Modifier, projects: List<VehicleProject>, onNew: () -> Unit, onOpen: (String) -> Unit, onDelete: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<VehicleProject?>(null) }
    val filtered = projects.filter { "${it.car} ${it.brand} ${it.year} ${it.system}".contains(search.trim(), ignoreCase = true) }
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("آرشیو کارگاه", color = Ink, fontWeight = FontWeight.Bold, fontSize = 23.sp); Text("${projects.size} پروژه · ذخیره‌شده روی دستگاه", color = Muted, fontSize = 11.sp) }
            FilledTonalIconButton(onClick = onNew) { Icon(Icons.Default.Add, "پروژه جدید", tint = Green) }
        }
        Spacer(Modifier.height(13.dp))
        OutlinedTextField(value = search, onValueChange = { search = it }, placeholder = { Text("جست‌وجوی خودرو یا مدار…", fontSize = 12.sp) }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true)
        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty()) EmptyProjects(onNew) else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            items(filtered, key = { it.id }) { project ->
                Box {
                    ProjectCard(project) { onOpen(project.id) }
                    IconButton(onClick = { deleting = project }, modifier = Modifier.align(Alignment.TopStart).size(36.dp)) { Icon(Icons.Default.DeleteOutline, "حذف پروژه", tint = Muted, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
    deleting?.let { project -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("حذف پروژه؟") }, text = { Text("«${project.car}» از آرشیو همین گوشی حذف می‌شود.") }, confirmButton = { TextButton(onClick = { onDelete(project.id); deleting = null }) { Text("حذف", color = Color(0xFFC34D45)) } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("انصراف") } }) }
}

@Composable
private fun NewProjectDialog(onDismiss: () -> Unit, onSave: (VehicleProject) -> Unit) {
    var car by remember { mutableStateOf("") }; var brand by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }; var system by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("پروژهٔ خودرو") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedTextField(car, { car = it }, label = { Text("نام خودرو *") }, placeholder = { Text("مثلاً پژو ۲۰۶ تیپ ۵") }, singleLine = true)
            OutlinedTextField(brand, { brand = it }, label = { Text("برند") }, placeholder = { Text("مثلاً Peugeot") }, singleLine = true)
            OutlinedTextField(year, { year = it }, label = { Text("سال ساخت") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), singleLine = true)
            OutlinedTextField(system, { system = it }, label = { Text("سیستم یا مدار") }, placeholder = { Text("مثلاً ECU و رله دوبل") }, singleLine = true)
        } }, confirmButton = { TextButton(enabled = car.isNotBlank(), onClick = { onSave(VehicleProject(car = car.trim(), brand = brand.trim().ifBlank { "نامشخص" }, year = year.trim().ifBlank { "نامشخص" }, system = system.trim().ifBlank { "ثبت‌نشده" })) }) { Text("ذخیرهٔ محلی") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable
private fun AnalysisScreen(modifier: Modifier, project: VehicleProject?, onPickMap: (String) -> Unit, onUpdate: (VehicleProject) -> Unit, onNotice: (String) -> Unit) {
    var code by remember(project?.id) { mutableStateOf("") }
    var notes by remember(project?.id) { mutableStateOf(project?.notes.orEmpty()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("میز نقشهٔ پروژه", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(project?.let { "${it.car} · ${it.system}" } ?: "اول از بخش پروژه‌ها یک خودرو انتخاب کن", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(13.dp))
        if (project == null) {
            EmptyProjects { onNotice("برای شروع، از تب پروژه‌ها یک پروژهٔ خودرو بسازید.") }
        } else {
            if (project.mapUri == null) {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF102D2B)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFF8BE3C4), modifier = Modifier.size(35.dp))
                        Text("نقشه‌ای به این پروژه پیوست نشده", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("فقط نقشه‌ای را وارد کن که منبع و خودرویش را تأیید کرده‌ای.", color = Color(0xFFC6D7D1), fontSize = 11.sp)
                        Button(onClick = { onPickMap(project.id) }, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Ink)) { Text("انتخاب تصویر یا PDF") }
                    }
                }
            } else {
                MapPreview(uri = Uri.parse(project.mapUri), mime = project.mapMime, context = context)
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (project.verified) "✓ تأییدشده توسط متخصص" else "در انتظار بازبینی شما", color = if (project.verified) Green else Color(0xFFB67C24), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    TextButton(onClick = { onPickMap(project.id) }) { Text("جایگزینی فایل") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = project.verified, onCheckedChange = { onUpdate(project.copy(verified = it)) })
                    Text("منبع و ارتباط این نقشه با خودرو را خودم بررسی کرده‌ام", fontSize = 10.sp, color = Muted)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("کدها و یادداشت‌های متخصص", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("دستی وارد می‌شوند؛ هیچ تشخیص خودکاری انجام نمی‌شود.", color = Muted, fontSize = 10.sp)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = code, onValueChange = { code = it }, modifier = Modifier.weight(1f), placeholder = { Text("مثلاً F 43 15A — فیوز ۴۳، ۱۵ آمپر", fontSize = 10.sp) }, singleLine = true, shape = RoundedCornerShape(12.dp))
                IconButton(onClick = { if (code.isNotBlank()) { onUpdate(project.copy(codes = project.codes + code.trim())); code = "" } }) { Icon(Icons.Default.AddCircle, "افزودن کد", tint = Green) }
            }
            project.codes.forEachIndexed { i, item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(start = 8.dp, end = 12.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, null, tint = Green, modifier = Modifier.size(18.dp)); Text(item, Modifier.weight(1f).padding(horizontal = 8.dp), fontSize = 11.sp, color = Ink)
                    IconButton(onClick = { onUpdate(project.copy(codes = project.codes.filterIndexed { index, _ -> index != i })) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Close, "حذف کد", tint = Muted, modifier = Modifier.size(16.dp)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("یادداشت فنی") }, placeholder = { Text("منبع نقشه، علائم، اندازه‌گیری‌ها…") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, shape = RoundedCornerShape(14.dp))
            Button(onClick = { onUpdate(project.copy(notes = notes)); onNotice("یادداشت در همین گوشی ذخیره شد.") }, modifier = Modifier.align(Alignment.End), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("ذخیرهٔ یادداشت") }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6E8)), shape = RoundedCornerShape(13.dp), modifier = Modifier.padding(bottom = 18.dp)) { Text("این برنامه نقشه را تحلیل یا صحت‌سنجی نمی‌کند. کدها و توضیحات را خودت با منبع فنی معتبر تطبیق بده.", color = Color(0xFF8B6429), fontSize = 10.sp, lineHeight = 16.sp, modifier = Modifier.padding(12.dp)) }
        }
    }
}

@Composable
private fun MapPreview(uri: Uri, mime: String?, context: android.content.Context) {
    if (mime == "application/pdf") {
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().height(210.dp).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.PictureAsPdf, null, tint = Color(0xFFC9544D), modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(8.dp)); Text("فایل PDF به پروژه پیوست شد", color = Ink, fontWeight = FontWeight.Bold)
                Text("پیش‌نمایش داخلی PDF در مرحلهٔ بعد اضافه می‌شود.", color = Muted, fontSize = 10.sp)
                TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }.onFailure { } }) { Text("بازکردن با برنامهٔ PDF گوشی") }
            }
        }
    } else {
        val bitmap by produceState<Bitmap?>(initialValue = null, key1 = uri) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = context.contentResolver
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    val sample = max(1, max(bounds.outWidth / 1800, bounds.outHeight / 1800))
                    val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtMost(8) }
                    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                }.getOrNull()
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF0F4F2)), contentAlignment = Alignment.Center) {
                if (bitmap != null) {
                    var scale by remember(uri) { mutableFloatStateOf(1f) }; var pan by remember(uri) { mutableStateOf(Offset.Zero) }
                    androidx.compose.foundation.Image(bitmap!!.asImageBitmap(), contentDescription = "نقشهٔ واردشده", modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = pan.x; translationY = pan.y }.pointerInput(uri) { detectTransformGestures { _, delta, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 5f); pan += delta } })
                    Text("برای بزرگ‌نمایی دو انگشت را باز کن", modifier = Modifier.align(Alignment.BottomCenter).padding(7.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = .85f)).padding(horizontal = 9.dp, vertical = 5.dp), fontSize = 9.sp, color = Muted)
                } else CircularProgressIndicator(color = Green)
            }
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier, dark: Boolean, onDark: (Boolean) -> Unit) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(8.dp)); Text("تنظیمات", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("نسخهٔ محلی و خصوصی", color = Muted, fontSize = 13.sp)
        SettingRow(Icons.Default.DarkMode, "تم تیره", "راحتی کار در محیط کم‌نور") { Switch(checked = dark, onCheckedChange = onDark) }
        SettingRow(Icons.Default.Translate, "زبان برنامه", "فعلاً فارسی") { Text("فارسی", color = Green, fontSize = 12.sp) }
        SettingRow(Icons.Default.CloudOff, "فضای ابری", "در این نسخه غیرفعال است") { Icon(Icons.Default.Lock, null, tint = Muted) }
        SettingRow(Icons.Default.GroupOff, "حساب کاربران", "فقط استفادهٔ شخصی روی این گوشی") { Icon(Icons.Default.Lock, null, tint = Muted) }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Mint)) {
            Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, null, tint = Green)
                Column { Text("نسخهٔ متخصص", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("نقشه‌ها و پروژه‌ها فقط روی دستگاه می‌مانند.", color = Muted, fontSize = 10.sp) }
            }
        }
        Text("Electruck · نسخهٔ اولیهٔ آفلاین", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Mint), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Green, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f)) { Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, fontSize = 10.sp) }; trailing()
    }
}
