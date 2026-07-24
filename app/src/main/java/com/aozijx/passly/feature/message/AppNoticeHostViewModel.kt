package com.aozijx.passly.feature.message

import androidx.lifecycle.ViewModel
import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.port.InAppNoticeStream
import com.aozijx.passly.domain.notice.port.NoticeTextResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class RenderedNotice(
    val text: String,
    val longDuration: Boolean
)

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
