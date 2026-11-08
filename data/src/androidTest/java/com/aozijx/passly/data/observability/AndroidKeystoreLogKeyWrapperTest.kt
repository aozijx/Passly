package com.aozijx.passly.data.observability

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyStore
import java.security.SecureRandom
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreLogKeyWrapperTest {
    @Before
    fun clearPriorWrappingKey() = deleteWrappingKey()

    @After
    fun removeWrappingKey() = deleteWrappingKey()

    private fun deleteWrappingKey() {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            if (containsAlias(AndroidKeystoreLogKeyWrapper.KEY_ALIAS)) {
                deleteEntry(AndroidKeystoreLogKeyWrapper.KEY_ALIAS)
            }
        }
    }

    @Test
    fun wrappingKeyRoundTripUsesAndroidKeystore() {
        val wrapper = AndroidKeystoreLogKeyWrapper()
        val dataKey = ByteArray(32).also(SecureRandom()::nextBytes)
        val aad = "passly-log-header-test".encodeToByteArray()

        val wrapped = wrapper.wrap(dataKey, aad)

        assertArrayEquals(dataKey, wrapper.unwrap(wrapped, aad))
    }
}
