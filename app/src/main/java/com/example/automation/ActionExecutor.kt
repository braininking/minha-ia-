package com.example.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.accessibility.AppAccessibilityService
import com.example.model.ActionType
import com.example.model.StepStatus
import com.example.model.TaskStep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URLEncoder

sealed class StepExecutionResult {
    data class Success(val message: String) : StepExecutionResult()
    data class Failure(val errorMessage: String) : StepExecutionResult()
    data class NeedsConfirmation(val prompt: String) : StepExecutionResult()
    object Cancelled : StepExecutionResult()
}

class ActionExecutor(private val context: Context) {

    companion object {
        private const val TAG = "ActionExecutor"
    }

    suspend fun executeStep(
        step: TaskStep,
        isCancelled: () -> Boolean
    ): StepExecutionResult = withContext(Dispatchers.Main) {
        if (isCancelled()) {
            step.status = StepStatus.CANCELLED
            return@withContext StepExecutionResult.Cancelled
        }

        step.status = StepStatus.RUNNING

        try {
            when (step.actionType) {
                ActionType.LAUNCH_APP -> {
                    val pkg = step.targetPackage
                    if (pkg.isNullOrBlank()) {
                        val error = "Pacote do aplicativo não informado."
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        return@withContext StepExecutionResult.Failure(error)
                    }

                    val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        step.status = StepStatus.COMPLETED
                        val msg = "Aplicativo '${step.targetLabel ?: pkg}' iniciado com sucesso."
                        step.resultMessage = msg
                        StepExecutionResult.Success(msg)
                    } else {
                        // Attempt fallback to market or report missing
                        val error = "Aplicativo '${step.targetLabel ?: pkg}' não está instalado no aparelho."
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        StepExecutionResult.Failure(error)
                    }
                }

                ActionType.OPEN_URL -> {
                    val url = step.targetQuery ?: "https://www.google.com"
                    val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    step.status = StepStatus.COMPLETED
                    val msg = "Navegador aberto em: $validUrl"
                    step.resultMessage = msg
                    StepExecutionResult.Success(msg)
                }

                ActionType.SEARCH_WEB -> {
                    val query = step.targetQuery ?: "Google"
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val searchUrl = "https://www.google.com/search?q=$encoded"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    step.status = StepStatus.COMPLETED
                    val msg = "Pesquisa realizada no Google para: \"$query\""
                    step.resultMessage = msg
                    StepExecutionResult.Success(msg)
                }

                ActionType.WAIT_DELAY -> {
                    val millis = (step.targetQuery?.toLongOrNull() ?: 1200L).coerceIn(0L, 10_000L)
                    val checkInterval = 200L
                    var elapsed = 0L
                    while (elapsed < millis) {
                        if (isCancelled()) {
                            step.status = StepStatus.CANCELLED
                            return@withContext StepExecutionResult.Cancelled
                        }
                        delay(checkInterval)
                        elapsed += checkInterval
                    }
                    step.status = StepStatus.COMPLETED
                    val msg = "Pausa de ${millis}ms concluída."
                    step.resultMessage = msg
                    StepExecutionResult.Success(msg)
                }

                ActionType.ACCESSIBILITY_CLICK -> {
                    val label = step.targetLabel ?: step.targetText ?: ""
                    val service = AppAccessibilityService.getInstance()
                    if (service == null) {
                        val error = "Serviço de Acessibilidade não está ativo. Ative-o nas Configurações para permitir cliques na interface."
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        return@withContext StepExecutionResult.Failure(error)
                    }

                    val clicked = service.clickElementByText(label)
                    if (clicked) {
                        step.status = StepStatus.COMPLETED
                        val msg = "Elemento '$label' clicado com sucesso na tela."
                        step.resultMessage = msg
                        StepExecutionResult.Success(msg)
                    } else {
                        val error = "Elemento '$label' não foi localizado na tela visível."
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        StepExecutionResult.Failure(error)
                    }
                }

                ActionType.ACCESSIBILITY_INPUT -> {
                    val textToEnter = step.targetText ?: ""
                    val field = step.targetLabel
                    val service = AppAccessibilityService.getInstance()
                    if (service == null) {
                        val error = "Serviço de Acessibilidade inativo para preenchimento de campo."
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        return@withContext StepExecutionResult.Failure(error)
                    }

                    val success = service.setTextOnInputField(textToEnter, field)
                    if (success) {
                        step.status = StepStatus.COMPLETED
                        val msg = "Texto inserido no campo '${field ?: "atual"}': \"$textToEnter\""
                        step.resultMessage = msg
                        StepExecutionResult.Success(msg)
                    } else {
                        val error = "Campo de entrada não localizado para inserção de texto."
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        StepExecutionResult.Failure(error)
                    }
                }

                ActionType.ACCESSIBILITY_SCROLL -> {
                    val service = AppAccessibilityService.getInstance()
                    if (service == null) {
                        val error = "Serviço de Acessibilidade inativo para rolar tela."
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        return@withContext StepExecutionResult.Failure(error)
                    }
                    val scrolled = service.scrollWindow(forward = true)
                    if (scrolled) {
                        step.status = StepStatus.COMPLETED
                        val msg = "Tela rolada com sucesso."
                        step.resultMessage = msg
                        StepExecutionResult.Success(msg)
                    } else {
                        val error = "Nenhum contêiner rolável encontrado."
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        StepExecutionResult.Failure(error)
                    }
                }

                ActionType.SEND_WHATSAPP_MESSAGE -> {
                    val message = step.targetText?.trim().orEmpty()
                    val contact = step.targetLabel?.trim().orEmpty()
                    val expectedPackage = step.targetPackage?.trim().orEmpty()
                    val service = AppAccessibilityService.getInstance()

                    if (message.isBlank() || contact.isBlank() || expectedPackage != "com.whatsapp") {
                        val error = "Envio bloqueado: mensagem, contato ou pacote do WhatsApp inválido."
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        return@withContext StepExecutionResult.Failure(error)
                    }
                    if (service == null) {
                        val error = "Serviço de Acessibilidade inativo. Não foi possível verificar a conversa antes do envio."
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        return@withContext StepExecutionResult.Failure(error)
                    }

                    val before = service.getCurrentScreenSummary()
                    val inWhatsApp = before.packageName.equals(expectedPackage, ignoreCase = true)
                    val contactVisible = before.visibleTexts.any { it.equals(contact, ignoreCase = true) } ||
                        before.visibleTexts.any { it.contains(contact, ignoreCase = true) }
                    val messageVisible = before.visibleTexts.any { it == message } ||
                        before.visibleTexts.any { it.contains(message) }

                    if (!inWhatsApp || !contactVisible || !messageVisible) {
                        val error = "Envio bloqueado: não consegui confirmar que a conversa de '$contact' está aberta e que o texto completo está preparado. Nenhuma mensagem foi enviada."
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        return@withContext StepExecutionResult.Failure(error)
                    }

                    // Only tap the visible WhatsApp send control after validating app, recipient, and draft.
                    val clicked = service.clickElementByText("Enviar") || service.clickElementByText("Send")
                    if (!clicked) {
                        val error = "Não encontrei o botão de envio do WhatsApp. A mensagem não foi enviada."
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        return@withContext StepExecutionResult.Failure(error)
                    }

                    delay(900)
                    val after = service.getCurrentScreenSummary()
                    val stillInWhatsApp = after.packageName.equals(expectedPackage, ignoreCase = true)
                    val messageInConversation = after.visibleTexts.any { it == message } ||
                        after.visibleTexts.any { it.contains(message) }
                    if (stillInWhatsApp && messageInConversation) {
                        step.status = StepStatus.COMPLETED
                        val msg = "O WhatsApp aceitou a ação de envio e o texto aparece na conversa de '$contact'. A entrega ao destinatário não foi confirmada."
                        step.resultMessage = msg
                        StepExecutionResult.Success(msg)
                    } else {
                        val error = "O botão foi acionado, mas não consegui verificar a mensagem na conversa. Confira o WhatsApp antes de tentar novamente."
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        StepExecutionResult.Failure(error)
                    }
                }

                ActionType.VERIFY_RESULT -> {
                    val service = AppAccessibilityService.getInstance()
                    val screenInfo = service?.getCurrentScreenSummary()
                    val target = step.targetLabel?.trim().orEmpty()
                    val expectedPackage = step.targetPackage?.trim().orEmpty()

                    val labelMatched = target.isNotBlank() && screenInfo != null && (
                        screenInfo.visibleTexts.any { it.contains(target, ignoreCase = true) } ||
                            screenInfo.packageName?.contains(target, ignoreCase = true) == true
                    )
                    val packageMatched = expectedPackage.isNotBlank() && screenInfo?.packageName
                        ?.equals(expectedPackage, ignoreCase = true) == true
                    val verified = labelMatched || packageMatched

                    if (verified) {
                        step.status = StepStatus.COMPLETED
                        val verifiedTarget = target.ifBlank { expectedPackage }
                        val msg = "Verificação concluída: '$verifiedTarget' foi confirmado na tela atual."
                        step.resultMessage = msg
                        StepExecutionResult.Success(msg)
                    } else {
                        val error = if (target.isBlank() && expectedPackage.isBlank()) {
                            "Não foi possível verificar o resultado: nenhum rótulo ou pacote esperado foi definido."
                        } else {
                            val expected = listOf(target, expectedPackage).filter { it.isNotBlank() }.joinToString(" / ")
                            "Não foi possível confirmar '$expected' na tela atual. Nenhum sucesso foi presumido."
                        }
                        step.status = StepStatus.FAILED
                        step.resultMessage = error
                        StepExecutionResult.Failure(error)
                    }
                }
            }
        } catch (e: CancellationException) {
            step.status = StepStatus.CANCELLED
            step.resultMessage = "Etapa cancelada pelo usuário."
            StepExecutionResult.Cancelled
        } catch (e: Exception) {
            Log.e(TAG, "Falha na execução da etapa: ${e.message}", e)
            val error = "Falha ao executar etapa: ${e.localizedMessage ?: e.message}"
            step.status = StepStatus.FAILED
            step.resultMessage = error
            StepExecutionResult.Failure(error)
        }
    }
}
