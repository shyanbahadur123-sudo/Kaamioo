package com.kaamio.nepal.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kaamio.nepal.BuildConfig
import com.kaamio.nepal.R
import com.kaamio.nepal.data.Course
import com.kaamio.nepal.data.UserProfile
import com.kaamio.nepal.payment.rememberKhaltiPay
import com.kaamio.nepal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningHubScreen(homeViewModel: HomeViewModel, learningViewModel: LearningViewModel, globalViewModel: GlobalViewModel, profile: UserProfile) {
    val theme = LocalKaamioTheme.current
    val courses by learningViewModel.coursesList.collectAsStateWithLifecycle()
    val isLoading by learningViewModel.isCoursesLoading.collectAsStateWithLifecycle()
    val selectedCategory by learningViewModel.selectedCourseCategory.collectAsStateWithLifecycle()
    val courseSearchQuery by learningViewModel.courseSearchQuery.collectAsStateWithLifecycle()
    val pendingCoursePidx by learningViewModel.pendingCoursePidx.collectAsStateWithLifecycle()
    val pendingCourse by learningViewModel.pendingCourse.collectAsStateWithLifecycle()

    // Open Khalti when a premium course payment has been initiated, then verify
    // and unlock server-side. Previously confirmCourseUnlock was never invoked,
    // so paid courses could not be purchased at all.
    val openCourseKhalti = pendingCoursePidx?.let { pidx ->
        rememberKhaltiPay(
            publicKey = BuildConfig.KHALTI_PUBLIC_KEY,
            pidx = pidx,
            environment = if (BuildConfig.KHALTI_ENV == "PROD") com.khalti.checkout.data.Environment.PROD else com.khalti.checkout.data.Environment.TEST,
            onResult = { success, transactionId, error ->
                if (success && transactionId != null) {
                    learningViewModel.confirmCourseUnlock(transactionId)
                } else {
                    learningViewModel.clearPendingCoursePidx()
                }
            }
        )
    }
    LaunchedEffect(openCourseKhalti) { openCourseKhalti?.invoke() }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Premium Header
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Learning Hub", style = Typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = theme.textPrimary)
                    IconButton(
                        onClick = { homeViewModel.navigateTo(Screen.TeacherDashboard) },
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(theme.surface).border(1.dp, theme.border, CircleShape)
                    ) {
                        Icon(Icons.Outlined.School, null, tint = theme.textPrimary, modifier = Modifier.size(22.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Search courses
                TextField(
                    value = courseSearchQuery, onValueChange = { learningViewModel.setCourseSearchQuery(it) },
                    placeholder = { Text("Search skills or courses...", color = theme.textTertiary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.textSecondary, modifier = Modifier.size(20.dp)) },
                    trailingIcon = if (courseSearchQuery.isNotEmpty()) {
                        { Icon(Icons.Default.Close, null, tint = theme.textSecondary, modifier = Modifier.size(20.dp).clickable { learningViewModel.setCourseSearchQuery("") }) }
                    } else null,
                    modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = theme.surface, unfocusedContainerColor = theme.surface,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = theme.textPrimary
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val categories = listOf("All", "Tech", "Trade", "Design", "Language")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(categories) { cat ->
                        FilterChip(text = cat, isSelected = selectedCategory == cat, onClick = { learningViewModel.setCourseCategory(cat) })
                    }
                }
            }
            
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { learningViewModel.refreshCourses() },
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading && courses.isEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(3) {
                            ShimmerBox(modifier = Modifier.fillMaxWidth().height(120.dp))
                        }
                    }
                } else if (courses.isEmpty()) {
                    KaamioEmptyState(icon = Icons.Outlined.School, title = "No courses yet", subtitle = "Enroll in a skill-building program to get started")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item {
                            Text("FEATURED PROGRAMS", style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(courses.take(3)) { course ->
                                    FeaturedCourseCard(
                                        course = course,
                                        onOpen = {
                                            learningViewModel.openCourse(course)
                                            homeViewModel.navigateTo(Screen.CourseDetail)
                                        },
                                        onEnroll = {
                                            if (course.isPremium && !course.isUnlocked) learningViewModel.initiateCourseUnlock(course)
                                            else learningViewModel.enrollInCourse(course.id)
                                        }
                                    )
                                }
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(12.dp)); Text("ALL COURSES", style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 2.sp) }
                        
                        items(courses.drop(3), key = { it.id }) { course ->
                            LuxuriousCourseCard(
                                course = course,
                                onOpen = {
                                    learningViewModel.openCourse(course)
                                    homeViewModel.navigateTo(Screen.CourseDetail)
                                },
                                onEnroll = {
                                    if (course.isPremium && !course.isUnlocked) learningViewModel.initiateCourseUnlock(course)
                                    else learningViewModel.enrollInCourse(course.id)
                                }
                            )
                        }
                        
                        item { Spacer(modifier = Modifier.height(140.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedCourseCard(course: Course, onEnroll: () -> Unit, onOpen: () -> Unit) {
    val theme = LocalKaamioTheme.current
    Card(
        modifier = Modifier.width(280.dp).height(200.dp).clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = theme.card),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, theme.border)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = course.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.card_placeholder)
            )
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 0.4f)))
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(course.title, style = Typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(course.instructor, style = Typography.labelSmall, color = theme.textSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(2.dp).background(theme.textTertiary, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${course.studentsCount} Students", style = Typography.labelSmall, color = theme.textSecondary)
                }
            }
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(course.duration, style = Typography.labelSmall, color = Color.White, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun LuxuriousCourseCard(course: Course, onEnroll: () -> Unit, onOpen: () -> Unit) {
    val theme = LocalKaamioTheme.current
    KaamioCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp, elevation = 4.dp, onClick = onOpen) {
        Row(modifier = Modifier.height(140.dp).fillMaxWidth()) {
            Box(modifier = Modifier.width(130.dp).fillMaxHeight()) {
                AsyncImage(
                    model = course.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.card_placeholder)
                )
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(theme.accent).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(course.category, style = Typography.labelSmall, color = theme.onAccent, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(course.title, style = Typography.titleMedium, color = theme.textPrimary, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text(course.instructor, style = Typography.bodySmall, color = theme.textSecondary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = theme.warn, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(course.rating.toString(), style = Typography.labelMedium, color = theme.textPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${course.studentsCount} Students", style = Typography.labelSmall, color = theme.textTertiary)
                    }
                    KaamioButton("ENROLL", onEnroll, modifier = Modifier.height(32.dp).width(80.dp), shape = RoundedCornerShape(10.dp))
                }
            }
        }
    }
}

@Composable
fun CommunityScreen(homeViewModel: HomeViewModel, communityViewModel: CommunityViewModel, globalViewModel: GlobalViewModel, profile: UserProfile) {
    val theme = LocalKaamioTheme.current
    val posts by communityViewModel.communityPosts.collectAsStateWithLifecycle()
    var postContent by remember { mutableStateOf("") }
    val isOffline by globalViewModel.isOffline.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Editorial Header
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text("Community", style = Typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = theme.textPrimary)
                Text("Insights from the workforce of Nepal", style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 1.sp)
            }
            
            if (isOffline) ConnectivityBanner()

            if (posts.isEmpty()) {
                KaamioEmptyState(icon = Icons.Outlined.Forum, title = "No community posts", subtitle = "Be the first to start a discussion!")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // High-Fidelity Post Creator
                    item {
                        KaamioCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp, backgroundColor = theme.surface) {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(theme.card)) {
                                    if (profile.photoUrl.isNotEmpty()) {
                                        AsyncImage(model = profile.photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp).align(Alignment.Center), tint = theme.textTertiary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                TextField(
                                    value = postContent, onValueChange = { postContent = it },
                                    placeholder = { Text("Share an insight or update...", color = theme.textTertiary) },
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = theme.textPrimary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Outlined.Image, null, tint = theme.textSecondary, modifier = Modifier.size(20.dp))
                                    Icon(Icons.Outlined.Link, null, tint = theme.textSecondary, modifier = Modifier.size(20.dp))
                                }
                                KaamioButton(
                                    text = "POST", 
                                    onClick = { if(postContent.isNotBlank()) { communityViewModel.postToCommunity(postContent, profile); postContent = "" } },
                                    modifier = Modifier.height(34.dp).width(70.dp),
                                    enabled = postContent.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    item {
                        Text("LATEST UPDATES", style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 2.sp, modifier = Modifier.padding(top = 8.dp))
                    }

                    items(posts, key = { it.id }) { post ->
                        LuxuriousPostCard(post) { communityViewModel.likePost(post.id) }
                    }
                    
                    item { Spacer(modifier = Modifier.height(140.dp)) }
                }
            }
        }
    }
}

