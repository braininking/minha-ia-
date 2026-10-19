# Integração de Inteligência Artificial — Gemini Mobile AI

O aplicativo possui uma camada intercambiável de IA baseada na interface `AIProvider`.

## Provedores Disponíveis

### 1. Motor Local Inteligente (Nativo / Offline)
- Totalmente funcional no dispositivo sem necessidade de chave de API ou conexão com internet.
- Analisa intenções em português (abertura de apps, pesquisas na web, preparação de mensagens no WhatsApp).
- Mapeia nomes de aplicativos em português para pacotes instalados no Android.
- Converte comandos em planos estruturados com etapas detalhadas e validação de segurança.

### 2. Google Gemini API (Cloud)
- Integração oficial via REST API utilizando modelos modernos (como `gemini-2.5-flash`).
- Acesso à chave de API configurada de forma segura através do painel de **Secrets do AI Studio** via `BuildConfig.GEMINI_API_KEY`.
- Se a chave não estiver configurada ou houver falha de rede, o sistema automaticamente aciona o Motor Local como fallback transparente, garantindo que o assistente nunca pare de funcionar.
- Nenhuma chave de API é gravada no banco de dados local ou exibida em texto simples.
