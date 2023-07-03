package com.codecad.core

import com.codecad.core.exception.InterpreterException
import java.util.*
import java.util.stream.Stream

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