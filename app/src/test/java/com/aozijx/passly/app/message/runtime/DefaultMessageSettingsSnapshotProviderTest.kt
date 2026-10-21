package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.port.MessageSettingsSource
import com.aozijx.passly.app.message.contract.MessageSettingsSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertSame
import org.junit.Test

class DefaultMessageSettingsSnapshotProviderTest {

    @Test
    fun `snapshot is unavailable until persisted settings emit`() {
        val source = object : MessageSettingsSource {
            override val messages = MutableSharedFlow<MessageSettings>()
        }

        val provider = DefaultMessageSettingsSnapshotProvider(source)

        assertSame(MessageSettingsSnapshot.Loading, provider.current())
    }
}
