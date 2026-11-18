package net.biodiscus.streammusic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
internal fun SwitchSetting(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = description?.let { text -> { Text(text) } },
        trailingContent = { Switch(checked = isChecked, onCheckedChange = null) },
        modifier = modifier.toggleable(value = isChecked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

/** A setting with a few options, shown as chips next to each other. They move to the next line when they do not fit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <Option> ChoiceSetting(
    title: String,
    options: List<Option>,
    chosenOption: Option,
    labelOf: @Composable (Option) -> String,
    onOptionChosen: (Option) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        if (description != null) {
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == chosenOption,
                    onClick = { onOptionChosen(option) },
                    label = { Text(labelOf(option)) },
                )
            }
        }
    }
}
