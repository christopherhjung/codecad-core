package com.codecad.core

import com.codecad.core.parser.ast.Expr
import com.codecad.core.parser.ast.ParamExpr


class Tracker() {
    val entries = mutableListOf<MutableMap<ParamExpr, Double>>()
    val params = mutableListOf<ParamExpr>()
    val errors = mutableListOf<Double>()

    fun addEntry(params: Collection<ParamExpr>, error: Expr){
        val entry = mutableMapOf<ParamExpr, Double>()
        entries.add(entry)
        errors.add(error.evalDouble())
        for(param in params){
            entry[param] = param.value
        }
    }

}
