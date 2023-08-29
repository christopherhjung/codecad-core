package com.codecad.core.brep

import com.codecad.core.volume.Debugger

class Shell(var faces : List<Face>) {
    fun debug(debugger: Debugger){
        for(face in faces){
            debugger.addFace(face)
        }
    }
}