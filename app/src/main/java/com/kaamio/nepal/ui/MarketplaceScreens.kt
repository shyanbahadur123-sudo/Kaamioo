package com.kaamio.nepal.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kaamio.nepal.R
import com.kaamio.nepal.data.JobListing
import com.kaamio.nepal.data.UserProfile
import com.kaamio.nepal.ui.theme.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceMainScreen(homeViewModel: HomeViewModel, marketViewModel: MarketViewModel, globalViewModel: GlobalViewModel, profile: UserProfile) {
    val activeTab by marketViewModel.activeMarketTab.collectAsStateWithLifecycle()
    val searchQuery by marketViewModel.marketSearchQuery.collectAsStateWithLifecycle()
    val activeChip by marketViewModel.selectedChipFilter.collectAsStateWithLifecycle()
    val jobsList by marketViewModel.jobsList.collectAsStateWithLifecycle()
    val isLoading by marketViewModel.isJobsLoading.collectAsStateWithLifecycle()
    val theme = LocalKaamioTheme.current
    
    val tabs = listOf(KaamioConstants.TAB_JOBS, KaamioConstants.TAB_LOCAL, KaamioConstants.TAB_FREELANCE)
    val pagerState = rememberPagerState(initialPage = tabs.indexOf(activeTab).coerceAtLeast(0)) { tabs.size }

    LaunchedEffect(activeTab) {
        val target = tabs.indexOf(activeTab).coerceAtLeast(0)
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        marketViewModel.setMarketTab(tabs[pagerState.currentPage])
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // High-Fidelity Marketplace Header
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text(text = "Marketplace", style = Typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = theme.textPrimary)
                Spacer(modifier = Modifier.height(24.dp))
                
                // Pill Tabs (Glassmorphic Selection)
                PremiumMarketTabs(
                    tabs = tabs,
                    labels = listOf("Jobs", "Local Experts", "Project Gigs"),
                    activeTab = activeTab,
                    onTabSelect = { marketViewModel.setMarketTab(it) }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Professional Search & Filter
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = searchQuery, onValueChange = { marketViewModel.setMarketSearchQuery(it) },
                        placeholder = { Text("Search skills, roles or location...", style = Typography.bodyLarge, color = theme.textTertiary, maxLines = 1) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.textSecondary, modifier = Modifier.size(24.dp)) },
                        modifier = Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = theme.surface, unfocusedContainerColor = theme.surface,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = theme.textPrimary, cursorColor = theme.textPrimary
                        ),
                        singleLine = true,
                        textStyle = Typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(theme.surface)
                            .border(1.dp, theme.border.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                            .clickable {
                                marketViewModel.setChipFilter(KaamioConstants.CHIP_ALL_ROLES)
                                marketViewModel.setMarketSearchQuery("")
                            }, 
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = stringResource(R.string.cd_reset_filters),
                            tint = theme.textPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Dynamic Filter Chips
                val chips = remember(activeTab) { marketViewModel.getMarketChipsForTab(activeTab) }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 24.dp)
                ) {
                    items(chips, key = { it }) { chip -> 
                        FilterChip(text = chip, isSelected = activeChip == chip, onClick = { marketViewModel.setChipFilter(chip) }) 
                    }
                }
            }
            
            // Content Area with Pull-to-Refresh
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { marketViewModel.refreshJobs() },
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val pageTab = tabs[page]
                    val pageJobs = remember(pageTab, jobsList) { marketViewModel.filterJobsByTab(jobsList, pageTab) }
                    MarketListView(pageJobs, isLoading, pageTab,
                        onAction = { jobId ->
                            if (pageTab == KaamioConstants.TAB_LOCAL) {
                                val target = pageJobs.find { it.id == jobId }
                                if (target != null) homeViewModel.startNewChat(target.ownerId, target.company, target.logoUrl)
                            } else {
                                marketViewModel.applyToJob(jobId)
                            }
                        },
                        onBookmark = { jobId, currentlyBookmarked -> marketViewModel.toggleBookmark(jobId, currentlyBookmarked) }
                    )
                }
            }
        }

        // Floating Action Button - Positioned in Bottom-Right Thumb Zone
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).padding(bottom = 100.dp)) {
            Surface(
                onClick = { homeViewModel.navigateTo(Screen.PostListing) },
                modifier = Modifier.size(64.dp).premiumShadow(elevation = 12.dp),
                shape = RoundedCornerShape(22.dp),
                color = theme.textPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, null, tint = theme.background, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun PostListingScreen(homeViewModel: HomeViewModel, marketViewModel: MarketViewModel, profile: UserProfile) {
    val activeTab by marketViewModel.activeMarketTab.collectAsStateWithLifecycle()
    val isPosting by marketViewModel.isPosting.collectAsStateWithLifecycle()
    val theme = LocalKaamioTheme.current
    
    var title by remember { mutableStateOf("") }
    var company by remember { mutableStateOf(profile.name) }
    var salary by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(profile.district) }
    var budget by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val (titleLabel, subtitle) = when (activeTab) {
        KaamioConstants.TAB_LOCAL -> "Offer Service" to "List your professional expert services"
        KaamioConstants.TAB_FREELANCE -> "Post Project" to "Define project scope for freelance talent"
        else -> "New Job" to "Hire for verified full-time or remote roles"
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            KaamioScreenHeader(titleLabel, onBack = { 
                homeViewModel.navigateTo(Screen.Market)
            })
            Text(subtitle, style = Typography.labelSmall, color = theme.textSecondary, modifier = Modifier.padding(horizontal = 32.dp))
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = Modifier.padding(horizontal = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                when (activeTab) {
                    KaamioConstants.TAB_JOBS -> {
                        KaamioTextField(value = title, onValueChange = { title = it }, label = "Role Title", placeholder = "e.g. Kotlin Developer")
                        KaamioTextField(value = company, onValueChange = { company = it }, label = "Company")
                        KaamioTextField(value = salary, onValueChange = { salary = it }, label = "Annual/Monthly Salary")
                    }
                    KaamioConstants.TAB_LOCAL -> {
                        KaamioTextField(value = title, onValueChange = { title = it }, label = "Service Name", placeholder = "e.g. Master Electrician")
                        KaamioTextField(value = company, onValueChange = { company = it }, label = "Business Display Name")
                        KaamioTextField(value = salary, onValueChange = { salary = it }, label = "Base Service Rate")
                    }
                    KaamioConstants.TAB_FREELANCE -> {
                        KaamioTextField(value = title, onValueChange = { title = it }, label = "Project Title", placeholder = "e.g. E-commerce Website")
                        KaamioTextField(value = budget, onValueChange = { budget = it }, label = "Fixed Project Budget")
                        KaamioTextField(value = deadline, onValueChange = { deadline = it }, label = "Delivery Deadline (Days)")
                    }
                }

                KaamioTextField(value = location, onValueChange = { location = it }, label = "Location/Remote")
                KaamioTextField(value = skills, onValueChange = { skills = it }, label = "Key Requirements", placeholder = "e.g. Java, 5yrs exp")
                
                Spacer(modifier = Modifier.height(24.dp))
                validationError?.let {
                    KaamioErrorBanner(error = it, modifier = Modifier.padding(vertical = 4.dp))
                }
                KaamioButton("Publish Opportunity", {
                    val isFreelance = activeTab == KaamioConstants.TAB_FREELANCE
                    when {
                        title.isBlank() -> validationError = "Role title is required"
                        location.isBlank() -> validationError = "Location is required"
                        isFreelance && budget.isBlank() ->
                            validationError = "Project budget is required"
                        !isFreelance && salary.isBlank() ->
                            validationError = "Salary / service rate is required"
                        isFreelance && deadline.trim().toIntOrNull() == null ->
                            validationError = "Deadline must be a number of days"
                        else -> {
                            validationError = null
                            marketViewModel.postJob(JobListing(
                                id = "j_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}",
                                title = title,
                                company = company,
                                logoUrl = "",
                                salary = if (isFreelance) budget else salary,
                                location = location,
                                ownerId = profile.kaamioId,
                                category = when(activeTab) {
                                    KaamioConstants.TAB_LOCAL -> "Local"
                                    KaamioConstants.TAB_FREELANCE -> "Freelance"
                                    else -> "Tech"
                                },
                                isRemote = location.contains("Remote", ignoreCase = true),
                                type = if(activeTab == KaamioConstants.TAB_JOBS) "Full-time" else "Contract",
                                budget = budget,
                                deadlineDays = if (isFreelance) deadline.trim().toIntOrNull() ?: 14 else 14,
                                preferredSkills = skills
                            )) { success ->
                                if (success) homeViewModel.navigateTo(Screen.Market)
                            }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(), isLoading = isPosting, enabled = !isPosting)
            }
        }
    }
}

@Composable
fun FilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val theme = LocalKaamioTheme.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) theme.textPrimary else theme.surface,
        border = if (isSelected) null else BorderStroke(1.dp, theme.border)
    ) {
        Text(
            text = text, 
            style = Typography.labelMedium, 
            color = if (isSelected) theme.background else theme.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun PremiumMarketTabs(tabs: List<String>, labels: List<String>, activeTab: String, onTabSelect: (String) -> Unit) {
    val theme = LocalKaamioTheme.current
    Box(
        modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp))
            .background(theme.surface).padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = activeTab == tab
                val label = labels[index]
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .then(if (isSelected) Modifier.shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = theme.textPrimary.copy(alpha = 0.6f), spotColor = theme.textPrimary.copy(alpha = 0.6f)) else Modifier)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isSelected) theme.textPrimary.copy(alpha = 0.18f) else Color.Transparent)
                        .then(if (isSelected) Modifier.border(1.dp, theme.textPrimary.copy(alpha = 0.1f), RoundedCornerShape(22.dp)) else Modifier)
                        .clickable { onTabSelect(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = label, style = Typography.labelMedium, color = if (isSelected) theme.textPrimary else theme.textSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MarketListView(jobs: List<JobListing>, isLoading: Boolean, tab: String, onAction: (String) -> Unit, onBookmark: (String, Boolean) -> Unit) {
    val theme = LocalKaamioTheme.current
    LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isLoading && jobs.isEmpty()) {
            items(4) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(160.dp)) }
        } else if (jobs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(theme.surface)
                            .border(1.dp, theme.border.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome, 
                            null, 
                            tint = theme.accent, 
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Curating Opportunities", 
                        style = Typography.headlineMedium, 
                        fontWeight = FontWeight.Black,
                        color = theme.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "We're matching new ${tab.lowercase()} to your professional profile. Check back in a few moments.",
                        style = Typography.bodyLarge,
                        color = theme.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            }
        } else {
            items(jobs, key = { it.id }) { job ->
                LuxuriousJobCard(job, tab,
                    onAction = { onAction(job.id) },
                    onBookmark = { onBookmark(job.id, job.isBookmarked) },
                    modifier = Modifier.animateItem()
                )
            }
            item { Spacer(modifier = Modifier.height(140.dp)) }
        }
    }
}

