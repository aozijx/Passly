package com.aozijx.passly.app.message.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.aozijx.passly.app.message.contract.InAppNoticeStream
import com.aozijx.passly.app.message.contract.NoticeTextResolver

@Composable
internal fun AppNoticeEffects(
    inAppNoticeStream: InAppNoticeStream,
    textResolver: NoticeTextResolver,
) {
    val context = LocalContext.current

    LaunchedEffect(inAppNoticeStream, textResolver, context) {
        inAppNoticeStream.notices.collect { notice ->
            val message = textResolver.resolve(notice)
            Toast
                .makeText(
                    context,
                    message.text,
                    if (message.level.longToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
                ).show()
        }
    }
}
