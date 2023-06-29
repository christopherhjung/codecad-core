package com.codecad.core.scope


object EmptyScope : Scope {
    override fun getValue(key: String?): Slot? {
        return null
    }
}