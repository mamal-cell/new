package com.madarban.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF102322)
private val Green = Color(0xFF17B890)
private val Mint = Color(0xFFE8F7F1)
private val Muted = Color(0xFF71817D)
private val Line = Color(0xFFE7ECE9)
private val Surface = Color(0xFFF7F9F7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ElectruckApp() }
    }
}

@Composable
fun ElectruckApp() {
    var page by remember { mutableStateOf(0) }
    var showNew by remember { mutableStateOf(false) }
    var showProject by remember { mutableStateOf(false) }
    var dark by remember { mutableStateOf(false) }
    val scheme = if (dark) darkColorScheme(primary = Green, background = Color(0xFF101917), surface = Color(0xFF182320)) else lightColorScheme(primary = Green, background = Surface, surface = Color.White)
    MaterialTheme(colorScheme = scheme) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                val labels = listOf("خانه", "پروژه‌ها", "تحلیل", "تنظیمات")
                val icons = listOf(Icons.Default.Home, Icons.Default.FolderOpen, Icons.Default.Hub, Icons.Default.Settings)
                labels.forEachIndexed { i, label -> NavigationBarItem(selected = page == i, onClick = { page = i }, icon = { Icon(icons[i], null) }, label = { Text(label, fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Green, selectedTextColor = Green, indicatorColor = Mint)) }
            }
        }) { pad ->
            when (page) {
                0 -> HomeScreen(Modifier.padding(pad), onNew = { showNew = true }, onProject = { showProject = true }, onAll = { page = 1 })
                1 -> ProjectsScreen(Modifier.padding(pad), onProject = { showProject = true }, onNew = { showNew = true })
                2 -> AnalysisScreen(Modifier.padding(pad))
                else -> SettingsScreen(Modifier.padding(pad), dark = dark, onDark = { dark = it })
            }
        }
        if (showNew) AlertDialog(onDismissRequest = { showNew = false }, title = { Text("پروژهٔ تازه") }, text = { Text("در نسخهٔ بعدی، انتخاب خودرو و بارگذاری نقشه به این مرحله اضافه می‌شود.") }, confirmButton = { TextButton(onClick = { showNew = false; page = 1 }) { Text("متوجه شدم") } }, icon = { Icon(Icons.Default.AddCircle, null, tint = Green) })
        if (showProject) ProjectDialog(onDismiss = { showProject = false })
    }
}

@Composable
fun HomeScreen(modifier: Modifier, onNew: () -> Unit, onProject: () -> Unit, onAll: () -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("سلام، استاد 👋", color = Muted, fontSize = 14.sp)
                    Text("کارگاه دیجیتال شما", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.size(46.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) { Text("م", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
        }
        item { HeroCard(onNew) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("پروژه‌ها", "۱۲", "۳ فعال", Icons.Default.Folder, Modifier.weight(1f))
                StatCard("نقشه‌های تحلیل‌شده", "۳۸", "این ماه +۶", Icons.Default.Bolt, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ادامهٔ کار", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("همهٔ پروژه‌ها  ←", color = Green, fontSize = 13.sp, modifier = Modifier.clickable { onAll() })
            }
        }
        item { ProjectCard("پژو ۲۰۶ تیپ ۵", "۱۳۹۸  •  سیستم انژکتور", "مدار ECU و رله دوبل", "آخرین ویرایش · امروز", Color(0xFFEAF2FF), Icons.Default.DirectionsCar, onProject) }
        item { ProjectCard("سمند EF7", "۱۳۹۵  •  دوگانه‌سوز", "مدار فن خنک‌کننده", "آخرین ویرایش · دیروز", Color(0xFFFFF2E5), Icons.Default.Air, onProject) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFEFF5F2)).padding(14.dp)) {
                Icon(Icons.Default.VerifiedUser, null, tint = Green, modifier = Modifier.size(20.dp))
                Text("فضای تخصصی شما · دسترسی کاربران فقط با تأیید شما", fontSize = 12.sp, color = Ink, maxLines = 2)
            }
        }
    }
}

