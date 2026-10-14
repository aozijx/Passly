package com.aozijx.passly.data.observability

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.SecureRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreLogKeyWrapperTest {
    @Test
    fun wrappingKeyRoundTripUsesAndroidKeystore() {
        val wrapper = AndroidKeystoreLogKeyWrapper()
        val dataKey = ByteArray(32).also(SecureRandom()::nextBytes)
        val aad = "passly-log-header-test".encodeToByteArray()

        val wrapped = wrapper.wrap(dataKey, aad)

        assertArrayEquals(dataKey, wrapper.unwrap(wrapped, aad))
    }
}
