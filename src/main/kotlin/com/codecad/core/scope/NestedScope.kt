package com.codecad.core.scope

class NestedScope(val parent: Scope, val child: Scope) : Scope {
    override fun getValue(key: String?): Slot? {
        val value = child.getValue(key)
        return value ?: parent.getValue(key)
    }

    override fun setObject(key: String?, value: Any?, define: Boolean): Boolean {
        return if (parent.setObject(key, value, false)) {
            true
        } else child.setObject(key, value, define)
    }

    companion object {
        fun mutual(parent: Scope?, map: MutableMap<String?, Slot?> = HashMap()): Scope {
            var child: Scope = MutualScope(map)
            if (parent != null) {
                child = NestedScope(parent, child)
            }
            return child
        }

        fun nest(parent: Scope, child: Scope): Scope {
            return NestedScope(parent, child)
        }

        fun readonly(scope: Scope): Scope {
            return ReadonlyScope(scope)
        }
    }
}