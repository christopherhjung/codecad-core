package com.codecad.core

class Project(val sketches: MutableList<Sketch> = mutableListOf(), val tracker: Tracker = Tracker()){
    val volumes = mutableListOf<Volume>()
}
