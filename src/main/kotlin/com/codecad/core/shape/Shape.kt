package com.codecad.core.shape

import com.codecad.core.part.Sketch


abstract class Shape{
    abstract fun build(sketch: Sketch)
}