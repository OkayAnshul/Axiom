package com.cosmiclaboratory.axiom.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.ui.theme.AxiomBlue80
import com.cosmiclaboratory.axiom.ui.theme.AxiomAmber80

/**
 * Developer Screen - About the creator of Axiom
 *
 * Features:
 * - Developer profile and bio
 * - Social media links (GitHub, LinkedIn, Instagram, Twitter)
 * - Feedback and bug report buttons
 * - Smooth animations and cosmic theme
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Pulsing animation for profile icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Developer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Profile Icon with pulsing animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AxiomBlue80, AxiomAmber80)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Developer",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }

            // Name and Tagline
            Text(
                text = "Anshul",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Deep Dive Developer | Building Tools That Actually Matter",
                style = MaterialTheme.typography.titleMedium,
                color = AxiomBlue80,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Bio Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = AxiomAmber80,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About Axiom",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Built Axiom because why not? If you're reading this, congrats – you found the 'About Developer' button! 🎉",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "This is just a side project made with way too much coffee ☕, a pinch of obsession, and the firm belief that markdown notes deserve better.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "If it helps someone out there, cool. If not, at least I had fun building it. 🤷‍♂️",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Social Links Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Links",
                            tint = AxiomAmber80,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connect With Me",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // GitHub
                    SocialLinkItem(
                        icon = Icons.Default.Code,
                        label = "GitHub",
                        username = "@OkayAnshul",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/OkayAnshul"))
                            context.startActivity(intent)
                        }
                    )

                    // LinkedIn
                    SocialLinkItem(
                        icon = Icons.Default.Work,
                        label = "LinkedIn",
                        username = "anshulisworking",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/anshulisworking"))
                            context.startActivity(intent)
                        }
                    )

                    // Instagram
                    SocialLinkItem(
                        icon = Icons.Default.PhotoCamera,
                        label = "Instagram",
                        username = "@axshzl.bin",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/axshzl.bin/"))
                            context.startActivity(intent)
                        }
                    )

                    // Twitter/X
                    SocialLinkItem(
                        icon = Icons.Default.Tag,
                        label = "Twitter/X",
                        username = "@ern404errFate",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/ern404errFate"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Action Buttons Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Get In Touch",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Send Feedback Button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("anshulisokay@gmail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "Axiom Feedback")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Hi Anshul,\n\nI wanted to share some feedback about Axiom:\n\n"
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, "Send Feedback"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AxiomBlue80
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Feedback")
                    }

                    // Report Issue Button
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("anshulisokay@gmail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "Axiom - Bug Report")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    """
                                    Hi Anshul,

                                    I found a bug in Axiom:

                                    **Bug Description:**
                                    [Describe the issue]

                                    **Steps to Reproduce:**
                                    1.
                                    2.
                                    3.

                                    **Expected Behavior:**
                                    [What should happen]

                                    **Actual Behavior:**
                                    [What actually happens]

                                    **Device Info:**
                                    - Android Version:
                                    - Device Model:

                                    Thanks!
                                    """.trimIndent()
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, "Report Issue"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Bug Report",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report an Issue")
                    }
                }
            }

            // Footer
            Text(
                text = "Made with ❤️ and lots of ☕",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

/**
 * Social link item component
 */
@Composable
private fun SocialLinkItem(
    icon: ImageVector,
    label: String,
    username: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AxiomBlue80,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = username,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
