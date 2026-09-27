# Auditoria de continuidade — Gasto Inteligente

Data: 12/09/2026. Escopo efetivamente disponível: `C:/Users/Felip/AndroidStudioProjects/GastoInteligente`.

**Registro histórico, anterior às correções.** O usuário autorizou posteriormente melhorias no Android. As alterações e a validação posterior estão em `CORRECOES-ANDROID-2026-09-12.md`; os resultados abaixo preservam o diagnóstico inicial.

## Diagnóstico principal e limites

A pasta contém o aplicativo Android, não o repositório da aplicação web descrito no handoff. O Android carrega `https://gastointeligente.com.br/app` em uma WebView. Não existem aqui package.json, código TanStack/React/TypeScript, migrations, policies Supabase ou backend. Não é possível concluir o estado do produto completo a partir deste cliente.

Foi lido o código Kotlin, configurações Gradle, manifest, recursos XML, testes e regras de empacotamento. Não foram modificados código funcional, dependências, banco ou configurações de produção. A execução do Gradle gerou arquivos de build/cache e relatórios. Esta auditoria não equivale a uma validação autenticada do site.

**Não foi possível localizar o ponto onde o desenvolvimento web anterior parou.** A busca por `findDuplicateGastoAdvanced`, `saldoDevedorEstimado`, `/adicionar` e `/confirmar` não retornou implementações. `src/lib/bens.ts` não existe nesta pasta. Não há evidência suficiente para afirmar se a correção de duplicidade ou de null chegou a ser feita. Não encontrar neste cliente não significa que o recurso não existe no produto.

## 1. Git e histórico

- Android: `git status --short --branch` retorna “not a git repository”. Não há branch, commits, branches antigas ou comparação de alterações locais disponível nesta pasta. Arquivos atuais podem conter trabalho não versionado; devem ser preservados.
- A pasta inicialmente vinculada à tarefa, `C:/Users/Felip/OneDrive/Documentos/ChatGPT/APLICATIVO GASTO INTELIGENTE`, contém apenas `.git`; status “No commits yet on master”, sem arquivos de aplicação.
- `.gitignore` do Android ignora `.gradle`, `local.properties`, parte de `.idea` e builds; `app/.gitignore` ignora `/build`. Isso não constitui histórico Git nem backup.
- Não foram encontrados AGENTS.md na raiz do projeto ou nos diretórios pai consultados.

## 2. Stack e estrutura

| Item | Configuração encontrada |
| --- | --- |
| Gerenciador/build | Gradle Wrapper 9.4.1, Kotlin DSL |
| Android Gradle Plugin | 9.2.1 |
| Plugin Kotlin Compose | 2.2.10 |
| Compose BOM | 2026.02.01 |
| Activity Compose | 1.8.0 |
| Core KTX / Lifecycle Runtime KTX | 1.10.1 / 2.6.1 |
| Biometric | 1.1.0 |
| Core Splashscreen | 1.2.0 |
| Security Crypto | 1.1.0; não há uso direto encontrado no código |
| JUnit / AndroidX JUnit / Espresso | 4.13.2 / 1.1.5 / 3.5.1 |
| SDK mínimo / alvo / compilação | 24 / 36 / 36.1 |
| JVM do daemon | Toolchain 21; compatibilidade de código Java 11 |
| Identificador / versão | com.gastointeligente.app / 1.0, versionCode 1 |

Versões extraídas de `gradle/libs.versions.toml`, `app/build.gradle.kts` e arquivos do wrapper. O plugin Compose não comprova sozinho a versão efetiva do compilador Kotlin integrado ao AGP.

Estrutura principal:

```text
app/
  build.gradle.kts
  proguard-rules.pro
  src/main/AndroidManifest.xml
  src/main/java/com/gastointeligente/app/MainActivity.kt
  src/main/java/com/gastointeligente/app/ui/theme/{Theme,Color,Type}.kt
  src/main/res/{drawable,mipmap-*,values,xml}/
  src/test/.../ExampleUnitTest.kt
  src/androidTest/.../ExampleInstrumentedTest.kt
gradle/{libs.versions.toml,gradle-daemon-jvm.properties,wrapper/}
build.gradle.kts
settings.gradle.kts
gradle.properties
local.properties
```

