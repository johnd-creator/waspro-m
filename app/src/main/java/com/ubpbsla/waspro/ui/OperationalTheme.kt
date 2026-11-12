package com.ubpbsla.waspro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.*

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubpbsla.waspro.R

internal val Ink = Color(0xFF142C40)

internal val FieldColors = lightColorScheme(
    primary = Color(0xFF006B5E), onPrimary = Color.White,
    primaryContainer = Color(0xFFCCF1E5), onPrimaryContainer = Color(0xFF064B40),
    tertiaryContainer = Color(0xFFFFEDBC), onTertiaryContainer = Color(0xFF694600),
    secondary = Color(0xFF385B74), secondaryContainer = Color(0xFFDFEBF4),
    onSecondaryContainer = Ink, background = Color(0xFFF2F7F5), onBackground = Ink,
    surface = Color.White, onSurface = Ink, surfaceContainerLow = Color(0xFFF2F6F8),
    surfaceContainer = Color(0xFFE8EFF2), onSurfaceVariant = Color(0xFF4C606C),
    outline = Color(0xFF70818A), outlineVariant = Color(0xFFD6E1E5),
)

internal val FieldDarkColors = darkColorScheme(
    primary = Color(0xFF86DBC6), onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005144), onPrimaryContainer = Color(0xFFB4F1DF),
    tertiaryContainer = Color(0xFF543D07), onTertiaryContainer = Color(0xFFFFE3A0),
    secondary = Color(0xFFA9CCDF), onSecondary = Color(0xFF103447),
    secondaryContainer = Color(0xFF254B60), onSecondaryContainer = Color(0xFFD1E8F4),
    background = Color(0xFF101A20), onBackground = Color(0xFFE0E9ED),
    surface = Color(0xFF152229), onSurface = Color(0xFFE0E9ED),
    surfaceContainerLow = Color(0xFF192930), surfaceContainer = Color(0xFF203139),
    onSurfaceVariant = Color(0xFFB8CAD1), outline = Color(0xFF859BA4), outlineVariant = Color(0xFF3D5059),
)

internal val FieldShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp),
)

private val Plex = FontFamily(
    Font(R.font.plex_regular), Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
)
internal val FieldTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Plex),
        displayMedium = base.displayMedium.copy(fontFamily = Plex),
        displaySmall = base.displaySmall.copy(fontFamily = Plex),
        headlineLarge = base.headlineLarge.copy(fontFamily = Plex),
        headlineMedium = base.headlineMedium.copy(fontFamily = Plex, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = Plex),
        titleLarge = base.titleLarge.copy(fontFamily = Plex, fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = Plex, fontWeight = FontWeight.Medium),
        titleSmall = base.titleSmall.copy(fontFamily = Plex),
        bodyLarge = base.bodyLarge.copy(fontFamily = Plex, letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.copy(fontFamily = Plex, letterSpacing = 0.sp),
        bodySmall = base.bodySmall.copy(fontFamily = Plex, letterSpacing = 0.sp, fontSize = 13.sp),
        labelLarge = base.labelLarge.copy(fontFamily = Plex, letterSpacing = 0.sp),
        labelMedium = base.labelMedium.copy(fontFamily = Plex, letterSpacing = 0.sp, fontSize = 13.sp),
        labelSmall = base.labelSmall.copy(fontFamily = Plex, letterSpacing = 0.sp),
    )
}

@Composable
internal fun LoginMasthead() {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).background(Ink)) {
        Image(painterResource(R.drawable.station), null, Modifier.matchParentSize(),
            contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd)
        Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(
            listOf(Ink.copy(alpha = .30f), Ink.copy(alpha = .30f), Ink.copy(alpha = .96f))
        )).heightIn(min = 232.dp).padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(shape = RoundedCornerShape(12.dp), color = Ink.copy(alpha = .90f)) {
                Text("WASPRO / MOBILE", Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp, color = Color.White)
            }
            Column {
                Text("Operasional tertata.\nLingkungan terjaga.", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("Pencatatan limbah & material FABA", Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCEE9E1))
            }
        }
    }
}

@Composable
internal fun LandscapeHeader(kicker: String, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(kicker, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold)
        if (subtitle.isNotBlank()) Text(subtitle, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SectionHeading(title: String) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
        HorizontalDivider(Modifier.weight(1f).padding(start = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
internal fun IconTile(icon: ImageVector) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
        Icon(icon, null, Modifier.padding(11.dp).size(22.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
internal fun InfoPanel(icon: ImageVector, title: String, detail: String) {
    FormPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(detail, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun FormPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

/** Decorative contours have no data meaning; reported values remain plain, accessible text. */
@Composable
internal fun MetricHero(title: String, value: String, icon: ImageVector) {
    Surface(shape = MaterialTheme.shapes.large, color = Ink, contentColor = Color.White) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width * .98f, size.height * .12f)
                for (radius in listOf(.42f, .62f, .82f)) {
                    drawCircle(Color(0xFF83D9BE).copy(alpha = .12f), size.height * radius, center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                }
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = Color(0xFFCAE8DF))
                    Surface(shape = MaterialTheme.shapes.small, color = Color(0xFF284B54)) {
                        Icon(icon, null, Modifier.padding(10.dp).size(22.dp), tint = Color(0xFFA8EFCE))
                    }
                }
                Text(value, style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Keeps form and report sections easy to scan without using color alone. */
@Composable
internal fun FormSectionTitle(number: Int, title: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
            Box(Modifier.sizeIn(minWidth = 28.dp, minHeight = 28.dp).padding(4.dp), contentAlignment = Alignment.Center) {
                Text(number.toString().padStart(2, '0'), style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold)
            }
        }
        Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
    }
}
