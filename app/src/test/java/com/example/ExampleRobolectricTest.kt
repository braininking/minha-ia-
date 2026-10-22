package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AIPlanner
import com.example.ai.LocalIntelligentProvider
import com.example.model.ActionType
import com.example.model.AutonomyLevel
import com.example.model.InstalledAppInfo
import com.example.model.TaskPlan
import com.example.model.TaskStep
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
        val verification = plan.steps.firstOrNull { it.actionType == ActionType.VERIFY_RESULT }
        assertNotNull(verification)
        assertEquals("com.android.chrome", verification!!.targetPackage)
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

        // A clear spoken send command authorizes the final action; the executor still
        // verifies the exact conversation and the complete draft before tapping Send.
        val validated = planner.validateAndApplyPolicy(rawPlan, AutonomyLevel.EQUILIBRADO).getOrThrow()
        val sendStep = validated.steps.firstOrNull { it.actionType == ActionType.SEND_WHATSAPP_MESSAGE }
        assertNotNull(sendStep)
        assertFalse("O comando falado explícito deve permitir envio automático após as verificações", sendStep!!.requiresConfirmation)
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

    @Test
    fun balancedAutonomyRequiresApprovalForPotentiallySensitiveUiActions() {
        val plan = TaskPlan(
            goal = "Preencher e confirmar formulário",
            steps = mutableListOf(
                TaskStep(
                    index = 0,
                    actionType = ActionType.ACCESSIBILITY_INPUT,
                    description = "Preencher campo",
                    targetText = "dados",
                    requiresConfirmation = false
                ),
                TaskStep(
                    index = 1,
                    actionType = ActionType.ACCESSIBILITY_CLICK,
                    description = "Clicar em enviar",
                    targetLabel = "Enviar",
                    requiresConfirmation = false
                )
            )
        )

        val validated = AIPlanner()
            .validateAndApplyPolicy(plan, AutonomyLevel.EQUILIBRADO)
            .getOrThrow()

        assertTrue(validated.steps.all { it.requiresConfirmation })
    }

    @Test
    fun advancedAutonomyCannotBypassSensitiveActionConfirmations() {
        val plan = TaskPlan(
            goal = "Preparar mensagem",
            steps = mutableListOf(
                TaskStep(
                    index = 0,
                    actionType = ActionType.SEND_WHATSAPP_MESSAGE,
                    description = "Preparar mensagem",
                    targetText = "Olá",
                    requiresConfirmation = false
                )
            )
        )

        val validated = AIPlanner()
            .validateAndApplyPolicy(plan, AutonomyLevel.AVANCADO)
            .getOrThrow()

        assertTrue(validated.steps.single().requiresConfirmation)
    }

    @Test
    fun plannerRejectsLaunchWithoutPackage() {
        val plan = TaskPlan(
            goal = "Abrir aplicativo",
            steps = mutableListOf(
                TaskStep(
                    index = 0,
                    actionType = ActionType.LAUNCH_APP,
                    description = "Abrir aplicativo sem pacote"
                )
            )
        )

        assertTrue(AIPlanner().validateAndApplyPolicy(plan, AutonomyLevel.EQUILIBRADO).isFailure)
    }

    @Test
    fun localWhatsAppPlanDoesNotClaimItCanVerifyMessageDelivery() = runBlocking {
        val provider = LocalIntelligentProvider()
        val plan = provider.planTask(
            "Mande uma mensagem para Marcos dizendo que cheguei",
            listOf(InstalledAppInfo("WhatsApp", "com.whatsapp")),
            null
        ).getOrThrow()

        assertTrue(plan.steps.any { it.actionType == ActionType.SEND_WHATSAPP_MESSAGE })
        assertTrue(plan.steps.any { it.actionType == ActionType.ACCESSIBILITY_CLICK })
        assertTrue(plan.steps.any { it.actionType == ActionType.ACCESSIBILITY_INPUT })
        assertFalse(plan.steps.any {
            it.actionType == ActionType.VERIFY_RESULT &&
                it.targetLabel == "Status WhatsApp"
        })
    }

    @Test
    fun parsesNaturalPortugueseWhatsAppCommandForMother() = runBlocking {
        val provider = LocalIntelligentProvider()
        val plan = provider.planTask(
            "Abra o WhatsApp e envie mensagem pra minha mãe, falando: \"Oi mãe, tudo bom?\"",
            listOf(InstalledAppInfo("WhatsApp", "com.whatsapp")),
            null
        ).getOrThrow()

        val search = plan.steps.first { it.actionType == ActionType.ACCESSIBILITY_INPUT }
        val message = plan.steps.first { it.actionType == ActionType.SEND_WHATSAPP_MESSAGE }
        assertEquals("Mãe", search.targetText)
        assertEquals("Mãe", message.targetLabel)
        assertEquals("Oi mãe, tudo bom?", message.targetText)
        assertEquals(ActionType.SEND_WHATSAPP_MESSAGE, plan.steps.last().actionType)
    }

}
