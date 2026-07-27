package com.aozijx.passly.core.telemetry

/**
 * 组合遥测发射器。
 *
 * 将事件分发给所有注册的子发射器。任一子发射器失败不影响其他。
 */
class CompositeTelemetryEmitter(
    private vararg val emitters: TelemetryEmitter
) : TelemetryEmitter {

    override fun emit(event: TelemetryEvent) {
        emitters.forEach { emitter ->
            runCatching { emitter.emit(event) }
        }
    }
}
