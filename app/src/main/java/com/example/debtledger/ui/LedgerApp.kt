package com.example.debtledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.debtledger.R
import com.example.debtledger.data.local.*
import com.example.debtledger.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import org.json.JSONObject
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor

val SunIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Sun", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 7f)
            curveToRelative(-2.76f, 0f, -5f, 2.24f, -5f, 5f)
            reflectiveCurveToRelative(2.24f, 5f, 5f, 5f)
            reflectiveCurveToRelative(5f, -2.24f, 5f, -5f)
            reflectiveCurveToRelative(-2.24f, -5f, -5f, -5f)
            close()
            moveTo(2f, 13f)
            horizontalLineToRelative(2f)
            curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
            reflectiveCurveToRelative(-0.45f, -1f, -1f, -1f)
            horizontalLineToRelative(-2f)
            curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
            reflectiveCurveToRelative(0.45f, 1f, 1f, 1f)
            close()
            moveTo(20f, 13f)
            horizontalLineToRelative(2f)
            curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
            reflectiveCurveToRelative(-0.45f, -1f, -1f, -1f)
            horizontalLineToRelative(-2f)
            curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
            reflectiveCurveToRelative(0.45f, 1f, 1f, 1f)
            close()
            moveTo(11f, 2f)
            verticalLineToRelative(2f)
            curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
            reflectiveCurveToRelative(1f, -0.45f, 1f, -1f)
            lineTo(13f, 2f)
            curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
            reflectiveCurveToRelative(-1f, 0.45f, -1f, 1f)
            close()
            moveTo(11f, 20f)
            verticalLineToRelative(2f)
            curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
            reflectiveCurveToRelative(1f, -0.45f, 1f, -1f)
            verticalLineToRelative(-2f)
            curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
            reflectiveCurveToRelative(-1f, 0.45f, -1f, 1f)
            close()
            moveTo(5.99f, 4.58f)
            curveToRelative(-0.39f, -0.39f, -1.03f, -0.39f, -1.41f, 0f)
            curveToRelative(-0.39f, 0.39f, -0.39f, 1.03f, 0f, 1.41f)
            lineToRelative(1.06f, 1.06f)
            curveToRelative(0.39f, 0.39f, 1.03f, 0.39f, 1.41f, 0f)
            curveToRelative(0.39f, -0.39f, 0.39f, -1.03f, 0f, -1.41f)
            lineTo(5.99f, 4.58f)
            close()
            moveTo(18.36f, 16.95f)
            curveToRelative(-0.39f, -0.39f, -1.03f, -0.39f, -1.41f, 0f)
            curveToRelative(-0.39f, 0.39f, -0.39f, 1.03f, 0f, 1.41f)
            lineToRelative(1.06f, 1.06f)
            curveToRelative(0.39f, 0.39f, 1.03f, 0.39f, 1.41f, 0f)
            curveToRelative(0.39f, -0.39f, 0.39f, -1.03f, 0f, -1.41f)
            lineToRelative(-1.06f, -1.06f)
            close()
            moveTo(19.42f, 5.99f)
            curveToRelative(0.39f, -0.39f, 0.39f, -1.03f, 0f, -1.41f)
            curveToRelative(-0.39f, -0.39f, -1.03f, -0.39f, -1.41f, 0f)
            lineToRelative(-1.06f, 1.06f)
            curveToRelative(-0.39f, 0.39f, -0.39f, 1.03f, 0f, 1.41f)
            curveToRelative(0.39f, 0.39f, 1.03f, 0.39f, 1.41f, 0f)
            lineToRelative(1.06f, -1.06f)
            close()
            moveTo(7.05f, 18.36f)
            curveToRelative(0.39f, -0.39f, 0.39f, -1.03f, 0f, -1.41f)
            curveToRelative(-0.39f, -0.39f, -1.03f, -0.39f, -1.41f, 0f)
            lineToRelative(-1.06f, 1.06f)
            curveToRelative(-0.39f, 0.39f, -0.39f, 1.03f, 0f, 1.41f)
            curveToRelative(0.39f, 0.39f, 1.03f, 0.39f, 1.41f, 0f)
            lineToRelative(1.06f, -1.06f)
            close()
        }
    }.build()

val MoonIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Moon", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
            reflectiveCurveToRelative(4.03f, 9f, 9f, 9f)
            reflectiveCurveToRelative(9f, -4.03f, 9f, -9f)
            curveToRelative(0f, -0.46f, -0.04f, -0.92f, -0.1f, -1.36f)
            curveToRelative(-0.98f, 1.37f, -2.58f, 2.26f, -4.4f, 2.26f)
            curveToRelative(-3.03f, 0f, -5.5f, -2.47f, -5.5f, -5.5f)
            curveToRelative(0f, -1.82f, 0.89f, -3.42f, 2.26f, -4.4f)
            curveToRelative(-0.44f, -0.06f, -0.9f, -0.1f, -1.36f, -0.1f)
            close()
        }
    }.build()

private fun currencyName(c: Currency) = if(c == Currency.ILS) "شيكل" else "دولار"
private fun amount(n: Long, c: Currency) = "\u2066${BigDecimal.valueOf(n, 2).toPlainString()}\u2069 ${currencyName(c)}"
private fun directionName(d: DebtDirection) = if(d == DebtDirection.RECEIVABLE) "لي عنده" else "عليّ له"
private fun statusName(d: DebtBalance) = when(d.status) {
    "CANCELLED" -> "ملغى"; "UNPAID" -> "غير مسدد"; "PARTIAL" -> "مسدد جزئيًا"; else -> "مسدد بالكامل"
}
private fun formatDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
private fun formatDisplayDate(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrDefault(isoDate)

private fun day(text: String): Long = try { LocalDate.parse(text.trim()).toEpochDay() }
    catch(e: Exception) { throw RuleViolation(RuleError.INVALID_DATE) }


private fun directionColor(direction: DebtDirection, isDark: Boolean): Color {
    return if (direction == DebtDirection.RECEIVABLE) {
        if (isDark) Color(0xFF81C784) else Color(0xFF00866A)
    } else {
        if (isDark) Color(0xFFFF8A65) else Color(0xFFD94800)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoneyDisplay(amountMinor: Long, currency: Currency, amountColor: Color, amountSize: TextUnit = 20.sp, fontWeight: FontWeight = FontWeight.Bold, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, verticalArrangement = Arrangement.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = BigDecimal.valueOf(amountMinor, 2).toPlainString(),
            style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            color = amountColor,
            fontSize = amountSize,
            fontWeight = fontWeight,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = currencyName(currency),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = (amountSize.value * 0.15).dp).align(Alignment.Bottom)
        )
    }
}

