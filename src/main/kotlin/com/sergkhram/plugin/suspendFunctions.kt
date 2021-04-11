package com.sergkhram.plugin

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <T> Iterable<T>.pforEach(coroutineContext: CoroutineContext, action: suspend (T) -> Unit) {
    val iterable = this
    coroutineScope {
        val listOfAsync = mutableListOf<Deferred<Any>>()
        iterable.forEach {
            listOfAsync.add(async(coroutineContext) { action(it) })
        }
        listOfAsync.awaitAll()
    }
}