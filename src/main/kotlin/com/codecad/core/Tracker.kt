package com.codecad.core

class Tracker() {
    val entries = mutableListOf<MutableMap<Parameter, Double>>()
    val params = mutableListOf<Parameter>()
    val errors = mutableListOf<Double>()

    fun addEntry(params: Collection<Parameter>, error: Value){
        val entry = mutableMapOf<Parameter, Double>()
        entries.add(entry)
        errors.add(error.value)
        for(param in params){
            entry[param] = param.value
        }
    }

}
