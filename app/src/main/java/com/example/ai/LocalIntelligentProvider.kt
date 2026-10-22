package com.example.ai

import com.example.model.ActionType
import com.example.model.ChatMessage
import com.example.model.InstalledAppInfo
import com.example.model.ScreenContext
import com.example.model.StepStatus
import com.example.model.TaskPlan
import com.example.model.TaskStep
import java.util.Locale

class LocalIntelligentProvider : AIProvider {

    override val providerId: String = "LOCAL_INTELLIGENT"
    override val displayName: String = "Motor Local Inteligente (Offline/Nativo)"
    override val isConfigured: Boolean = true

    override suspend fun generateChatResponse(
        prompt: String,
        history: List<ChatMessage>
    ): Result<String> {
        val lower = prompt.trim().lowercase(Locale.ROOT)

        val reply = when {
            lower.contains("quem é você") || lower.contains("o que você é") -> {
                "Sou o Gemini Mobile AI, seu assistente autônomo nativo para Android. Fui projetado para interpretar comandos em português, executar tarefas automatizadas no seu aparelho (como abrir apps, navegar por interfaces e preparar mensagens) e operar de forma segura sob seus níveis de autonomia definidos."
            }
            lower.contains("o que você pode fazer") || lower.contains("quais comandos") || lower.contains("ajuda") -> {
                "Posso realizar diversas tarefas no seu celular:\n" +
                        "• Abrir qualquer aplicativo instalado no seu celular (ex: 'Abra a calculadora', 'Abra o Chrome', 'Abra o YouTube')\n" +
                        "• Pesquisar na Web (ex: 'Abra o Chrome e pesquise aplicativos gratuitos de edição de vídeo')\n" +
                        "• Preparar mensagens no WhatsApp (ex: 'Mande uma mensagem para Marcos dizendo que chego às 18 horas')\n" +
                        "• Interagir com botões e campos de texto via Acessibilidade\n" +
                        "• Manter execução e escuta em segundo plano com botão flutuante"
            }
            lower.contains("whatsapp") -> {
                "Identifiquei um comando de mensagem. Posso preparar a conversa no WhatsApp, preencher o texto e aguardar sua confirmação antes de enviar. Exemplo: 'Mande mensagem para Marcos dizendo que já cheguei'."
            }
            lower.contains("segurança") || lower.contains("autonomia") -> {
                "Opero em três níveis de segurança:\n" +
                        "1. Conservador: solicita aprovação antes de qualquer ação.\n" +
                        "2. Equilibrado: navega e pesquisa automaticamente; confirma mensagens e submissões.\n" +
                        "3. Avançado: executa sequências pré-autorizadas com auditoria contínua."
            }
            else -> {
                "Compreendido. Comando analisado com sucesso pelo Gemini Mobile AI. Pronto para planejar e executar a automação solicitada com segurança."
            }
        }

        return Result.success(reply)
    }

