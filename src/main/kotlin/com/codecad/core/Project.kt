package com.codecad.core

import com.codecad.core.sketch.World

class Project(){
    val sketches: MutableList<Sketch> = mutableListOf()
    val tracker: Tracker = Tracker()
    val volumes = mutableListOf<Volume>()
    val world = World()
}
