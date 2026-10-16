package com.aozijx.passly.core.observability

/** Process-local monotonic time for measuring operation durations. */
fun monotonicTimeMs(): Long = System.nanoTime() / 1_000_000L
