# Web local no APK debug

Por padrão, debug e release abrem `https://gastointeligente.com.br/app`.
Somente o debug compilado com `-PlocalWeb=true` abre `http://127.0.0.1:5173/app`.
O APK não empacota o frontend web.

1. Na pasta `gasto-facil-financeiro`, execute:
   ```powershell
   node node_modules/vite/bin/vite.js dev --host 127.0.0.1 --port 5173 --strictPort
   ```
2. Com o emulador aberto, execute pelo Android SDK:
   ```powershell
   adb -s emulator-5554 reverse tcp:5173 tcp:5173
   ```
3. Compile com `gradlew.bat :app:assembleDebug -PlocalWeb=true`, instale o debug
   e abra o aplicativo. Mantenha o servidor ligado.
   O túnel precisa ser refeito após reiniciar o emulador/ADB.

O HTTP é autorizado apenas para 127.0.0.1 pela configuração de rede exclusiva
do source set debug. A navegação interna local aceita somente a porta 5173.
Release não inclui essa configuração e mantém HTTP desabilitado.
Inspeção do WebView é habilitada apenas em debug.

O login local é separado da origem de produção. Não copiar tokens ou simular
sessão. Usar uma sessão local válida ou credenciais QA autorizadas. O frontend
local mantém o backend já configurado no projeto; não é um ambiente de dados
isolado. OAuth e bridges com origens de produção não foram alterados por esta
correção e não fazem parte da validação local de navegação.

Validação realizada: build debug, sete testes NavigationPolicyTest, dezoito
testes web mobile-navigation; geração do BuildConfig e manifesto de release
para verificar que a URL e a política de rede de produção permanecem intactas.
