package com.example.slmplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slmplay.ui.components.GlassCard
import com.example.ui.theme.*

data class UpcomingFeature(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val badgeText: String = "À VENIR",
    val bulletPoints: List<String> = emptyList()
)

@Composable
fun UpcomingFeaturesScreen(
    accentColor: Color = AppleCrimson,
    onOpenPrivateSpace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val upcomingList = listOf(
        UpcomingFeature(
            id = "slm_ai",
            title = "Slm AI",
            subtitle = "Intelligence Artificielle de nouvelle génération",
            description = "Une AI sans limite pensée pour améliorer le confort de l'user.",
            icon = Icons.Default.AutoAwesome,
            gradientColors = listOf(Color(0xFFFF2D55), Color(0xFFAF52DE)),
            bulletPoints = listOf(
                "Analyse du style musical & adaptation automatique",
                "Confort d'écoute optimisé en temps réel sans contrainte",
                "Assistance intelligente pour la gestion de votre bibliothèque"
            )
        ),
        UpcomingFeature(
            id = "slm_space",
            title = "Slm Space",
            subtitle = "Espace privé & fonctionnalités supérieures",
            description = "Un espace privé où vous pouvez avoir votre vie privée et accès à des fonctions supérieures.",
            icon = Icons.Default.Shield,
            gradientColors = listOf(Color(0xFF00C7BE), Color(0xFF007AFF)),
            bulletPoints = listOf(
                "Protection renforcée et confidentialité absolue de vos écoutes",
                "Accès exclusif à des fonctions audio avancées",
                "Environnement dédié et isolé pour vos contenus personnels"
            )
        ),
        UpcomingFeature(
            id = "slm_rewards",
            title = "Slm Rewards",
            subtitle = "Programme de récompenses communautaire",
            description = "Vous recevrez des jetons de manière hebdomadaire au cas où il y aurait une version premium mais ce n'est pas sûr.",
            icon = Icons.Default.Stars,
            gradientColors = listOf(Color(0xFFFF9500), Color(0xFFFFCC00)),
            bulletPoints = listOf(
                "Attribution automatique de jetons chaque semaine",
                "Accumulation de points sans obligation d'achat",
                "Préparation d'avantages futurs si une version premium voit le jour"
            )
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("upcoming_features_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Banner
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkGlassElevated,
                borderColor = accentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(16.dp, CircleShape, spotColor = accentColor)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(accentColor, ApplePurple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Fonctions à venir",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Découvrez les innovations et modules en préparation pour les prochaines évolutions de SLM Play.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFD60A))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Aperçu informatif • Déploiement progressif",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MODULES EN COURS DE PRÉPARATION",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "${upcomingList.size} fonctions",
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Feature Cards
        items(upcomingList.size) { index ->
            val feature = upcomingList[index]

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upcoming_card_${feature.id}"),
                backgroundColor = DarkGlassCard,
                borderColor = feature.gradientColors.first().copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Header with Icon, Title, and "À VENIR" Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(feature.gradientColors)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = feature.title,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = feature.title,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = feature.subtitle,
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // "À VENIR" Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            feature.gradientColors.first().copy(alpha = 0.25f),
                                            feature.gradientColors.last().copy(alpha = 0.25f)
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    feature.gradientColors.first().copy(alpha = 0.6f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = feature.badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = feature.gradientColors.first(),
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary description requested by the user
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = feature.description,
                            fontSize = 13.5.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 19.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Detail points
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        feature.bulletPoints.forEach { point ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = feature.gradientColors.first(),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = point,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        if (feature.id == "slm_space") {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onOpenPrivateSpace,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF007AFF)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Déverrouiller SLM Space",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Gratitude Card: "Merci d'utiliser cette application"
        item {
            Spacer(modifier = Modifier.height(6.dp))
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_gratitude_card"),
                backgroundColor = DarkGlassElevated,
                borderColor = Color(0xFFFF2D55).copy(alpha = 0.4f),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF2D55).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFF2D55).copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Cœur",
                            tint = Color(0xFFFF2D55),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Merci d'utiliser cette application",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Votre soutien et votre confiance font grandir SLM Play. Restez connectés pour les prochaines nouveautés !",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