@Composable
private fun AutoResizedNameText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxFontSize: TextUnit = 18.sp,
    minFontSize: TextUnit = 14.sp
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        var fontSize by remember(text, density.fontScale, maxWidth, maxFontSize, minFontSize) {
            mutableStateOf(maxFontSize)
        }

        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.lineCount > 0) {
                    val hasEllipsis = textLayoutResult.isLineEllipsized(0) || textLayoutResult.didOverflowWidth
                    if (hasEllipsis && fontSize > minFontSize) {
                        fontSize = (fontSize.value - 1).sp
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerApp(vm: LedgerViewModel, isDark: Boolean) {
    val persons by vm.persons.collectAsStateWithLifecycle()
    val debts by vm.debts.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val darkTheme by vm.darkTheme.collectAsStateWithLifecycle()

    var hasShownSplash by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf(if (hasShownSplash) "home" else "splash") }
    var personId by rememberSaveable { mutableStateOf<String?>(null) }
    var debtId by rememberSaveable { mutableStateOf<String?>(null) }
    var paymentId by rememberSaveable { mutableStateOf<String?>(null) }
    var currencyCode by rememberSaveable { mutableStateOf("ILS") }
    var leave by remember { mutableStateOf(false) }
    val stateHolder = rememberSaveableStateHolder()
    var resumeDebt by rememberSaveable { mutableStateOf(false) }
    var correction by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf("") }
    var operationIdState by rememberSaveable { mutableStateOf<String?>(null) }

    // Bottom Sheets states
    var showPersonForm by rememberSaveable { mutableStateOf(false) }
    var showDebtForm by rememberSaveable { mutableStateOf(false) }
    var showPaymentForm by rememberSaveable { mutableStateOf(false) }

    // Draft storage for debt form when navigating to add person
    var draftDebtPerson by rememberSaveable { mutableStateOf("") }
    var draftDebtDirection by rememberSaveable { mutableStateOf("RECEIVABLE") }
    var draftDebtCurrency by rememberSaveable { mutableStateOf("ILS") }
    var draftDebtMoney by rememberSaveable { mutableStateOf("") }
    var draftDebtDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var draftDebtDesc by rememberSaveable { mutableStateOf("") }
    var draftDebtNotes by rememberSaveable { mutableStateOf("") }

    val currency = Currency.valueOf(currencyCode)
    val person = persons.find { it.id == personId }
    val debt = debts.find { it.id == debtId }

    fun open(target: String) {
        vm.clearError()
        when (target) {
            "personForm" -> showPersonForm = true
            "debtForm" -> showDebtForm = true
            "paymentForm" -> showPaymentForm = true
            else -> page = target
        }
    }
    
    fun back() {
        if(showPersonForm || showDebtForm || showPaymentForm) {
            leave = true
        } else {
            open(when(page) { "detail" -> if(personId == null) "debts" else "person"; "person" -> "people"; else -> "home" })
        }
    }

    BackHandler(enabled = page !in listOf("splash", "home") || showPersonForm || showDebtForm || showPaymentForm) { back() }

    if(page == "splash") {
        SplashScreen(onTimeout = {
            hasShownSplash = true
            open("home")
        })
        return
    }

    val titles = mapOf(
        "home" to "ملخصك المالي", "people" to "الأشخاص", "person" to (person?.name ?: "الشخص"),
        "debts" to "الديون", "detail" to "تفاصيل الدين", "activity" to "سجل النشاط", "settings" to "الإعدادات"
    )

    Scaffold(
        topBar = {
            if(page !in listOf("splash")) {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 20.dp, vertical = 12.dp)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Image(
                                            painter = painterResource(R.drawable.app_icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "إدارة مالية شخصية",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "دفتر الديون",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            Surface(
                                onClick = { vm.setDarkTheme(!isDark) },
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isDark) SunIcon else MoonIcon,
                                        contentDescription = if (isDark)
                                            "تفعيل الوضع الفاتح" else "تفعيل الوضع الداكن",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    if (page != "home") {
                        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if(page !in listOf("people", "debts", "settings")) {
                                    IconButton(onClick = { back() }, enabled = !busy, modifier = Modifier.offset(x = 8.dp)) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                                    }
                                }
                                Text(titles[page] ?: "", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                            
                            if(page == "detail" && debt != null && debt.cancelledAt == null) {
                                Row {
                                    TextButton(onClick = { debtId = debt.id; open("debtForm") }) { Text("تعديل") }
                                    TextButton(onClick = { correction = "cancelDebt"; reason = "" }) { Text("إلغاء الدين", color = MaterialTheme.colorScheme.error) }
                                }
                            }

                            if(page == "people") {
                                Surface(
                                    onClick = {
                                        stateHolder.removeState("personFormnew")
                                        personId = null
                                        open("personForm")
                                    },
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "إضافة شخص",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if(page in listOf("home", "people", "debts", "settings")) {
                NavigationBar(containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White) {
                    listOf(
                        Triple("home", "الرئيسية", Icons.Default.Home),
                        Triple("people", "الأشخاص", Icons.Default.Person),
                        Triple("debts", "الديون", Icons.Default.List),
                        Triple("settings", "الإعدادات", Icons.Default.Settings)
                    ).forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = page == route,
                            onClick = { open(route) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = if(isDark) Color(0xFF3A82A8).copy(alpha=0.3f) else Color(0xFFE8F0FE),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.outline,
                                unselectedTextColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                }
            }

            stateHolder.SaveableStateProvider(page + when {
                showDebtForm -> (debtId ?: "new")
                showPersonForm -> (personId ?: "new")
                showPaymentForm -> (paymentId ?: "new")
                else -> ""
            }) {
                when(page) {
                    "home" -> {
                        val hour = LocalTime.now().hour
                        val greeting = if(hour in 4..11) "صباح الخير،" else if(hour in 12..17) "نهار سعيد،" else "مساء الخير،"

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(greeting, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                            }
                            IconButton(
                                onClick = { open("activity") },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "الإشعارات", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Currency Selector Tabs
                        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Currency.entries.forEach { c ->
                                val selected = currency == c
                                Button(
                                    onClick = { currencyCode = c.name },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if(selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if(selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(currencyName(c), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        val rows = debts.filter { it.cancelledAt == null && it.currency == currency }
                        val balance = runCatching {
                            LedgerRules.aggregate(currency, rows.map { BalanceInput(it.currency, it.direction, DebtAmounts(it.originalAmountMinor, it.paidMinor)) })
                        }.getOrNull()

                        if(balance != null) {
                            Card(
                                Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(32.dp)
                            ) {
                                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("صافي الديون · ${currencyName(currency)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)) {
                                            Text("مستقل لكل عملة", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            when {
                                                balance.netMinor > 0 -> "لك "
                                                balance.netMinor < 0 -> "عليك "
                                                else -> "الرصيد متعادل"
                                            },
                                            fontSize = 32.sp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (balance.netMinor != 0L) {
                                            MoneyDisplay(Math.abs(balance.netMinor), currency, MaterialTheme.colorScheme.onPrimary, 32.sp)
                                        }
                                    }
                                    Text("لك عند الآخرين ناقص ما عليك لهم", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f))
                                }
                            }

                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val isFontLarge = LocalDensity.current.fontScale > 1.15f
                                val stackVertically = isFontLarge || maxWidth < 340.dp

                                if (stackVertically) {
                                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        BalanceCard(
                                            label = "لك عند الآخرين",
                                            amountMinor = balance.receivableMinor, currency = currency,
                                            accentColor = Color(0xFF00866A),
                                            icon = Icons.Default.KeyboardArrowDown,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        BalanceCard(
                                            label = "عليك للآخرين",
                                            amountMinor = balance.payableMinor, currency = currency,
                                            accentColor = Color(0xFFD94800),
                                            icon = Icons.Default.KeyboardArrowUp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        BalanceCard(
                                            label = "لك عند الآخرين",
                                            amountMinor = balance.receivableMinor, currency = currency,
                                            accentColor = Color(0xFF00866A),
                                            icon = Icons.Default.KeyboardArrowDown,
                                            modifier = Modifier.weight(1f)
                                        )
                                        BalanceCard(
                                            label = "عليك للآخرين",
                                            amountMinor = balance.payableMinor, currency = currency,
                                            accentColor = Color(0xFFD94800),
                                            icon = Icons.Default.KeyboardArrowUp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                stateHolder.removeState("debtFormnew")
                                personId = null
                                debtId = null
                                draftDebtPerson = ""
                                draftDebtDirection = "RECEIVABLE"
                                draftDebtCurrency = currencyCode
                                draftDebtMoney = ""
                                draftDebtDate = LocalDate.now().toString()
                                draftDebtDesc = ""
                                draftDebtNotes = ""
                                open("debtForm")
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("إضافة دين جديد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusCountCard("مفتوح", rows.count { it.remainingMinor > 0 }, Modifier.weight(1f))
                            StatusCountCard("جزئي", rows.count { it.status == "PARTIAL" }, Modifier.weight(1f))
                            StatusCountCard("مكتمل", rows.count { it.status == "PAID" }, Modifier.weight(1f))
                        }

                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("آخر النشاط", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { open("activity") }) {
                                    Text("عرض السجل", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            val recentActivity = activity.take(3)
                            if(recentActivity.isEmpty()) {
                                EmptyStateView(
                                    message = "لا توجد معاملات بعد",
                                    actionLabel = "إضافة دين جديد",
                                    onAction = {
                                        stateHolder.removeState("debtFormnew")
                                        personId = null
                                        debtId = null
                                        draftDebtPerson = ""
                                        draftDebtDirection = "RECEIVABLE"
                                        draftDebtCurrency = currencyCode
                                        draftDebtMoney = ""
                                        draftDebtDate = LocalDate.now().toString()
                                        draftDebtDesc = ""
                                        draftDebtNotes = ""
                                        open("debtForm")
                                    }
                                )
                            } else {
                                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        recentActivity.forEach { ActivityRowCompact(it, persons, debts) }
                                    }
                                }
                            }
                        }
                    }

                    "people" -> {
                        var query by rememberSaveable { mutableStateOf("") }
                        var archived by rememberSaveable { mutableStateOf(false) }

                        Field("ابحث بالاسم أو الهاتف", query, { query = it }, leadingIcon = Icons.Default.Search)
                        
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            val filtered = persons.filter { (archived || it.archivedAt == null) && (it.name.contains(query, true) || it.phone?.contains(query) == true) }
                            Text("كل الأشخاص (${filtered.size})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Checkbox(checked = archived, onCheckedChange = { archived = it })
                                Text("المؤرشفون", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        val filtered = persons.filter { (archived || it.archivedAt == null) && (it.name.contains(query, true) || it.phone?.contains(query) == true) }
                        if(filtered.isEmpty()) {
                            EmptyStateView(
                                message = "لا يوجد أشخاص مطابقون",
                                actionLabel = "إضافة شخص جديد",
                                onAction = { stateHolder.removeState("personFormnew"); personId = null; open("personForm") }
                            )
                        } else {
                            filtered.forEach { p ->
                            val balanceItems = mutableListOf<@Composable () -> Unit>()
                            Currency.entries.forEach { cur ->
                                val b = runCatching {
                                    val rows = debts.filter { it.personId == p.id && it.cancelledAt == null && it.currency == cur }
                                    LedgerRules.aggregate(cur, rows.map { BalanceInput(cur, it.direction, DebtAmounts(it.originalAmountMinor, it.paidMinor)) })
                                }.getOrNull()
                                if (b != null && (b.receivableMinor > 0 || b.payableMinor > 0)) {
                                    if (b.receivableMinor > 0) {
                                        balanceItems.add {
                                            val txt = buildAnnotatedString {
                                                withStyle(SpanStyle(color = directionColor(DebtDirection.RECEIVABLE, isDark), fontWeight = FontWeight.Bold)) {
                                                    append("لك عنده ")
                                                }
                                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)) {
                                                    append(amount(b.receivableMinor, cur))
                                                }
                                            }
                                            Text(txt, style = MaterialTheme.typography.bodyMedium, maxLines = 1, softWrap = false)
                                        }
                                    }
                                    if (b.payableMinor > 0) {
                                        balanceItems.add {
                                            val txt = buildAnnotatedString {
                                                withStyle(SpanStyle(color = directionColor(DebtDirection.PAYABLE, isDark), fontWeight = FontWeight.Bold)) {
                                                    append("عليك له ")
                                                }
                                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)) {
                                                    append(amount(b.payableMinor, cur))
                                                }
                                            }
                                            Text(txt, style = MaterialTheme.typography.bodyMedium, maxLines = 1, softWrap = false)
                                        }
                                    }
                                }
                            }

                            Card(
                                Modifier.fillMaxWidth().clickable { personId = p.id; open("person") },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(Modifier.padding(20.dp).fillMaxWidth()) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(p.name.take(1), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            val displayName = p.name + (if(p.archivedAt != null) " · مؤرشف" else "")
                                            AutoResizedNameText(text = displayName)
                                            p.phone?.let { phone ->
                                                Text(
                                                    text = "\u2066$phone\u2069",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontSize = 14.sp,
                                                        textDirection = TextDirection.Ltr
                                                    ),
                                                    color = MaterialTheme.colorScheme.outline,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                    }

                                    if (balanceItems.isNotEmpty()) {
                                        Spacer(Modifier.height(12.dp))
                                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            balanceItems.forEach { item -> item() }
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }

                    "person" -> if(person != null) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
                            Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(64.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(person.name.take(1), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Column {
                                    var showFullNameDialog by remember { mutableStateOf(false) }

                                    AutoResizedNameText(text = person.name, maxFontSize = 22.sp, minFontSize = 16.sp)
                                    
                                    TextButton(
                                        onClick = { showFullNameDialog = true },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("عرض الاسم", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }

                                    person.phone?.let { Text("\u2066$it\u2069", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline) }

                                    if (showFullNameDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showFullNameDialog = false },
                                            title = { Text("الاسم كاملاً") },
                                            text = { Text(person.name, style = MaterialTheme.typography.bodyLarge) },
                                            confirmButton = {
                                                TextButton(onClick = { showFullNameDialog = false }) {
                                                    Text("إغلاق")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        CurrencyPicker(currency) { currencyCode = it.name }
                        val rows = debts.filter { it.personId == person.id && it.currency == currency && it.cancelledAt == null }
                        val b = runCatching { LedgerRules.aggregate(currency, rows.map { BalanceInput(currency, it.direction, DebtAmounts(it.originalAmountMinor, it.paidMinor)) }) }.getOrNull()

                        if(b != null) {
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val isFontLarge = LocalDensity.current.fontScale > 1.15f
                                val stackVertically = isFontLarge || maxWidth < 340.dp

                                if (stackVertically) {
                                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
                                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                val labelText = buildAnnotatedString {
                                                    withStyle(SpanStyle(color = directionColor(DebtDirection.RECEIVABLE, isDark), fontWeight = FontWeight.Bold)) {
                                                        append("لك عنده")
                                                    }
                                                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.outline)) {
                                                        append(" · ${currencyName(currency)}")
                                                    }
                                                }
                                                Text(labelText, style = MaterialTheme.typography.bodyMedium)
                                                MoneyDisplay(b.receivableMinor, currency, directionColor(DebtDirection.RECEIVABLE, isDark), 24.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("المحصّل: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                                    MoneyDisplay(b.collectedMinor, currency, MaterialTheme.colorScheme.outline, 14.sp, FontWeight.Normal)
                                                }
                                            }
                                        }
                                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
                                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                val labelText = buildAnnotatedString {
                                                    withStyle(SpanStyle(color = directionColor(DebtDirection.PAYABLE, isDark), fontWeight = FontWeight.Bold)) {
                                                        append("عليك له")
                                                    }
                                                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.outline)) {
                                                        append(" · ${currencyName(currency)}")
                                                    }
                                                }
                                                Text(labelText, style = MaterialTheme.typography.bodyMedium)
                                                MoneyDisplay(b.payableMinor, currency, directionColor(DebtDirection.PAYABLE, isDark), 24.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("المسدّد: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                                    MoneyDisplay(b.repaidMinor, currency, MaterialTheme.colorScheme.outline, 14.sp, FontWeight.Normal)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
                                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                val labelText = buildAnnotatedString {
                                                    withStyle(SpanStyle(color = directionColor(DebtDirection.RECEIVABLE, isDark), fontWeight = FontWeight.Bold)) {
                                                        append("لك عنده")
                                                    }
                                                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.outline)) {
                                                        append(" · ${currencyName(currency)}")
                                                    }
                                                }
                                                Text(labelText, style = MaterialTheme.typography.bodyMedium)
                                                MoneyDisplay(b.receivableMinor, currency, directionColor(DebtDirection.RECEIVABLE, isDark), 24.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("المحصّل: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                                    MoneyDisplay(b.collectedMinor, currency, MaterialTheme.colorScheme.outline, 14.sp, FontWeight.Normal)
                                                }
                                            }
                                        }
                                        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
                                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                val labelText = buildAnnotatedString {
                                                    withStyle(SpanStyle(color = directionColor(DebtDirection.PAYABLE, isDark), fontWeight = FontWeight.Bold)) {
                                                        append("عليك له")
                                                    }
                                                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.outline)) {
                                                        append(" · ${currencyName(currency)}")
                                                    }
                                                }
                                                Text(labelText, style = MaterialTheme.typography.bodyMedium)
                                                MoneyDisplay(b.payableMinor, currency, directionColor(DebtDirection.PAYABLE, isDark), 24.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("المسدّد: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                                    MoneyDisplay(b.repaidMinor, currency, MaterialTheme.colorScheme.outline, 14.sp, FontWeight.Normal)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                stateHolder.removeState("debtFormnew")
                                debtId = null
                                draftDebtPerson = person.id
                                draftDebtDirection = "RECEIVABLE"
                                draftDebtCurrency = currencyCode
                                draftDebtMoney = ""
                                draftDebtDate = LocalDate.now().toString()
                                draftDebtDesc = ""
                                draftDebtNotes = ""
                                open("debtForm")
                            },
                            enabled = person.archivedAt == null,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("إضافة دين لهذا الشخص", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { open("personForm") }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(16.dp)) { Text("تعديل الشخص") }
                            TextButton(onClick = { correction = if(person.archivedAt == null) "archive" else "unarchive"; reason = "" }, enabled = !busy, modifier = Modifier.weight(1f).height(48.dp)) {
                                Text(if(person.archivedAt == null) "أرشفة" else "إلغاء الأرشفة")
                            }
                        }

                        var zeroDebts by remember(person.id) { mutableStateOf(false) }
                        LaunchedEffect(person.id) {
                            zeroDebts = vm.dao.debtCount(person.id) == 0L
                        }
                        if(zeroDebts) {
                            OutlinedButton(
                                onClick = { correction = "deletePerson"; reason = "" },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("حذف الشخص نهائيًا")
                            }
                        }

                        Text("الديون (${rows.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if(rows.isEmpty()) {
                            Text("لا توجد ديون مسجلة لهذا الشخص", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                        } else {
                            rows.forEach { d -> DebtCardModern(d, person.name, isDark) { debtId = d.id; open("detail") } }
                        }
                    }

                    "debts" -> {
                        var query by rememberSaveable { mutableStateOf("") }
                        var stateFilter by rememberSaveable { mutableStateOf("ALL") }
                        var direction by rememberSaveable { mutableStateOf("ALL") }
                        var fromDate by rememberSaveable { mutableStateOf("") }
                        var toDate by rememberSaveable { mutableStateOf("") }
                        var showFilterSheet by rememberSaveable { mutableStateOf(false) }
                        var dateRangeError by rememberSaveable { mutableStateOf(false) }

                        CurrencyPicker(currency) { currencyCode = it.name }

                        Field("ابحث بالشخص أو الوصف", query, { query = it }, leadingIcon = Icons.Default.Search)
                        
                        val activeFiltersCount = listOf(direction != "ALL", stateFilter != "ALL", fromDate.isNotBlank(), toDate.isNotBlank()).count { it }
                        OutlinedButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(if (activeFiltersCount > 0) "تصفية متقدمة (مفعل: $activeFiltersCount)" else "تصفية الديون", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        val fromDayNum = try { fromDate.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it).toEpochDay() } } catch (_: Exception) { -1L }
                        val toDayNum = try { toDate.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it).toEpochDay() } } catch (_: Exception) { -1L }

                        dateRangeError = (fromDayNum == -1L || toDayNum == -1L || (fromDayNum != null && toDayNum != null && fromDayNum > toDayNum))
                        if(dateRangeError && (fromDate.isNotBlank() || toDate.isNotBlank())) {
                            Text("تواريخ البحث غير صالحة أو 'من تاريخ' يتجاوز 'إلى تاريخ'", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }

                        val validDates = !dateRangeError && (fromDayNum != -1L && toDayNum != -1L && (fromDayNum == null || toDayNum == null || fromDayNum <= toDayNum))

                        val allDebtsList = debts.filter { d ->
                            d.currency == currency &&
                            (direction == "ALL" || d.direction.name == direction) &&
                            (when(stateFilter) {
                                "CANCELLED" -> d.cancelledAt != null
                                "OPEN" -> d.cancelledAt == null && d.remainingMinor > 0
                                "UNPAID" -> d.cancelledAt == null && d.status == "UNPAID"
                                "PARTIAL" -> d.cancelledAt == null && d.status == "PARTIAL"
                                "PAID" -> d.cancelledAt == null && d.status == "PAID"
                                else -> true
                            }) &&
                            (d.description.contains(query, true) || persons.find { it.id == d.personId }?.name?.contains(query, true) == true) &&
                            (!validDates || (
                                (fromDayNum == null || d.debtDate >= fromDayNum) &&
                                (toDayNum == null || d.debtDate <= toDayNum)
                            ))
                        }

                        if(showFilterSheet) {
                            @OptIn(ExperimentalMaterial3Api::class)
                            ModalBottomSheet(onDismissRequest = { showFilterSheet = false }, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
                                Column(
                                    Modifier
                                        .padding(24.dp)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("تصفية الديون", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { showFilterSheet = false }) { Icon(Icons.Default.Close, null) }
                                    }
                                    
                                    Text("الاتجاه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("ALL" to "الكل", "RECEIVABLE" to "لي", "PAYABLE" to "عليّ").forEach { (id, label) ->
                                            FilterChip(
                                                selected = direction == id,
                                                onClick = { direction = id },
                                                label = { Text(label, maxLines = 1) },
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                        }
                                    }

                                    Text("الحالة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                                        val isFontLarge = LocalDensity.current.fontScale > 1.15f
                                        val statusList = listOf("ALL" to "الكل", "OPEN" to "مفتوحة", "UNPAID" to "غير مسدد", "PARTIAL" to "جزئي", "PAID" to "مسدد", "CANCELLED" to "ملغى")

                                        if (isFontLarge || maxWidth < 380.dp) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    statusList.take(3).forEach { (id, label) ->
                                                        FilterChip(
                                                            selected = stateFilter == id,
                                                            onClick = { stateFilter = id },
                                                            label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                    }
                                                }
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    statusList.drop(3).forEach { (id, label) ->
                                                        FilterChip(
                                                            selected = stateFilter == id,
                                                            onClick = { stateFilter = id },
                                                            label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                statusList.forEach { (id, label) ->
                                                    FilterChip(
                                                        selected = stateFilter == id,
                                                        onClick = { stateFilter = id },
                                                        label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    DatePickerField("من تاريخ", fromDate) { fromDate = it }
                                    DatePickerField("إلى تاريخ", toDate) { toDate = it }

                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(
                                            onClick = { showFilterSheet = false },
                                            modifier = Modifier.weight(1f).height(52.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Text("تطبيق الفلاتر", fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                direction = "ALL"
                                                stateFilter = "ALL"
                                                fromDate = ""
                                                toDate = ""
                                                showFilterSheet = false
                                            },
                                            modifier = Modifier.weight(1f).height(52.dp),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Text("مسح الفلاتر", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.height(32.dp))
                                }
                            }
                        }

                        if(allDebtsList.isEmpty()) {
                            EmptyStateView(
                                message = "لا توجد ديون مطابقة",
                                actionLabel = "إضافة دين جديد",
                                onAction = {
                                    stateHolder.removeState("debtFormnew")
                                    personId = null
                                    debtId = null
                                    draftDebtPerson = ""
                                    draftDebtDirection = "RECEIVABLE"
                                    draftDebtCurrency = currencyCode
                                    draftDebtMoney = ""
                                    draftDebtDate = LocalDate.now().toString()
                                    draftDebtDesc = ""
                                    draftDebtNotes = ""
                                    open("debtForm")
                                }
                            )
                        } else {
                            allDebtsList.forEach { d -> DebtCardModern(d, persons.find { it.id == d.personId }?.name ?: "", isDark) { personId = d.personId; debtId = d.id; open("detail") } }
                        }
                    }

                    "detail" -> if(debt != null) {
                        val payments by remember(debt.id) { vm.payments(debt.id) }.collectAsStateWithLifecycle(emptyList())
                        val p = persons.find { it.id == debt.personId }

                        if(debt.cancelledAt != null) {
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(24.dp)) {
                                Text("هذا الدين ملغى ولا يمكن إجراء تعديلات أو دفعات عليه", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                            }
                        }

                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(32.dp)) {
                            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if(debt.direction == DebtDirection.RECEIVABLE) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if(debt.direction == DebtDirection.RECEIVABLE) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                                contentDescription = null,
                                                tint = if(debt.direction == DebtDirection.RECEIVABLE) Color(0xFF00866A) else Color(0xFFD94800)
                                            )
                                        }
                                    }
                                    Column {
                                        val prefix = if (debt.direction == DebtDirection.RECEIVABLE) "لك عند " else "عليك لـ "
                                        val prefixColor = directionColor(debt.direction, isDark)
                                        val headerText = buildAnnotatedString {
                                            withStyle(SpanStyle(color = prefixColor, fontWeight = FontWeight.Bold)) {
                                                append(prefix)
                                            }
                                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)) {
                                                append(p?.name ?: "")
                                            }
                                        }
                                        Text(headerText, style = MaterialTheme.typography.titleLarge)
                                        Text("${debt.description} · ${currencyName(debt.currency)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                                    }
                                }

                                Divider()

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("أصل الدين", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                        MoneyDisplay(debt.originalAmountMinor, debt.currency, MaterialTheme.colorScheme.onSurface, 18.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text(if(debt.direction == DebtDirection.RECEIVABLE) "المحصّل" else "المسدّد", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                        MoneyDisplay(debt.paidMinor, debt.currency, Color(0xFF00866A), 18.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("المتبقي", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                        MoneyDisplay(debt.remainingMinor, debt.currency, MaterialTheme.colorScheme.primary, 18.sp)
                                    }
                                }

                                if(debt.cancelledAt == null && debt.remainingMinor > 0) {
                                    Button(
                                        onClick = {
                                            operationIdState = UUID.randomUUID().toString()
                                            correction = "confirmFullPayment"
                                        },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text(if(debt.direction == DebtDirection.RECEIVABLE) "تسجيل تحصيل المتبقي" else "تسجيل سداد المتبقي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { stateHolder.removeState("paymentFormnew"); paymentId = null; open("paymentForm") },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("إضافة دفعة جزئية", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        val activeCount = payments.count { it.voidedAt == null }
                        val voidedCount = payments.count { it.voidedAt != null }
                        Text("سجل الدفعات ($activeCount دفعة فعالة · $voidedCount دفعة ملغاة)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if(payments.isEmpty()) {
                                    Text("لم تسجل دفعات بعد", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                                } else {
                                    payments.forEach { payment ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(amount(payment.amountMinor, debt.currency) + if(payment.voidedAt != null) " (ملغاة)" else "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if(payment.voidedAt != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                                                Text("تاريخ الدفعة: ${formatDate(payment.paymentDate)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                                payment.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
                                            }
                                            if(debt.cancelledAt == null && payment.voidedAt == null) {
                                                Row {
                                                    TextButton(onClick = { paymentId = payment.id; open("paymentForm") }) { Text("تعديل") }
                                                    TextButton(onClick = { paymentId = payment.id; correction = "voidPayment"; reason = "" }) { Text("إلغاء", color = MaterialTheme.colorScheme.error) }
                                                }
                                            }
                                        }
                                        Divider()
                                    }
                                }
                                Text("تاريخ إنشاء الدين: ${formatDate(debt.debtDate)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    "activity" -> {
                        if(activity.isEmpty()) {
                            EmptyStateView(message = "السجل فارغ", actionLabel = "العودة للرئيسية", onAction = { open("home") })
                        } else {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                activity.forEach { ActivityRowModern(it, persons, debts) }
                            }
                        }
                    }

                    "settings" -> {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
                                Row(Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Column {
                                            Text("الوضع الداكن", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("تفعيل المظهر الداكن دائمًا", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                    Switch(
                                        checked = darkTheme == true,
                                        onCheckedChange = { checked -> vm.setDarkTheme(checked) }
                                    )
                                }
                            }
                            listOf(
                                Triple("نسخة احتياطية مشفرة", "هذه الميزة غير متاحة حالياً في هذا الإصدار", Icons.Default.Info),
                                Triple("اللغة", "العربية", Icons.Default.Info),
                                Triple("معلومات التطبيق", "0.1.0", Icons.Default.Info)
                            ).forEach { (title, subtitle, icon) ->
                                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
                                    Row(Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            if (title == "معلومات التطبيق") {
                                                val context = LocalContext.current
                                                val currentVersion = remember(context) {
                                                    runCatching {
                                                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                                                    }.getOrDefault("0.1.0") ?: "0.1.0"
                                                }
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("دفتر الديون", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                    Text("الإصدار $currentVersion", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("برمجيات هوم", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            } else {
                                                Column {
                                                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                if (showPersonForm) {
                    @OptIn(ExperimentalMaterial3Api::class)
                    ModalBottomSheet(onDismissRequest = { leave = true }, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                        Box(Modifier.padding(24.dp).imePadding()) {
                            PersonForm(vm, person, busy, onClose = { leave = true }) { id ->
                                personId = id
                                showPersonForm = false
                                if(resumeDebt) {
                                    resumeDebt = false
                                    draftDebtPerson = id
                                    showDebtForm = true
                                } else if (page != "person") {
                                    open("person")
                                }
                            }
                        }
                    }
                }
                if (showDebtForm) {
                    @OptIn(ExperimentalMaterial3Api::class)
                    ModalBottomSheet(onDismissRequest = { leave = true }, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                        Box(Modifier.padding(24.dp).imePadding().verticalScroll(rememberScrollState())) {
                            DebtForm(vm, persons, debt, personId, busy, isDark,
                                draftPerson = draftDebtPerson,
                                draftDirection = draftDebtDirection,
                                draftCurrency = draftDebtCurrency,
                                draftMoney = draftDebtMoney,
                                draftDate = draftDebtDate,
                                draftDesc = draftDebtDesc,
                                draftNotes = draftDebtNotes,
                                onClose = { leave = true },
                                onDraftChange = { p: String, dir: String, cur: String, mon: String, dat: String, des: String, not: String ->
                                    draftDebtPerson = p
                                    draftDebtDirection = dir
                                    draftDebtCurrency = cur
                                    draftDebtMoney = mon
                                    draftDebtDate = dat
                                    draftDebtDesc = des
                                    draftDebtNotes = not
                                },
                                addPerson = {
                                    resumeDebt = true
                                    stateHolder.removeState("personFormnew")
                                    personId = null
                                    showDebtForm = false
                                    showPersonForm = true
                                },
                                saved = { id -> 
                                    debtId = id
                                    showDebtForm = false
                                    if (page != "detail") open("detail") 
                                }
                            )
                        }
                    }
                }
                if (showPaymentForm && debt != null) {
                    @OptIn(ExperimentalMaterial3Api::class)
                    ModalBottomSheet(onDismissRequest = { leave = true }, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), containerColor = MaterialTheme.colorScheme.background, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                        Box(Modifier.padding(24.dp).imePadding()) {
                            PaymentForm(vm, debt, paymentId, busy, onClose = { leave = true }) { 
                                showPaymentForm = false 
                            }
                        }
                    }
                }
            }
        }
    }

    if(leave) {
        AlertDialog(
            onDismissRequest = { leave = false },
            title = { Text("مغادرة النماذج غير المحفوظة؟") },
            text = { Text("أي بيانات مدخلة ستفقد عند المغادرة.") },
            confirmButton = {
                TextButton(onClick = {
                    leave = false
                    if (showPersonForm) showPersonForm = false
                    if (showDebtForm) showDebtForm = false
                    if (showPaymentForm) showPaymentForm = false
                    resumeDebt = false
                }) { Text("مغادرة") }
            },
            dismissButton = { TextButton(onClick = { leave = false }) { Text("متابعة التعديل") } }
        )
    }

    // Full payment confirmation dialog
    if(correction == "confirmFullPayment" && debt != null) {
        val p = persons.find { it.id == debt.personId }
        val opId = operationIdState ?: remember { UUID.randomUUID().toString() }.also { operationIdState = it }
        val noteText = if(debt.direction == DebtDirection.RECEIVABLE) "تحصيل كامل" else "سداد كامل"
        AlertDialog(
            onDismissRequest = { if(!busy) correction = null },
            title = { Text("تأكيد تسجيل الدفعة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الشخص: ${p?.name}", style = MaterialTheme.typography.bodyLarge)
                    Text("النوع: ${if(debt.direction == DebtDirection.RECEIVABLE) "تحصيل" else "سداد"}", style = MaterialTheme.typography.bodyLarge)
                    Text("المبلغ: ${amount(debt.remainingMinor, debt.currency)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("تاريخ العملية: ${formatDate(LocalDate.now().toEpochDay())}", style = MaterialTheme.typography.bodyLarge)
                    Text("الملاحظة: $noteText", style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        vm.act(success = {
                            correction = null
                            operationIdState = null
                            open("detail")
                        }) {
                            vm.repository.addPayment(opId, debt.id, debt.remainingMinor, LocalDate.now().toEpochDay(), noteText)
                        }
                    }
                ) { Text("تأكيد") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { correction = null; operationIdState = null }) { Text("تراجع") } }
        )
    }

    if(correction != null && correction != "confirmFullPayment") {
        AlertDialog(
            onDismissRequest = { if(!busy) correction = null },
            title = { Text("تأكيد العملية والتصحيح") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when(correction) {
                            "cancelDebt" -> "إلغاء تسجيل الدين هو تصحيح لتسجيل خاطئ (وليس تنازلاً). سيلغي هذا الإجراء كافة الدفعات الفعالة معه ويؤثر على إجماليات العملة."
                            "voidPayment" -> "إلغاء الدفعة سيزيد المتبقي على الدين بقيمتها."
                            "archive" -> "أرشفة الشخص تحافظ على الديون ضمن الحسابات وتسمح بالتحصيل والسداد، لكنها تمنع إضافة ديون جديدة حتى إلغاء الأرشفة."
                            "unarchive" -> "إلغاء أرشفة الشخص ستعيد إتاحة إضافة ديون له."
                            "deletePerson" -> "حذف الشخص نهائيًا — لا توجد عليه أي معاملات."
                            else -> "تأكيد العملية"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if(correction in listOf("cancelDebt", "voidPayment")) {
                        Field("سبب التصحيح والمراجعة *", reason, { reason = it })
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && (correction !in listOf("cancelDebt", "voidPayment") || reason.isNotBlank()),
                    onClick = {
                        val action = correction
                        vm.act(success = {
                            correction = null
                            if(action == "deletePerson") open("people")
                            else if(action == "cancelDebt") open("detail")
                        }) {
                            when(action) {
                                "cancelDebt" -> vm.repository.cancelDebt(requireNotNull(debtId), reason)
                                "voidPayment" -> vm.repository.voidPayment(requireNotNull(paymentId), reason)
                                "archive" -> vm.repository.archivePerson(requireNotNull(personId))
                                "unarchive" -> vm.repository.unarchivePerson(requireNotNull(personId))
                                "deletePerson" -> vm.repository.deleteEmptyPerson(requireNotNull(personId))
                            }
                            null
                        }
                    }
                ) { Text("تأكيد") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { correction = null }) { Text("تراجع") } }
        )
    }
}

@Composable
private fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1800)
        onTimeout()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon),
                        contentDescription = "شعار دفتر الديون",
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "إدارة مالية شخصية",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "دفتر الديون",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BalanceCard(label: String, amountMinor: Long, currency: Currency, accentColor: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = accentColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            MoneyDisplay(amountMinor, currency, MaterialTheme.colorScheme.onSurface, 20.sp)
        }
    }
}

@Composable
private fun StatusCountCard(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun EmptyStateView(message: String, actionLabel: String, onAction: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
            Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), shape = RoundedCornerShape(16.dp)) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    change: (String) -> Unit,
    numeric: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = change,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = if(numeric) KeyboardType.Decimal else KeyboardType.Text),
        singleLine = true,
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        shape = RoundedCornerShape(16.dp),
        textStyle = LocalTextStyle.current.copy(textDirection = if(numeric) TextDirection.Ltr else TextDirection.Rtl)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    dateText: String,
    onDateSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = runCatching { LocalDate.parse(dateText).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrDefault(System.currentTimeMillis())
    )

    Box(modifier = Modifier.fillMaxWidth().clickable { showDialog = true }) {
        OutlinedTextField(
            value = formatDisplayDate(dateText),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = "اختر التاريخ")
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
        )
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(selectedDate.toString())
                    }
                    showDialog = false
                }) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("إلغاء") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonDropdown(
    persons: List<PersonEntity>,
    selectedId: String,
    onSelected: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedPerson = persons.find { it.id == selectedId }
    val displayText = selectedPerson?.name ?: "اختر الشخص"

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("الشخص *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            persons.filter { it.archivedAt == null || it.id == selectedId }.forEach { person ->
                DropdownMenuItem(
                    text = { Text(person.name) },
                    onClick = {
                        onSelected(person.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CurrencyPicker(currency: Currency, change: (Currency) -> Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Currency.entries.forEach { c ->
            val selected = currency == c
            Button(
                onClick = { change(c) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if(selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(currencyName(c), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DebtCardModern(debt: DebtBalance, name: String, isDark: Boolean, click: () -> Unit) {
    val complete = debt.remainingMinor <= 0
    val prefix = if (debt.direction == DebtDirection.RECEIVABLE) "لك عند " else "عليك لـ "
    val prefixColor = directionColor(debt.direction, isDark)

    val titleText = buildAnnotatedString {
        withStyle(SpanStyle(color = prefixColor, fontWeight = FontWeight.Bold)) {
            append(prefix)
        }
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)) {
            append(name)
        }
    }

    Card(
        Modifier.fillMaxWidth().clickable(onClick = click),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = if(debt.cancelledAt != null) Color(0xFFF1F3F4) else if(debt.direction == DebtDirection.RECEIVABLE) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if(debt.cancelledAt != null) Icons.Default.Close else if(debt.direction == DebtDirection.RECEIVABLE) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = if(debt.cancelledAt != null) Color(0xFF5F6368) else if(debt.direction == DebtDirection.RECEIVABLE) Color(0xFF00866A) else Color(0xFFD94800),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when {
                        debt.cancelledAt != null -> Color(0xFFF1F3F4)
                        complete -> Color(0xFFF1F3F4)
                        debt.paidMinor > 0 -> Color(0xFFE8F0FE)
                        else -> Color(0xFFFEF7E0)
                    },
                    modifier = Modifier.wrapContentSize().padding(top = 2.dp)
                ) {
                    Text(
                        text = statusName(debt),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = when {
                            debt.cancelledAt != null -> Color(0xFF5F6368)
                            complete -> Color(0xFF5F6368)
                            debt.paidMinor > 0 -> Color(0xFF1967D2)
                            else -> Color(0xFFB06000)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(debt.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column {
                        if(complete) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("الأصل: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                MoneyDisplay(debt.originalAmountMinor, debt.currency, MaterialTheme.colorScheme.outline, 14.sp)
                            }
                        }
                        MoneyDisplay(debt.remainingMinor, debt.currency, MaterialTheme.colorScheme.onSurface, 18.sp)
                    }
                    Text(
                        text = formatDate(debt.debtDate),
                        style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

private fun auditDescription(json: String?, persons: List<PersonEntity>, debts: List<DebtBalance>): String {
    if(json == null) return "—"
    return runCatching {
        val value = JSONObject(json)
        if(value.has("name")) {
            val n = value.getString("name")
            val phone = value.optString("phone", "").takeIf { it.isNotBlank() && it != "null" }
            "الشخص: $n" + (phone?.let { " · الهاتف: $phone" } ?: "")
        } else {
            val related = debts.find { it.id == value.optString("debtId") }
            val pid = value.optString("personId", "").takeIf { it.isNotBlank() && it != "null" } ?: related?.personId
            val person = persons.find { it.id == pid }
            val currency = if(value.has("currency")) Currency.valueOf(value.getString("currency")) else related?.currency
            val minor = if(value.has("originalAmountMinor")) value.getLong("originalAmountMinor") else value.optLong("amountMinor")
            val desc = value.optString("description", "").takeIf { it.isNotBlank() && it != "null" }
            val notes = value.optString("notes", "").takeIf { it.isNotBlank() && it != "null" }
            val dir = value.optString("direction", "").takeIf { it.isNotBlank() && it != "null" }
            listOfNotNull(
                person?.name?.let { "الشخص: $it" },
                dir?.let { if(it == "RECEIVABLE") "الاتجاه: لي عنده" else "الاتجاه: عليّ له" },
                currency?.let { c -> minor.let { "المبلغ: ${amount(it, c)}" } },
                desc?.let { "البيان: $it" },
                notes?.let { "ملاحظات: $it" }
            ).joinToString(" · ")
        }
    }.getOrDefault("تفاصيل العمليات المسجلة")
}

@Composable
private fun ActivityRowCompact(event: AuditEntity, persons: List<PersonEntity>, debts: List<DebtBalance>) {
    val action = when(event.action) {
        "CREATE" -> "إنشاء معاملة"; "EDIT" -> "تعديل معاملة"; "VOID", "VOID_WITH_DEBT" -> "إلغاء دفعة"
        "CANCEL" -> "إلغاء دين"; "ARCHIVE" -> "أرشفة"; "UNARCHIVE" -> "إلغاء الأرشفة"; else -> "إجراء"
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFE8F0FE),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(action, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(auditDescription(event.afterJson ?: event.beforeJson, persons, debts), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
        }
    }
}

@Composable
private fun ActivityRowModern(event: AuditEntity, persons: List<PersonEntity>, debts: List<DebtBalance>) {
    val action = when(event.action) {
        "CREATE" -> "إنشاء معاملة"; "EDIT" -> "تعديل معاملة"; "VOID", "VOID_WITH_DEBT" -> "إلغاء دفعة وثيقة"
        "CANCEL" -> "إلغاء دين مسجل"; "ARCHIVE" -> "أرشفة حساب"; "UNARCHIVE" -> "إلغاء الأرشفة"; else -> "إجراء مالي"
    }
    val timeStr = Instant.ofEpochMilli(event.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime().toString().replace('T', ' ')
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(action, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Text("تفاصيل ما بعد التعديل / الإدراج:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Text(auditDescription(event.afterJson, persons, debts), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if(event.beforeJson != null) {
                Text("تفاصيل ما قبل التعديل:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text(auditDescription(event.beforeJson, persons, debts), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun PersonForm(vm: LedgerViewModel, person: PersonEntity?, busy: Boolean, onClose: () -> Unit, saved: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(person?.name ?: "") }
    var phone by rememberSaveable { mutableStateOf(person?.phone ?: "") }
    var notes by rememberSaveable { mutableStateOf(person?.notes ?: "") }
    var nameError by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if(person == null) "إضافة شخص جديد" else "تعديل بيانات الشخص", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "إغلاق") }
        }
        Field("الاسم *", name, { name = it; nameError = it.isBlank() })
        if(nameError) {
            Text("الاسم مطلوب ولا يمكن أن يكون فارغاً", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Field("الهاتف (اختياري)", phone, { phone = it }, numeric = true)
        Field("ملاحظات (اختياري)", notes, { notes = it })
        Button(
            enabled = !busy && name.isNotBlank(),
            onClick = {
                if(name.isBlank()) { nameError = true; return@Button }
                vm.act({ saved(requireNotNull(it)) }) { vm.repository.savePerson(person?.id, name, phone.ifBlank { null }, notes.ifBlank { null }) }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("حفظ الشخص", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DebtForm(
    vm: LedgerViewModel,
    persons: List<PersonEntity>,
    debt: DebtBalance?,
    initialPerson: String?,
    busy: Boolean,
    isDark: Boolean,
    draftPerson: String,
    draftDirection: String,
    draftCurrency: String,
    draftMoney: String,
    draftDate: String,
    draftDesc: String,
    draftNotes: String,
    onClose: () -> Unit,
    onDraftChange: (String, String, String, String, String, String, String) -> Unit,
    addPerson: () -> Unit,
    saved: (String) -> Unit
) {
    var selected by rememberSaveable { mutableStateOf(debt?.personId ?: draftPerson.takeIf { it.isNotEmpty() } ?: initialPerson ?: "") }
    var direction by rememberSaveable { mutableStateOf(debt?.direction?.name ?: draftDirection) }
    var currency by rememberSaveable { mutableStateOf(debt?.currency?.name ?: draftCurrency) }
    var pendingCurrency by remember { mutableStateOf<String?>(null) }
    var money by rememberSaveable { mutableStateOf(debt?.let { BigDecimal.valueOf(it.originalAmountMinor, 2).toPlainString() } ?: draftMoney) }
    var date by rememberSaveable { mutableStateOf(debt?.let { LocalDate.ofEpochDay(it.debtDate).toString() } ?: draftDate) }
    var description by rememberSaveable { mutableStateOf(debt?.description ?: draftDesc) }
    var notes by rememberSaveable { mutableStateOf(debt?.notes ?: draftNotes) }
    
    var touchedMoney by rememberSaveable { mutableStateOf(false) }
    var touchedDesc by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selected, direction, currency, money, date, description, notes) {
        onDraftChange(selected, direction, currency, money, date, description, notes)
    }

    val history by remember(debt?.id) { if(debt != null) vm.payments(debt.id) else flowOf(emptyList<PaymentEntity>()) }.collectAsStateWithLifecycle(emptyList())
    val locked = history.isNotEmpty()

    val moneyValidation = runCatching { MoneyParser.positive(money, Currency.valueOf(currency)) }.exceptionOrNull()
    val moneyError = touchedMoney && moneyValidation != null
    val descError = touchedDesc && description.isBlank()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if(debt == null) "إضافة دين جديد" else "تعديل بيانات الدين", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "إغلاق") }
        }
        
        PersonDropdown(
            persons = persons,
            selectedId = selected,
            onSelected = { selected = it },
            enabled = !locked
        )

        if(!locked) {
            TextButton(onClick = addPerson) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("إضافة شخص جديد")
            }
        }

        if(locked) {
            Text("الشخص والاتجاه والعملة ثابتة بعد تسجيل أول دفعة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            val lockedText = buildAnnotatedString {
                withStyle(SpanStyle(color = directionColor(DebtDirection.valueOf(direction), isDark), fontWeight = FontWeight.Bold)) {
                    append(directionName(DebtDirection.valueOf(direction)))
                }
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    append(" · ${currencyName(Currency.valueOf(currency))}")
                }
            }
            Text(lockedText, style = MaterialTheme.typography.titleMedium)
        } else {
            Text("الاتجاه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { direction = "RECEIVABLE" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(direction == "RECEIVABLE") (if(isDark) Color(0xFF1B382B) else Color(0xFFE6F4EA)) else MaterialTheme.colorScheme.surface,
                        contentColor = if(direction == "RECEIVABLE") directionColor(DebtDirection.RECEIVABLE, isDark) else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("لي عنده", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { direction = "PAYABLE" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(direction == "PAYABLE") (if(isDark) Color(0xFF3E2723) else Color(0xFFFCE8E6)) else MaterialTheme.colorScheme.surface,
                        contentColor = if(direction == "PAYABLE") directionColor(DebtDirection.PAYABLE, isDark) else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("عليّ له", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Text("العملة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CurrencyPicker(Currency.valueOf(currency)) { newCur ->
                if(newCur.name != currency) {
                    if(money.isNotBlank()) {
                        pendingCurrency = newCur.name
                    } else {
                        currency = newCur.name
                    }
                }
            }
        }

        if(pendingCurrency != null) {
            AlertDialog(
                onDismissRequest = { pendingCurrency = null },
                title = { Text("تغيير العملة؟") },
                text = { Text("سيُمسح المبلغ المدخل لتجنب تسجيله بالعملة الخطأ. لا يوجد تحويل تلقائي.") },
                confirmButton = { TextButton(onClick = { currency = pendingCurrency!!; money = ""; pendingCurrency = null }) { Text("تغيير") } },
                dismissButton = { TextButton(onClick = { pendingCurrency = null }) { Text("تراجع") } }
            )
        }

        OutlinedTextField(
            value = money,
            onValueChange = { money = it; touchedMoney = true },
            label = { Text("المبلغ *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            isError = moneyError
        )
        if(moneyError) {
            val errText = when(moneyValidation) {
                is RuleViolation -> when(moneyValidation.code) {
                    RuleError.AMOUNT_PRECISION -> "المبلغ يقبل منزلتين عشريتين كحد أقصى"
                    RuleError.NON_POSITIVE_AMOUNT -> "المبلغ يجب أن يكون أكبر من صفر"
                    RuleError.AMOUNT_OUT_OF_RANGE -> "المبلغ خارج الحدود المسموحة"
                    else -> "صيغة المبلغ غير صالحة"
                }
                else -> "أدخل مبلغًا صحيحًا أكبر من صفر"
            }
            Text(errText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        DatePickerField("تاريخ الدين", date) { date = it }
        
        OutlinedTextField(
            value = description,
            onValueChange = { description = it; touchedDesc = true },
            label = { Text("سبب الدين *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            isError = descError
        )
        if(descError) Text("سبب الدين مطلوب", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

        Field("ملاحظات (اختياري)", notes, { notes = it })

        Button(
            enabled = !busy && selected.isNotEmpty() && description.isNotBlank() && moneyValidation == null,
            onClick = {
                touchedMoney = true
                touchedDesc = true
                if(selected.isNotEmpty() && description.isNotBlank() && moneyValidation == null) {
                    val parsedMoney = MoneyParser.positive(money, Currency.valueOf(currency))
                    val dayNum = day(date)
                    vm.act({ saved(requireNotNull(it)) }) {
                        vm.repository.saveDebt(
                            debt?.id, selected, DebtDirection.valueOf(direction),
                            Currency.valueOf(currency), parsedMoney.amountMinor, dayNum,
                            description, notes.ifBlank { null }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("حفظ الدين", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PaymentForm(vm: LedgerViewModel, debt: DebtBalance, paymentId: String?, busy: Boolean, onClose: () -> Unit, saved: () -> Unit) {
    val history by remember(debt.id) { vm.payments(debt.id) }.collectAsStateWithLifecycle(emptyList())
    val existing = history.find { it.id == paymentId }
    if(paymentId != null && existing == null) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("تحميل الدفعة…")
        }
        return
    }

    key(existing?.id) {
        var money by rememberSaveable { mutableStateOf(existing?.let { BigDecimal.valueOf(it.amountMinor, 2).toPlainString() } ?: "") }
        var date by rememberSaveable { mutableStateOf(existing?.let { LocalDate.ofEpochDay(it.paymentDate).toString() } ?: LocalDate.now().toString()) }
        var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
        var reason by rememberSaveable { mutableStateOf("") }
        var touchedMoney by rememberSaveable { mutableStateOf(false) }
        val operationId = rememberSaveable { UUID.randomUUID().toString() }

        val moneyValidation = runCatching { MoneyParser.positive(money, debt.currency) }.exceptionOrNull()
        val moneyError = touchedMoney && moneyValidation != null

        Column(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if(existing == null) (if(debt.direction == DebtDirection.RECEIVABLE) "تسجيل تحصيل" else "تسجيل سداد") else "تعديل الدفعة", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "إغلاق") }
                }

                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("المتبقي على الدين", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        MoneyDisplay(debt.remainingMinor, debt.currency, MaterialTheme.colorScheme.primary, 24.sp)
                    }
                }

                OutlinedTextField(
                    value = money,
                    onValueChange = { money = it; touchedMoney = true },
                    label = { Text("المبلغ *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                    isError = moneyError
                )
                if(moneyError) Text("أدخل مبلغًا صحيحًا أكبر من صفر ضمن المتبقي", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

                if(existing == null) {
                    OutlinedButton(
                        onClick = { money = BigDecimal.valueOf(debt.remainingMinor, 2).toPlainString() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("تعبئة المتبقي كاملًا", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }

                DatePickerField("تاريخ الدفعة", date) { date = it }
                Field("ملاحظات (اختياري)", notes, { notes = it })
                if(existing != null) {
                    Field("سبب التصحيح والمراجعة *", reason, { reason = it })
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                enabled = !busy && moneyValidation == null && (existing == null || reason.isNotBlank()),
                onClick = {
                    touchedMoney = true
                    if(moneyValidation == null && (existing != null && reason.isNotBlank() || existing == null)) {
                        vm.act({ saved() }) {
                            val parsed = MoneyParser.positive(money, debt.currency)
                            val dayNum = day(date)
                            if(existing == null) {
                                vm.repository.addPayment(operationId, debt.id, parsed.amountMinor, dayNum, notes.ifBlank { null })
                            } else {
                                vm.repository.editPayment(existing.id, parsed.amountMinor, dayNum, notes.ifBlank { null }, reason)
                                existing.id
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("تأكيد الحفظ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
