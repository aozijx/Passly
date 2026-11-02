package com.aozijx.passly.core.permission.internal

import android.content.ContextWrapper
import android.content.SharedPreferences
import com.aozijx.passly.core.permission.model.RuntimePermission
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreferencesPermissionRequestHistoryTest {
    @Test
    fun `request history keys remain explicit and stable`() {
        assertEquals("CAMERA", RuntimePermission.CAMERA.requestHistoryKey)
        assertEquals("POST_NOTIFICATIONS", RuntimePermission.POST_NOTIFICATIONS.requestHistoryKey)
    }

    @Test
    fun `reads legacy keys and marks permissions independently`() {
        val values = mutableMapOf<String, Any>("CAMERA" to true, "POST_NOTIFICATIONS" to true)
        val preferences = memoryPreferences(values)
        val context =
            object : ContextWrapper(null) {
                override fun getSharedPreferences(
                    name: String,
                    mode: Int,
                ): SharedPreferences = preferences
            }
        val history = SharedPreferencesPermissionRequestHistory(context)

        assertTrue(history.wasRequested(RuntimePermission.CAMERA))
        assertTrue(history.wasRequested(RuntimePermission.POST_NOTIFICATIONS))

        history.markRequested(RuntimePermission.CAMERA)

        assertTrue(history.wasRequested(RuntimePermission.CAMERA))
        assertTrue(history.wasRequested(RuntimePermission.POST_NOTIFICATIONS))
    }

    @Test
    fun `first mark and repeated mark use the existing per permission key`() {
        val values = mutableMapOf<String, Any>()
        val preferences = memoryPreferences(values)
        val context =
            object : ContextWrapper(null) {
                override fun getSharedPreferences(
                    name: String,
                    mode: Int,
                ): SharedPreferences = preferences
            }
        val history = SharedPreferencesPermissionRequestHistory(context)

        assertFalse(history.wasRequested(RuntimePermission.CAMERA))
        assertFalse(history.wasRequested(RuntimePermission.POST_NOTIFICATIONS))

        history.markRequested(RuntimePermission.CAMERA)
        history.markRequested(RuntimePermission.CAMERA)

        assertTrue(history.wasRequested(RuntimePermission.CAMERA))
        assertFalse(history.wasRequested(RuntimePermission.POST_NOTIFICATIONS))
        assertTrue(values["CAMERA"] == true)
        assertFalse("POST_NOTIFICATIONS" in values)

        history.markRequested(RuntimePermission.POST_NOTIFICATIONS)

        assertTrue(history.wasRequested(RuntimePermission.POST_NOTIFICATIONS))
        assertTrue(values["POST_NOTIFICATIONS"] == true)
    }

    private fun memoryPreferences(values: MutableMap<String, Any>): SharedPreferences {
        lateinit var editor: SharedPreferences.Editor
        editor =
            Proxy.newProxyInstance(
                SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java),
            ) { _, method, arguments ->
                when (method.name) {
                    "putBoolean" -> {
                        values[arguments!![0] as String] = arguments[1] as Boolean
                        editor
                    }

                    "apply" -> {
                        null
                    }

                    "commit" -> {
                        true
                    }

                    else -> {
                        error("Unexpected SharedPreferences.Editor call: ${method.name}")
                    }
                }
            } as SharedPreferences.Editor
        return Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
        ) { _, method, arguments ->
            when (method.name) {
                "getBoolean" -> {
                    values[arguments!![0] as String] as? Boolean ?: arguments[1] as Boolean
                }

                "edit" -> {
                    editor
                }

                else -> {
                    error("Unexpected SharedPreferences call: ${method.name}")
                }
            }
        } as SharedPreferences
    }
}
