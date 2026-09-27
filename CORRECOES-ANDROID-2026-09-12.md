# Correções Android — 12/09/2026

Este trabalho aplica a etapa possível com o código disponível após a auditoria. O código web, Supabase, pagamentos e WhatsApp continuam fora desta pasta. Nenhuma integração financeira nova foi criada e nenhum dado de produção foi modificado.

## Alterações

- **Release:** Fragment 1.5.4 passou a ser dependência explícita, alinhando a compilação com a versão já resolvida em runtime. Não foi feita atualização geral das bibliotecas nem supressão do lint.
- **Anexos:** filtros preservam PDF e imagem juntos, seleção única/múltipla e tipos por extensão. Um mesmo URI devolvido em data e ClipData é entregue uma única vez. Conteúdo vazio/cancelado não reaproveita a última foto.
- **Câmera:** escolha explícita entre tirar foto e escolher arquivo; permissão solicitada somente ao tirar foto. Em caso de negativa ou falha ao abrir a câmera, permite escolher um arquivo salvo. A captura só é entregue quando há resultado de sucesso e arquivo não vazio. O JPEG da câmera não é oferecido a um input que aceita apenas PNG.
- **Envios concorrentes:** um segundo pedido não substitui o callback de um envio aberto. Resultados externos em andamento são sinalizados no estado salvo para impedir associação com um novo envio após recriação.
- **Lifecycle:** saveState/restoreState preservam histórico e página corrente; cookies são persistidos ao pausar; WebView acompanha pause/resume. Mudanças de histórico atualizam o estado do botão Voltar.
- **Splash:** installSplashScreen foi ligado ao ciclo de criação da Activity. Retirada a configuração databaseEnabled obsoleta; DOM storage permanece habilitado.
- **Testes:** adicionadas verificações de filtros e resultados de arquivo, cancelamento, concorrência de pedidos e restauração da página. As fixtures não criam gastos nem fazem login em produção.

## Arquivos envolvidos

- `app/build.gradle.kts` e `gradle/libs.versions.toml`.
- `app/src/main/java/com/gastointeligente/app/MainActivity.kt`.
- `app/src/main/java/com/gastointeligente/app/FileSelection.kt`.
- `app/src/main/java/com/gastointeligente/app/FilePicker.kt`.
- `app/src/test/java/com/gastointeligente/app/FileSelectionTest.kt`.
- `app/src/androidTest/java/com/gastointeligente/app/FilePickerTest.kt`.
- `app/src/androidTest/java/com/gastointeligente/app/MainActivityTest.kt`.

Backup do código anterior: `C:/Users/Felip/OneDrive/Documentos/ChatGPT/APLICATIVO GASTO INTELIGENTE/backups/android-antes-correcoes-20260912-181424.zip`. Não inclui local.properties nem credenciais. O projeto segue sem histórico Git.

## Validação final

| Verificação | Resultado |
| --- | --- |
| lintDebug | Passou, zero erros; 31 avisos de manutenção permanecem |
| testDebugUnitTest | 10 testes passaram (9 novos e 1 original) |
| connectedDebugAndroidTest | 9 testes passaram (8 novos e 1 original) no Pixel_9_Pro_XL, Android 17/API 37 |
| assembleDebug | Passou |
| assembleRelease | Passou; APK unsigned |

As primeiras execuções dos novos testes de UI identificaram limitações da fixture loadDataWithBaseURL, incompatibilidade do Espresso 3.5.1 com injeção de eventos no Android 17 e necessidade de aguardar callbacks assíncronos. Os testes finais usam resposta HTML interceptada em domínio `.invalid`, Instrumentation para a tecla Voltar e latches com timeout. Nenhum teste foi removido, pulado ou teve a expectativa funcional enfraquecida.

Comandos executados: `gradlew.bat :app:lintDebug :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:connectedDebugAndroidTest --continue --console=plain`; após os ajustes somente nos testes instrumentados, `gradlew.bat :app:lintDebug :app:connectedDebugAndroidTest --continue --console=plain`. Não houve alteração posterior do código funcional ao build release aprovado.

Relatórios: `app/build/reports/lint-results-debug.html`, `app/build/reports/tests/testDebugUnitTest/index.html` e `app/build/reports/androidTests/connected/debug/index.html`.

Artefatos: `app/build/outputs/apk/debug/app-debug.apk` e `app/build/outputs/apk/release/app-release-unsigned.apk`.

## Limites e próximos testes em aparelho

- A restauração do histórico não promete preservar todos os formulários HTML, uploads em andamento, sessão Supabase ou tokens expirados. Ao destruir a Activity, um upload ainda aberto é cancelado; o usuário pode precisar selecioná-lo novamente.
- Capturas concluídas ficam no cache para a WebView consumir. O backend continua responsável por validar arquivo e detectar gasto duplicado; remover URIs repetidos no seletor não implementa deduplicação financeira.
- Validar em aparelho físico: câmera permitida/negada/revogada, captura e seleção PDF real, retorno após background, biometria e versões Android mais antigas. Os testes automatizados de lifecycle não validam login ou biometria no site.
- O APK release gerado é **unsigned**. Ainda precisa da assinatura de distribuição original antes de publicação. O APK debug serve para teste.
- As pendências de segurança da ponte biométrica, backup, links externos e erros offline descritas na auditoria permanecem fora deste lote.
- Para continuar WhatsApp/pagamentos sem inventar uma arquitetura paralela, trazer do outro computador o código web, migrations, funções de servidor e exemplo de configuração sem valores secretos. Somente o banco Supabase não substitui o código do site.
