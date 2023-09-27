package com.codecad.core.brep.curve

import com.codecad.core.ast.vec.Vec
import com.codecad.core.brep.Workplane

abstract class Conic<T : Vec<T>>(val workplane: Workplane<T>) : Curve<T>() {

}