package com.kaamio.nepal.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaamio.nepal.ui.HomeViewModel
import com.kaamio.nepal.ui.KaamioBackButton

enum class KaamioContainerShape { Circle, Squircle, SoftSquare }

data class KaamioIconData(
    val name: String, val category: String, val icon: ImageVector, val accentColor: Color, val nepalDetail: String
)

@Composable
fun KaamioIcon(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = Color.White, size: Dp = 24.dp) {
    Icon(icon, null, modifier = modifier.size(size), tint = tint)
}

fun getKaamioIconCatalog(): List<KaamioIconData> {
    return listOf(
        KaamioIconData("Search", "Navigation", Icons.Default.Search, KaamioColors.FindJobs, "Locate verified work"),
        KaamioIconData("Profile", "Identity", Icons.Default.Person, KaamioColors.HireWorkers, "Professional credentials"),
        KaamioIconData("Trust", "Safety", Icons.Default.Shield, KaamioColors.LearnSkills, "Ledger integrity")
    )
}

@Composable
fun KaamioIconShowcaseScreen(homeViewModel: HomeViewModel) {
    val icons = remember { getKaamioIconCatalog() }
    Box(modifier = Modifier.fillMaxSize().background(KaamioColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(32.dp), verticalAlignment = Alignment.CenterVertically) {
                KaamioBackButton { homeViewModel.retryInit() }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Design System", style = Typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(icons) { item ->
                    Card(
                        modifier = Modifier.height(180.dp),
                        colors = CardDefaults.cardColors(containerColor = KaamioColors.Card),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(item.accentColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(item.icon, null, tint = item.accentColor)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(item.name, style = Typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(item.nepalDetail, style = Typography.labelSmall, color = KaamioColors.TextSecondary, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
