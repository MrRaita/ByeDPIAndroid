package io.github.dovecoteescapee.byedpi.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dovecoteescapee.byedpi.R

// ---------------------------------------------------------------------------------------------
// Segmented card groups: first/last item get large outer corners, items in between small ones.
// (Hand-built so we do not depend on the still-moving expressive list-item alpha APIs.)
// ---------------------------------------------------------------------------------------------

private val OuterRadius = 28.dp
private val InnerRadius = 6.dp

/** Corner radii of one row inside a segmented group. */
@Immutable
class ItemShape(val top: Dp, val bottom: Dp)

private fun groupItemShape(index: Int, count: Int): ItemShape =
    ItemShape(
        top = if (index == 0) OuterRadius else InnerRadius,
        bottom = if (index == count - 1) OuterRadius else InnerRadius,
    )

/** False while the service is running: the settings are visible but cannot be changed. */
val LocalPrefsEnabled = compositionLocalOf { true }

class PrefGroupScope {
    internal val items = mutableListOf<@Composable (ItemShape) -> Unit>()

    fun item(content: @Composable (ItemShape) -> Unit) {
        items.add(content)
    }
}

@Composable
fun PrefGroup(
    title: String? = null,
    hint: String? = null,
    build: @Composable PrefGroupScope.() -> Unit,
) {
    val scope = PrefGroupScope()
    scope.build()
    if (scope.items.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
            )
        } else {
            Spacer(Modifier.height(12.dp))
        }
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
        scope.items.forEachIndexed { index, item ->
            if (index > 0) Spacer(Modifier.height(2.dp))
            item(groupItemShape(index, scope.items.size))
        }
    }
}

@Composable
private fun PrefRow(
    shape: ItemShape,
    title: String,
    summary: String?,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    trailing: (@Composable () -> Unit)? = null,
) {
    val enabled = enabled && LocalPrefsEnabled.current
    val colors = MaterialTheme.colorScheme
    val titleColor = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f)
    val summaryColor = if (enabled) colors.onSurfaceVariant else colors.onSurface.copy(alpha = 0.38f)

    // Expressive press feedback: the row rounds off its corners while pressed, springing back on release.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val spring: FiniteAnimationSpec<Dp> =
        if (LocalReduceMotion.current) snap() else expressiveFastSpatial()
    val top by animateDpAsState(if (pressed) OuterRadius else shape.top, spring, label = "row-top")
    val bottom by animateDpAsState(if (pressed) OuterRadius else shape.bottom, spring, label = "row-bottom")
    val rowShape = RoundedCornerShape(
        topStart = top.coerceAtLeast(0.dp),
        topEnd = top.coerceAtLeast(0.dp),
        bottomEnd = bottom.coerceAtLeast(0.dp),
        bottomStart = bottom.coerceAtLeast(0.dp),
    )

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
                if (summary != null) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = summaryColor,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(16.dp))
                trailing()
            }
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = rowShape,
            color = colors.surfaceContainer,
            interactionSource = interaction,
            content = content,
        )
    } else {
        Surface(shape = rowShape, color = colors.surfaceContainer, content = content)
    }
}

// ---------------------------------------------------------------------------------------------
// Preference types
// ---------------------------------------------------------------------------------------------

@Composable
fun SwitchPref(
    shape: ItemShape,
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
) {
    PrefRow(
        shape = shape,
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = { onChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled && LocalPrefsEnabled.current,
            )
        },
    )
}

