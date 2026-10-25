package de.dirk.kalender.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * One UI look (as in Samsung Health): grey page, white borderless cards with large radius,
 * big left-aligned page title, teal-green accent, flat bottom bar without indicator.
 */

object OneUi {
    val Accent = Color(0xFF00A88E)
    val AccentDark = Color(0xFF2CC5A8)
    val CardRadius = 26.dp
}

@Composable
fun OneUiTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(
        primary = OneUi.AccentDark, onPrimary = Color.Black,
        primaryContainer = Color(0xFF0E3B33), onPrimaryContainer = Color(0xFFB6F0E3),
        background = Color(0xFF000000), onBackground = Color(0xFFF5F5F5),
        surface = Color(0xFF171717), onSurface = Color(0xFFF5F5F5),
        surfaceVariant = Color(0xFF262626), onSurfaceVariant = Color(0xFF9E9E9E),
        outlineVariant = Color(0xFF2E2E2E),
    ) else lightColorScheme(
        primary = OneUi.Accent, onPrimary = Color.White,
        primaryContainer = Color(0xFFD9F3EE), onPrimaryContainer = Color(0xFF00463B),
        background = Color(0xFFF2F2F4), onBackground = Color(0xFF111111),
        surface = Color(0xFFFFFFFF), onSurface = Color(0xFF111111),
        surfaceVariant = Color(0xFFEDEDF0), onSurfaceVariant = Color(0xFF7A7A80),
        outlineVariant = Color(0xFFE6E6E9),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Large page title that scrolls with the content (One UI "extended app bar"). */
@Composable
fun BigTitle(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(start = 8.dp, top = 56.dp, bottom = 28.dp)) {
        Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Compact bar that fades in once the big title has scrolled away. */
@Composable
fun CollapsedBar(title: String, visible: Boolean) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding()
                .height(56.dp).padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Card(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(OneUi.CardRadius))
            .background(MaterialTheme.colorScheme.surface).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (title != null) Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
fun SubText(text: String) =
    Text(text, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun RowDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

@Composable
fun SwitchRow(title: String, sub: String? = null, checked: Boolean, onChange: (Boolean) -> Unit, lead: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (lead != null) { lead(); Spacer(Modifier.width(14.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            if (sub != null) SubText(sub)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
                uncheckedBorderColor = Color.Transparent,
            )
        )
    }
}

/** One UI style segmented control: grey track, white pill on the selected item. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).clip(CircleShape)
                    .background(if (sel) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, fontSize = 14.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    color = if (sel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Colored round icon, like the category icons on the Samsung Health home screen. */
@Composable
fun RoundIcon(icon: ImageVector, tint: Color, size: Int = 44) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.52f).dp))
    }
}

/** Tile for the "add widget" grid. */
@Composable
fun RowScope.Tile(label: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RoundIcon(icon, tint)
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PillButton(text: String, filled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(CircleShape)
            .background(if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/** Flat bottom bar: icon + label, selected item bold and filled, no indicator pill. */
@Composable
fun BottomBar(items: List<Triple<String, ImageVector, ImageVector>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).navigationBarsPadding()
            .padding(top = 8.dp, bottom = 10.dp)
    ) {
        items.forEachIndexed { i, (label, outlined, filled) ->
            val sel = i == selected
            val color = if (sel) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                Modifier.weight(1f).clickable { onSelect(i) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(if (sel) filled else outlined, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(3.dp))
                Text(label, fontSize = 12.sp, color = color, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
