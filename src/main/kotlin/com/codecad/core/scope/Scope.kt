package com.codecad.core.scope

import com.codecad.core.exception.InterpreterException

interface Scope {
    fun getObject(key: String?): Any? {
        val value = getValue(key) ?: throw InterpreterException("Expected value of $key")
        return value.value
    }

    fun getValue(key: String?): Slot?
    fun values(): Collection<Slot> {
        return emptyList()
    }

    fun setObject(key: String?, value: Any?, define: Boolean): Boolean {
        if (define) {
            throw InterpreterException("Store of $key not supported")
        }
        return false
    }
}