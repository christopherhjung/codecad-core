package com.codecad.core.face

import com.codecad.common.Plane
import com.codecad.core.face.entity.DirectedPlane
import com.codecad.core.face.entity.Edge
import kotlin.math.atan2

class RotaryComparator(plane: Plane) : (Edge) -> Double{
    private val directedPlane = DirectedPlane.from(plane)

    override fun invoke(p1: Edge): Double {
        val aDirection = (p1.target.point - p1.source.point)//.normalized()
        return atan2(aDirection.dot(directedPlane.first), aDirection.dot(directedPlane.second))
    }
}