@Composable
fun LuxuriousJobCard(job: JobListing, tab: String, onAction: () -> Unit, onBookmark: () -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalKaamioTheme.current
    KaamioCard(
        modifier = modifier.fillMaxWidth(), 
        padding = 0.dp, // Content will handle internal padding
        elevation = 2.dp,
        onClick = onAction
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.surface)
                            .border(1.dp, theme.border.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = job.logoUrl, 
                            contentDescription = "Logo of ${job.company}", 
                            modifier = Modifier.fillMaxSize().padding(12.dp), 
                            contentScale = ContentScale.Fit, 
                            error = painterResource(R.drawable.kaamio_logo)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = job.title, 
                            style = Typography.titleLarge, 
                            color = theme.textPrimary, 
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = job.company.uppercase(), 
                                style = Typography.labelSmall, 
                                color = theme.textSecondary,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (job.isVerifiedCompany) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Verified, 
                                    contentDescription = null, 
                                    tint = theme.accent, 
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                IconButton(
                    onClick = { onBookmark() },
                    modifier = Modifier.size(32.dp).offset(x = 8.dp, y = (-4).dp)
                ) {
                    Icon(
                        imageVector = if (job.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(
                            if (job.isBookmarked) R.string.cd_remove_bookmark else R.string.cd_bookmark_listing
                        ),
                        tint = if (job.isBookmarked) theme.accent else theme.textTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Contextual Metadata
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Badge(job.type)
                if (job.isRemote) Badge("Remote")
                if (tab == KaamioConstants.TAB_FREELANCE) Badge("${job.deadlineDays} Days")
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Location - Clean & Direct
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = theme.textTertiary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = job.location, 
                        style = Typography.bodySmall, 
                        color = theme.textTertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Primary Footer
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (tab == KaamioConstants.TAB_FREELANCE) "Est. Budget" else "Starting at", 
                        style = Typography.labelSmall, 
                        color = theme.textTertiary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (tab == KaamioConstants.TAB_FREELANCE) job.budget else job.salary, 
                        style = Typography.headlineMedium, 
                        color = theme.textPrimary, 
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                }
                
                val actionText = when (tab) {
                    KaamioConstants.TAB_LOCAL -> "Message"
                    KaamioConstants.TAB_FREELANCE -> "Bid Now"
                    else -> if (job.isApplied) "Applied" else "Apply"
                }
                
                KaamioButton(
                    text = actionText,
                    onClick = onAction,
                    modifier = Modifier.height(52.dp).widthIn(min = 130.dp),
                    containerColor = if (job.isApplied) theme.surface else theme.textPrimary,
                    contentColor = if (job.isApplied) theme.textPrimary else theme.background,
                    enabled = !job.isApplied,
                    shape = RoundedCornerShape(16.dp),
                    height = 52.dp
                )
            }
        }
    }
}

@Composable
fun Badge(text: String) {
    val theme = LocalKaamioTheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(theme.textPrimary.copy(alpha = 0.06f))
            .border(1.dp, theme.textPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text.uppercase(), 
            style = Typography.labelSmall, 
            color = theme.textSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )
    }
}