    override suspend fun planTask(
        userGoal: String,
        installedApps: List<InstalledAppInfo>,
        screenContext: ScreenContext?
    ): Result<TaskPlan> {
        val raw = userGoal.trim()
        val lower = raw.lowercase(Locale.ROOT)
        val steps = mutableListOf<TaskStep>()

        // Helper to match app name
        fun findApp(nameKeyword: String): InstalledAppInfo? {
            return installedApps.firstOrNull {
                it.appName.lowercase(Locale.ROOT).contains(nameKeyword) ||
                        it.packageName.lowercase(Locale.ROOT).contains(nameKeyword)
            }
        }

        // Scenario 1: WhatsApp message
        // Ex: "Mande uma mensagem para o Marcos dizendo que chegarei às 18 horas"
        // Ex: "Enviar mensagem no WhatsApp para Ana dizendo parabens"
        if (lower.contains("whatsapp") || (lower.contains("mensagem") && lower.contains("dizendo"))) {
            val contact = extractContact(raw)
            val messageText = extractMessageText(raw)

            val whatsappApp = findApp("whatsapp")
            val targetPkg = whatsappApp?.packageName ?: "com.whatsapp"

            steps.add(
                TaskStep(
                    index = 0,
                    actionType = ActionType.LAUNCH_APP,
                    description = "Abrir o aplicativo WhatsApp",
                    targetPackage = targetPkg,
                    targetLabel = "WhatsApp",
                    requiresConfirmation = false
                )
            )
            steps.add(
                TaskStep(
                    index = 1,
                    actionType = ActionType.WAIT_DELAY,
                    description = "Aguardar carregamento da interface do WhatsApp",
                    targetQuery = "1200",
                    requiresConfirmation = false
                )
            )
            if (!contact.isNullOrBlank()) {
                steps.add(
                    TaskStep(
                        index = 2,
                        actionType = ActionType.ACCESSIBILITY_CLICK,
                        description = "Localizar e abrir conversa com '$contact'",
                        targetLabel = contact,
                        requiresConfirmation = false
                    )
                )
            }
            steps.add(
                TaskStep(
                    index = steps.size,
                    actionType = ActionType.SEND_WHATSAPP_MESSAGE,
                    description = "Preparar mensagem para '$contact': \"$messageText\"",
                    targetLabel = contact,
                    targetText = messageText,
                    requiresConfirmation = true // Mandatory review before sending!
                )
            )
            // Opening the share intent only prepares text; Android does not expose reliable
            // confirmation that the recipient was selected or the message was sent.

            return Result.success(TaskPlan(goal = raw, steps = steps))
        }

        // Scenario 2: Web Search in Chrome or browser
        // Ex: "Abra o Chrome e pesquise aplicativos gratuitos de edição de vídeo"
        if (lower.contains("pesquise") || lower.contains("pesquisar") || lower.contains("busca")) {
            val query = extractSearchQuery(raw)
            val chromeApp = findApp("chrome")
            val targetPkg = chromeApp?.packageName ?: "com.android.chrome"

            steps.add(
                TaskStep(
                    index = 0,
                    actionType = ActionType.LAUNCH_APP,
                    description = "Abrir navegador Chrome",
                    targetPackage = targetPkg,
                    targetLabel = "Google Chrome",
                    requiresConfirmation = false
                )
            )
            steps.add(
                TaskStep(
                    index = 1,
                    actionType = ActionType.WAIT_DELAY,
                    description = "Aguardar abertura do navegador",
                    targetQuery = "1000",
                    requiresConfirmation = false
                )
            )
            steps.add(
                TaskStep(
                    index = 2,
                    actionType = ActionType.SEARCH_WEB,
                    description = "Pesquisar no Google por: \"$query\"",
                    targetQuery = query,
                    requiresConfirmation = false
                )
            )
            steps.add(
                TaskStep(
                    index = 3,
                    actionType = ActionType.VERIFY_RESULT,
                    description = "Verificar exibição dos resultados da pesquisa",
                    targetLabel = query,
                    requiresConfirmation = false
                )
            )

            return Result.success(TaskPlan(goal = raw, steps = steps))
        }

        // Scenario 3: Open specific application
        // Ex: "Abra a calculadora", "Abra o YouTube", "Abrir configurações"
        if (lower.startsWith("abra ") || lower.startsWith("abrir ") || lower.contains("abra o ") || lower.contains("abra a ")) {
            val appQuery = extractAppQuery(raw)
            val matchedApp = findApp(appQuery) ?: guessKnownApp(appQuery)

            val pkg = matchedApp?.packageName ?: "com.android.$appQuery"
            val appName = matchedApp?.appName ?: appQuery.replaceFirstChar { it.uppercase() }

            steps.add(
                TaskStep(
                    index = 0,
                    actionType = ActionType.LAUNCH_APP,
                    description = "Iniciar o aplicativo $appName",
                    targetPackage = pkg,
                    targetLabel = appName,
                    requiresConfirmation = false
                )
            )
            steps.add(
                TaskStep(
                    index = 1,
                    actionType = ActionType.WAIT_DELAY,
                    description = "Aguardar inicialização da interface",
                    targetQuery = "1200",
                    requiresConfirmation = false
                )
            )
            steps.add(
                TaskStep(
                    index = 2,
                    actionType = ActionType.VERIFY_RESULT,
                    description = "Verificar se $appName está em primeiro plano",
                    targetLabel = appName,
                    requiresConfirmation = false
                )
            )

            return Result.success(TaskPlan(goal = raw, steps = steps))
        }

        // Scenario 4: Generic action with screen context or web fallback
        steps.add(
            TaskStep(
                index = 0,
                actionType = ActionType.SEARCH_WEB,
                description = "Processar intenção: \"$raw\"",
                targetQuery = raw,
                requiresConfirmation = false
            )
        )
        steps.add(
            TaskStep(
                index = 1,
                actionType = ActionType.VERIFY_RESULT,
                description = "Verificar conclusão da tarefa",
                requiresConfirmation = false
            )
        )

        return Result.success(TaskPlan(goal = raw, steps = steps))
    }

