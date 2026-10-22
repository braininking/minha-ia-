# Integração de Inteligência Artificial — Gemini Mobile AI

O aplicativo possui uma camada intercambiável de IA baseada na interface `AIProvider`.

## Provedores Disponíveis

### 1. Motor Local Inteligente (Nativo / Offline)
- Totalmente funcional no dispositivo sem necessidade de chave de API ou conexão com internet.
- Analisa intenções em português (abertura de apps, pesquisas na web, preparação de mensagens no WhatsApp).
- Mapeia nomes de aplicativos em português para pacotes instalados no Android.
- Converte comandos em planos estruturados com etapas detalhadas e validação de segurança.

### 2. Google Gemini API (Cloud)
- Integração via REST API com `gemini-2.5-flash` para respostas de chat e geração de planos estruturados em JSON.
- Acesso à chave de API configurada de forma segura através do painel de **Secrets do AI Studio** via `BuildConfig.GEMINI_API_KEY`.
- O botão de teste de conexão consulta a API de modelos; não exibe sucesso sem uma resposta HTTP válida.
- Se a chave não estiver configurada, a rede falhar ou o plano retornado for inválido, o sistema aciona o Motor Local como fallback.
- O planejador local aplica a política de confirmação independentemente do que o modelo Gemini solicitar. Cliques, preenchimento de campos e preparação de mensagens exigem confirmação nos modos Equilibrado e Avançado.
- O contexto enviado ao Gemini para planejar tarefas inclui o comando, o catálogo limitado de apps e o pacote atual da tela; o texto visível da tela não é enviado.
- Nenhuma chave de API é gravada no banco de dados local ou exibida em texto simples.
