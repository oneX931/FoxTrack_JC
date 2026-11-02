package com.example.foxtrack.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foxtrack.BuildConfig
import com.example.foxtrack.R
import com.example.foxtrack.data.AppContainer
import com.example.foxtrack.data.local.AppLanguage
import com.example.foxtrack.data.local.ThemePreference
import com.example.foxtrack.domain.User
import com.example.foxtrack.ui.components.Attribution
import com.example.foxtrack.ui.components.Avatar
import com.example.foxtrack.ui.components.FoxButton
import com.example.foxtrack.ui.components.FoxButtonStyle
import com.example.foxtrack.ui.components.FoxTabs
import com.example.foxtrack.ui.components.FoxTextField
import com.example.foxtrack.ui.components.LocalToaster
import com.example.foxtrack.ui.components.PasswordTextField
import com.example.foxtrack.ui.theme.FoxTheme
import com.example.foxtrack.ui.util.applyLanguage
import com.example.foxtrack.ui.util.asString
import com.example.foxtrack.ui.viewmodel.FoxViewModelFactory
import com.example.foxtrack.ui.viewmodel.SettingsEvent
import com.example.foxtrack.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(container: AppContainer, user: User) {
    val vm: SettingsViewModel = viewModel(factory = FoxViewModelFactory { SettingsViewModel(container) })
    val state by vm.state.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val context = LocalContext.current

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var username by rememberSaveable(user.id) { mutableStateOf(user.username) }
    var newEmail by rememberSaveable { mutableStateOf("") }
    var emailPassword by rememberSaveable { mutableStateOf("") }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var showEmailPassword by rememberSaveable { mutableStateOf(false) }
    var showCurrentPassword by rememberSaveable { mutableStateOf(false) }
    var showNewPassword by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is SettingsEvent.Toast -> toaster.show(event.event.kind, event.event.text.resolve(context))
                SettingsEvent.EmailSaved -> {
                    newEmail = ""
                    emailPassword = ""
                }

                SettingsEvent.PasswordSaved -> {
                    currentPassword = ""
                    newPassword = ""
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setAvatar(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FoxTabs(
            labels = listOf(
                stringResource(R.string.settings_tab_profile),
                stringResource(R.string.settings_tab_security),
                stringResource(R.string.settings_tab_appearance),
                stringResource(R.string.settings_tab_data),
            ),
            selected = tab,
            onSelect = { tab = it },
            scrollable = true,
        )

        when (tab) {
            0 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(user = user, size = 80.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FoxButton(
                            text = stringResource(R.string.settings_avatar_change),
                            onClick = {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            style = FoxButtonStyle.Secondary,
                            compact = true,
                            loading = state.avatarBusy,
                            icon = R.drawable.ic_camera,
                        )
                        if (user.avatar != null) {
                            FoxButton(
                                text = stringResource(R.string.settings_avatar_remove),
                                onClick = vm::removeAvatar,
                                style = FoxButtonStyle.Ghost,
                                compact = true,
                                enabled = !state.avatarBusy,
                                icon = R.drawable.ic_trash,
                            )
                        }
                    }
                }
                FoxTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        vm.clearUsernameError()
                    },
                    label = stringResource(R.string.settings_username),
                    error = state.usernameError?.asString(),
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        if (username.trim() != user.username) vm.saveUsername(username)
                    },
                )
                FoxButton(
                    text = stringResource(R.string.settings_username_save),
                    onClick = { vm.saveUsername(username) },
                    enabled = username.trim() != user.username,
                    loading = state.usernameSaving,
                )
            }

            1 -> {
                FoxTextField(
                    value = user.email,
                    onValueChange = {},
                    label = stringResource(R.string.settings_email_current),
                    enabled = false,
                )
                FoxTextField(
                    value = newEmail,
                    onValueChange = {
                        newEmail = it
                        vm.clearEmailError("email")
                    },
                    label = stringResource(R.string.settings_email_new),
                    error = state.emailErrors["email"]?.asString(),
                    keyboardType = KeyboardType.Email,
                )
                PasswordTextField(
                    value = emailPassword,
                    onValueChange = {
                        emailPassword = it
                        vm.clearEmailError("currentPassword")
                    },
                    label = stringResource(R.string.settings_password_current),
                    visible = showEmailPassword,
                    onToggleVisible = { showEmailPassword = !showEmailPassword },
                    error = state.emailErrors["currentPassword"]?.asString(),
                    leadingIcon = null,
                )
                FoxButton(
                    text = stringResource(R.string.settings_email_save),
                    onClick = { vm.saveEmail(newEmail.trim(), emailPassword) },
                    enabled = newEmail.isNotBlank() && emailPassword.isNotEmpty(),
                    loading = state.emailSaving,
                )

                HorizontalDivider(color = FoxTheme.colors.border)

                PasswordTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        vm.clearPasswordError("currentPassword")
                    },
                    label = stringResource(R.string.settings_password_current),
                    visible = showCurrentPassword,
                    onToggleVisible = { showCurrentPassword = !showCurrentPassword },
                    error = state.passwordErrors["currentPassword"]?.asString(),
                    leadingIcon = null,
                    imeAction = ImeAction.Next,
                )
                PasswordTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        vm.clearPasswordError("newPassword")
                    },
                    label = stringResource(R.string.settings_password_new),
                    visible = showNewPassword,
                    onToggleVisible = { showNewPassword = !showNewPassword },
                    error = state.passwordErrors["newPassword"]?.asString(),
                    hint = stringResource(R.string.auth_field_password_hint),
                    leadingIcon = null,
                )
                FoxButton(
                    text = stringResource(R.string.settings_password_save),
                    onClick = { vm.savePassword(currentPassword, newPassword) },
                    enabled = currentPassword.isNotEmpty() && newPassword.isNotEmpty(),
                    loading = state.passwordSaving,
                )
            }

            2 -> AppearanceTab(container)

            else -> DataTab(container, onSave = vm::saveApiKey)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AppearanceTab(container: AppContainer) {
    val settings = container.settings
    val language by settings.language.collectAsStateWithLifecycle()
    val dark = FoxTheme.colors.isDark
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            FoxTabs(
                labels = listOf(
                    stringResource(R.string.settings_theme_light),
                    stringResource(R.string.settings_theme_dark),
                ),
                selected = if (dark) 1 else 0,
                onSelect = { settings.setTheme(if (it == 1) ThemePreference.DARK else ThemePreference.LIGHT) },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            FoxTabs(
                labels = listOf("Русский", "English"),
                selected = if (language == AppLanguage.EN) 1 else 0,
                onSelect = { applyLanguage(context, settings, if (it == 1) AppLanguage.EN else AppLanguage.RU) },
            )
        }
    }
}

@Composable
private fun DataTab(container: AppContainer, onSave: (String) -> Unit) {
    val stored by container.settings.apiKey.collectAsStateWithLifecycle()
    var key by rememberSaveable(stored) { mutableStateOf(stored) }
    var visible by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PasswordTextField(
            value = key,
            onValueChange = { key = it },
            label = stringResource(R.string.settings_api_key_label),
            visible = visible,
            onToggleVisible = { visible = !visible },
            hint = stringResource(R.string.settings_api_key_hint),
            leadingIcon = R.drawable.ic_key,
            imeAction = ImeAction.Done,
            onImeAction = { onSave(key) },
        )
        Text(
            text = stringResource(R.string.settings_api_key_from_build),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FoxButton(
            text = stringResource(R.string.settings_api_key_save),
            onClick = { onSave(key) },
            enabled = key.trim() != stored,
        )
        HorizontalDivider(color = FoxTheme.colors.border)
        Text(
            text = stringResource(R.string.settings_about),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "${stringResource(R.string.app_name)} · ${stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Attribution()
    }
}
