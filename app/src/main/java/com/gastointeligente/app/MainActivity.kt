package com.gastointeligente.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

class MainActivity : FragmentActivity() {

    @Volatile private var safeTopCssPx = 0
    @Volatile private var safeBottomCssPx = 0
    @Volatile private var safeLeftCssPx = 0
    @Volatile private var safeRightCssPx = 0

    private inner class AndroidSystemUiBridge {
        @JavascriptInterface
        fun getInsets(): String = JSONObject()
            .put("top", safeTopCssPx)
            .put("bottom", safeBottomCssPx)
            .toString()

        @JavascriptInterface
        fun setSurface(surface: String) {
            if (surface !in setOf("dark", "light", "brand")) return
            runOnUiThread {
                if (isAllowedUrl(webViewRef?.url)) applySystemSurface(surface)
            }
        }
    }

    private fun applySystemSurface(surface: String) {
        val light = surface == "light"
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
        webViewRef?.setBackgroundColor(if (light) 0xFFFAFAFB.toInt() else 0xFF101214.toInt())
    }

    private fun publishSafeInsets() {
        val top = safeTopCssPx
        val bottom = safeBottomCssPx
        val left = safeLeftCssPx
        val right = safeRightCssPx
        webViewRef?.evaluateJavascript(
            """(function(){var d=document.documentElement;d.style.setProperty('--android-safe-top','${top}px');d.style.setProperty('--android-safe-bottom','${bottom}px');d.style.setProperty('--android-safe-left','${left}px');d.style.setProperty('--android-safe-right','${right}px');window.dispatchEvent(new Event('android-system-insets'));})();""",
            null,
        )
    }

    private var webViewRef: WebView? = null
    private var downloads: WebDownloads? = null
    private var secureSession: WebSecureSession? = null
    private var pendingReport: ReadyDownload? = null
    private var rendererFailed by mutableStateOf(false)
    private var recoveryUrl = BuildConfig.WEB_APP_URL
    private var initialPageVisible = false
    private var webStateToRestore: Bundle? = null
    private val oauthPreferences by lazy { getSharedPreferences("android-oauth", MODE_PRIVATE) }

    private fun receiveOAuth(url: String) {
        val state = oauthPreferences.getString("state", null)
        val attempt = state?.let { OAuthAttempt(it, oauthPreferences.getLong("startedAt", 0)) }
        val result = AndroidOAuth.accept(url, attempt, System.currentTimeMillis())
        if (result == null) {
            Toast.makeText(this, "Este retorno de login é inválido ou expirou. Inicie o login novamente no aplicativo.", Toast.LENGTH_LONG).show()
            return
        }
        if (!oauthPreferences.edit().clear().commit()) {
            Toast.makeText(this, "Não foi possível concluir o login. Tente novamente.", Toast.LENGTH_LONG).show()
            return
        }
        if (result.pageUrl == null) {
            Toast.makeText(this, result.error, Toast.LENGTH_LONG).show()
            webViewRef?.loadUrl("${AndroidOAuth.ORIGIN}/login")
            return
        }
        webStateToRestore = null
        recoveryUrl = result.pageUrl
        if (rendererFailed) rendererFailed = false
        else webViewRef?.loadUrl(result.pageUrl)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val url = intent.dataString
        // Do not keep OAuth tokens in the Activity's intent or saved state.
        intent.data = null
        if (url != null && AndroidOAuth.isCallback(url)) receiveOAuth(url)
    }

