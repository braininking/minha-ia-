# Arquitetura — Gemini Mobile AI

O **Gemini Mobile AI** é estruturado segundo os princípios de Clean Architecture e MVVM (Model-View-ViewModel) para Android nativo em Kotlin e Jetpack Compose.

## 1. Visão Geral dos Módulos

```text
com.example/
├── GeminiMobileApp.kt             # Application class, inicialização de Singletons e Banco
├── MainActivity.kt                # Host Activity, Edge-to-Edge e NavigationBar M3
├── model/                         # Entidades de Domínio e Modelos de Tarefa
│   ├── TaskModels.kt              # AssistantState, AutonomyLevel, ActionType, TaskPlan, TaskStep
│   └── ChatAndAuditModels.kt      # ChatMessage, AuditLogEntry
├── data/                          # Camada de Persistência Local
│   ├── SettingsRepository.kt      # DataStore de Preferências (IA, Voz, Autonomia, Serviços)
│   └── local/                     # Room Database
│       ├── AppDatabase.kt         # Database Singleton
│       ├── Entities.kt            # ChatEntity, TaskEntity, AuditEntity
│       └── Daos.kt                # ChatDao, TaskDao, AuditDao
├── ai/                            # Camada de Inteligência Artificial Modular
│   ├── AIProvider.kt              # Interface desacoplada de IA
│   ├── LocalIntelligentProvider.kt# Motor de Processamento de Linguagem Natural Local (Offline)
│   ├── GeminiProvider.kt          # Integração Oficial com a API do Google Gemini
│   └── AIPlanner.kt               # Validador de Segurança e Enforce de Políticas de Autonomia
├── accessibility/                 # Serviço de Acessibilidade
│   └── AppAccessibilityService.kt # Inspeção de nós de tela, cliques e preenchimento de campos
├── automation/                    # Motor de Execução e Ciclo de Vida
│   ├── ActionExecutor.kt          # Execução física de etapas com verificação e cancelamento
│   └── TaskManager.kt             # Máquina de estados, cancelamento instantâneo e auditoria
├── voice/                         # Camada de Fala e Reconhecimento
│   └── SpeechManager.kt           # Reconhecimento pt-BR e TextToSpeech com nível de áudio
├── services/                      # Serviços em Segundo Plano
│   ├── AssistantForegroundService.kt # Foreground service com notificação permanente
│   └── FloatingBubbleService.kt   # Overlay holográfico flutuante (bolha arrastável)
└── ui/                            # Camada de Apresentação (Jetpack Compose)
    ├── theme/                     # Cores futuristas (Preto #050706, Verde Neon #39FF14)
    ├── components/
    │   ├── HolographicOrb.kt      # Esfera holográfica animada nativa em Canvas
    │   ├── TaskPlanCard.kt        # Card interativo de etapas, autorização e progresso
    │   └── TopBar.kt              # Barra de status do assistente e acessibilidade
    └── screens/
        ├── HomeScreen.kt          # Cockpit futurista com orbe, voz e comandos
        ├── ChatScreen.kt          # Histórico de conversas com leitura em áudio
        ├── TasksScreen.kt         # Histórico de tarefas e logs de auditoria
        └── SettingsScreen.kt      # Configurações de IA, voz, autonomia e privacidade
```

## 2. Separação Decisão vs Execução
- A IA (Gemini ou Motor Local) gera apenas propostas de planos estruturados em JSON tipado (`TaskPlan`).
- O `AIPlanner` filtra qualquer ação fora da lista autorizada (`ALLOWED_ACTIONS`).
- O `ActionExecutor` checa a política de autonomia antes de disparar qualquer ação real.
- Em caso de comando de cancelamento pelo usuário, a execução é interrompida imediatamente.
