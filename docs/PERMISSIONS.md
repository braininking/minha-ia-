# Permissões do Android — Gemini Mobile AI

O aplicativo segue o princípio do menor privilégio, declarando apenas permissões estritamente necessárias para a automação e o assistente de voz.

| Permissão | Tipo | Finalidade |
|---|---|---|
| `android.permission.INTERNET` | Normal (Instalação) | Conexão com o provedor Gemini Cloud e busca web. |
| `android.permission.ACCESS_NETWORK_STATE` | Normal (Instalação) | Monitorar conectividade de rede antes de chamadas remotas. |
| `android.permission.RECORD_AUDIO` | Perigosa (Tempo de Execução) | Reconhecimento de comandos de voz em português via SpeechRecognizer. |
| `android.permission.POST_NOTIFICATIONS` | Perigosa (Tempo de Execução, Android 13+) | Notificação persistente do serviço em primeiro plano. |
| `android.permission.FOREGROUND_SERVICE` | Normal (Instalação) | Manter o assistente disponível em segundo plano. |
| `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` | Normal (Instalação, Android 14+) | Execução contínua de rotinas de automação autorizadas. |
| `android.permission.SYSTEM_ALERT_WINDOW` | Especial (Configurações do Sistema) | Exibição da bolha flutuante holográfica sobre outros aplicativos. |
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | Especial (Acessibilidade) | Inspeção da interface e realização de toques/digitação sob supervisão. |

## Como Ativar o Serviço de Acessibilidade
1. Abra as Configurações do Android.
2. Acesse **Acessibilidade** > **Aplicativos Instalados**.
3. Selecione **Gemini Mobile AI - Controle e Automação**.
4. Ative a chave e confirme a permissão no sistema.
