package com.codecad.core.scope

class MutualScope @JvmOverloads constructor(private val map: MutableMap<String?, Slot?> = HashMap()) : Scope {
    constructor(global: Scope?) : this(HashMap<String?, Slot?>()) {}

    fun toStatic(): StaticScope {
        return StaticScope(HashMap(map))
    }

    override fun values(): Collection<Slot> {
        return map.values.filterNotNull()
    }

    override fun getValue(key: String?): Slot? {
        return map[key]
    }

    override fun setObject(key: String?, value: Any?, define: Boolean): Boolean {
        var wrapper = map[key]
        if (wrapper == null) {
            return if (define) {
                wrapper = Slot(value)
                map[key] = wrapper
                true
            } else false
        }
        wrapper.value = value
        return true
    }
}