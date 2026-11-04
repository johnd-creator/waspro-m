package com.ubpbsla.waspro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubpbsla.waspro.R

internal val FieldColors = lightColorScheme(
    primary = Color(0xFF116951), onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EDAB), onPrimaryContainer = Color(0xFF193C24),
    secondary = Color(0xFF466755), secondaryContainer = Color(0xFFDCEEDC),
    onSecondaryContainer = Color(0xFF203F30), background = Color(0xFFF4F6F0),
    onBackground = Color(0xFF182B24), surface = Color.White, onSurface = Color(0xFF182B24),
    surfaceContainerLow = Color(0xFFF0F5EF), surfaceContainer = Color(0xFFEAF1EA),
    onSurfaceVariant = Color(0xFF47594E), outline = Color(0xFF708174), outlineVariant = Color(0xFFD5DFD4),
)

@Composable
internal fun LandscapeHeader(kicker: String, title: String, subtitle: String) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF164A40), contentColor = Color.White) {
        Column(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp)) {
                Text(kicker, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.6.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFFD8EDAB))
                Spacer(Modifier.height(8.dp))
                Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                if (subtitle.isNotBlank()) Text(subtitle, Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD8E9DF))
            }
            Image(painterResource(R.drawable.operational_landscape), null,
                Modifier.fillMaxWidth().height(72.dp), contentScale = ContentScale.FillBounds)
        }
    }
}

@Composable
internal fun SectionHeading(title: String) {
    Text(title, Modifier.padding(top = 8.dp).semantics { heading() },
        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
internal fun FormPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), content = content)
    }
}
