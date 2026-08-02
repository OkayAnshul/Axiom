package com.cosmiclaboratory.axiom.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.cosmiclaboratory.axiom.ui.components.LocalSnackbarHostState
import com.cosmiclaboratory.axiom.ui.design.components.AxiomEmptyState
import com.cosmiclaboratory.axiom.ui.screens.today.TodayScreen
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import kotlin.reflect.KClass

private data class Tab(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val matches: KClass<*>
)

@Composable
fun AxiomNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startOnboarding: Boolean = false
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    val tabs = remember {
        listOf(
            Tab("Today", Icons.Filled.WbSunny, Today, Today::class),
            Tab("Journal", Icons.AutoMirrored.Filled.MenuBook, Journal, Journal::class),
            Tab("Patterns", Icons.Filled.ShowChart, Patterns, Patterns::class),
            Tab("Ask", Icons.Filled.AutoAwesome, Ask, Ask::class)
        )
    }

    // Derived from the destination HIERARCHY, not a hardcoded route set — that
    // set went stale every time a route was renamed.
    val showBottomBar = destination?.hierarchyContains(Main::class) == true

    Scaffold(
        modifier = modifier,
        containerColor = AxiomTheme.colors.canvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = AxiomTheme.colors.surface) {
                    tabs.forEach { tab ->
                        val selected = destination?.hierarchyContains(tab.matches) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTab(navController, tab.route) },
                            // Labels are always shown; an icon-only nav bar is the
                            // same discoverability failure as an unlabelled button.
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label, style = AxiomTheme.type.uiLabelSmall) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AxiomTheme.colors.onAccentSoft,
                                selectedTextColor = AxiomTheme.colors.ink,
                                indicatorColor = AxiomTheme.colors.accentSoft,
                                unselectedIconColor = AxiomTheme.colors.inkFaint,
                                unselectedTextColor = AxiomTheme.colors.inkFaint
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            NavHost(
                navController = navController,
                startDestination = if (startOnboarding) Onboarding else Main,
                modifier = Modifier.padding(padding),
                // Fade + a slight scale, not a horizontal slide. Sliding implies a
                // linear sequence, which is wrong for peer tabs.
                enterTransition = { fadeIn(tween(180)) + scaleIn(tween(220), 0.97f) },
                exitTransition = { fadeOut(tween(140)) },
                popEnterTransition = { fadeIn(tween(180)) + scaleIn(tween(220), 1.02f) },
                popExitTransition = { fadeOut(tween(140)) + scaleOut(tween(180), 0.98f) }
            ) {
                composable<Onboarding> {
                    Placeholder("Onboarding", "Lands in Phase 7.")
                }

                navigation<Main>(startDestination = Today) {
                    composable<Today>(
                        deepLinks = listOf(navDeepLink { uriPattern = AxiomDeepLinks.TODAY })
                    ) {
                        TodayScreen(
                            onNewEntry = { navController.navigate(Composer()) },
                            onVoiceEntry = { navController.navigate(Composer(voice = true)) },
                            onOpenEntry = { id -> navController.navigate(Reader(id)) },
                            onWriteAboutPrompt = { qId ->
                                navController.navigate(Composer(questionId = qId))
                            },
                            onSeeAllEntries = { navigateTab(navController, Journal) },
                            onOpenSettings = { navController.navigate(Settings) }
                        )
                    }

                    composable<Journal>(
                        deepLinks = listOf(navDeepLink { uriPattern = AxiomDeepLinks.JOURNAL })
                    ) {
                        Placeholder("Journal", "The timeline lands in Phase 5.")
                    }

                    composable<Patterns>(
                        deepLinks = listOf(navDeepLink { uriPattern = AxiomDeepLinks.PATTERNS })
                    ) {
                        Placeholder("Patterns", "Lands in Phase 7, on top of the ML work.")
                    }

                    composable<Ask> {
                        Placeholder("Ask", "Grounded chat lands in Phase 6.")
                    }
                }

                composable<Composer>(
                    deepLinks = listOf(
                        navDeepLink { uriPattern = AxiomDeepLinks.COMPOSER },
                        navDeepLink { uriPattern = "${AxiomDeepLinks.COMPOSER}?voice={voice}" }
                    )
                ) { entry ->
                    val args = entry.toRoute<Composer>()
                    Placeholder(
                        "Composer",
                        "entryId=${args.entryId} questionId=${args.questionId} voice=${args.voice}\nLands in Phase 4."
                    )
                }

                composable<Reader>(
                    deepLinks = listOf(navDeepLink { uriPattern = "${AxiomDeepLinks.SCHEME}://reader/{entryId}" })
                ) { entry ->
                    Placeholder("Reader", "entryId=${entry.toRoute<Reader>().entryId}\nLands in Phase 4.")
                }

                composable<EntryInsight> { Placeholder("Insight", "Lands in Phase 6.") }
                composable<Search> { Placeholder("Search", "Lands in Phase 5.") }
                composable<Calendar> { Placeholder("Calendar", "Lands in Phase 5.") }

                composable<Settings> { Placeholder("Settings", "Lands in Phase 7.") }
                composable<SettingsAi>(
                    deepLinks = listOf(navDeepLink { uriPattern = AxiomDeepLinks.SETTINGS_AI })
                ) { Placeholder("AI settings", "Lands in Phase 7.") }
            }
        }
    }
}

/** True when [target] appears anywhere in this destination's parent chain. */
private fun NavDestination.hierarchyContains(target: KClass<*>): Boolean =
    hierarchy.any { node -> node.hasRoute(target) }

private fun navigateTab(navController: NavHostController, route: Any) {
    navController.navigate(route) {
        // Save/restore per-tab state, and pop to the graph root so the back stack
        // cannot accumulate one entry per tab switch.
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Temporary surface for destinations that land in later phases. */
@Composable
private fun Placeholder(title: String, body: String) {
    AxiomEmptyState(title = title, body = body)
}
