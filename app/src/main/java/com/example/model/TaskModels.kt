package com.example.model

import java.util.UUID

enum class AssistantState(val labelPt: String) {
    IDLE("Disponível"),
    LISTENING("Escutando..."),
    INTERPRETING("Interpretando..."),
    PLANNING("Planejando..."),
    EXECUTING("Executando..."),
    VERIFYING("Verificando..."),
    COMPLETED("Concluído"),
    ERROR("Erro"),
    PAUSED("Pausado")
}

enum class AutonomyLevel(val titlePt: String, val descriptionPt: String) {
    CONSERVADOR(
        "Conservador",
        "Planeja tarefas e solicita confirmação antes de executar qualquer ação relevante na interface."
    ),
    EQUILIBRADO(
        "Equilibrado",
        "Executa automaticamente ações seguras de navegação. Solicita confirmação para envio de mensagens e submissões."
    ),
    AVANCADO(
        "Avançado",
        "Executa sequências contínuas de ações previamente mapeadas, mantendo bloqueios para dados críticos e relatório de auditoria."
    )
}

enum class ActionType(val labelPt: String) {
    LAUNCH_APP("Abrir Aplicativo"),
    OPEN_URL("Abrir URL / Navegador"),
    ACCESSIBILITY_CLICK("Clicar no Elemento"),
    ACCESSIBILITY_INPUT("Preencher Campo"),
    ACCESSIBILITY_SCROLL("Rolar Tela"),
    SEND_WHATSAPP_MESSAGE("Preparar Mensagem WhatsApp"),
    SEARCH_WEB("Pesquisar na Web"),
    WAIT_DELAY("Aguardar Interface"),
    VERIFY_RESULT("Verificar Resultado")
}

enum class StepStatus {
    PENDING,
    RUNNING,
    WAITING_APPROVAL,
    COMPLETED,
    FAILED,
    SKIPPED,
    CANCELLED
}

enum class TaskStatus(val labelPt: String) {
    CREATED("Criada"),
    WAITING_CONFIRMATION("Aguardando Autorização"),
    PLANNING("Em Planejamento"),
    EXECUTING("Em Execução"),
    VERIFYING("Em Verificação"),
    PAUSED("Pausada"),
    COMPLETED("Concluída"),
    FAILED("Falhou"),
    CANCELLED("Cancelada")
}

data class TaskStep(
    val id: String = UUID.randomUUID().toString(),
    val index: Int,
    val actionType: ActionType,
    val description: String,
    val targetPackage: String? = null,
    val targetLabel: String? = null,
    val targetText: String? = null,
    val targetQuery: String? = null,
    var status: StepStatus = StepStatus.PENDING,
    var resultMessage: String? = null,
    var requiresConfirmation: Boolean = false
)

data class TaskPlan(
    val id: String = UUID.randomUUID().toString(),
    val goal: String,
    val steps: MutableList<TaskStep>,
    var currentStepIndex: Int = 0,
    var status: TaskStatus = TaskStatus.CREATED,
    val createdAt: Long = System.currentTimeMillis(),
    var finishedAt: Long? = null,
    var autonomyLevel: AutonomyLevel = AutonomyLevel.EQUILIBRADO,
    var errorMessage: String? = null
) {
    fun currentStep(): TaskStep? {
        return if (currentStepIndex in steps.indices) steps[currentStepIndex] else null
    }

    fun isFinished(): Boolean {
        return status in listOf(TaskStatus.COMPLETED, TaskStatus.FAILED, TaskStatus.CANCELLED)
    }
}

data class ScreenContext(
    val packageName: String? = null,
    val activityName: String? = null,
    val visibleTexts: List<String> = emptyList(),
    val clickableElements: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean = false
)
