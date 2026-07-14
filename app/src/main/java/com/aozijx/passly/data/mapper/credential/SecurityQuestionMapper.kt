package com.aozijx.passly.data.mapper.credential

import com.aozijx.passly.data.model.payload.credential.SecurityQuestionPayload
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toSecurityQuestionPayload(): SecurityQuestionPayload? {
    if (securityQuestion.isNullOrBlank() && securityAnswer.isNullOrBlank()) {
        return null
    }
    return SecurityQuestionPayload(
        question = securityQuestion,
        answer = securityAnswer
    )
}

fun VaultEntry.mergeSecurityQuestion(payload: SecurityQuestionPayload?): VaultEntry {
    payload ?: return this
    return copy(
        securityQuestion = payload.question,
        securityAnswer = payload.answer
    )
}