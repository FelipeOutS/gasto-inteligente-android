package com.gastointeligente.app

import android.app.KeyguardManager
import android.content.Context
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyFactory
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 30)
class DeviceCredentialSessionTest {
    @Test fun sessionKeyRequiresPerUseAuthenticationAndVaultRemainsBiometricOnly() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(context.packageName.endsWith(".offlinevalidation"))
        assumeTrue((context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure)
        val store=SecureSessionStore(context)
        store.clear()
        try {
            assertEquals(BIOMETRIC_STRONG,store.allowedAuthenticators()) // old envelopes retain their policy
            assertEquals(BIOMETRIC_STRONG or DEVICE_CREDENTIAL,store.allowedAuthenticators(forSave=true))
            assertEquals(BIOMETRIC_STRONG,SecureSessionStore(context,"synthetic-vault").allowedAuthenticators(forSave=true))
            store.save(JSONObject().put("access_token","synthetic-access").put("refresh_token","synthetic-refresh").put("userId","synthetic-owner").put("email","test@example.invalid"))
            assertTrue(store.hasSession())
            assertEquals(BIOMETRIC_STRONG or DEVICE_CREDENTIAL,store.allowedAuthenticators())
            val keys=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
            val key=keys.getKey("gi.session.device.v2",null)
            val info=KeyFactory.getInstance(key.algorithm,"AndroidKeyStore").getKeySpec(key,KeyInfo::class.java)
            assertTrue(info.isUserAuthenticationRequired)
            assertEquals(0,info.userAuthenticationValidityDurationSeconds)
            assertEquals(KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,info.userAuthenticationType)
            val disk=context.getSharedPreferences("secure-session-v1",Context.MODE_PRIVATE).getString("envelope","")!!
            assertFalse(disk.contains("synthetic-access"))
            var denied=false
            try { val locked=store.prepareUnlock(); store.unlock(locked,locked.cipher) } catch(_:Exception) { denied=true }
            assertTrue("No decryption without a system-authenticated CryptoObject",denied)
        } finally { store.clear() }
        assertFalse(store.hasSession())
    }
}
