package com.kaamio.nepal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaamio.nepal.ui.theme.*

@Composable
fun MyActivitiesScreen(homeViewModel: HomeViewModel, viewModel: MyActivitiesViewModel) {
    val theme = LocalKaamioTheme.current
    val applications by viewModel.myApplications.collectAsStateWithLifecycle(initialValue = emptyList())
    val listings by viewModel.myListings.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val hasContent = applications.isNotEmpty() || listings.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            KaamioScreenHeader("Active Tasks", onBack = { homeViewModel.navigateTo(Screen.Profile) })

            if (!hasContent) {
                KaamioEmptyState(
                    icon = Icons.Filled.Assignment,
                    title = "No active tasks",
                    subtitle = "Jobs you apply to or list will appear here."
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (applications.isNotEmpty()) {
                        item { PremiumSectionHeader("Job Applications", horizontalPadding = 0.dp) }
                        items(applications) { job ->
                            ApplicationItem(job.title, job.company)
                        }
                    }
                    if (listings.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(16.dp)); PremiumSectionHeader("My Listings", horizontalPadding = 0.dp) }
                        items(listings) { job ->
                            ApplicationItem(job.title, job.location)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(120.dp)) }
                }
            }
        }
    }
}

@Composable
fun ApplicationItem(title: String, subtitle: String) {
    val theme = LocalKaamioTheme.current
    KaamioCard(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(theme.surface, shape = MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Assignment, null, tint = theme.textPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = Typography.bodyLarge, color = theme.textPrimary, fontWeight = FontWeight.Bold)
                Text(subtitle, style = Typography.bodySmall, color = theme.textSecondary)
            }
        }
    }
}