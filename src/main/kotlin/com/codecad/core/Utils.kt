package com.codecad.core

import com.codecad.core.exception.InterpreterException
import java.util.*
import java.util.stream.Stream

infix fun ClosedRange<Double>.step(step: Double): Iterable<Double> {
    require(start.isFinite())
    require(endInclusive.isFinite())
    require(step > 0.0) { "Step must be positive, was: $step." }
    val sequence = generateSequence(start) { previous ->
        if (previous == Double.POSITIVE_INFINITY) return@generateSequence null
        val next = previous + step
        if (next > endInclusive) null else next
    }
    return sequence.asIterable()
}

object Utils {
    fun getIterator(obj: Any?): Iterator<*> {
        return if (obj is Array<*>) {
            Arrays.stream(obj as Array<Any>?).iterator()
        } else if (obj is Iterable<*>) {
            obj.iterator()
        } else if (obj is Stream<*>) {
            obj.iterator()
        }else{
            throw InterpreterException("Value not iterable")
        }
    }
}


fun <T> Iterable<T>.rollover() : Iterable<Pair<T, T>> where T : Any
{
    return Iterable {
        var empty = true
        var last = false
        var firstObject: T? = null
        var lastObject : T? = null
        val delegate = iterator()

        if(delegate.hasNext()){
            empty = false
            firstObject = delegate.next()
            lastObject = firstObject
        }

        object : Iterator<Pair<T, T>>{
            override fun hasNext(): Boolean {
                return !empty && ( delegate.hasNext() || !last )
            }

            override fun next(): Pair<T, T> {
                return if(delegate.hasNext()){
                    val nextObject = delegate.next()
                    val next = Pair(lastObject!!, nextObject)
                    lastObject = nextObject
                    next
                }else{
                    last = true
                    Pair(lastObject!!, firstObject!!)
                }
            }
        }
    }
}
