package com.aozijx.passly.feature.autofill.attack

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.service.autofill.FillResponse
import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillManager
import android.widget.EditText
import android.widget.LinearLayout

internal const val CLAIMED_WEB_DOMAIN = "login.example.com"
internal const val FINISH_ATTACK_ACTIVITY = "com.aozijx.passly.feature.autofill.attack.FINISH"
internal const val CAPTURE_PREFERENCES = "autofill-origin-attack-capture"
internal const val CAPTURE_RECEIVED = "received"
internal const val CAPTURE_PACKAGE = "package"
internal const val CAPTURE_DOMAIN = "domain"
internal const val CAPTURE_AMBIGUOUS = "ambiguous"
internal const val CAPTURE_FIELD_COUNT = "field-count"

class AutofillOriginAttackActivity : Activity() {
    private lateinit var username: SpoofedWebOriginEditText
    private var requestSent = false
    private val finishReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username =
            SpoofedWebOriginEditText(this).apply {
                hint = "Username"
                setAutofillHints(View.AUTOFILL_HINT_USERNAME)
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
            }
        val password =
            EditText(this).apply {
                hint = "Password"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                setAutofillHints(View.AUTOFILL_HINT_PASSWORD)
            }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(username)
                addView(password)
            },
        )
        val finishFilter = IntentFilter(FINISH_ATTACK_ACTIVITY)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(finishReceiver, finishFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(finishReceiver, finishFilter)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(finishReceiver)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || requestSent || !::username.isInitialized) return
        requestSent = true
        username.post {
            username.requestFocus()
            getSystemService(AutofillManager::class.java).requestAutofill(username)
        }
    }
}

private class SpoofedWebOriginEditText(
    context: Context,
) : EditText(context) {
    override fun onProvideAutofillStructure(
        structure: ViewStructure,
        flags: Int,
    ) {
        super.onProvideAutofillStructure(structure, flags)
        structure.setWebDomain(CLAIMED_WEB_DOMAIN)
    }
}
