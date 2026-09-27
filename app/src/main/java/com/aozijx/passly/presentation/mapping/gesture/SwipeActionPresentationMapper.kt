package com.aozijx.passly.presentation.mapping.gesture

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.shared.gesture.SwipeActionUiModel

fun SwipeActionType.toUiModel(): SwipeActionUiModel = when (this) {
    SwipeActionType.DELETE -> SwipeActionUiModel.DELETE
    SwipeActionType.DETAIL -> SwipeActionUiModel.DETAIL
    SwipeActionType.COPY_PASSWORD -> SwipeActionUiModel.COPY_PASSWORD
    SwipeActionType.COPY_USERNAME -> SwipeActionUiModel.COPY_USERNAME
}

fun SwipeActionUiModel.toDomainModel(): SwipeActionType = when (this) {
    SwipeActionUiModel.DELETE -> SwipeActionType.DELETE
    SwipeActionUiModel.DETAIL -> SwipeActionType.DETAIL
    SwipeActionUiModel.COPY_PASSWORD -> SwipeActionType.COPY_PASSWORD
    SwipeActionUiModel.COPY_USERNAME -> SwipeActionType.COPY_USERNAME
}