    private fun extractContact(raw: String): String? {
        val regex = Regex("para\\s+(?:o\\s+|a\\s+)?([A-Za-zÀ-ÖØ-öø-ÿ]+)", RegexOption.IGNORE_CASE)
        val match = regex.find(raw)
        return match?.groupValues?.getOrNull(1)?.replaceFirstChar { it.uppercase() }
    }

    private fun extractMessageText(raw: String): String {
        val regex = Regex("dizendo\\s+(?:que\\s+)?(.+)", RegexOption.IGNORE_CASE)
        val match = regex.find(raw)
        return match?.groupValues?.getOrNull(1)?.trim() ?: "Olá! Mensagem enviada pelo assistente."
    }

    private fun extractSearchQuery(raw: String): String {
        val regex = Regex("pesquis(?:e|ar)\\s+(?:por\\s+|sobre\\s+)?(.+)", RegexOption.IGNORE_CASE)
        val match = regex.find(raw)
        var query = match?.groupValues?.getOrNull(1)?.trim() ?: raw
        query = query.replace(Regex("^(?:no\\s+google|na\\s+web)\\s*", RegexOption.IGNORE_CASE), "")
        return if (query.isNotBlank()) query else raw
    }

    private fun extractAppQuery(raw: String): String {
        var clean = raw.replace(Regex("^(?:abra|abrir)\\s+(?:o|a|os|as)?\\s*", RegexOption.IGNORE_CASE), "").trim()
        clean = clean.split(" e ", " para ", " dizendo ").firstOrNull()?.trim() ?: clean
        return clean.lowercase(Locale.ROOT)
    }

    private fun guessKnownApp(query: String): InstalledAppInfo? {
        return when {
            query.contains("calculadora") -> InstalledAppInfo("Calculadora", "com.google.android.calculator")
            query.contains("chrome") || query.contains("navegador") -> InstalledAppInfo("Chrome", "com.android.chrome")
            query.contains("youtube") -> InstalledAppInfo("YouTube", "com.google.android.youtube")
            query.contains("whatsapp") -> InstalledAppInfo("WhatsApp", "com.whatsapp")
            query.contains("spotify") -> InstalledAppInfo("Spotify", "com.spotify.music")
            query.contains("mapas") || query.contains("maps") -> InstalledAppInfo("Google Maps", "com.google.android.apps.maps")
            query.contains("gmail") || query.contains("email") -> InstalledAppInfo("Gmail", "com.google.android.gm")
            query.contains("config") || query.contains("ajustes") -> InstalledAppInfo("Configurações", "com.android.settings")
            query.contains("câmera") || query.contains("camera") -> InstalledAppInfo("Câmera", "com.android.camera")
            query.contains("relógio") || query.contains("relogio") || query.contains("alarme") -> InstalledAppInfo("Relógio", "com.google.android.deskclock")
            query.contains("contatos") -> InstalledAppInfo("Contatos", "com.google.android.contacts")
            else -> null
        }
    }
}
