# Gemini Mobile AI — Assistente Android com Controle do Celular

Aplicativo nativo para Android com interface futurista em preto profundo e verde neon, inteligência artificial integrada, controle de aplicativos e automação de tarefas sob supervisão do usuário.

## Principais Funcionalidades

1. **Esfera Holográfica Nativa (Jetpack Compose Canvas):**
   - Animação em 60fps com anéis geométricos concêntricos em rotação, pulso de núcleo quântico e ondas de áudio responsivas a comandos de voz.
   - Reação visual dinâmica aos estados: *Disponível*, *Escutando*, *Interpretando*, *Planejando*, *Executando*, *Verificando*, *Concluído* e *Erro*.

2. **Controle Real do Android:**
   - Abertura de aplicativos instalados no aparelho por nome em português (ex: Calculadora, Chrome, YouTube, WhatsApp).
   - Pesquisa na Web automatizada e navegação por URLs.
   - Preparação de mensagens no WhatsApp com revisão prévia do destinatário e do texto.
   - Serviço de Acessibilidade (`AppAccessibilityService`) para identificação de elementos na tela, toques e preenchimento de campos.

3. **Níveis de Segurança & Autonomia:**
   - **Conservador:** Solicita aprovação prévia antes de qualquer ação relevante na interface.
   - **Equilibrado:** Executa navegação e leituras automaticamente; exige autorização expressa para envio de mensagens ou submissões.
   - **Avançado:** Encadeia execuções sequenciais com tripwires de segurança e registro completo em log de auditoria.

4. **Inteligência Artificial Híbrida e Flexível:**
   - **Motor Local Inteligente:** Funciona 100% offline, sem necessidade de internet ou chaves de API, interpretando comandos e gerando planos estruturados de ação.
   - **Google Gemini API:** Integração cloud configurável via painel Secrets do AI Studio com fallback automático e transparente para o motor local.

5. **Comandos de Voz & Resposta Falada:**
   - Reconhecimento de voz em português brasileiro com visualizador de amplitude no orbe.
   - Respostas faladas via TextToSpeech em português com controle de tom e velocidade.

6. **Segundo Plano & Botão Flutuante:**
   - Serviço em primeiro plano com notificação persistente para disponibilidade contínua.
   - Bolha flutuante holográfica (Overlay) com arraste na tela e acesso rápido.

7. **Persistência Local (Room & DataStore):**
   - Banco de dados SQLite local contendo histórico de mensagens, planos de execução e logs de auditoria.
   - Configurações de preferências preservadas localmente.

## Como Compilar e Gerar o APK

```bash
# Compilar projeto e executar testes locais
gradle :app:testDebugUnitTest

# Gerar APK de depuração
gradle :app:assembleDebug
```
O APK gerado estará em: `app/build/outputs/apk/debug/app-debug.apk`.
