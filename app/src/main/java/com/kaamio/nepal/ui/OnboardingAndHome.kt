package com.kaamio.nepal.ui

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.kaamio.nepal.R
import com.kaamio.nepal.data.UserProfile
import com.kaamio.nepal.ui.theme.*
import java.util.Calendar
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(homeViewModel: HomeViewModel, authViewModel: AuthViewModel, profileViewModel: ProfileViewModel, profile: UserProfile) {
    var authMode by rememberSaveable { mutableIntStateOf(-2) } // -2 is Entry Selection, -1 is Walkthrough
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("") }
    var agreedToTerms by remember { mutableStateOf(false) }

    val isAuthLoading by authViewModel.isAuthLoading.collectAsStateWithLifecycle()
    val authError by authViewModel.authError.collectAsStateWithLifecycle()
    val authSuccess by authViewModel.authSuccess.collectAsStateWithLifecycle()
    val pendingEmailVerification by authViewModel.pendingEmailVerification.collectAsStateWithLifecycle()
    val isEmailVerified by authViewModel.isEmailVerified.collectAsStateWithLifecycle()
    val theme = LocalKaamioTheme.current
    val context = LocalContext.current
    val googleErrorBase = stringResource(R.string.error_google_sign_in)

    // Google Sign-In via play-services-auth
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            authViewModel.loginWithGoogle(credential)
        } catch (e: Exception) {
            authViewModel.setAuthError(googleErrorBase.replace("%1\$s", e.message ?: ""))
        }
    }
    val launchGoogleSignIn: () -> Unit = {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleLauncher.launch(GoogleSignIn.getClient(context, options).signInIntent)
    }

    // Reactive Auth Redirect - Ensures UI switches to role selection if user signs in
    LaunchedEffect(profile.isLoggedIn, profile.profileCompleted) {
        if (profile.isLoggedIn) {
            if (!profile.profileCompleted) {
                authMode = 2
            }
        } else {
            // If not logged in, we only switch to Sign In (0) if we've already
            // passed the entry stages.
            if (authMode > 1) {
                authMode = 0
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        if (pendingEmailVerification && !isEmailVerified) {
            EmailVerifyScreen(emailInput, isAuthLoading, authError, authSuccess, { authViewModel.resendVerificationEmail() }, { authViewModel.checkEmailVerified { if(it) authMode = 2 } }, { authMode = 0 })
        } else {
            when (authMode) {
                -2 -> KaamioEntrySelectionScreen { choice ->
                    if (choice == "education") homeViewModel.toggleGateway()
                    authMode = -1 
                }
                -1 -> WalkthroughScreen { authMode = 0 }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 32.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(48.dp))
                        when (authMode) {
                            0 -> SignInScreen(
                                emailInput, { emailInput = it }, passwordInput, { passwordInput = it },
                                { authViewModel.loginWithEmail(emailInput, passwordInput) },
                                { authMode = 1 }, isAuthLoading, authError,
                                { authViewModel.forgotPassword(emailInput) }, authSuccess,
                                authViewModel,
                                launchGoogleSignIn
                            )
                            1 -> SignUpScreen(nameInput, { nameInput = it }, emailInput, { emailInput = it }, passwordInput, { passwordInput = it }, { authViewModel.signUpWithEmail(emailInput, passwordInput, nameInput, agreedToTerms) }, { authMode = 0 }, isAuthLoading, authError, agreedToTerms, { agreedToTerms = it })
                            2 -> RoleSelectionScreen { 
                                selectedRole = it
                                authMode = 3 
                            }
                            3 -> CompleteProfileScreen(selectedRole, profileViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KaamioEntrySelectionScreen(onChoiceSelected: (String) -> Unit) {
    val theme = LocalKaamioTheme.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Quiet-Luxury ambient glow bleeding in from the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            theme.accent.copy(alpha = 0.14f),
                            theme.accent.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // High-Fidelity Identity Mark
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(500)) +
                    scaleIn(initialScale = 0.9f, animationSpec = tween(550, easing = FastOutSlowInEasing))
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .shadow(20.dp, RoundedCornerShape(30.dp), ambientColor = theme.accent.copy(alpha = 0.35f), spotColor = theme.accent.copy(alpha = 0.2f))
                        .clip(RoundedCornerShape(30.dp))
                        .background(theme.surface)
                        .border(1.dp, theme.border, RoundedCornerShape(30.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.kaamio_logo),
                        contentDescription = stringResource(R.string.cd_kaamio_logo),
                        tint = theme.textPrimary,
                        modifier = Modifier.size(58.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Hero Copy
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(600, delayMillis = 120)) +
                    slideInVertically(animationSpec = tween(600, easing = FastOutSlowInEasing)) { it / 3 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.entry_welcome_eyebrow).uppercase(),
                        style = Typography.labelLarge,
                        color = theme.textTertiary,
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.entry_welcome_title),
                            style = Typography.displayMedium,
                            color = theme.textPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-2.5).sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(11.dp).clip(CircleShape).background(theme.accent))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.entry_welcome_subtitle),
                        style = Typography.bodyLarge,
                        color = theme.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Destination Cards
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(650, delayMillis = 220)) +
                    slideInVertically(animationSpec = tween(650, easing = FastOutSlowInEasing)) { it / 3 }
            ) {
                Column {
                    EntryChoiceCard(
                        title = stringResource(R.string.entry_work_title),
                        subtitle = stringResource(R.string.entry_work_subtitle),
                        icon = Icons.Default.Work,
                        accent = theme.accent,
                        onClick = { onChoiceSelected("work") }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    EntryChoiceCard(
                        title = stringResource(R.string.entry_education_title),
                        subtitle = stringResource(R.string.entry_education_subtitle),
                        icon = Icons.Default.School,
                        accent = theme.success,
                        onClick = { onChoiceSelected("education") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(52.dp))

            // Trust Tagline
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = theme.divider)
                Text(
                    text = stringResource(R.string.entry_tagline),
                    style = Typography.labelSmall,
                    color = theme.textTertiary,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = theme.divider)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun EntryChoiceCard(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    val theme = LocalKaamioTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "scale")
    val arrowOffset by animateDpAsState(if (isPressed) 6.dp else 0.dp, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "arrow")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = if (isPressed) 3.dp else 12.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(theme.surface)
            .border(1.dp, if (isPressed) accent.copy(alpha = 0.5f) else theme.border.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(subtitle, style = Typography.bodySmall, color = theme.textSecondary, lineHeight = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp).offset(x = arrowOffset)
                )
            }
        }
    }
}

