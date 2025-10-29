package com.senapp.util

import kotlinx.coroutines.*

class Debouncer(
    private val delayMs: Long = 600,
    private val scope: CoroutineScope
) {
    private var job: Job? = null
    fun submit(block: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            delay(delayMs)
            block()
        }
    }
}