Arquitetura: uma `FragmentActivity` usa Compose/AndroidView para hospedar WebView. A mesma Activity concentra navegação, cookies, ponte JavaScript para biometria, permissões de câmera e seleção/captura de arquivos. Não há camada financeira nativa ou banco local de domínio encontrado.

Rotas: somente o endereço inicial `/app` está fixado no cliente. Outras rotas são servidas remotamente. Manifest tem uma Activity de launcher e um FileProvider não exportado; não há intent filters de deep links. Uma lista de rotas web não pode ser inferida pelo endereço inicial.

Ambiente: `local.properties` contém a chave `sdk.dir`, cujo valor foi omitido. Para executar o wrapper usei o JBR do Android Studio por `JAVA_HOME` apenas no processo de execução. Não existe `.env.example` ou declaração de variáveis Supabase/Mercado Pago/WhatsApp neste projeto. O domínio está no código, não em configuração por ambiente.

## 3. Verificação executada

Comando: `gradlew.bat :app:lintDebug :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease --continue --console=plain`.

| Verificação | Resultado |
| --- | --- |
| Lint debug | **FALHOU**: 33 ocorrências fatais de InvalidFragmentVersionForActivityResult e 30 avisos |
| Compilação Kotlin | Passou nas variantes executadas; aviso de databaseEnabled obsoleto |
| Typecheck TypeScript | Não aplicável: não há TypeScript ou package.json |
| Teste unitário debug | **1/1 passou**; testa apenas 2 + 2 = 4 |
| Teste instrumentado | **1/1 passou**, connectedDebugAndroidTest no Pixel_9_Pro_XL, Android 17/API 37; testa apenas packageName |
| Inicialização local | APK instalado com sucesso no emulador; am start -W retornou Status: ok, inicialização fria em 2.358 ms |
| APK debug | **PASSOU**, app/build/outputs/apk/debug/app-debug.apk |
| Build release | **FALHOU** em lintVitalRelease pelos mesmos 33 registros |
| Build completo da chamada | Exit code 1; 95 tarefas acionáveis, 66 executadas e 29 atualizadas |
| Lint/testes/build web | Indisponíveis sem repositório web |

Não foi executado `clean`; o Gradle reutilizou resultados válidos de tarefas existentes. Não há comprovação de build limpo de produção. Nenhum baseline ou supressão foi adicionado.

Evidências geradas:

- `app/build/reports/lint-results-debug.html` e `.xml`.
- `app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt`.
- `app/build/test-results/testDebugUnitTest/TEST-com.gastointeligente.app.ExampleUnitTest.xml`.
- `app/build/outputs/androidTest-results/connected/debug/` e `app/build/reports/androidTests/connected/debug/`.

O emulador foi iniciado sem janela, utilizando o AVD existente, sem salvar snapshot ao sair. O teste instrumentado terminou com BUILD SUCCESSFUL. Não houve login, criação de gastos ou transações de teste em produção. Inicializar a Activity não comprova que o site carregou corretamente ou que os fluxos autenticados funcionam; não foi feita inspeção visual. O buffer de crashes consultado continha um crash de com.android.chrome durante o boot, que não foi atribuído ao aplicativo.

Os 33 erros repetem o mesmo local, `MainActivity.kt:47`; não representam 33 bugs independentes. `dependencyInsight` confirmou **Fragment 1.2.5 no debugCompileClasspath**, trazido por Biometric 1.1.0, e **Fragment 1.5.4 no debugRuntimeClasspath**, por resolução transitiva com AppCompat/Splashscreen. Portanto, a mensagem de lint não deve ser interpretada apenas pela versão de runtime. É necessário alinhar a dependência de compilação e verificar as duas variantes.

Avisos do lint: 13 recursos sem uso, 7 avisos GradleDependency, 3 UseTomlInstead, 2 IconLocation, 2 MonochromeLauncherIcon e um de cada AndroidGradlePluginVersion, NewerVersionAvailable e OldTargetApi. Avisos de versões novas não são autorização nem motivo suficiente para atualização em massa.

