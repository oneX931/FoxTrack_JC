package com.example.foxtrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foxtrack.R
import com.example.foxtrack.data.AppContainer
import com.example.foxtrack.ui.components.FoxButton
import com.example.foxtrack.ui.components.FoxTabs
import com.example.foxtrack.ui.components.FoxTextField
import com.example.foxtrack.ui.components.FormBanner
import com.example.foxtrack.ui.components.LanguageToggleButton
import com.example.foxtrack.ui.components.Logo
import com.example.foxtrack.ui.components.Panel
import com.example.foxtrack.ui.components.PasswordTextField
import com.example.foxtrack.ui.components.ThemeToggleButton
import com.example.foxtrack.ui.util.asString
import com.example.foxtrack.ui.viewmodel.AuthViewModel
import com.example.foxtrack.ui.viewmodel.FoxViewModelFactory

@Composable
fun AuthScreen(container: AppContainer) {
    val vm: AuthViewModel = viewModel(factory = FoxViewModelFactory { AuthViewModel(container) })
    val state by vm.state.collectAsStateWithLifecycle()
    val language by container.settings.language.collectAsStateWithLifecycle()

    var mode by rememberSaveable { mutableIntStateOf(0) }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    val register = mode == 1
    val appName = stringResource(R.string.app_name)
    val title = stringResource(if (register) R.string.auth_register_title else R.string.auth_login_title, appName)
    val submit = {
        vm.submit(
            register = register,
            username = username.trim(),
            email = email.trim().lowercase(),
            password = password,
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        val minHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LanguageToggleButton(settings = container.settings, language = language)
                ThemeToggleButton(settings = container.settings)
            }
            Spacer(Modifier.height(8.dp))
            Logo(markSize = 48.dp)
            Spacer(Modifier.height(24.dp))
            Panel(modifier = Modifier.widthIn(max = 440.dp)) {
                FoxTabs(
                    labels = listOf(stringResource(R.string.auth_tab_login), stringResource(R.string.auth_tab_register)),
                    selected = mode,
                    onSelect = {
                        mode = it
                        vm.resetErrors()
                    },
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(if (register) R.string.auth_register_subtitle else R.string.auth_login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))

                val formError = state.formError
                if (formError != null) {
                    FormBanner(text = formError.asString())
                    Spacer(Modifier.height(16.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (register) {
                        FoxTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                vm.clearFieldError("username")
                            },
                            label = stringResource(R.string.auth_field_username),
                            error = state.fieldErrors["username"]?.asString(),
                            hint = stringResource(R.string.auth_field_username_hint),
                            leadingIcon = R.drawable.ic_user,
                        )
                    }
                    FoxTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            vm.clearFieldError("email")
                        },
                        label = stringResource(R.string.auth_field_email),
                        error = state.fieldErrors["email"]?.asString(),
                        leadingIcon = R.drawable.ic_mail,
                        keyboardType = KeyboardType.Email,
                    )
                    PasswordTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            vm.clearFieldError("password")
                        },
                        label = stringResource(R.string.auth_field_password),
                        visible = showPassword,
                        onToggleVisible = { showPassword = !showPassword },
                        error = state.fieldErrors["password"]?.asString(),
                        hint = if (register) stringResource(R.string.auth_field_password_hint) else null,
                        imeAction = ImeAction.Done,
                        onImeAction = { submit() },
                    )
                }
                Spacer(Modifier.height(20.dp))
                FoxButton(
                    text = stringResource(
                        when {
                            state.submitting && register -> R.string.auth_register_submitting
                            state.submitting -> R.string.auth_login_submitting
                            register -> R.string.auth_register_submit
                            else -> R.string.auth_login_submit
                        },
                    ),
                    onClick = { submit() },
                    modifier = Modifier.fillMaxWidth(),
                    loading = state.submitting,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.auth_local_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