@Composable
fun HeroCard(onNew: () -> Unit) {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF102D2B)), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            CircuitArt(Modifier.align(Alignment.CenterEnd).width(180.dp).height(160.dp))
            Column(Modifier.padding(22.dp).fillMaxWidth(0.78f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = Color(0x3322C99A), shape = RoundedCornerShape(50)) { Text("دستیار تخصصی برق خودرو", color = Color(0xFF8BE3C4), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)) }
                Text("نقشه را زنده کن.", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("نقشهٔ جدید را وارد کن و تحلیل مدار را از همان‌جا شروع کن.", fontSize = 13.sp, color = Color(0xFFC6D7D1), lineHeight = 19.sp)
                Button(onClick = onNew, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Ink), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("شروع پروژهٔ جدید", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CircuitArt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Color(0xFF1FC99A).copy(alpha = .42f)
        val x = size.width * .25f; val y = size.height * .2f
        drawLine(c, Offset(x, y), Offset(x + 80, y), 3f, cap = StrokeCap.Round)
        drawLine(c, Offset(x + 80, y), Offset(x + 80, y + 47), 3f)
        drawLine(c, Offset(x + 80, y + 47), Offset(x + 145, y + 47), 3f)
        drawLine(c, Offset(x + 80, y + 47), Offset(x + 80, y + 103), 3f)
        drawLine(c, Offset(x + 80, y + 103), Offset(x + 130, y + 103), 3f)
        listOf(Offset(x, y), Offset(x + 80, y), Offset(x + 80, y + 47), Offset(x + 145, y + 47), Offset(x + 80, y + 103), Offset(x + 130, y + 103)).forEach { drawCircle(c, 7f, it) }
    }
}

@Composable
fun StatCard(title: String, value: String, foot: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title, color = Muted, fontSize = 12.sp); Icon(icon, null, tint = Green, modifier = Modifier.size(18.dp)) }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(value, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold); Text(foot, color = Green, fontSize = 10.sp, modifier = Modifier.padding(bottom = 5.dp)) }
        }
    }
}

@Composable
fun ProjectCard(name: String, meta: String, circuit: String, updated: String, tint: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(tint), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Ink, modifier = Modifier.size(27.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp)
                Text(meta, color = Muted, fontSize = 11.sp)
                Text(circuit, color = Green, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(updated, color = Muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = Muted)
        }
    }
}

@Composable
fun ProjectsScreen(modifier: Modifier, onProject: () -> Unit, onNew: () -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("آرشیو کارگاه", color = Ink, fontWeight = FontWeight.Bold, fontSize = 24.sp); Text("پروژه‌های خودرو و نقشه‌ها", color = Muted, fontSize = 13.sp) }; FilledTonalIconButton(onClick = onNew) { Icon(Icons.Default.Add, "پروژهٔ جدید", tint = Green) } }
        Spacer(Modifier.height(18.dp)); OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("جست‌وجوی خودرو یا مدار…", fontSize = 13.sp) }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true)
        Spacer(Modifier.height(14.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("همهٔ پروژه‌ها ۱۲", "ایرانی ۸", "خارجی ۴").forEachIndexed { i, t -> FilterChip(selected = i == 0, onClick = {}, label = { Text(t, fontSize = 11.sp) }) } }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) { item { ProjectCard("پژو ۲۰۶ تیپ ۵", "۱۳۹۸ • سیستم انژکتور", "مدار ECU و رله دوبل", "امروز", Color(0xFFEAF2FF), Icons.Default.DirectionsCar, onProject) }; item { ProjectCard("سمند EF7", "۱۳۹۵ • دوگانه‌سوز", "مدار فن خنک‌کننده", "دیروز", Color(0xFFFFF2E5), Icons.Default.Air, onProject) }; item { ProjectCard("پراید ۱۳۱", "۱۳۹۲ • انژکتوری", "مدار شارژ و دینام", "۲ روز پیش", Color(0xFFEAF7EE), Icons.Default.BatteryChargingFull, onProject) }; item { ProjectCard("Toyota Hilux", "2019 • 2.8L Diesel", "Glow plug & ECU", "هفتهٔ پیش", Color(0xFFF0ECFF), Icons.Default.LocalShipping, onProject) } }
    }
}

