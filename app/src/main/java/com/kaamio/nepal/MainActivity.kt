package com.kaamio.nepal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.kaamio.nepal.ui.*
import com.kaamio.nepal.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean

private const val MAIN_GRAPH_ROUTE = "main"

@Composable
private fun NavHostController.mainGraphEntry(entry: NavBackStackEntry): NavBackStackEntry =
    remember(entry) { getBackStackEntry(MAIN_GRAPH_ROUTE) }

@Composable
private fun NavHostController.mainMarketViewModel(entry: NavBackStackEntry): MarketViewModel =
    hiltViewModel(mainGraphEntry(entry))

@Composable
private fun NavHostController.mainLearningViewModel(entry: NavBackStackEntry): LearningViewModel =
    hiltViewModel(mainGraphEntry(entry))

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val keepSplash = AtomicBoolean(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplash.get() }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MainComposable(keepSplash) }
    }
}

@Composable
private fun MainComposable(keepSplash: AtomicBoolean) {
    val homeViewModel: HomeViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val globalViewModel: GlobalViewModel = hiltViewModel()
    
    val userProfile by profileViewModel.userProfile.collectAsStateWithLifecycle()
    val isReady by homeViewModel.isReady.collectAsStateWithLifecycle()
    val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* granted or denied; app continues regardless */ }
    val permissionContext = LocalContext.current
    LaunchedEffect(isReady) {
        if (isReady && Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(permissionContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(isReady) { if (isReady) keepSplash.set(false) }

    LaunchedEffect(Unit) {
        homeViewModel.currentScreen.collect { screen ->
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != screen.route) {
                // If the target screen is the parent of current, just pop
                val parentOfCurrent = parentScreen(currentRoute ?: "")
                if (parentOfCurrent != null && parentOfCurrent.route == screen.route) {
                    navController.popBackStack()
                } else {
                    navController.navigate(screen.route) {
                        if (screen.route == "onboarding" || screen.route == "loading" || currentRoute == "loading") {
                            popUpTo(0) { inclusive = true }
                        } else {
                            launchSingleTop = true
                            // If navigating to a root tab, ensure state is saved/restored
                            if (screen.route in listOf("home", "market", "learn", "community", "chat", "profile")) {
                                popUpTo(MAIN_GRAPH_ROUTE) { saveState = true }
                                restoreState = true
                            }
                        }
                    }
                }
            }
        }
    }

    val currentScreenState by homeViewModel.currentScreen.collectAsStateWithLifecycle()
    BackHandler(enabled = parentScreen(currentScreenState.route) != null) {
        parentScreen(currentScreenState.route)?.let(homeViewModel::navigateTo)
    }

    fun handleIntent(intent: android.content.Intent?) {
        val targetScreen = intent?.getStringExtra("screen")
        if (targetScreen != null) {
            homeViewModel.navigateTo(screenForDeepLink(targetScreen))
            return
        }
        val data = intent?.data ?: return
        data.lastPathSegment?.lowercase()?.let { path ->
            when (path) {
                "market", "marketplace" -> homeViewModel.navigateTo(Screen.Market)
                "learn", "learning" -> homeViewModel.navigateTo(Screen.Learn)
                "community" -> homeViewModel.navigateTo(Screen.Community)
                "chat", "messages" -> homeViewModel.navigateTo(Screen.Chat)
                "profile" -> homeViewModel.navigateTo(Screen.Profile)
                "trust", "trust-ledger" -> homeViewModel.navigateTo(Screen.TrustLedger)
            }
        }
    }

    val activity = LocalContext.current as? ComponentActivity
    val activityContext = LocalContext.current
    DisposableEffect(Unit) {
        val disposable = activity?.addOnNewIntentListener { intent ->
            handleIntent(intent)
            false
        }
        onDispose { (disposable as? AutoCloseable)?.close() }
    }
    LaunchedEffect(Unit) {
        handleIntent(activity?.intent)
    }

    MyApplicationTheme(darkTheme = isDarkMode) {
        val theme = LocalKaamioTheme.current
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route
        val currentGateway by homeViewModel.currentGateway.collectAsStateWithLifecycle()

        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            SnackbarBroker.messages.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }
        
        val showBottomNav = when (currentRoute) {
            Screen.Home.route, Screen.Market.route, Screen.Learn.route, 
            Screen.Chat.route, Screen.Profile.route,
            Screen.TeacherDashboard.route, Screen.MyActivities.route -> true
            else -> false
        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            containerColor = theme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(theme.background)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Loading.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Loading.route) { 
                        CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                            LoadingScreenView() 
                        }
                    }
                    composable(
                        route = Screen.Error("{message}").route,
                        arguments = listOf(navArgument("message") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val message = backStackEntry.arguments?.getString("message") ?: "Unknown Error"
                        CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                            ErrorScreenView(message = message, onRetry = { homeViewModel.retryInit() })
                        }
                    }
                    composable(Screen.Onboarding.route) { 
                        val authViewModel: AuthViewModel = hiltViewModel()
                        CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                            OnboardingScreen(homeViewModel, authViewModel, profileViewModel, userProfile)
                        }
                    }
                    navigation(
                        startDestination = Screen.Home.route,
                        route = MAIN_GRAPH_ROUTE
                    ) {
                        composable(Screen.Home.route) { entry ->
                            val marketViewModel: MarketViewModel = navController.mainMarketViewModel(entry)
                            val learningViewModel: LearningViewModel = navController.mainLearningViewModel(entry)
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                HomeScreen(homeViewModel, marketViewModel, learningViewModel, globalViewModel, userProfile) 
                            }
                        }
                        composable(Screen.Market.route) { entry ->
                            val marketViewModel: MarketViewModel = navController.mainMarketViewModel(entry)
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                MarketplaceMainScreen(homeViewModel, marketViewModel, globalViewModel, userProfile) 
                            }
                        }
                        composable(Screen.Learn.route) { entry ->
                            val learningViewModel: LearningViewModel = navController.mainLearningViewModel(entry)
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                LearningHubScreen(homeViewModel, learningViewModel, globalViewModel, userProfile) 
                            }
                        }
                        composable(Screen.Community.route) { 
                            val communityViewModel: CommunityViewModel = hiltViewModel()
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                CommunityScreen(homeViewModel, communityViewModel, globalViewModel, userProfile) 
                            }
                        }
                        composable(Screen.Chat.route) { 
                            val chatViewModel: ChatViewModel = hiltViewModel()
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                MessagesScreen(homeViewModel, chatViewModel, userProfile)
                            }
                        }
                        composable(Screen.Negotiation.route) { 
                            val chatViewModel: ChatViewModel = hiltViewModel()
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                NegotiationChatScreen(homeViewModel, chatViewModel, userProfile)
                            }
                        }
                        composable(Screen.TrustLedger.route) { 
                            val trustProfileViewModel: ProfileViewModel = hiltViewModel()
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                TrustLedgerScreen(homeViewModel, trustProfileViewModel, userProfile)
                            }
                        }
                        composable(Screen.TeacherDashboard.route) { entry ->
                            val learningViewModel: LearningViewModel = navController.mainLearningViewModel(entry)
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                TeachingDashboardScreen(homeViewModel, learningViewModel, userProfile) 
                            }
                        }
                        composable(Screen.CourseDetail.route) { entry ->
                            val learningViewModel: LearningViewModel = navController.mainLearningViewModel(entry)
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                CourseDetailScreen(homeViewModel, learningViewModel)
                            }
                        }
                        composable(Screen.Profile.route) { 
                            val profileEditViewModel: ProfileViewModel = hiltViewModel()
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                ProfileScreen(homeViewModel, profileEditViewModel, userProfile) 
                            }
                        }
                        composable(Screen.Settings.route) { 
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                SettingsScreen(homeViewModel, settingsViewModel, globalViewModel, userProfile) 
                            }
                        }
                        composable(Screen.Notifications.route) {
                            val notificationsVM: NotificationsViewModel = hiltViewModel()
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                NotificationsScreen(homeViewModel, notificationsVM)
                            }
                        }
                        composable(Screen.IconShowcase.route) { 
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                KaamioIconShowcaseScreen(homeViewModel) 
                            }
                        }
                        composable(Screen.PostListing.route) { entry ->
                            val marketViewModel: MarketViewModel = navController.mainMarketViewModel(entry)
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                PostListingScreen(homeViewModel, marketViewModel, userProfile) 
                            }
                        }
                        composable(Screen.CreateCourse.route) { entry ->
                            val learningViewModel: LearningViewModel = navController.mainLearningViewModel(entry)
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                CreateCourseScreen(homeViewModel, learningViewModel, userProfile)
                            }
                        }
                        composable(Screen.EditProfile.route) {
                            val editProfileVM: ProfileViewModel = hiltViewModel()
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                EditProfileScreen(homeViewModel, editProfileVM, userProfile)
                            }
                        }
                        composable(Screen.Payment.route) {
                            val paymentVM: PaymentViewModel = hiltViewModel()
                            val pendingJobId by homeViewModel.pendingPaymentJobId.collectAsStateWithLifecycle()
                            val pendingWorkerId by homeViewModel.pendingPaymentWorkerId.collectAsStateWithLifecycle()
                            val pendingWorkerName by homeViewModel.pendingPaymentWorkerName.collectAsStateWithLifecycle()
                            val pendingAmount by homeViewModel.pendingPaymentAmount.collectAsStateWithLifecycle()
                            LaunchedEffect(pendingJobId) {
                                if (pendingJobId != null) paymentVM.observeEscrow(pendingJobId ?: "")
                            }
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                PaymentScreen(
                                    homeViewModel = homeViewModel,
                                    paymentViewModel = paymentVM,
                                    profile = userProfile,
                                    jobId = pendingJobId ?: "",
                                    workerId = pendingWorkerId,
                                    workerName = pendingWorkerName,
                                    defaultAmount = pendingAmount
                                )
                            }
                        }
                        composable(Screen.MyActivities.route) {
                            val activitiesVM: MyActivitiesViewModel = hiltViewModel()
                            CompositionLocalProvider(
    LocalContext provides LocaleHelper.wrap(activityContext, currentLanguage),
    LocalActivityResultRegistryOwner provides (activity!!)
) {
                                MyActivitiesScreen(homeViewModel, activitiesVM)
                            }
                        }
                    }
                }
                
                if (showBottomNav) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp)
                    ) {
                        KaamioBottomNav(
                            currentRoute = currentRoute ?: Screen.Home.route, 
                            gateway = currentGateway,
                            onNavigate = { screen -> homeViewModel.navigateTo(screen) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorScreenView(message: String, onRetry: () -> Unit) {
    val theme = LocalKaamioTheme.current
    Box(modifier = Modifier.fillMaxSize().background(theme.background).padding(KaamioSpacing.xxl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(96.dp).clip(KaamioShapes.extraLarge).background(theme.card), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Warning, contentDescription = null, tint = theme.error, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(KaamioSpacing.xl))
            Text(text = stringResource(R.string.error_generic_title), style = Typography.headlineLarge, color = theme.textPrimary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(KaamioSpacing.sm))
            Text(text = message, style = Typography.bodyLarge, color = theme.textSecondary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(KaamioSpacing.xxl))
            KaamioButton(text = stringResource(R.string.action_retry), onClick = onRetry, modifier = Modifier.fillMaxWidth(0.6f))
        }
    }
}

@Composable
fun LoadingScreenView() {
    val theme = LocalKaamioTheme.current
    Box(modifier = Modifier.fillMaxSize().background(theme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(72.dp).clip(KaamioShapes.large).background(theme.card), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.accent, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(KaamioSpacing.lg))
            Text(stringResource(R.string.loading), style = Typography.bodyLarge, color = theme.textSecondary)
        }
    }
}

private fun parentScreen(route: String): Screen? = when (route) {
    Screen.Payment.route -> Screen.Market
    Screen.Negotiation.route -> Screen.Chat
    Screen.PostListing.route -> Screen.Market
    Screen.CreateCourse.route -> Screen.TeacherDashboard
    Screen.EditProfile.route -> Screen.Profile
    Screen.Settings.route -> Screen.Profile
    Screen.Notifications.route -> Screen.Home
    Screen.MyActivities.route -> Screen.Home
    Screen.TrustLedger.route -> Screen.Profile
    Screen.TeacherDashboard.route -> Screen.Learn
    Screen.CourseDetail.route -> Screen.Learn
    else -> null
}

private fun screenForDeepLink(name: String): Screen = when (name.lowercase()) {
    "market", "marketplace" -> Screen.Market
    "learn", "learning" -> Screen.Learn
    "community" -> Screen.Community
    "chat", "messages" -> Screen.Chat
    "profile" -> Screen.Profile
    "trust_ledger", "trust", "trust-ledger" -> Screen.TrustLedger
    else -> Screen.Home
}
