package com.codecad.core.scope

import com.codecad.core.exception.InterpreterException
import com.codecad.core.parser.ObjectFunction
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import java.util.stream.Collectors

class StaticScope(private val map: Map<String?, Slot?>) : Scope {
    override fun getValue(key: String?): Slot? {
        return map[key]
    }

    override fun values(): Collection<Slot> {
        return map.values.filterNotNull()
    }

    class Builder {
        var map: MutableMap<String?, Slot?> = HashMap()
        fun add(key: String?, value: Any?): Builder {
            assert(!map.containsKey(key))
            map[key] = Slot(value)
            return this
        }

        fun addMethod(key: String?, function: ObjectFunction?): Builder {
            add(key, function)
            return this
        }

        fun addMethod(key: String?, obj: Any, methodName: String): Builder {
            val objClass: Class<*> = obj.javaClass
            val methods = Arrays.stream(objClass.declaredMethods)
                .filter { it: Method -> it.name == methodName }
                .peek { method: Method -> method.isAccessible = true }
                .toList().toTypedArray()
            return addMethodsByName(obj, key, methods)
        }

        fun addMethods(obj: Any): Builder {
            val nonStatic = Arrays.stream(obj.javaClass.declaredMethods)
                .filter { it: Method -> it.modifiers and Modifier.STATIC == 0 }
                .toList().toTypedArray()
            return addMethodsByName(obj, obj.javaClass.declaredMethods)
        }

        fun addStaticMethods(clazz: Class<*>): Builder {
            return addStaticMethods(clazz.simpleName, clazz)
        }

        fun addStaticMethods(className: String?, clazz: Class<*>): Builder {
            val staticMethods = Arrays.stream(clazz.declaredMethods)
                .filter { it: Method -> it.modifiers and Modifier.STATIC != 0 }
                .toList().toTypedArray()
            return addMethodsByClass(null, className, staticMethods)
        }

        fun addMethodsByClass(obj: Any?, className: String?, methods: Array<Method>): Builder {
            val resultMap = collectMethods(obj, methods)
            className?.let { add(it, resultMap) }
                ?: resultMap.forEach { (key: String?, value: Any?) -> add(key, value) }
            return this
        }

        fun addMethodsByName(obj: Any?, methodName: String?, methods: Array<Method>): Builder {
            val func = toFunc(obj, methods)
            add(methodName, func)
            return this
        }

        fun addMethodsByName(obj: Any?, methods: Array<Method>): Builder {
            val resultMap = collectMethods(obj, methods)
            resultMap.forEach { (key: String?, value: Any?) -> add(key, value) }
            return this
        }

        fun build(): StaticScope {
            return StaticScope(HashMap(map))
        }

        companion object {
            private fun toFunc(obj: Any?, method: Method): ObjectFunction {
                return object : ObjectFunction {
                    override fun call(args: Array<Any?>): Any {
                        try {
                            return method.invoke(obj, *args)
                        } catch (e: IllegalAccessException) {
                            throw InterpreterException("Method reflect invocation failed!", e)
                        } catch (e: InvocationTargetException) {
                            throw InterpreterException("Method reflect invocation failed!", e)
                        }
                    }
                }
            }

            private fun isAssignableTo(method: Method, arg: Array<Any?>?): Boolean {
                val types = method.parameterTypes
                if (types.size != arg!!.size) {
                    return false
                }
                for (idx in arg.indices) {
                    if (!types[idx].isAssignableFrom(arg[idx]!!.javaClass)) {
                        return false
                    }
                }
                return true
            }

            private fun toFunc(obj: Any?, methods: Array<Method>): ObjectFunction {
                return if (methods.size == 1) {
                    toFunc(obj, methods[0])
                } else object : ObjectFunction {
                    override fun call(args: Array<Any?>): Any {
                        try {
                            for (method in methods) {
                                if (isAssignableTo(method, args)) {
                                    return method.invoke(obj, *args)
                                }
                            }
                            throw InterpreterException("No matching function was found")
                        } catch (e: IllegalAccessException) {
                            throw InterpreterException("Method reflect invocation failed!", e)
                        } catch (e: InvocationTargetException) {
                            throw InterpreterException("Method reflect invocation failed!", e)
                        }
                    }
                }
            }

            private fun collectMethods(obj: Any?, methods: Array<Method>): Map<String, Any> {
                val map = Arrays.stream(methods)
                    .collect(
                        Collectors.groupingBy { it.name }
                    )
                val resultMap = HashMap<String, Any>()
                for ((name, overloadMethods) in map) {
                    val func = toFunc(obj, overloadMethods.toTypedArray())
                    resultMap[name] = func
                }
                return resultMap
            }
        }
    }

    companion object {
        fun builder(): Builder {
            return Builder()
        }
    }
}