@Composable
fun WalkthroughScreen(onFinished: () -> Unit) {
    val theme = LocalKaamioTheme.current
    val pages = listOf(
        WalkthroughPage(
            "Professional Gateway", 
            "Connecting Nepal's top specialists with high-fidelity projects and world-class opportunities.", 
            null, // Using custom logo for first page
            R.drawable.walkthrough_work
        ),
        WalkthroughPage(
            "Verified Trust", 
            "A transparent reputation ecosystem where every professional is verified and every contract is secure.", 
            Icons.Default.Shield,
            R.drawable.walkthrough_trust
        ),
        WalkthroughPage(
            "Escrow Protection", 
            "Secure payments via Khalti & eSewa. Funds are released only after you approve the work.", 
            Icons.Default.AccountBalanceWallet,
            R.drawable.card_placeholder
        ),
        WalkthroughPage(
            "The Learning Hub", 
            "Master new skills, earn verified credentials, and accelerate your professional growth.", 
            Icons.Default.School,
            R.drawable.walkthrough_learn
        )
    )
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        containerColor = theme.background,
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                // Persistent Brand Logo at Top
                Icon(
                    painter = painterResource(id = R.drawable.kaamio_logo),
                    contentDescription = "Kaamio Logo",
                    tint = theme.textPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KaamioButton(
                    text = if (pagerState.currentPage < pages.lastIndex) "Continue" else "Get Started",
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinished()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(
                    onClick = onFinished,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = "Skip to Secure Login", 
                        color = theme.textTertiary, 
                        style = Typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // High-Fidelity Interactive Pager
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) { index ->
                Column(modifier = Modifier.fillMaxSize()) {
                    // Image Hero with Parallax-ready Clipping
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .clip(RoundedCornerShape(bottomStart = 80.dp, bottomEnd = 80.dp))
                            .background(theme.surface)
                    ) {
                        Image(
                            painter = painterResource(id = pages[index].imageRes),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(40.dp)
                                .graphicsLayer {
                                    val pageOffset = (pagerState.currentPage - index) + pagerState.currentPageOffsetFraction
                                    scaleX = 1.1f - (kotlin.math.abs(pageOffset) * 0.2f)
                                    scaleY = 1.1f - (kotlin.math.abs(pageOffset) * 0.2f)
                                    alpha = 1f - (kotlin.math.abs(pageOffset) * 0.8f)
                                    rotationZ = pageOffset * 15f
                                },
                            contentScale = ContentScale.Fit
                        )
                        // Atmospheric Depth
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, theme.background.copy(alpha = 0.6f))
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // Content Area
                    Column(modifier = Modifier.padding(horizontal = 32.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (pages[index].icon != null) {
                                Icon(
                                    imageVector = pages[index].icon!!, 
                                    contentDescription = null, 
                                    tint = theme.accent, 
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                            }
                            Text(
                                text = "KAAMIO PROFESSIONAL", 
                                style = Typography.labelSmall, 
                                color = theme.textTertiary, 
                                letterSpacing = 3.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                        
                        Text(
                            text = pages[index].title, 
                            style = Typography.displayMedium, 
                            color = theme.textPrimary, 
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-2).sp,
                            lineHeight = 48.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = pages[index].description,
                            style = Typography.bodyLarge,
                            color = theme.textSecondary,
                            lineHeight = 32.sp,
                            fontSize = 19.sp,
                            modifier = Modifier.graphicsLayer { alpha = 0.9f }
                        )
                    }
                }
            }

            // Indicators Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 32.dp)
                    .offset(y = 100.dp), // Adjust based on visual center
                horizontalArrangement = Arrangement.Start
            ) {
                pages.forEachIndexed { i, _ ->
                    val isSelected = pagerState.currentPage == i
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 40.dp else 10.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(if (isSelected) theme.accent else theme.border.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
        }
    }
}

private data class WalkthroughPage(val title: String, val description: String, val icon: ImageVector?, val imageRes: Int)

@Composable
fun SignInScreen(email: String, onEmailChange: (String) -> Unit, pass: String, onPassChange: (String) -> Unit, onSignIn: () -> Unit, onSignUpInstead: () -> Unit, isLoading: Boolean, error: String?, onForgotPassword: () -> Unit, success: String? = null, authViewModel: AuthViewModel, onGoogleSignIn: () -> Unit = {}) {
    val theme = LocalKaamioTheme.current
    val context = LocalContext.current
    val googleErrorBase = stringResource(R.string.error_google_sign_in)

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            authViewModel.loginWithGoogle(credential)
        } catch (e: Exception) {
            authViewModel.setAuthError(googleErrorBase.replace("%1\$s", e.message ?: ""))
        }
    }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Column(modifier = Modifier.fillMaxWidth()) {
        // High-Fidelity Brand Hero
        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(600)) +
                slideInVertically(animationSpec = tween(600, easing = FastOutSlowInEasing)) { -it / 4 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .shadow(16.dp, RoundedCornerShape(26.dp), ambientColor = theme.accent.copy(alpha = 0.3f), spotColor = theme.accent.copy(alpha = 0.18f))
                        .clip(RoundedCornerShape(26.dp))
                        .background(theme.surface)
                        .border(1.dp, theme.border, RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.KeyboardCommandKey, null, tint = theme.textPrimary, modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.sign_in_title),
                    style = Typography.displaySmall,
                    color = theme.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.sign_in_subtitle),
                    style = Typography.bodyLarge,
                    color = theme.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        if (error != null) KaamioErrorBanner(error, modifier = Modifier.padding(bottom = 24.dp))
        if (success != null) KaamioSuccessBanner(success, modifier = Modifier.padding(bottom = 24.dp))

        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(600, delayMillis = 120)) +
                slideInVertically(animationSpec = tween(600, easing = FastOutSlowInEasing)) { it / 4 }
        ) {
            Column {
                KaamioTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = stringResource(R.string.label_email),
                    placeholder = stringResource(R.string.hint_email),
                    leadingIcon = Icons.Outlined.Email
                )
                Spacer(modifier = Modifier.height(18.dp))
                KaamioTextField(
                    value = pass,
                    onValueChange = onPassChange,
                    label = stringResource(R.string.label_password),
                    isPassword = true,
                    leadingIcon = Icons.Outlined.Lock
                )

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = stringResource(R.string.btn_forgot_password),
                        style = Typography.labelLarge,
                        color = theme.textSecondary,
                        modifier = Modifier
                            .clickable { onForgotPassword() }
                            .padding(vertical = 12.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(600, delayMillis = 200)) +
                slideInVertically(animationSpec = tween(600, easing = FastOutSlowInEasing)) { it / 4 }
        ) {
            KaamioButton(
                text = stringResource(R.string.btn_sign_in),
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
                isLoading = isLoading
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(600, delayMillis = 260))
        ) {
            KaamioOrDivider(stringResource(R.string.or_continue_with))
        }
        Spacer(modifier = Modifier.height(24.dp))
        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(600, delayMillis = 320)) +
                slideInVertically(animationSpec = tween(600, easing = FastOutSlowInEasing)) { it / 4 }
        ) {
            KaamioSecondaryButton(
                text = stringResource(R.string.btn_continue_google),
                icon = Icons.Outlined.AccountCircle,
                onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.text_no_account), color = theme.textSecondary, style = Typography.bodyMedium)
            Text(
                text = stringResource(R.string.link_create_one),
                color = theme.textPrimary,
                style = Typography.bodyMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clickable { onSignUpInstead() }
                    .padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun SignUpScreen(name: String, onNameChange: (String) -> Unit, email: String, onEmailChange: (String) -> Unit, pass: String, onPassChange: (String) -> Unit, onSignUp: () -> Unit, onSignInInstead: () -> Unit, isLoading: Boolean, error: String?, agreedToTerms: Boolean = false, onTermsChange: (Boolean) -> Unit = {}) {
    val theme = LocalKaamioTheme.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(theme.textPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PersonAdd, null, tint = theme.background, modifier = Modifier.size(32.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.sign_up_title), 
            style = Typography.displaySmall, 
            color = theme.textPrimary, 
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp
        )
        Text(
            text = "Create your verified professional identity.",
            style = Typography.bodyLarge,
            color = theme.textSecondary
        )

        Spacer(modifier = Modifier.height(40.dp))
        if (error != null) KaamioErrorBanner(error, modifier = Modifier.padding(bottom = 24.dp))
        
        KaamioTextField(
            value = name, 
            onValueChange = onNameChange, 
            label = stringResource(R.string.label_full_name), 
            placeholder = stringResource(R.string.hint_full_name),
            leadingIcon = Icons.Outlined.Badge
        )
        Spacer(modifier = Modifier.height(20.dp))
        KaamioTextField(
            value = email, 
            onValueChange = onEmailChange, 
            label = stringResource(R.string.label_email), 
            placeholder = stringResource(R.string.hint_email),
            leadingIcon = Icons.Outlined.Email
        )
        Spacer(modifier = Modifier.height(20.dp))
        KaamioTextField(
            value = pass, 
            onValueChange = onPassChange, 
            label = stringResource(R.string.label_password), 
            isPassword = true,
            leadingIcon = Icons.Outlined.Lock
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTermsChange(!agreedToTerms) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = agreedToTerms,
                onCheckedChange = { onTermsChange(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = theme.textPrimary,
                    uncheckedColor = theme.textSecondary,
                    checkmarkColor = theme.background
                )
            )
            Text(
                text = stringResource(R.string.text_agree_terms),
                style = Typography.bodySmall,
                color = theme.textSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        KaamioButton(
            text = stringResource(R.string.btn_create_account), 
            onClick = onSignUp, 
            modifier = Modifier.fillMaxWidth(), 
            isLoading = isLoading,
            enabled = agreedToTerms && !isLoading
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.text_already_account), color = theme.textSecondary, style = Typography.bodyMedium)
            Text(
                text = stringResource(R.string.link_sign_in_instead),
                color = theme.textPrimary,
                style = Typography.bodyMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clickable { onSignInInstead() }
                    .padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun RoleSelectionScreen(onRoleSelect: (String) -> Unit) {
    val theme = LocalKaamioTheme.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(theme.textPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Diversity3, null, tint = theme.background, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.role_selection_title), 
            style = Typography.displaySmall, 
            color = theme.textPrimary, 
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp
        )
        Text(
            text = "Tailor your experience to your professional goals.",
            style = Typography.bodyLarge,
            color = theme.textSecondary
        )

        Spacer(modifier = Modifier.height(40.dp))
        
        KaamioRoleCard(
            title = stringResource(R.string.role_work_title), 
            subtitle = stringResource(R.string.role_work_desc), 
            icon = Icons.Outlined.WorkOutline, 
            isSelected = false,
            onClick = { onRoleSelect("work") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        KaamioRoleCard(
            title = stringResource(R.string.role_hire_title), 
            subtitle = stringResource(R.string.role_hire_desc), 
            icon = Icons.Outlined.PersonAdd, 
            isSelected = false,
            onClick = { onRoleSelect("hire") }
        )
    }
}

@Composable
fun CompleteProfileScreen(role: String, profileViewModel: ProfileViewModel) {
    val theme = LocalKaamioTheme.current
    var phone by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    
    val isUpdating by profileViewModel.isUpdating.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(theme.textPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.VerifiedUser, null, tint = theme.background, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Essential Details", 
            style = Typography.displaySmall, 
            color = theme.textPrimary, 
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp
        )
        Text(
            text = "Required for a verified Kaamio account.",
            style = Typography.bodyLarge,
            color = theme.textSecondary
        )

        Spacer(modifier = Modifier.height(40.dp))
        
        KaamioTextField(
            value = phone, 
            onValueChange = { phone = it }, 
            label = "Phone Number", 
            placeholder = "98XXXXXXXX",
            leadingIcon = Icons.Outlined.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                KaamioTextField(value = province, onValueChange = { province = it }, label = "Province", placeholder = "e.g. Bagmati")
            }
            Box(modifier = Modifier.weight(1f)) {
                KaamioTextField(value = district, onValueChange = { district = it }, label = "District", placeholder = "e.g. Kathmandu")
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        KaamioTextField(
            value = experience, 
            onValueChange = { experience = it }, 
            label = if (role == "work") "Years of Experience" else "Company / Business Name",
            placeholder = if (role == "work") "e.g. 5 years in Carpentry" else "e.g. Ktm Builders",
            leadingIcon = if (role == "work") Icons.Outlined.Timeline else Icons.Outlined.Business
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        KaamioButton(
            text = "Complete Onboarding", 
            onClick = { 
                profileViewModel.completeOnboarding(role, province, district, experience, phone, "English")
            }, 
            modifier = Modifier.fillMaxWidth(),
            isLoading = isUpdating
        )
    }
}

@Composable
fun EmailVerifyScreen(email: String, isLoading: Boolean, error: String?, success: String?, onResend: () -> Unit, onCheck: () -> Unit, onBack: () -> Unit) {
    val theme = LocalKaamioTheme.current
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.MarkEmailRead, null, tint = theme.textPrimary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.email_verify_title), style = Typography.headlineMedium, color = theme.textPrimary, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.email_verify_desc, email), color = theme.textSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        KaamioButton(stringResource(R.string.email_verify_check), onCheck, modifier = Modifier.fillMaxWidth(), isLoading = isLoading)
        Spacer(modifier = Modifier.height(16.dp))
        KaamioSecondaryButton(stringResource(R.string.email_verify_resend), null, onResend, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun HomeScreen(homeViewModel: HomeViewModel, marketViewModel: MarketViewModel, learningViewModel: LearningViewModel, globalViewModel: GlobalViewModel, profile: UserProfile) {
    val currentGateway by homeViewModel.currentGateway.collectAsStateWithLifecycle()
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingRes = when { 
        hour < 12 -> R.string.greeting_morning 
        hour < 17 -> R.string.greeting_afternoon 
        else -> R.string.greeting_evening 
    }
    val theme = LocalKaamioTheme.current
    var quickActionsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { quickActionsVisible = true }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            // High-Fidelity Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { homeViewModel.toggleGateway() }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    hour < 12 -> Icons.Outlined.WbSunny
                                    hour < 17 -> Icons.Outlined.LightMode
                                    else -> Icons.Outlined.Nightlight
                                },
                                contentDescription = null,
                                tint = theme.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${stringResource(greetingRes).uppercase()} • ${currentGateway.uppercase()}", 
                                style = Typography.labelSmall, 
                                color = theme.textSecondary, 
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.SyncAlt, null, tint = theme.accent, modifier = Modifier.size(12.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = profile.name.ifEmpty { stringResource(R.string.greeting_explorer) }, 
                            style = Typography.displaySmall,
                            fontWeight = FontWeight.Black, 
                            color = theme.textPrimary,
                            letterSpacing = (-1).sp
                        )
                    }
                    
                    // Reputation & Profile Status
                    Surface(
                        onClick = { homeViewModel.navigateTo(Screen.TrustLedger) },
                        shape = RoundedCornerShape(20.dp),
                        color = theme.surface,
                        border = BorderStroke(1.dp, theme.border.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (profile.trustScore > 80) theme.success else theme.accent)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${profile.trustScore}%", 
                                style = Typography.labelLarge, 
                                color = theme.textPrimary, 
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Clean Quick Action Grid
            item {
                PremiumSectionHeader(
                    if (currentGateway == "education") "Learning Universe" else stringResource(R.string.section_quick_actions)
                )
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedVisibility(visible = quickActionsVisible, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (currentGateway == "education") {
                            // Education Dashboard
                            Row(modifier = Modifier.fillMaxWidth()) {
                                QuickActionCard(stringResource(R.string.action_learn_skills), "Credentials", Icons.Outlined.School, KaamioColors.LearnSkills, Modifier.weight(1f)) {
                                    homeViewModel.navigateTo(Screen.Learn)
                                }
                                QuickActionCard(stringResource(R.string.educator_hub_title), stringResource(R.string.educator_hub_desc), Icons.Default.AutoAwesome, KaamioColors.TeachSkills, Modifier.weight(1f)) {
                                    homeViewModel.navigateTo(Screen.TeacherDashboard)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                QuickActionCard("Course Credentials", "View Certifications", Icons.Outlined.WorkspacePremium, theme.accent, Modifier.weight(1f)) {
                                    // Navigate to credentials
                                }
                                QuickActionCard("Skill Assessment", "Verify Proficiency", Icons.Outlined.AssignmentTurnedIn, theme.success, Modifier.weight(1f)) {
                                    // Navigate to assessments
                                }
                            }
                        } else {
                            // Work Dashboard
                            Row(modifier = Modifier.fillMaxWidth()) {
                                QuickActionCard(stringResource(R.string.action_find_job), stringResource(R.string.role_work_desc), Icons.Default.Search, KaamioColors.FindJobs, Modifier.weight(1f)) {
                                    marketViewModel.setMarketTab(KaamioConstants.TAB_JOBS)
                                    homeViewModel.navigateTo(Screen.Market)
                                }
                                QuickActionCard(stringResource(R.string.action_hire_worker), stringResource(R.string.role_hire_desc), Icons.Outlined.PersonAdd, KaamioColors.HireWorkers, Modifier.weight(1f)) {
                                    marketViewModel.setMarketTab(KaamioConstants.TAB_FREELANCE)
                                    homeViewModel.navigateTo(Screen.Market)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                QuickActionCard(stringResource(R.string.market_tab_service), "Expert Services", Icons.Default.Engineering, theme.accent, Modifier.weight(1f)) {
                                    marketViewModel.setMarketTab(KaamioConstants.TAB_LOCAL)
                                    homeViewModel.navigateTo(Screen.Market)
                                }
                                QuickActionCard(stringResource(R.string.my_work_title), "Track progress", Icons.Outlined.Assignment, KaamioColors.TrustLedger, Modifier.weight(1f)) {
                                    homeViewModel.navigateTo(Screen.MyActivities)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(title: String, subtitle: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f)
    val theme = LocalKaamioTheme.current

    Card(
        modifier = modifier
            .padding(8.dp)
            .height(175.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        colors = CardDefaults.cardColors(containerColor = theme.card),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, if(isPressed) accent.copy(alpha = 0.3f) else theme.border.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent.copy(alpha = 0.1f))
                    .border(1.dp, accent.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(26.dp))
            }
            Column {
                Text(title, style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, style = Typography.labelSmall, color = theme.textSecondary)
            }
        }
    }
}
