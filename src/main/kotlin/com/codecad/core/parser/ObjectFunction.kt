package com.codecad.core.parser

import com.codecad.core.scope.Scope

fun interface ObjectFunction {
    fun call(scope: Scope, args: Array<Any?>): Any
}