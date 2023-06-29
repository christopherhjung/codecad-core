package com.codecad.core.scope

class Range(private val startIndex: Int, private val endIndex: Int) : Iterable<Int?> {
    override fun iterator(): Iterator<Int> {
        return object : Iterator<Int> {
            var current = startIndex
            override fun hasNext(): Boolean {
                return current < endIndex
            }

            override fun next(): Int {
                return ++current
            }
        }
    }
}