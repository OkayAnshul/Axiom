package com.cosmiclaboratory.axiom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.cosmiclaboratory.axiom.ui.navigation.AxiomNavigation
import com.cosmiclaboratory.axiom.ui.navigation.AxiomScreen
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import com.cosmiclaboratory.axiom.utils.ShareIntentHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            AxiomTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var sharedContent by remember { mutableStateOf<ShareIntentHandler.SharedContent?>(null) }
                    
                    // Handle Quick Settings tile action and share intents
                    LaunchedEffect(intent) {
                        val action = intent?.getStringExtra("action")
                        if (action == "create_note") {
                            navController.navigate(AxiomScreen.NoteDetail.createRoute()) {
                                popUpTo(AxiomScreen.NotesList.route) { inclusive = false }
                            }
                        } else {
                            // Handle shared content
                            val shared = ShareIntentHandler.extractSharedContent(intent)
                            if (shared != null) {
                                sharedContent = shared
                                navController.navigate(AxiomScreen.NoteDetail.createRoute()) {
                                    popUpTo(AxiomScreen.NotesList.route) { inclusive = false }
                                }
                            }
                        }
                    }
                    
                    AxiomNavigation(
                        navController = navController,
                        sharedContent = sharedContent,
                        onSharedContentConsumed = { sharedContent = null }
                    )
                }
            }
        }
    }
}

