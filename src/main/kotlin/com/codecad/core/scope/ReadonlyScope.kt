package com.codecad.core.scope

class ReadonlyScope(val scope: Scope) : Scope {
    override fun getValue(key: String?): Slot? {
        return scope.getValue(key)
    }
}