package com.example.foxtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.foxtrack.R
import com.example.foxtrack.data.AppContainer
import com.example.foxtrack.data.repo.SessionState
import com.example.foxtrack.domain.User
import com.example.foxtrack.ui.components.Avatar
import com.example.foxtrack.ui.components.FoxIcon
import com.example.foxtrack.ui.components.FoxIconButton
import com.example.foxtrack.ui.components.FoxSnackbarHost
import com.example.foxtrack.ui.components.Logo
import com.example.foxtrack.ui.components.LocalToaster
import com.example.foxtrack.ui.components.Spinner
import com.example.foxtrack.ui.components.ThemeToggleButton
import com.example.foxtrack.ui.components.rememberToaster
import com.example.foxtrack.ui.screens.AddGameScreen
import com.example.foxtrack.ui.screens.AuthScreen
import com.example.foxtrack.ui.screens.GameDetailScreen
import com.example.foxtrack.ui.screens.LibraryScreen
import com.example.foxtrack.ui.screens.SettingsScreen
import com.example.foxtrack.ui.theme.FoxTheme

object Routes {
    const val LIBRARY = "library"
    const val ADD = "add"
    const val GAME = "game/{id}"
    const val SETTINGS = "settings"
    const val ARG_ID = "id"

    fun game(id: Long) = "game/$id"
}

@Composable
fun FoxTrackRoot(container: AppContainer) {
    val session by container.session.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { container.session.restore() }

    when (val current = session) {
        SessionState.Loading -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Spinner(size = 32.dp)
        }

        SessionState.SignedOut -> AuthScreen(container)

        is SessionState.SignedIn -> key(current.user.id) {
            MainShell(container = container, user = current.user)
        }
    }
}

@Composable
private fun MainShell(container: AppContainer, user: User) {
    val navController = rememberNavController()
    val snackbarHost = remember { SnackbarHostState() }
    val toaster = rememberToaster(snackbarHost)
    val language by container.settings.language.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val topLevel = route == Routes.LIBRARY || route == Routes.ADD
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    CompositionLocalProvider(LocalToaster provides toaster) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                FoxHeader(
                    container = container,
                    user = user,
                    route = route,
                    onBack = { navController.popBackStack() },
                    onOpenSettings = {
                        navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                    },
                    onLogout = { container.session.signOut() },
                )
            },
            bottomBar = {
                if (topLevel && !imeVisible) FoxBottomBar(navController = navController, route = route)
            },
            snackbarHost = { FoxSnackbarHost(state = snackbarHost) },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.LIBRARY,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        container = container,
                        onOpenGame = { id -> navController.navigate(Routes.game(id)) },
                        onAddGame = { navController.navigateTopLevel(Routes.ADD) },
                    )
                }
                composable(Routes.ADD) {
                    AddGameScreen(
                        container = container,
                        onOpenGame = { id -> navController.navigate(Routes.game(id)) },
                    )
                }
                composable(
                    route = Routes.GAME,
                    arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong(Routes.ARG_ID) ?: 0L
                    GameDetailScreen(
                        container = container,
                        gameId = id,
                        languageTag = language.tag,
                        onBack = { navController.popBackStack() },
                        onDeleted = { navController.popBackStack() },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(container = container, user = user)
                }
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun FoxHeader(
    container: AppContainer,
    user: User,
    route: String?,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    val showBack = route == Routes.SETTINGS || route == Routes.GAME
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            FoxIconButton(
                icon = R.drawable.ic_arrow_left,
                contentDescription = stringResource(
                    if (route == Routes.SETTINGS) R.string.settings_close else R.string.detail_back,
                ),
                onClick = onBack,
            )
            Text(
                text = stringResource(if (route == Routes.SETTINGS) R.string.settings_title else R.string.detail_back),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        } else {
            Logo(modifier = Modifier.padding(start = 8.dp).weight(1f))
        }
        ThemeToggleButton(settings = container.settings)
        UserMenu(user = user, onOpenSettings = onOpenSettings, onLogout = onLogout)
    }
}

@Composable
private fun UserMenu(user: User, onOpenSettings: () -> Unit, onLogout: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val colors = FoxTheme.colors
    val description = stringResource(R.string.menu_account)
    Box {
        IconButton(onClick = { open = true }) {
            Avatar(
                user = user,
                size = 32.dp,
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(text = user.username, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = colors.border)
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_settings)) },
                leadingIcon = { FoxIcon(id = R.drawable.ic_settings, size = 18.dp) },
                onClick = {
                    open = false
                    onOpenSettings()
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.menu_logout), color = colors.danger) },
                leadingIcon = { FoxIcon(id = R.drawable.ic_log_out, size = 18.dp, tint = colors.danger) },
                onClick = {
                    open = false
                    onLogout()
                },
            )
        }
    }
}

@Composable
private fun FoxBottomBar(navController: NavHostController, route: String?) {
    val scheme = MaterialTheme.colorScheme
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = scheme.onPrimaryContainer,
        selectedTextColor = scheme.onSurface,
        indicatorColor = scheme.primaryContainer,
        unselectedIconColor = scheme.onSurfaceVariant,
        unselectedTextColor = scheme.onSurfaceVariant,
    )
    NavigationBar(containerColor = scheme.surface) {
        NavigationBarItem(
            selected = route == Routes.LIBRARY,
            onClick = { navController.navigateTopLevel(Routes.LIBRARY) },
            icon = { FoxIcon(id = R.drawable.ic_library, size = 24.dp) },
            label = { Text(stringResource(R.string.nav_library)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = route == Routes.ADD,
            onClick = { navController.navigateTopLevel(Routes.ADD) },
            icon = { FoxIcon(id = R.drawable.ic_plus, size = 24.dp) },
            label = { Text(stringResource(R.string.nav_add_short)) },
            colors = itemColors,
        )
    }
}