@Composable
fun LuxuriousPostCard(post: com.kaamio.nepal.data.CommunityPost, onLike: () -> Unit) {
    val theme = LocalKaamioTheme.current
    val isLiked = post.isLiked
    val likedColor by animateColorAsState(if (isLiked) theme.textPrimary else theme.textTertiary, label = "like_color")

    KaamioCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp, elevation = 4.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(theme.surface)) {
                AsyncImage(model = post.authorAvatar, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(post.authorName, style = Typography.titleMedium, color = theme.textPrimary, fontWeight = FontWeight.Bold)
                Text(post.authorRole.uppercase(), style = Typography.labelSmall, color = theme.accent, letterSpacing = 1.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(post.content, style = Typography.bodyLarge, color = theme.textPrimary, lineHeight = 24.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onLike() }) {
                Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = likedColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(post.likesCount.toString(), style = Typography.labelMedium, color = likedColor)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, tint = theme.textTertiary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(post.commentsCount.toString(), style = Typography.labelMedium, color = theme.textTertiary)
            }
            Icon(Icons.Outlined.Share, null, tint = theme.textTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun TeachingDashboardScreen(homeViewModel: HomeViewModel, learningViewModel: LearningViewModel, profile: UserProfile) {
    val theme = LocalKaamioTheme.current
    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KaamioBackButton { homeViewModel.navigateTo(Screen.Learn) }
                Spacer(modifier = Modifier.width(20.dp))
                Text("Educator Hub", style = Typography.displaySmall, color = theme.textPrimary)
            }
            
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Noticeable and Eyecatchy Launch Action
                Surface(
                    onClick = { homeViewModel.navigateTo(Screen.CreateCourse) },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = theme.surface,
                    border = BorderStroke(2.dp, theme.border)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(theme.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = theme.onAccent, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("LAUNCH NEW CURRICULUM", style = Typography.titleMedium, color = theme.textPrimary, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        Text("Share your expertise with Nepal", style = Typography.labelSmall, color = theme.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateCourseScreen(homeViewModel: HomeViewModel, learningViewModel: LearningViewModel, profile: UserProfile) {
    val theme = LocalKaamioTheme.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tech") }
    var price by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(32.dp), verticalAlignment = Alignment.CenterVertically) {
                KaamioBackButton { homeViewModel.navigateTo(Screen.TeacherDashboard) }
                Spacer(modifier = Modifier.width(20.dp))
                Text("New Curriculum", style = Typography.displaySmall, color = theme.textPrimary)
            }
            
            Column(modifier = Modifier.padding(horizontal = 32.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                KaamioTextField(value = title, onValueChange = { title = it }, label = "Course Title", placeholder = "e.g. Modern Web Development")
                KaamioTextField(value = description, onValueChange = { description = it }, label = "About the Course", placeholder = "Describe what students will learn...", modifier = Modifier.heightIn(min = 120.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KaamioTextField(value = duration, onValueChange = { duration = it }, label = "Total Time", placeholder = "e.g. 12 Hours", modifier = Modifier.weight(1f))
                    KaamioTextField(value = price, onValueChange = { price = it }, label = "Course Price", placeholder = "NPR 2,500", modifier = Modifier.weight(1f))
                }

                Column {
                    Text("CATEGORY", style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("Tech", "Trade", "Design").forEach { cat ->
                            FilterChip(cat, category == cat, { category = cat })
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                KaamioButton("Publish Curriculum", { 
                    learningViewModel.createCourse(com.kaamio.nepal.data.Course(
                        id = "c_${System.currentTimeMillis()}",
                        title = title,
                        description = description,
                        instructor = profile.name,
                        instructorId = profile.kaamioId,
                        duration = duration,
                        price = price,
                        category = category,
                        rating = 0f,
                        studentsCount = "0",
                        thumbnailUrl = "",
                        modules = "",
                        isUnlocked = false
                    ))
                    homeViewModel.navigateTo(Screen.TeacherDashboard)
                }, modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun CourseDetailScreen(homeViewModel: HomeViewModel, learningViewModel: LearningViewModel) {
    val theme = LocalKaamioTheme.current
    val selectedCourse by learningViewModel.selectedCourse.collectAsStateWithLifecycle()
    val isEnrolled by learningViewModel.enrolledCourseIds.collectAsStateWithLifecycle()
    val course = selectedCourse

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(32.dp), verticalAlignment = Alignment.CenterVertically) {
                KaamioBackButton { learningViewModel.closeCourse(); homeViewModel.navigateTo(Screen.Learn) }
                Spacer(modifier = Modifier.width(20.dp))
                Text("Course Details", style = Typography.displaySmall, color = theme.textPrimary)
            }

            if (course == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Select a course to view details.", style = Typography.bodyLarge, color = theme.textSecondary)
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (course.thumbnailUrl.isNotEmpty()) {
                        AsyncImage(
                            model = course.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.card_placeholder)
                        )
                    }
                    Text(course.title, style = Typography.displaySmall, color = theme.textPrimary)
                    Text("by ${course.instructor}", style = Typography.bodyMedium, color = theme.textSecondary)
                    if (course.description.isNotEmpty()) {
                        Text(course.description, style = Typography.bodyLarge, color = theme.textPrimary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Badge(course.category)
                        if (course.duration.isNotBlank()) Badge(course.duration)
                        if (course.isPremium) Badge(course.price)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    KaamioButton(
                        text = when {
                            course.isPremium && !course.isUnlocked && !isEnrolled.contains(course.id) -> "Unlock for ${course.price}"
                            isEnrolled.contains(course.id) || course.isUnlocked -> "Enrolled"
                            else -> "Enroll Free"
                        },
                        onClick = {
                            if (course.isPremium && !course.isUnlocked) learningViewModel.initiateCourseUnlock(course)
                            else learningViewModel.enrollInCourse(course.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}