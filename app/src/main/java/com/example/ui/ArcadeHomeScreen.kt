package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameScore
import com.example.model.GameCategory
import com.example.model.GameInfo
import com.example.model.GameRegistry
import com.example.ui.components.HapticHelper
import com.example.ui.theme.ArcadeTheme

@Composable
fun ArcadeHomeScreen(
    highScores: Map<String, GameScore>,
    selectedCategory: GameCategory,
    onCategorySelected: (GameCategory) -> Unit,
    onGameSelected: (String) -> Unit,
    onPlayRandom: () -> Unit,
    onGenerateMusic: () -> Unit
) {
    val context = LocalContext.current
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles

    val filteredGames = GameRegistry.games.filter {
        selectedCategory == GameCategory.ALL || it.category == selectedCategory
    }

    val totalPlayed = highScores.values.sumOf { it.gamesPlayed }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Header Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Generated icon thumbnail
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.neonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_arcade_logo),
                        contentDescription = "Arcade Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "ARCADE 10",
                        style = textStyles.gameHeaderTitle.copy(
                            fontSize = 20.sp,
                            color = colors.neonCyan
                        )
                    )
                    Text(
                        text = "10 Games • Infinite Fun",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Generate Music Button
                androidx.compose.material3.IconButton(
                    onClick = {
                        HapticHelper.playClick(context)
                        onGenerateMusic()
                    },
                    modifier = Modifier
                        .background(colors.neonPurple, RoundedCornerShape(14.dp))
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Generate Music",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Quick Play Random button
                Button(
                    onClick = {
                        HapticHelper.playClick(context)
                        onPlayRandom()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.neonCyan,
                        contentColor = Color(0xFF003730)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("random_game_button").height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Random Game",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Surprise", style = textStyles.hudLabel, fontSize = 12.sp)
                }
            }
        }

        // Stats pill
        if (totalPlayed > 0) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎮", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PLAYED: $totalPlayed",
                            style = textStyles.hudLabel,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Trophy",
                            tint = colors.scoreGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${highScores.count { it.value.highScore > 0 }} HIGH SCORES",
                            style = textStyles.hudLabel,
                            color = colors.scoreGold
                        )
                    }
                }
            }
        }

        // Category Filter Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(GameCategory.entries) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        HapticHelper.playClick(context)
                        onCategorySelected(category)
                    },
                    label = {
                        Text(
                            text = category.displayName.uppercase(),
                            style = textStyles.hudLabel,
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.neonPink,
                        selectedLabelColor = Color.White,
                        containerColor = colors.surfaceCard,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) colors.neonPink else colors.border
                    )
                )
            }
        }

        // Games List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filteredGames, key = { it.id }) { game ->
                val scoreData = highScores[game.id]
                GameCard(
                    game = game,
                    scoreData = scoreData,
                    onClick = {
                        HapticHelper.playClick(context)
                        onGameSelected(game.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun GameCard(
    game: GameInfo,
    scoreData: GameScore?,
    onClick: () -> Unit
) {
    val bestScore = scoreData?.highScore ?: 0
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("game_card_${game.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Big Emoji Badge with neon halo
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(game.accentColorHex).copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                    .border(1.dp, Color(game.accentColorHex).copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = game.iconEmoji,
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Game Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = game.title,
                        style = textStyles.gameCardTitle,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Category badge
                    Box(
                        modifier = Modifier
                            .background(Color(game.accentColorHex).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = game.subtitle.uppercase(),
                            style = textStyles.hudLabel,
                            fontSize = 9.sp,
                            color = Color(game.accentColorHex)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = game.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // High score badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bestScore > 0) {
                        Text(
                            text = "★ BEST: $bestScore ${game.scoreUnit.uppercase()}",
                            style = textStyles.hudLabel,
                            color = colors.scoreGold
                        )
                    } else {
                        Text(
                            text = "READY TO PLAY",
                            style = textStyles.hudLabel,
                            color = colors.neonCyan.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play Button Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(colors.neonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color(0xFF003730),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