## 4. Matriz de funcionalidades

CONCLUÍDO se refere apenas ao escopo comprovado; PARCIAL não implica falha de toda a funcionalidade. NÃO ENCONTRADO significa ausente deste código, não “não iniciado” no produto.

| Área | Status | Evidência / problema | Próxima ação |
| --- | --- | --- | --- |
| Empacotamento debug | CONCLUÍDO | APK gerado pelo Gradle | Validar fluxos em aparelho |
| Release Android | COM BUG | Lint fatal em MainActivity.kt:47 | Alinhar Fragment e repetir lint/release |
| WebView | PARCIAL | MainActivity.kt:330–398; carrega produção | Exercitar navegação, sessão, offline |
| Sessão e lifecycle | PARCIAL | Cookies/DOM storage habilitados; não restaura WebView | Testar recriação, morte de processo e expiração |
| Biometria/Cofre | PARCIAL | Ponte AndroidBiometric e prompt local | Validar contrato e segurança com código web |
| Seleção de arquivos | COM BUG | MainActivity.kt:283–290; aceita só imagem se lista também contém PDF; múltiplos sempre habilitados | Respeitar MIME types e modo |
| Foto pela câmera | PARCIAL | ACTION_IMAGE_CAPTURE sem fluxo próprio de permissão | Testar primeira captura com permissão negada |
| Splash/ícones | PARCIAL | Recursos presentes; falta installSplashScreen | Verificar versões Android suportadas |
| Erros/offline | PARCIAL | Não há tratamento nativo de onReceivedError/onReceivedHttpError | Definir estados recuperáveis |
| Testes Android | PARCIAL | Dois testes de template, sem cobertura funcional | Cobrir regressões de WebView/arquivos/biometria |
| Gastos, receitas, cartões | NÃO ENCONTRADO | Nenhuma implementação financeira nativa | Obter fonte web |
| Orçamento, contas, assinaturas, metas, guardado | NÃO ENCONTRADO | Nenhuma implementação de domínio | Obter fonte web |
| Planos, trial, AuthGate, PremiumLockModal, admin | NÃO ENCONTRADO | Nenhum gate backend ou matriz local | Auditar fonte web e servidor |
| Supabase, tabelas, migrations, RLS | NÃO ENCONTRADO | Nenhum SQL/backend | Auditar policies e isolamento no repositório correto |
| OCR, importações, deduplicação, bens | NÃO ENCONTRADO | Há transporte de arquivos; não há interpretação/salvamento | Verificar ponto prioritário no código web |
| Mercado Inteligente | NÃO ENCONTRADO | Sem providers/cache/produtos locais | Verificar Joanin, Carrefour e demais integrações no web |
| Empresa, clientes, fornecedores | NÃO ENCONTRADO | Sem código desses módulos | Auditar fonte web |
| Mercado Pago | NÃO ENCONTRADO | Sem webhook/payment_events no cliente | Verificar idempotência e autorização no backend |
| WhatsApp | NÃO ENCONTRADO | Sem pipeline, testes ou gates locais | Verificar backend e suítes reais |
| Radar, investimentos, IA, logos, i18n web | NÃO ENCONTRADO | Sem implementações locais | Auditar fonte web |

Não foram atribuídos preços, prazo de trial, limites de IA ou status operacional a essas áreas com base apenas no handoff. Não há suporte local para atestar RLS, ausência de vazamento entre contas, logs sem secrets, pagamentos ou integridade financeira.

## 5. Achados técnicos

1. **Release bloqueado — confirmado por execução.** Fragment de compilação 1.2.5 não atende à regra do Activity Result. Arquivos: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `MainActivity.kt:47`. Corrigir dependência explicitamente e preservar o lint.

2. **Seletor restringe incorretamente formatos — confirmado por leitura.** Com `accept=image/*,application/pdf`, o código escolhe somente `image/*`; PDF fica indisponível. `EXTRA_ALLOW_MULTIPLE=true` ignora seleção única. Para entradas sem accept, também restringe a imagens. Arquivo: `MainActivity.kt:283–290`. O impacto específico em cada tela web exige conhecer o input da tela.

