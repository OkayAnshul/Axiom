package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Modern Split Pane Editor Integration Test Suite
 * 
 * Android integration tests for the modern editor implementation
 */
@RunWith(AndroidJUnit4::class)
class ModernSplitPaneEditorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun modernSplitPaneEditor_rendersCorrectly() {
        // Arrange
        val title = mutableStateOf("Test Note")
        val content = mutableStateOf("This is a test note with some content.")
        
        // Act
        composeTestRule.setContent {
            AxiomTheme {
                ModernSplitPaneEditor(
                    title = title.value,
                    content = content.value,
                    onTitleChange = { title.value = it },
                    onContentChange = { content.value = it },
                    isPreviewVisible = true,
                    wordCount = 8,
                    characterCount = 39,
                    lastModified = System.currentTimeMillis()
                )
            }
        }
        
        // Assert - basic rendering verification
        composeTestRule.onNodeWithText("Test Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is a test note with some content.").assertIsDisplayed()
        composeTestRule.onNodeWithText("8").assertIsDisplayed() // Word count
        composeTestRule.onNodeWithText("words").assertIsDisplayed()
    }

    @Test
    fun modernSplitPaneEditor_singlePaneMode() {
        // Arrange
        val title = mutableStateOf("Test Note")
        val content = mutableStateOf("Test content")
        
        // Act
        composeTestRule.setContent {
            AxiomTheme {
                ModernSplitPaneEditor(
                    title = title.value,
                    content = content.value,
                    onTitleChange = { title.value = it },
                    onContentChange = { content.value = it },
                    isPreviewVisible = false, // Single pane mode
                    wordCount = 2,
                    characterCount = 12
                )
            }
        }
        
        // Assert
        composeTestRule.onNodeWithText("Test Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test content").assertIsDisplayed()
        composeTestRule.onNodeWithText("2").assertIsDisplayed() // Word count should still be visible
    }
}