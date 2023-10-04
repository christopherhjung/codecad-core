package com.codecad.core.sketch

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Line

fun offsetFace(sketchFace: SketchFace, offset: Double) : SketchFace{
    val faceBounds = arrayListOf<FaceBound<Vec2>>()
    for( bound in sketchFace.bounds ){
        val initLoop = bound.loop

        var lastLoop : Loop<Vec2>? = null
        var initOffsetLoop : Loop<Vec2>? = null

        var currentLoop = initLoop
        var currentEdge = currentLoop.edge.normalized()
        val initNormal = startNormal(currentEdge)
        val initBound = currentEdge.bound!!
        var currentStartVertex = Vertex(initBound.start.point + initNormal * offset)
        val initStartVertex = currentStartVertex
        while( true ){
            val nextLoop = currentLoop.next
            val nextEdge = nextLoop.edge.normalized()
            val nextBound = nextEdge.bound!!

            val currentBound = currentEdge.bound!!
            val currentCurve = currentEdge.curve

            val currentEndNormal = endNormal(currentEdge)
            val nextStartNormal = startNormal(nextEdge)

            val lastIter = initLoop === nextLoop

            val currentEndVertex = Vertex(currentBound.end.point + currentEndNormal * offset)
            val nextStartVertex = if(lastIter){
                initStartVertex
            }else{
                Vertex(nextBound.start.point + nextStartNormal * offset)
            }

            val offsetEdge = when(currentCurve){
                is Line -> {
                    val origin = currentCurve.origin
                    val currentDirection = currentCurve.direction

                    val offsetOrigin = origin + currentEndNormal * offset
                    val offsetLine = Line(offsetOrigin, currentDirection)

                    Edge(offsetLine,
                        EdgeBound(
                            currentStartVertex,
                            currentEndVertex,
                            currentBound.sense
                        )
                    )
                }
                else -> throw RuntimeException()
            }

            val offsetLoop = Loop(OrientedEdge(offsetEdge, EdgeOrientation.Forward))

            if(lastLoop != null){
                lastLoop.followedBy(offsetLoop)
            }else{
                initOffsetLoop = offsetLoop
            }

            lastLoop = if(currentEndNormal.crossZ(nextStartNormal) * offset < 0.0){
                //no arc
                val pointVertex = Vertex(currentBound.end.point)
                val first = Loop.wrap(Edge.line(currentEndVertex, pointVertex))
                val second = Loop.wrap(Edge.line(pointVertex, nextStartVertex))

                offsetLoop.followedBy(first)
                first.followedBy(second)
                second
            }else{
                //arc
                val workplane = Workplane(currentBound.end.point, Vec2.DirY, Vec2.DirX)
                val arc = Loop.wrap(Edge.arc(workplane, currentEndVertex, nextStartVertex, Sense.Same))
                offsetLoop.followedBy(arc)
                arc
            }

            if(lastIter){
                lastLoop.followedBy(initOffsetLoop!!)
                break
            }

            currentLoop = nextLoop
            currentEdge = nextEdge
            currentStartVertex = nextStartVertex
        }

        lastLoop?.followedBy(initOffsetLoop!!)
        faceBounds.add(FaceBound(initOffsetLoop!!, FaceBoundKind.OuterBound))
    }

    return SketchFace(faceBounds)
}





/*
    is Circle -> {
        val center = curve.workplane.origin
        val radius = curve.radius

        val startOffsetVertex = lastVertex ?: Vertex(start + (start - center).scaleTo(offset))
        val endOffsetVertex = Vertex(end + (end - center).scaleTo(offset))

        if(radius + offset < 0.0){
            val centerVertex = Vertex(center)

            val firstEdge = Edge.line(
                startOffsetVertex,
                centerVertex,
            )

            val secondEdge = Edge.line(
                centerVertex,
                endOffsetVertex,
            )

        }else{
            val offsetCircle = Circle(curve.workplane, radius + offset)

            val offsetEdge = Edge(offsetCircle,
                EdgeBound(
                    startOffsetVertex,
                    endOffsetVertex,
                    edgeBound.sense
                )
            )
        }
    }*/