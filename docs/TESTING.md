# Guia de Testes — Gemini Mobile AI

## 1. Testes Automatizados (JVM / Robolectric)

Para executar a suíte de testes locais:
```bash
gradle :app:testDebugUnitTest
```

Casos de teste cobertos:
- Verificação do nome e integridade dos recursos (`readStringFromContext`).
- Planejamento de tarefas de busca e abertura do Chrome em português (`testLocalAIPlannerForChromeSearch`).
- Planejamento de mensagens no WhatsApp com retenção de aprovação no modo Equilibrado (`testWhatsAppPlanningWithAutonomySafety`).
- Política de Autonomia Conservadora forçando autorização em todas as ações (`testConservativeAutonomyRequiresConfirmationForAllActionSteps`).

## 2. Roteiro de Testes Manuais no Dispositivo Real (ex: Samsung Galaxy A72)

1. **Interface e Visual Futurista:**
   - Confirmar tema preto profundo `#050706` com brilho verde neon `#39FF14`.
   - Verificar rotação e pulso suave da esfera holográfica.
2. **Comando de Abertura de Aplicativo:**
   - Digitar ou falar: "Abra a Calculadora".
   - Observar o planejamento, a transição do estado do orbe e a abertura do aplicativo.
3. **Pesquisa no Chrome:**
   - Digitar: "Abra o Chrome e pesquise aplicativos gratuitos de edição de vídeo".
   - Verificar a sequência de etapas no card de tarefas e a exibição da busca no navegador.
4. **Envio de Mensagem no WhatsApp:**
   - Digitar: "Mande uma mensagem para o Marcos dizendo que chegarei às 18 horas".
   - Confirmar que o assistente pausa no modo Equilibrado exibindo o botão "Autorizar".
   - Clicar em "Autorizar" e verificar o direcionamento ao WhatsApp com o texto preparado.
5. **Cancelamento:**
   - Iniciar qualquer tarefa e clicar em "Interromper Tarefa".
   - Confirmar que a execução é abortada imediatamente.
6. **Serviço em Segundo Plano:**
   - Ativar o serviço nas Configurações e verificar a notificação fixa na barra de status.
   - Ativar o botão flutuante e verificar se a bolha verde neon responde ao toque sobre outros apps.
