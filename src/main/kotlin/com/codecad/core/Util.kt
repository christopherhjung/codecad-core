package com.codecad.core

import com.codecad.common.PointD
/*
fun <T> List<T>.rollover() : Iterable<Pair<T, T>> where T : Any
{
    val list = ArrayList(this)
    list.add(list.first())
    return list.zipWithNext()
}
*/

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

fun <T> Iterable<T>.lookahead() : Iterable<Pair<T, T>> where T : Any
{
    return Iterable {
        var empty = true
        var lastObject : T? = null
        val delegate = iterator()

        if(delegate.hasNext()){
            empty = false
            lastObject = delegate.next()
        }

        object : Iterator<Pair<T, T>>{
            override fun hasNext(): Boolean {
                return !empty && delegate.hasNext()
            }

            override fun next(): Pair<T, T> {
                val nextObject = delegate.next()
                val next = Pair(lastObject!!, nextObject)
                lastObject = nextObject
                return next
            }
        }
    }
}