@Composable
fun AnalysisScreen(modifier: Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp)); Text("میز تحلیل", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("نمایش مدار، کدها و یادداشت‌های فنی", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp)); Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFECEFEC)).padding(4.dp)) { listOf("نقشه", "کدخوان", "توضیحات").forEachIndexed { i, s -> Box(Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if (tab == i) Color.White else Color.Transparent).clickable { tab = i }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text(s, color = if (tab == i) Green else Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } } }
        Spacer(Modifier.height(16.dp))
        if (tab == 0) {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF102D2B))) { Box(Modifier.fillMaxWidth().height(230.dp)) { CircuitArt(Modifier.fillMaxSize()); Column(Modifier.align(Alignment.TopStart).padding(18.dp)) { Text("PEUGEOT 206 · ECU", color = Color.White, fontWeight = FontWeight.Bold); Text("نقشهٔ نمایشی مدار", color = Color(0xFF9EB6AE), fontSize = 12.sp) }; Surface(Modifier.align(Alignment.BottomEnd).padding(12.dp), color = Color(0xFF1B3D38), shape = RoundedCornerShape(12.dp)) { Text("＋   −   ⤢", color = Color.White, modifier = Modifier.padding(12.dp)) } } }
            Spacer(Modifier.height(15.dp)); Text("اجزای شناسایی‌شده", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(9.dp)); CodeRow("F 43", "فیوز شمارهٔ ۴۳", "15A", Green); CodeRow("R2", "رلهٔ دوبل", "۱۲V", Color(0xFF497CF3)); CodeRow("GND", "اتصال بدنه", "زمین", Color(0xFFEF9B38))
        } else if (tab == 1) {
            Spacer(Modifier.height(12.dp)); CodeRow("F 43 15A", "فیوز شمارهٔ ۴۳ · ۱۵ آمپر", "تأیید نشده", Green); Spacer(Modifier.height(9.dp)); CodeRow("R2", "رلهٔ دوبل انژکتور", "۱۲V", Color(0xFF497CF3)); Spacer(Modifier.height(24.dp)); Text("یادداشت متخصص", color = Ink, fontWeight = FontWeight.Bold); Text("تشخیص‌ها در نسخهٔ نمایشی نمونه هستند. در نسخهٔ عملیاتی نتیجهٔ OCR و تحلیل، با امکان بازبینی و تأیید شما ارائه می‌شود.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
        } else {
            Spacer(Modifier.height(14.dp)); Text("پژو ۲۰۶ تیپ ۵ · مدار ECU", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(10.dp)); Text("این صفحه برای توضیح سیستم، شرایط تحلیل و اطلاعات خودرو در نظر گرفته شده است. نسخهٔ عملیاتی باید مدل، سال ساخت و منبع نقشه را پیش از نتیجه‌گیری بررسی کند.", color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
fun CodeRow(code: String, description: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = color, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f)) { Text(code, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(description, color = Muted, fontSize = 11.sp) }
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun SettingsScreen(modifier: Modifier, dark: Boolean, onDark: (Boolean) -> Unit) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Spacer(Modifier.height(8.dp)); Text("تنظیمات", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("شخصی‌سازی فضای کارگاه", color = Muted, fontSize = 13.sp)
        SettingRow(Icons.Default.DarkMode, "تم تیره", "راحتی کار در محیط کم‌نور") { Switch(checked = dark, onCheckedChange = onDark) }
        SettingRow(Icons.Default.Translate, "زبان برنامه", "فارسی  ·  English") { Text("فارسی  ▾", color = Green, fontSize = 12.sp) }
        SettingRow(Icons.Default.AutoAwesome, "دستیار هوشمند", "فعال‌سازی با کنترل متخصص") { Switch(checked = false, onCheckedChange = {}) }
        SettingRow(Icons.Default.Group, "مدیریت دسترسی", "تأیید و مدیریت کاربران") { Icon(Icons.Default.ChevronLeft, null, tint = Muted) }
        SettingRow(Icons.Default.CloudSync, "پشتیبان‌گیری", "همگام‌سازی پروژه‌های شما") { Icon(Icons.Default.ChevronLeft, null, tint = Muted) }
        Spacer(Modifier.height(3.dp)); Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Mint)) { Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.VerifiedUser, null, tint = Green); Column { Text("مالک کارگاه", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("دسترسی کاربران فقط با تأیید شما فعال می‌شود.", color = Muted, fontSize = 11.sp) } } }
        Text("Electruck  ·  نسخهٔ نمایشی ۰.۱.۰", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
    }
}

@Composable
fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Mint), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Green, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f)) { Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, fontSize = 11.sp) }; trailing()
    }
}

@Composable
fun ProjectDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("پژو ۲۰۶ تیپ ۵") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("مدار ECU و رله دوبل", color = Green, fontWeight = FontWeight.Bold); Text("این پروژهٔ نمونه، نمای کلی مسیر کار در اپ را نشان می‌دهد. نقشهٔ واقعی، تشخیص OCR و ذخیره‌سازی ابری هنوز پیاده‌سازی نشده‌اند.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp); Text("خودرو · ۱۳۹۸  |  آخرین ویرایش · امروز", color = Muted, fontSize = 11.sp) } }, confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }, icon = { Icon(Icons.Default.DirectionsCar, null, tint = Green) })
}