3. **Estado de navegação não é restaurado — confirmado por leitura.** `onCreate` sempre cria WebView e carrega `/app`; não há saveState/restoreState. `onDestroy` cancela seleção pendente e destrói a WebView. Recriação pode perder página/formulário/seleção. Isso **não prova** que cookies são apagados ou que o bug histórico de retorno ao login tem esta causa. Cookies são aceitos e flush ocorre em onPageFinished; o ciclo de refresh/logout depende do web.

4. **Captura de foto sem pedir CAMERA neste fluxo — risco reproduzível pendente.** Manifest declara CAMERA, mas pedido runtime está apenas no fluxo getUserMedia/PermissionRequest. openFileChooser oferece ACTION_IMAGE_CAPTURE sem verificar a permissão. Pode falhar em instalação nova ou após revogação. Referência: [MediaStore.ACTION_IMAGE_CAPTURE](https://developer.android.com/reference/android/provider/MediaStore#ACTION_IMAGE_CAPTURE). Arquivos: `MainActivity.kt:217–245,249–309`, manifest.

5. **Links externos são cancelados sem encaminhamento — confirmado por leitura.** `shouldOverrideUrlLoading` retorna true para host fora da allowlist e não abre navegador/Intent. Links externos de checkout, OAuth, WhatsApp, telefone ou e-mail ficam sem ação quando passam por esse callback. Não há evidência de qual fluxo real utiliza tais links. Arquivo: `MainActivity.kt:376–388`.

6. **Biometria precisa de auditoria conjunta com web — risco, não bypass comprovado.** Ponte usa addJavascriptInterface; o objeto alcança frames e a chamada não identifica a origem. Allowlist aceita todos os subdomínios e não verifica scheme. Resultado booleano é enviado ao documento corrente, sem correlação com solicitação/página; o evento por si só não é prova de autorização backend. HTTP está bloqueado no manifest. Validar os frames carregados, CSP, navegação durante prompt e o código de desbloqueio do Cofre. Referência: [segurança de pontes WebView](https://developer.android.com/privacy-and-security/risks/insecure-webview-native-bridges). Arquivo: `MainActivity.kt:85–204,351–354`.

7. **Backup e compartilhamento amplos — risco de configuração.** allowBackup=true; backup_rules e data_extraction_rules existem, mas não estão ligados ao manifest, também apontado pelo lint. FileProvider não exportado é positivo, porém compartilha raízes inteiras de files/cache via path=".". Não foi demonstrado vazamento. Delimitar conteúdo sensível e os diretórios efetivamente necessários. Arquivos: manifest e `res/xml/*`.

8. **Splash incompleto na integração — verificação visual pendente.** Tema e ícone existem, mas MainActivity não chama installSplashScreen antes de super.onCreate. Conferir compatibilidade e aplicação do tema final em API 24–30 e 31+. Referência: [migração de splash Android](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate).

9. **Cobertura funcional inexistente nos testes fornecidos.** ExampleUnitTest testa aritmética; ExampleInstrumentedTest testa packageName. Nenhum testa login, upload, persistência, biometria, navegação ou dados financeiros. Não há testes web/WhatsApp nesta árvore.

10. **Manutenção secundária.** Temas Compose de template não são invocados pela Activity; recursos sem uso confirmados no lint; security-crypto não tem chamadas diretas encontradas. TODO relevante apenas em regras de backup de template. Não há deduplicador duplicado encontrado. databaseEnabled gera aviso de depreciação. Não remover dependências transitivas/recursos sem avaliar impacto.

Não há logs explícitos de tokens no código nativo lido; isso não audita logs do WebView, bibliotecas, servidor ou produção. Recursos externos nativos encontrados: domínio do produto, câmera/seleção de arquivos, biometria e repositórios de dependências Google/Maven/Gradle/Foojay.

## 6. Plano de conclusão

Itens web abaixo são verificações propostas pelo handoff, não bugs confirmados nesta auditoria.

| Prioridade | Problema e impacto | Arquivos envolvidos | Solução proposta | Testes necessários |
| --- | --- | --- | --- | --- |
| P0 | Fonte web ausente; não se pode validar integridade de dados | Repositório web ainda não disponível | Localizar/exportar fonte original com migrations e histórico | Stack, lint, typecheck, testes e build; inventário real |
| P0 | Isolamento/RLS e permissões administrativas não auditados | SQL, server functions, auth/gates a localizar | Matriz SELECT/INSERT/UPDATE/DELETE e revisão service role | Duas contas e acesso cruzado; endpoints admin e storage |
| P0 | Duplicidade e semântica null históricas sem verificação | findDuplicateGastoAdvanced, bens.ts, adicionar/confirmar a localizar | Comparar fluxo foto com importadores; preservar confirmação de casos ambíguos e null | Mesmo documento; OCR variante; ±1 dia; mesmo valor/lojas distintas; compras legítimas; null sem financiamento |
| P0 | Eventos financeiros potencialmente repetidos, estado desconhecido | Webhooks Mercado Pago/WhatsApp e constraints a localizar | Auditar idempotência transacional, retries e concorrência | Evento repetido/concorrente; reembolso; confirmação repetida |
| P0 | Código Android sem histórico | Raiz Android | Criar versionamento/backup preservando estado e excluindo dados locais de SDK e chaves | Revisar arquivos antes do primeiro commit |
| P1 | Build release bloqueado | app/build.gradle.kts; libs.versions.toml | Alinhar Fragment no compile/runtime com versão compatível | dependencyInsight nas variantes; lint; testes; assembleRelease |
| P1 | Upload restringe formatos e modo | MainActivity.kt:276–315 | Respeitar acceptTypes, MIME extras e FileChooserParams.mode | PDF+imagem, PDF isolado, CSV, seleção única/múltipla e cancelamento |
| P1 | Câmera e callback vulneráveis a lifecycle | MainActivity.kt:43–82,217–315,432–439 | Tratar permissão do fluxo de captura e resultados pendentes; evitar URIs duplicadas/arquivos vazios | Primeira instalação, permissão negada/revogada, rotação, cancelamento e câmera sem resultado |
| P1 | Navegação/sessão após recriação sem garantia | MainActivity.kt:320–405 e auth web | Restaurar estado quando adequado; verificar sessão sem limpar storage indiscriminadamente | Rotação, background, processo encerrado, token expirado, logout explícito |
| P1 | Biometria/backup precisam limitar exposição | MainActivity.kt:85–204; manifest; res/xml | Revisar contrato com web, origens, correlação, repetição e política de backup/FileProvider | Navegar durante prompt, iframe não confiável, cancelamento, tentativa repetida e restauração de backup |
| P1 | Links externos não funcionam no callback | MainActivity.kt:376–388 | Política explícita por scheme/destino e abertura externa segura | Checkout, OAuth se usado, WhatsApp e retorno ao app |
| P1 | Falta recuperação de erros e validação mobile | MainActivity.kt; themes.xml; telas web | Estados offline/retry, splash e área segura/insets | API 24, 30, 31 e alvo; offline, lento, HTTP 500, reload /gastos |
| P1 | Planos/trial/pagamentos sem auditoria | Matriz de planos, trial, endpoints a localizar | Uma regra central de autorização validada no servidor | Expiração/timezone; antigo/novo; upgrade/downgrade; webhook duplicado |
| P1 | Cobertura insuficiente | app/src/test e app/src/androidTest; testes web a localizar | Testes de comportamento dos fluxos críticos após diagnóstico | Rodar suítes funcionais sem substituir por templates |
| P2 | Mercado/Empresa e APIs sem confirmação | Providers e módulos web a localizar | Reutilizar arquitetura existente; concluir integrações após verificar estado real | Fallback, cache, limites, volume e gates |
| P2 | Recursos/dependências sem uso e avisos | ui/theme, res, Gradle | Limpeza pequena após estabilização; revisar acessibilidade/i18n | Build, lint e comparação visual |
| P3 | iOS e melhorias futuras | Projeto ainda não disponível | Adiar até critérios críticos de web/backend/Android estarem comprovados | Plano específico posterior |

Release deve continuar bloqueado até a correção dos achados P0/P1 aplicáveis, validação do código web e build de produção verificado. Compilar debug e passar teste de template não comprova prontidão do produto.
