package com.codecad.core.parser.ast

import com.codecad.core.exception.InterpreterException
import com.codecad.core.scope.Scope
import com.codecad.core.sketch.World

class FieldExpr(
    world: World,
    private val objExpr: Expr,
    private val name: String?,
    private val optional: Boolean = false
) : Expr(world) {

    override fun eval(scope: Scope): Any? {
        val value = objExpr.eval(scope)
        if (value == null) {
            if (optional) return null
            throw InterpreterException("Null pointer exception")
        }
        if (value is Map<*, *>) {
            val map = value as Map<String?, Any>
            return map[name]
        }
        val valClass: Class<*> = value.javaClass
        return try {
            val field = valClass.getDeclaredField(name)
            field.isAccessible = true
            field[value]
        } catch (e: NoSuchFieldException) {
            throw InterpreterException("Expected map or valid object in field expr!", e)
        } catch (e: IllegalAccessException) {
            throw InterpreterException("Expected map or valid object in field expr!", e)
        }
    }

    override fun assign(scope: Scope, value: Any?, define: Boolean): Any? {
        val obj = objExpr.eval(scope)
        if (obj == null) {
            if (optional) return null
            throw InterpreterException("Null pointer exception")
        }
        if (obj is Map<*, *>) {
            val map = obj as MutableMap<String?, Any?>
            return map.put(name, value)
        }
        val valClass: Class<*> = obj.javaClass
        return try {
            val field = valClass.getDeclaredField(name)
            field.isAccessible = true
            field[value] = obj
            obj
        } catch (e: NoSuchFieldException) {
            throw InterpreterException("Expected map or valid object in field expr!", e)
        } catch (e: IllegalAccessException) {
            throw InterpreterException("Expected map or valid object in field expr!", e)
        }
    }
}