    private val saveReportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val report = pendingReport
        pendingReport = null
        if (report != null) {
            val destination = result.data?.data
            if (result.resultCode != RESULT_OK || destination?.scheme != "content" || destination.authority == "$packageName.fileprovider") {
                report.file.delete()
            } else {
                val appContext = applicationContext
                Thread {
                    val message = try {
                        appContext.contentResolver.openOutputStream(destination, "wt").use { output ->
                            checkNotNull(output)
                            report.file.inputStream().use { it.copyTo(output) }
                        }
                        "Relatório salvo."
                    } catch (_: Exception) {
                        "Não foi possível salvar o relatório. Exporte novamente e escolha outro destino."
                    } finally { report.file.delete() }
                    runOnUiThread { Toast.makeText(appContext, message, Toast.LENGTH_LONG).show() }
                }.start()
            }
        }
    }

    private fun saveReport(report: ReadyDownload): Boolean {
        if (pendingReport != null || isFinishing || isDestroyed) return false
        pendingReport = report
        return try {
            saveReportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE).setType(report.mime)
                .putExtra(Intent.EXTRA_TITLE, report.name))
            true
        } catch (_: Exception) {
            pendingReport = null
            Toast.makeText(this, "Não foi possível abrir o seletor para salvar o relatório.", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun releaseWebView(view: WebView, rendererGone: Boolean = false) {
        if (view.tag == "gi-released") return
        view.tag = "gi-released"
        if (webViewRef === view) {
            downloads?.close()
            downloads = null
            secureSession?.close()
            secureSession = null
            webViewRef = null
        }
        // Android requires detaching the WebView before destroy().
        (view.parent as? ViewGroup)?.removeView(view)
        if (!rendererGone) {
            view.stopLoading()
            view.webChromeClient = null
            view.removeJavascriptInterface("AndroidBiometric")
        }
        view.destroy()
    }

    private var pendingPermissionRequest: PermissionRequest? = null
    private val cameraPermissionRequestCode = 9001

    private val geolocation: WebGeolocation = WebGeolocation(
        currentPage = { webViewRef?.url },
        hasPermission = {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        },
        requestPermission = {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        },
    )

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            geolocation.complete(result[Manifest.permission.ACCESS_COARSE_LOCATION] == true || result[Manifest.permission.ACCESS_FINE_LOCATION] == true)
        }

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null
    private var cameraImageFile: File? = null
    private var fileSelection: FileSelection? = null
    private var fileSourceDialog: AlertDialog? = null
    private var awaitingFileResult = false

    private val captureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            awaitingFileResult = false
            val uri = cameraImageUri
            val hasImage = cameraImageFile?.length()?.let { it > 0 } == true
            finishFileSelection(if (success && hasImage && uri != null) arrayOf(uri) else null)
        }

    private val capturePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            awaitingFileResult = false
            if (filePathCallback != null) {
                if (granted) launchCamera()
                else {
                    Toast.makeText(this, "Câmera não autorizada. Escolha uma foto ou arquivo salvo.", Toast.LENGTH_LONG).show()
                    launchFilePicker()
                }
            }
        }

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            awaitingFileResult = false
            val uris = if (result.resultCode == RESULT_OK) {
                fileSelection?.readPickerResult(result.data, "$packageName.fileprovider")
            } else null
            finishFileSelection(uris)
        }

    @Suppress("unused")
    inner class AndroidBiometricBridge {

        @JavascriptInterface
        fun isAvailable(): Boolean {
            return this@MainActivity.isBiometricAvailable()
        }

        @JavascriptInterface
        fun isBiometricAvailable(): Boolean {
            return this@MainActivity.isBiometricAvailable()
        }

        @JavascriptInterface
        fun authenticate() {
            runOnUiThread {
                this@MainActivity.showBiometricPrompt()
            }
        }

        @JavascriptInterface
        fun requestAuthentication() {
            authenticate()
        }

        @JavascriptInterface
        fun unlock() {
            authenticate()
        }
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)

        return biometricManager.canAuthenticate(BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        if (!isBiometricAvailable()) {
            sendBiometricResult(
                success = false,
                error = "Biometria indisponível ou não cadastrada neste aparelho."
            )
            return
        }

        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    sendBiometricResult(success = true)
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    sendBiometricResult(
                        success = false,
                        error = errString.toString()
                    )
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Cofre Pessoal")
            .setSubtitle("Use sua digital ou reconhecimento facial")
            .setNegativeButtonText("Usar PIN do Cofre")
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun sendBiometricResult(success: Boolean, error: String? = null) {
        val json = """
            {
                "success": $success,
                "error": ${JSONObject.quote(error ?: "")}
            }
        """.trimIndent()

        val script = """
            window.__androidBiometricLastResult = $json;

            window.dispatchEvent(
                new CustomEvent('AndroidBiometricResult', {
                    detail: $json
                })
            );

            if (
                window.AndroidBiometricCallback &&
                typeof window.AndroidBiometricCallback.onResult === 'function'
            ) {
                window.AndroidBiometricCallback.onResult($json);
            }
        """.trimIndent()

        webViewRef?.post {
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    private fun isAllowedUrl(url: String?): Boolean {
        return navigationTarget(url) == NavigationTarget.INTERNAL
    }

    private fun handleNavigation(url: String?, mainFrame: Boolean, userGesture: Boolean): Boolean {
        if (mainFrame && url != null && isAllowedUrl(webViewRef?.url)) {
            val start = AndroidOAuth.start(url, System.currentTimeMillis())
            if (start != null) {
                oauthPreferences.edit().putString("state", start.attempt.state).putLong("startedAt", start.attempt.startedAt).apply()
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, start.browserUrl.toUri()).addCategory(Intent.CATEGORY_BROWSABLE))
                } catch (_: Exception) {
                    oauthPreferences.edit().clear().apply()
                    Toast.makeText(this, "Instale ou habilite um navegador para entrar com Google.", Toast.LENGTH_LONG).show()
                }
                // Keep the site on a usable login screen if the browser flow is canceled.
                webViewRef?.loadUrl("${AndroidOAuth.ORIGIN}/login")
                return true
            }
        }
        return when (navigationTarget(url, mainFrame, userGesture)) {
            NavigationTarget.INTERNAL -> false
            NavigationTarget.BLOCKED -> true
            NavigationTarget.EXTERNAL -> {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, url!!.toUri()).addCategory(Intent.CATEGORY_BROWSABLE))
                } catch (_: Exception) {
                    Toast.makeText(this, "Nenhum aplicativo disponível para abrir este link.", Toast.LENGTH_LONG).show()
                }
                true
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermissionForWebView(request: PermissionRequest) {
        pendingPermissionRequest?.deny()
        pendingPermissionRequest = request

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            cameraPermissionRequestCode
        )
    }

    private fun handleWebViewPermissionRequest(request: PermissionRequest) {
        val origin = request.origin?.toString()

        if (!isAllowedUrl(origin)) {
            request.deny()
            return
        }

        val wantsCamera = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)

        if (!wantsCamera) {
            request.deny()
            return
        }

        if (hasCameraPermission()) {
            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        } else {
            requestCameraPermissionForWebView(request)
        }
    }

    private fun launchCamera() {
        if (filePathCallback == null) return
        try {
            val imageFile = File.createTempFile(
                "gasto_inteligente_camera_",
                ".jpg",
                cacheDir
            )
            cameraImageFile = imageFile

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile
            )

            cameraImageUri = uri
            awaitingFileResult = true
            captureLauncher.launch(uri)
        } catch (_: Exception) {
            awaitingFileResult = false
            Toast.makeText(this, "Não foi possível abrir a câmera. Escolha um arquivo salvo.", Toast.LENGTH_LONG).show()
            launchFilePicker()
        }
    }

    private fun requestPhoto() {
        if (hasCameraPermission()) launchCamera()
        else {
            awaitingFileResult = true
            capturePermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchFilePicker() {
        val selection = fileSelection ?: return
        try {
            awaitingFileResult = true
            fileChooserLauncher.launch(Intent.createChooser(selection.createPickerIntent(), "Selecionar arquivo"))
        } catch (_: Exception) {
            awaitingFileResult = false
            Toast.makeText(this, "Não foi possível abrir o seletor de arquivos.", Toast.LENGTH_LONG).show()
            finishFileSelection(null)
        }
    }

    private fun finishFileSelection(uris: Array<Uri>?) {
        val callback = filePathCallback
        filePathCallback = null
        fileSelection = null
        // Keep a successful capture in cache until WebView has consumed it.
        if (!awaitingFileResult && (cameraImageUri == null || uris?.contains(cameraImageUri) != true)) {
            cameraImageFile?.delete()
        }
        cameraImageFile = null
        cameraImageUri = null
        callback?.onReceiveValue(uris)
    }

    private fun openFileChooser(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams?
    ): Boolean {
        // An older activity result must never complete a newer upload request.
        if (filePathCallback != null || awaitingFileResult) {
            callback.onReceiveValue(null)
            return true
        }
        filePathCallback = callback
        val selection = FileSelection.from(
            params?.acceptTypes?.toList().orEmpty(),
            params?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE,
        ) { extension -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) }
        fileSelection = selection

        if (!selection.allowsCamera) {
            launchFilePicker()
        } else if (params?.isCaptureEnabled == true) {
            requestPhoto()
        } else {
            fileSourceDialog = AlertDialog.Builder(this)
                .setTitle("Adicionar comprovante ou arquivo")
                .setItems(arrayOf("Tirar foto", "Escolher arquivo")) { _, index ->
                    if (index == 0) requestPhoto() else launchFilePicker()
                }
                .setOnCancelListener { finishFileSelection(null) }
                .setOnDismissListener { fileSourceDialog = null }
                .show()
        }
        return true
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashStartedAt = android.os.SystemClock.elapsedRealtime()
        installSplashScreen().setKeepOnScreenCondition {
            !initialPageVisible && android.os.SystemClock.elapsedRealtime() - splashStartedAt < 3000
        }
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applySystemSurface("dark")
        awaitingFileResult = savedInstanceState?.getBoolean("awaitingFileResult") == true
        // Never restore a page from a different build endpoint (e.g. production in local debug).
        webStateToRestore = savedInstanceState?.takeIf {
            it.getString("webAppUrl") == BuildConfig.WEB_APP_URL
        }?.getBundle("webViewState")
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        val callbackUrl = intent.dataString
        intent.data = null
        if (callbackUrl != null && AndroidOAuth.isCallback(callbackUrl)) receiveOAuth(callbackUrl)
        savedInstanceState?.getString("pendingReportPath")?.let { path ->
            val file = File(path)
            if (file.canonicalFile.parentFile == File(cacheDir, "reports").canonicalFile && file.isFile) {
                pendingReport = ReadyDownload(file, DownloadAssembler.safeName(savedInstanceState.getString("pendingReportName") ?: "relatorio"),
                    savedInstanceState.getString("pendingReportMime") ?: "application/octet-stream")
            }
        }

        setContent {
            var canGoBack by remember { mutableStateOf(false) }
            var failedUrl by remember { mutableStateOf<String?>(null) }

            BackHandler(enabled = canGoBack) {
                failedUrl = null
                webViewRef?.let { web ->
                    // Let the visible web overlay consume Back before changing routes.
                    web.evaluateJavascript(
                        """(function() {
                          if (!document.querySelector('#bank-picker-options[data-state="open"], .cartoes-form-dialog[data-state="open"], [role="menu"][data-state="open"]')) return false;
                          document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape', bubbles: true, cancelable: true }));
                          return true;
                        })();"""
                    ) { handled ->
                        if (handled != "true" && webViewRef === web) web.goBack()
                    }
                }
            }

            Box(Modifier.fillMaxSize().background(Color(0xFF101214)).imePadding()) {
            if (!rendererFailed) AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {

                        webViewRef = this
                        setBackgroundColor(0xFF101214.toInt())
                        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                            val safe = insets.getInsets(
                                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                            )
                            val density = resources.displayMetrics.density
                            val nextTop = (safe.top / density).roundToInt()
                            val nextBottom = (safe.bottom / density).roundToInt()
                            val nextLeft = (safe.left / density).roundToInt()
                            val nextRight = (safe.right / density).roundToInt()
                            if (nextTop != safeTopCssPx || nextBottom != safeBottomCssPx || nextLeft != safeLeftCssPx || nextRight != safeRightCssPx) {
                                safeTopCssPx = nextTop
                                safeBottomCssPx = nextBottom
                                safeLeftCssPx = nextLeft
                                safeRightCssPx = nextRight
                                post { publishSafeInsets() }
                            }
                            insets
                        }
                        post { ViewCompat.requestApplyInsets(this) }
                        downloads = WebDownloads(this, { pendingReport == null }, ::saveReport).also { it.attach() }
                        secureSession = WebSecureSession(this@MainActivity, this).also { it.attach() }
                        setDownloadListener { _, _, _, _, _ ->
                            Toast.makeText(context, "Não foi possível baixar este arquivo. Para relatórios, use a opção Exportar. Se necessário, atualize o Android System WebView.", Toast.LENGTH_LONG).show()
                        }

                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setGeolocationEnabled(true)
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.mediaPlaybackRequiresUserGesture = false

                        settings.allowFileAccess = true
                        settings.allowContentAccess = true

                        addJavascriptInterface(
                            AndroidBiometricBridge(),
                            "AndroidBiometric"
                        )
                        addJavascriptInterface(AndroidSystemUiBridge(), "AndroidSystemUi")

                        webChromeClient = object : WebChromeClient() {

                            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                                geolocation.request(origin) { allowed -> callback.invoke(origin, allowed, false) }
                            }

                            override fun onGeolocationPermissionsHidePrompt() {
                                geolocation.cancel()
                            }

                            override fun onPermissionRequest(request: PermissionRequest) {
                                runOnUiThread {
                                    handleWebViewPermissionRequest(request)
                                }
                            }

                            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                                if (pendingPermissionRequest === request) pendingPermissionRequest = null
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                if (filePathCallback == null) return false
                                return openFileChooser(filePathCallback, fileChooserParams)
                            }
                        }

                        webViewClient = object : WebViewClient() {

                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? =
                                request?.let { OfflinePage.intercept(applicationContext, it) }

                            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                                geolocation.cancel()
                                finishFileSelection(null)
                                pendingPermissionRequest = null
                                releaseWebView(view, rendererGone = true)
                                canGoBack = false
                                failedUrl = recoveryUrl
                                rendererFailed = true
                                return true
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return handleNavigation(request?.url?.toString(), request?.isForMainFrame == true, request?.hasGesture() == true)
                            }

                            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                url: String?
                            ): Boolean {
                                return handleNavigation(url, mainFrame = true, userGesture = false)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                geolocation.cancel()
                                downloads?.onNavigation()
                                secureSession?.cancel()
                                if (isAllowedUrl(url)) recoveryUrl = Uri.parse(url).buildUpon().fragment(null).build().toString()
                                failedUrl = null
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true && isAllowedUrl(request.url.toString())) {
                                    failedUrl = request.url.toString()
                                }
                            }

                            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, response: WebResourceResponse?) {
                                super.onReceivedHttpError(view, request, response)
                                if (request?.isForMainFrame == true && (response?.statusCode ?: 0) >= 500 && isAllowedUrl(request.url.toString())) {
                                    failedUrl = request.url.toString()
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                canGoBack = this@apply.canGoBack()
                                CookieManager.getInstance().flush()
                            }

                            override fun onPageCommitVisible(view: WebView?, url: String?) {
                                super.onPageCommitVisible(view, url)
                                initialPageVisible = true
                                if (isAllowedUrl(url)) publishSafeInsets()
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                canGoBack = view?.canGoBack() == true
                            }
                        }

                        val restored = webStateToRestore?.let { restoreState(it) }
                        webStateToRestore = null
                        if (restored == null) loadUrl(recoveryUrl)
                    }
                },
                update = { webView ->
                    canGoBack = webView.canGoBack()
                },
                onRelease = { releaseWebView(it) },
            )
            if (failedUrl != null) {
                Column(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF0B0F14)).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Não foi possível carregar esta página", color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text("Verifique sua conexão ou tente novamente em instantes.", color = Color.LightGray)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        failedUrl = null
                        recoveryUrl = OfflinePage.URL
                        if (rendererFailed) rendererFailed = false else webViewRef?.loadUrl(OfflinePage.URL)
                    }) { Text("Registrar gasto sem internet") }
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = {
                        val retryUrl = failedUrl
                        failedUrl = null
                        if (rendererFailed) rendererFailed = false
                        else if (isAllowedUrl(retryUrl)) webViewRef?.loadUrl(retryUrl!!)
                    }) { Text("Tentar novamente") }
                }
            }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("webAppUrl", BuildConfig.WEB_APP_URL)
        pendingReport?.let {
            outState.putString("pendingReportPath", it.file.absolutePath)
            outState.putString("pendingReportName", it.name)
            outState.putString("pendingReportMime", it.mime)
        }
        outState.putBoolean("awaitingFileResult", awaitingFileResult)
        webViewRef?.let { webView ->
            val state = Bundle()
            // The callback route clears its fragment after SDK initialization.
            // Never snapshot it before that has happened.
            if (webView.url?.contains("access_token=") != true && recoveryUrl.contains("access_token=").not() &&
                webView.saveState(state) != null) outState.putBundle("webViewState", state)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        webViewRef?.onPause()
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webViewRef?.onResume()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != cameraPermissionRequestCode) return

        val request = pendingPermissionRequest
        pendingPermissionRequest = null

        if (request == null) return

        val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

        if (granted) {
            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        } else {
            request.deny()
        }
    }

    override fun onDestroy() {
        geolocation.cancel()
        fileSourceDialog?.dismiss()
        fileSourceDialog = null
        finishFileSelection(null)
        pendingPermissionRequest?.deny()
        pendingPermissionRequest = null

        webViewRef?.let { releaseWebView(it) }

        super.onDestroy()
    }

    override fun onStop() {
        secureSession?.onHostStopped()
        super.onStop()
    }
}
