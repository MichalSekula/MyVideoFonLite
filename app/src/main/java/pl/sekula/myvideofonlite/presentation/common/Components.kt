package pl.sekula.myvideofonlite.presentation.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Pastylka statusu z kolorową kropką. */
@Composable
fun StatusPill(label: String, color: Color, modifier: Modifier = Modifier, onDark: Boolean = false) {
    Row(
        modifier
            .clip(CircleShape)
            .background(color.copy(alpha = if (onDark) 0.22f else 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (onDark) Color.White else color,
        )
    }
}

/** Okrągła ikona w kolorowym tle. */
@Composable
fun IconBadge(
    @DrawableRes icon: Int,
    container: Color,
    content: Color,
    size: Dp = 48.dp,
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(size * 0.5f))
    }
}

/** Duży okrągły przycisk akcji z podpisem (podgląd na żywo). */
@Composable
fun RoundAction(
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    vector: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.38f
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(60.dp).clip(CircleShape).background(container.copy(alpha = container.alpha * alpha)),
            contentAlignment = Alignment.Center,
        ) {
            val tint = content.copy(alpha = alpha)
            when {
                icon != null -> Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
                vector != null -> Icon(vector, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            maxLines = 2,
        )
    }
}

/** Karta sekcji z nagłówkiem (ustawienia). */
@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}
