package com.codecad.core
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt


class Util{
    companion object{
        fun load(name: String) : String{
            return String(Util::class.java.classLoader.getResourceAsStream(name)?.readAllBytes()!!)
        }
    }
}

fun main(args: Array<String>) {

    val value = 1e-200
    println((hypot(value, value) - sqrt(value.pow(2) + value.pow(2))) / value)
}


