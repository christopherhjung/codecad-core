package com.codecad.core

import org.springframework.boot.runApplication

class Util{
    companion object{
        fun load(name: String) : String{
            return String(Util::class.java.classLoader.getResourceAsStream(name)?.readAllBytes()!!)
        }
    }
}



fun main(args: Array<String>) {
    runApplication<Application>()
}
