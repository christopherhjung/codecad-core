package com.codecad.core

import com.codecad.core.sketch.Expr
import com.codecad.core.sketch.Param

class Tracker() {
    val entries = mutableListOf<MutableMap<Param, Double>>()
    val params = mutableListOf<Param>()
    val errors = mutableListOf<Double>()

    fun addEntry(params: Collection<Param>, error: Expr){
        val entry = mutableMapOf<Param, Double>()
        entries.add(entry)
        errors.add(error.evalDouble())
        for(param in params){
            entry[param] = param.value
        }
    }

}
