package net.biodiscus.streammusic.feature.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.biodiscus.streammusic.R
import net.biodiscus.streammusic.core.model.DeviceGroup
import net.biodiscus.streammusic.core.model.DeviceGroupColor
import net.biodiscus.streammusic.core.model.DeviceGroupIcon
import net.biodiscus.streammusic.core.model.DeviceGroupMember
import net.biodiscus.streammusic.core.model.DeviceProtocol
import net.biodiscus.streammusic.core.model.isSavableGroup
import net.biodiscus.streammusic.core.model.isValidGroupName

/** The play button streams to the group. Clicking the rest of the row opens a menu to edit or delete it. */
@Composable
internal fun DeviceGroupRow(
    group: DeviceGroup,
    isOnNetwork: Boolean,
    onPlayClicked: () -> Unit,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    Box(modifier) {
        ListItem(
            modifier = Modifier.clickable { isMenuExpanded = true },
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(group.icon.toImageVector(), contentDescription = null, tint = group.color.toColor())
                    Spacer(Modifier.width(8.dp))
                    Text(group.name, color = group.color.toColor())
                }
            },
            supportingContent = { Text(groupDescription(group, isOnNetwork)) },
            trailingContent = {
                IconButton(onClick = onPlayClicked, enabled = isOnNetwork) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.devices_play_group))
                }
            },
        )
        DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.devices_edit_group)) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = {
                    isMenuExpanded = false
                    onEditClicked()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.devices_delete_group), color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = {
                    isMenuExpanded = false
                    onDeleteClicked()
                },
            )
        }
    }
}

@Composable
private fun groupDescription(group: DeviceGroup, isOnNetwork: Boolean): String {
    val memberNames = group.members.joinToString(", ") { member -> member.name }
    if (isOnNetwork) return memberNames
    return "$memberNames\n${stringResource(R.string.devices_group_not_on_network)}"
}

/** The colour of the new group's name is picked at random. */
@Composable
internal fun SaveGroupDialog(
    onSaveClicked: (String, DeviceGroupIcon, DeviceGroupColor) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf(DeviceGroupIcon.SPEAKER_GROUP) }
    val color = rememberSaveable { DeviceGroupColor.entries.random() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.devices_save_group_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                GroupNameAndIconFields(
                    name = name,
                    onNameChange = { newName -> name = newName },
                    icon = icon,
                    onIconChange = { chosenIcon -> icon = chosenIcon },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSaveClicked(name, icon, color) }, enabled = isValidGroupName(name)) {
                Text(stringResource(R.string.devices_save_group_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.devices_choose_protocol_cancel)) }
        },
    )
}

/** Changes the name, the icon, which devices stay in the group and the protocol each device streams with. */
@Composable
internal fun EditGroupDialog(
    group: DeviceGroup,
    protocolChoicesFor: (DeviceGroupMember) -> List<DeviceProtocol>,
    onSaveClicked: (String, DeviceGroupIcon, List<DeviceGroupMember>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(group.name) }
    var icon by rememberSaveable { mutableStateOf(group.icon) }
    var checkedHosts by rememberSaveable { mutableStateOf<Set<String>>(group.members.mapTo(LinkedHashSet()) { member -> member.host }) }
    var protocolByHost by rememberSaveable {
        mutableStateOf<Map<String, DeviceProtocol>>(group.members.associateTo(LinkedHashMap()) { member -> member.host to member.protocol })
    }
    val keptMembers = group.members
        .filter { member -> member.host in checkedHosts }
        .map { member -> member.copy(protocol = protocolByHost.getValue(member.host)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.devices_edit_group_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                GroupNameAndIconFields(
                    name = name,
                    onNameChange = { newName -> name = newName },
                    icon = icon,
                    onIconChange = { chosenIcon -> icon = chosenIcon },
                )
                group.members.forEach { member ->
                    GroupMemberRow(
                        member = member,
                        protocol = protocolByHost.getValue(member.host),
                        protocolChoices = protocolChoicesFor(member),
                        isChecked = member.host in checkedHosts,
                        onProtocolChosen = { protocol -> protocolByHost = protocolByHost + (member.host to protocol) },
                        onClick = { checkedHosts = toggleSelection(checkedHosts, member.host) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSaveClicked(name, icon, keptMembers) },
                enabled = isSavableGroup(name, keptMembers.size),
            ) {
                Text(stringResource(R.string.devices_save_group_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.devices_choose_protocol_cancel)) }
        },
    )
}

@Composable
internal fun DeleteGroupDialog(
    group: DeviceGroup,
    onConfirmed: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.devices_delete_group_title, group.name)) },
        text = { Text(stringResource(R.string.devices_delete_group_body)) },
        confirmButton = {
            TextButton(onClick = onConfirmed) {
                Text(stringResource(R.string.devices_delete_group_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.devices_choose_protocol_cancel)) }
        },
    )
}

