package com.codecad.core.sketch

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line

fun offsetFace(sketchFace: SketchFace, offset: Double) : SketchFace{
    val faceBounds = arrayListOf<FaceBound<Vec2>>()
    for( bound in sketchFace.bounds ){
        val offsetLoop = offsetLoop(bound.loop, offset)
        faceBounds.add(FaceBound(offsetLoop, FaceBoundKind.OuterBound))
    }

    return SketchFace(faceBounds)
}

fun offsetLoop(initLoop: Loop<Vec2>, offset: Double) : Loop<Vec2>{
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

        val bound = currentEdge.bound!!
        val curve = currentEdge.curve

        val currentEndNormal = endNormal(currentEdge)
        val nextStartNormal = startNormal(nextEdge)

        val lastIter = initLoop === nextLoop
        val currentEndOffsetVec = currentEndNormal * offset

        val currentEndVertex = Vertex(bound.end.point + currentEndOffsetVec)
        val nextStartVertex = if(lastIter){
            initStartVertex
        }else{
            Vertex(nextBound.start.point + nextStartNormal * offset)
        }

        val offsetCurve = when(curve){
            is Line -> {
                Line(curve.origin + currentEndOffsetVec, curve.direction)
            }
            is Circle -> {
                val radius = curve.radius
                if(radius + offset < 0.0){
                    throw RuntimeException("xxx")
                }else{
                    Circle(curve.workplane, radius + offset)
                }
            }
            else -> throw RuntimeException()
        }

        val offsetEdge =
            Edge(offsetCurve,
                EdgeBound(
                    currentStartVertex,
                    currentEndVertex,
                    bound.sense
                )
            )

        val offsetLoop = Loop(OrientedEdge(offsetEdge, EdgeOrientation.Forward))

        if(lastLoop != null){
            lastLoop.followedBy(offsetLoop)
        }else{
            initOffsetLoop = offsetLoop
        }

        val nextOffsetLoop = if(currentEndNormal.crossZ(nextStartNormal) * offset < 0.0){
            //no arc
            val pointVertex = Vertex(bound.end.point)
            val first = Loop.wrap(Edge.line(currentEndVertex, pointVertex))
            val second = Loop.wrap(Edge.line(pointVertex, nextStartVertex))

            offsetLoop.followedBy(first)
            first.followedBy(second)
            second
        }else{
            //arc
            val workplane = Workplane(bound.end.point, Vec2.DirY, Vec2.DirX)
            val arc = Loop.wrap(Edge.arc(workplane, currentEndVertex, nextStartVertex, Sense.Same))
            offsetLoop.followedBy(arc)
            arc
        }

        if(lastIter){
            nextOffsetLoop.followedBy(initOffsetLoop!!)
            break
        }

        lastLoop = nextOffsetLoop
        currentLoop = nextLoop
        currentEdge = nextEdge
        currentStartVertex = nextStartVertex
    }

    lastLoop?.followedBy(initOffsetLoop!!)
    return initOffsetLoop!!
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