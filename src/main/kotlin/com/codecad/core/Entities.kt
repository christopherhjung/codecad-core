package com.codecad.core

class ModelCollection {
    val objects = mutableListOf<Model>()
}

class Model{
    val points = mutableListOf<PointD>()
    val faces = mutableListOf<Path>()
    val volumes = mutableListOf<Mesh>()
}

class Path{
    val points = mutableListOf<PointD>()
}