@Composable
private fun GroupMemberRow(
    member: DeviceGroupMember,
    protocol: DeviceProtocol,
    protocolChoices: List<DeviceProtocol>,
    isChecked: Boolean,
    onProtocolChosen: (DeviceProtocol) -> Unit,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(member.name) },
        supportingContent = {
            ProtocolDropdown(protocol = protocol, choices = protocolChoices, onProtocolChosen = onProtocolChosen)
        },
        trailingContent = { Checkbox(checked = isChecked, onCheckedChange = null) },
    )
}

@Composable
private fun ProtocolDropdown(
    protocol: DeviceProtocol,
    choices: List<DeviceProtocol>,
    onProtocolChosen: (DeviceProtocol) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { isExpanded = true }, enabled = choices.size > 1) {
            Text(protocol.displayName)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.displayName) },
                    trailingIcon = { if (choice == protocol) Icon(Icons.Filled.Check, contentDescription = null) },
                    onClick = {
                        isExpanded = false
                        onProtocolChosen(choice)
                    },
                )
            }
        }
    }
}

@Composable
private fun GroupNameAndIconFields(
    name: String,
    onNameChange: (String) -> Unit,
    icon: DeviceGroupIcon,
    onIconChange: (DeviceGroupIcon) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.devices_group_name_label)) },
            singleLine = true,
        )
        Spacer(Modifier.height(16.dp))
        GroupIconPicker(selectedIcon = icon, onIconSelected = onIconChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupIconPicker(
    selectedIcon: DeviceGroupIcon,
    onIconSelected: (DeviceGroupIcon) -> Unit,
) {
    FlowRow {
        DeviceGroupIcon.entries.forEach { icon ->
            FilledIconToggleButton(
                checked = icon == selectedIcon,
                onCheckedChange = { onIconSelected(icon) },
            ) {
                Icon(icon.toImageVector(), contentDescription = icon.name)
            }
        }
    }
}

private fun DeviceGroupIcon.toImageVector(): ImageVector =
    when (this) {
        DeviceGroupIcon.SPEAKER_GROUP -> Icons.Filled.SpeakerGroup
        DeviceGroupIcon.FOLDER -> Icons.Filled.Folder
        DeviceGroupIcon.LIBRARY -> Icons.Filled.LibraryMusic
        DeviceGroupIcon.CATEGORY -> Icons.Filled.Category
    }

/** Mid-tone colours that stay readable on both the light and the dark theme. */
private fun DeviceGroupColor.toColor(): Color =
    when (this) {
        DeviceGroupColor.RED -> Color(0xFFE53935)
        DeviceGroupColor.ORANGE -> Color(0xFFFB8C00)
        DeviceGroupColor.GREEN -> Color(0xFF43A047)
        DeviceGroupColor.TEAL -> Color(0xFF00897B)
        DeviceGroupColor.BLUE -> Color(0xFF1E88E5)
        DeviceGroupColor.INDIGO -> Color(0xFF5C6BC0)
        DeviceGroupColor.PURPLE -> Color(0xFF8E24AA)
        DeviceGroupColor.PINK -> Color(0xFFD81B60)
    }
