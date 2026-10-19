package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AIPlanner
import com.example.ai.LocalIntelligentProvider
import com.example.model.ActionType
import com.example.model.AutonomyLevel
import com.example.model.InstalledAppInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Gemini Mobile AI", appName)
    }

    @Test
    fun testLocalAIPlannerForChromeSearch() = runBlocking {
        val provider = LocalIntelligentProvider()
        val installed = listOf(
            InstalledAppInfo("Google Chrome", "com.android.chrome")
        )
        val result = provider.planTask("Abra o Chrome e pesquise aplicativos de edição de vídeo", installed, null)
        assertTrue(result.isSuccess)
        val plan = result.getOrThrow()
        assertTrue(plan.steps.any { it.actionType == ActionType.LAUNCH_APP })
        assertTrue(plan.steps.any { it.actionType == ActionType.SEARCH_WEB })
    }

    @Test
    fun testWhatsAppPlanningWithAutonomySafety() = runBlocking {
        val provider = LocalIntelligentProvider()
        val planner = AIPlanner()
        val installed = listOf(
            InstalledAppInfo("WhatsApp", "com.whatsapp")
        )
        val rawResult = provider.planTask("Mande uma mensagem para o Marcos dizendo que chego às 18h", installed, null)
        assertTrue(rawResult.isSuccess)
        val rawPlan = rawResult.getOrThrow()

        // Test Equilibrado autonomy: WhatsApp send requires confirmation
        val validated = planner.validateAndApplyPolicy(rawPlan, AutonomyLevel.EQUILIBRADO).getOrThrow()
        val sendStep = validated.steps.firstOrNull { it.actionType == ActionType.SEND_WHATSAPP_MESSAGE }
        assertNotNull(sendStep)
        assertTrue("No modo equilibrado o envio de mensagem deve exigir confirmação", sendStep!!.requiresConfirmation)
    }

    @Test
    fun testConservativeAutonomyRequiresConfirmationForAllActionSteps() = runBlocking {
        val provider = LocalIntelligentProvider()
        val planner = AIPlanner()
        val rawPlan = provider.planTask("Abra a calculadora", emptyList(), null).getOrThrow()

        val validated = planner.validateAndApplyPolicy(rawPlan, AutonomyLevel.CONSERVADOR).getOrThrow()
        val launchStep = validated.steps.first { it.actionType == ActionType.LAUNCH_APP }
        assertTrue("No modo conservador a abertura de app deve exigir confirmação", launchStep.requiresConfirmation)
    }
}