@Composable
fun ChoicePref(
    shape: ItemShape,
    title: String,
    value: String,
    entries: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    val label = entries.firstOrNull { it.first == value }?.second ?: value

    PrefRow(
        shape = shape,
        title = title,
        summary = label,
        enabled = enabled,
        onClick = { open = true },
    )

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    entries.forEach { (entryValue, entryLabel) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = entryValue == value,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onSelect(entryValue)
                                        open = false
                                    },
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            RadioButton(selected = entryValue == value, onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text(entryLabel, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { open = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun TextPref(
    shape: ItemShape,
    title: String,
    value: String,
    onSave: (String) -> Unit,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    multiline: Boolean = false,
    maxLength: Int? = null,
    validate: (String) -> Boolean = { true },
) {
    var open by remember { mutableStateOf(false) }

    PrefRow(
        shape = shape,
        title = title,
        summary = value.ifEmpty { stringResource(R.string.not_set) },
        enabled = enabled,
        onClick = { open = true },
    )

    if (open) {
        var text by remember { mutableStateOf(value) }
        val valid = validate(text)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = if (maxLength != null) it.take(maxLength) else it },
                    singleLine = !multiline,
                    minLines = if (multiline) 3 else 1,
                    isError = !valid,
                    supportingText = if (!valid) {
                        { Text(stringResource(R.string.invalid_value)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSave(text)
                        open = false
                    },
                    enabled = valid,
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

/** A plain tappable row (navigates somewhere or performs an action). */
@Composable
fun ActionPref(
    shape: ItemShape,
    title: String,
    onClick: () -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
    githubIcon: Boolean = false,
) {
    PrefRow(
        shape = shape,
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        trailing = if (githubIcon) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_github_36),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            null
        },
    )
}

/** Read-only row, e.g. the version. */
@Composable
fun InfoPref(shape: ItemShape, title: String, summary: String) {
    PrefRow(shape = shape, title = title, summary = summary, enabled = true, onClick = null)
}

@Composable
fun openUrl(): (String) -> Unit {
    val context = LocalContext.current
    return { url ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            // no browser installed
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Segmented switcher (connected button group look): the selected segment is filled and fully rounded
// ---------------------------------------------------------------------------------------------

@Composable
fun ModeSwitcher(
    selectedIndex: Int,
    labels: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val enabled = LocalPrefsEnabled.current
    val reduce = LocalReduceMotion.current
    val shapeSpec: FiniteAnimationSpec<Dp> = if (reduce) snap() else expressiveSpatial()
    val colorSpec: FiniteAnimationSpec<Color> = if (reduce) snap() else expressiveEffects()
    val outer = 24.dp
    val inner = 8.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val first = index == 0
            val last = index == labels.lastIndex

            val startRadius by animateDpAsState(
                targetValue = if (selected || first) outer else inner,
                animationSpec = shapeSpec,
                label = "segment-start",
            )
            val endRadius by animateDpAsState(
                targetValue = if (selected || last) outer else inner,
                animationSpec = shapeSpec,
                label = "segment-end",
            )
            val container by animateColorAsState(
                targetValue = if (selected) colors.primary else colors.surfaceContainerHigh,
                animationSpec = colorSpec,
                label = "segment-container",
            )
            val content by animateColorAsState(
                targetValue = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                animationSpec = colorSpec,
                label = "segment-content",
            )

            Surface(
                onClick = { onSelect(index) },
                enabled = enabled,
                shape = RoundedCornerShape(
                    topStart = startRadius.coerceAtLeast(0.dp),
                    bottomStart = startRadius.coerceAtLeast(0.dp),
                    topEnd = endRadius.coerceAtLeast(0.dp),
                    bottomEnd = endRadius.coerceAtLeast(0.dp),
                ),
                color = container,
                contentColor = content,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(text = label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Palette picker: a grid of color swatches
// ---------------------------------------------------------------------------------------------

@Composable
fun PalettePref(
    shape: ItemShape,
    title: String,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(
            topStart = shape.top,
            topEnd = shape.top,
            bottomEnd = shape.bottom,
            bottomStart = shape.bottom,
        ),
        color = colors.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
            Spacer(Modifier.height(14.dp))
            Palettes.all.chunked(6).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    rowItems.forEach { palette ->
                        PaletteSwatch(
                            palette = palette,
                            selected = palette.id == selectedId,
                            onClick = { onSelect(palette.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatch(palette: AppPalette, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val enabled = LocalPrefsEnabled.current
    val reduce = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = if (reduce) snap() else expressiveFastSpatial(),
        label = "swatch-scale",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (selected) Modifier.border(3.dp, colors.onSurface, CircleShape) else Modifier)
            .padding(if (selected) 5.dp else 0.dp)
            .clip(CircleShape)
            .background(palette.swatch)
            .semantics { contentDescription = palette.label }
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
