package com.ubpbsla.waspro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubpbsla.waspro.R

private val Ink = Color(0xFF142C40)
private val Ocean = Color(0xFF104D59)
internal val FieldColors = lightColorScheme(
    primary = Color(0xFF006B5E), onPrimary = Color.White,
    primaryContainer = Color(0xFFCCF1E5), onPrimaryContainer = Color(0xFF064B40),
    secondary = Color(0xFF385B74), secondaryContainer = Color(0xFFDFEBF4),
    onSecondaryContainer = Ink, background = Color(0xFFF3F6F8), onBackground = Ink,
    surface = Color.White, onSurface = Ink, surfaceContainerLow = Color(0xFFF2F6F8),
    surfaceContainer = Color(0xFFE8EFF2), onSurfaceVariant = Color(0xFF4C606C),
    outline = Color(0xFF70818A), outlineVariant = Color(0xFFD6E1E5),
)

internal val FieldShapes = Shapes(
    small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
internal fun LandscapeHeader(kicker: String, title: String, subtitle: String) {
    Surface(shape = MaterialTheme.shapes.large, color = Ink, contentColor = Color.White) {
        Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Ink, Ocean)))) {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp)) {
                Text(kicker, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFF9EE3CD))
                Spacer(Modifier.height(6.dp))
                Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                if (subtitle.isNotBlank()) Text(subtitle, Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE0EDF0))
            }
            Image(painterResource(R.drawable.operational_landscape), null,
                Modifier.fillMaxWidth().height(56.dp), contentScale = ContentScale.FillBounds)
        }
    }
}

@Composable
internal fun SectionHeading(title: String) {
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Spacer(Modifier.size(4.dp, 17.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun IconTile(icon: ImageVector) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
        Icon(icon, null, Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
internal fun InfoPanel(icon: ImageVector, title: String, detail: String) {
    FormPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon)
            Column {
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
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}
