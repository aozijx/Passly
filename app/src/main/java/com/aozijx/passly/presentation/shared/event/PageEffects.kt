package com.aozijx.passly.presentation.shared.event

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

/** Delivers one-shot effects only to the page visit that produced them. */
internal class PageEffects<Effect : Any> {
    private data class Delivery<Effect>(
        val owner: Any,
        val effect: Effect,
    )

    private val channel = Channel<Delivery<Effect>>(Channel.BUFFERED)
    private var activeOwner: Any? = null

    val currentOwner: Any?
        get() = activeOwner

    val effects: Flow<Effect> =
        channel
            .receiveAsFlow()
            .filter { activeOwner === it.owner }
            .map { it.effect }

    fun enter(owner: Any = Any()): Any {
        activeOwner = owner
        return owner
    }

    fun exit(owner: Any) {
        if (activeOwner === owner) activeOwner = null
    }

    fun isActive(owner: Any): Boolean = activeOwner === owner

    fun tryEmit(effect: Effect): Boolean {
        val owner = activeOwner ?: return false
        return tryEmit(owner, effect)
    }

    fun tryEmit(
        owner: Any,
        effect: Effect,
    ): Boolean = isActive(owner) && channel.trySend(Delivery(owner, effect)).isSuccess

    suspend fun send(
        owner: Any,
        effect: Effect,
    ) {
        if (isActive(owner)) channel.send(Delivery(owner, effect))
    }
}

/**
 * Collects one-shot page effects while their host is visible. The supplied flow may buffer effects
 * until resume; it must reject effects from a page visit after that visit exits.
 */
internal suspend fun <Effect : Any> collectPageEffects(
    lifecycle: Lifecycle,
    effects: Flow<Effect>,
    onEffect: suspend (Effect) -> Unit,
) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        effects.collect(onEffect)
    }
}
