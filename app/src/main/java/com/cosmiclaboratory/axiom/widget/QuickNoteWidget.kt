package com.cosmiclaboratory.axiom.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.cornerRadius
import com.cosmiclaboratory.axiom.MainActivity

class QuickNoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                QuickNoteContent()
            }
        }
    }

    @Composable
    private fun QuickNoteContent() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1976D2)))
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✏️",
                    style = TextStyle(
                        fontSize = 32.sp,
                        color = ColorProvider(Color.White)
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(8.dp))
                
                Text(
                    text = "Quick Entry",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = ColorProvider(Color.White)
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(4.dp))
                
                Text(
                    text = "Tap to create",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(Color.White.copy(alpha = 0.8f))
                    )
                )
            }
        }
    }
}