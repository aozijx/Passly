package com.aozijx.passly.app.message.presentation

import androidx.lifecycle.ViewModel
import com.aozijx.passly.data.message.model.NoticeLevel
import com.aozijx.passly.app.message.contract.InAppNoticeStream
import com.aozijx.passly.app.message.contract.NoticeTextResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@HiltViewModel
class AppNoticeHostViewModel @Inject constructor(
    inAppNoticeStream: InAppNoticeStream,
    textResolver: NoticeTextResolver
) : ViewModel() {
    val toastMessages: Flow<RenderedNotice> = inAppNoticeStream.notices.map { notice ->
        val resolved = textResolver.resolve(notice)
        RenderedNotice(
            text = resolved.text,
            longDuration = resolved.level.ordinal >= NoticeLevel.ERROR.ordinal
        )
    }
}
