package com.ubpbsla.waspro

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.ubpbsla.waspro.ui.LoginScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun loginRequiresCredentialsAndSubmitsEnteredValues() {
        var email = ""
        var password = ""
        compose.setContent { MaterialTheme { LoginScreen(false) { first, second -> email = first; password = second } } }
        compose.onNodeWithText("Masuk").assertIsNotEnabled()
        compose.onNodeWithText("Email").performTextInput("operator@example.test")
        compose.onNodeWithText("Password", substring = false).performTextInput("test-password")
        compose.onNodeWithText("Masuk").performClick()
        assertEquals("operator@example.test", email)
        assertEquals("test-password", password)
    }

    @Test fun keyboardNextMovesToPasswordAndDoneSubmits() {
        var submitted = false
        compose.setContent { MaterialTheme { LoginScreen(false) { _, _ -> submitted = true } } }
        compose.onNodeWithText("Email").performTextInput("operator@example.test")
        compose.onNodeWithText("Email").performImeAction()
        compose.onNodeWithText("Password").assertIsFocused().performTextInput("test-password")
        compose.onNodeWithText("Password").performImeAction()
        compose.runOnIdle { org.junit.Assert.assertTrue(submitted) }
    }
}
