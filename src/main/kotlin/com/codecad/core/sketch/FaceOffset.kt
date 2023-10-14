package com.codecad.core.sketch

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import kotlin.math.abs

fun offsetFace(sourceFace : SketchFace, offset: Double) : SketchFace{
    if(offset == 0.0) return sourceFace
    val rawOffsetFace = offsetFaceRaw(sourceFace, offset)
    val edges = rawOffsetFace.bounds.flatMap { bound -> bound.loop.map { it.edge.edge } }
    val cutEdges = cutLines(edges)
    val loops = connectVerticesMirrored(cutEdges)
    val offsetFace = generateFaces(loops)
    val tree = nestHoles(offsetFace.bounds)
    return SketchFace((tree.children + tree.children.flatMap { it.children }).map { it.bound })
}


fun offsetFaceRaw(sketchFace: SketchFace, offset: Double) : SketchFace{
    val faceBounds = arrayListOf<FaceBound<Vec2>>()
    for( bound in sketchFace.bounds ){
        val offsetLoop = offsetLoopRaw(bound.loop, offset)
        faceBounds.add(FaceBound(offsetLoop, FaceBoundKind.OuterBound))
    }

    return SketchFace(faceBounds)
}

fun offsetLoopRaw(initLoop: Loop<Vec2>, offset: Double) : Loop<Vec2>{
    var lastLoop : Loop<Vec2>? = null
    var initOffsetLoop : Loop<Vec2>? = null

    var currentLoop = initLoop
    var currentEdge = currentLoop.edge.normalized()

    val initNormal = startNormal(currentEdge)
    val initBound = currentEdge.bound
    var currentStartVertex = Vertex(initBound.start.point + initNormal * offset)
    while( true ){
        val nextLoop = currentLoop.next
        val nextEdge = nextLoop.edge.normalized()
        val nextBound = nextEdge.bound

        val bound = currentEdge.bound
        val curve = currentEdge.curve

        val currentEndNormal = endNormal(currentEdge)
        val nextStartNormal = startNormal(nextEdge)

        val lastIter = initLoop === nextLoop
        val currentEndOffsetVec = currentEndNormal * offset

        val nextStartVertex = Vertex(nextBound.start.point + nextStartNormal * offset)

        val normalDiff = (currentEndNormal - nextStartNormal).squaredLength()

        val currentEndVertex = if(normalDiff < 1e-5){
            nextStartVertex
        }else{
            Vertex(bound.end.point + currentEndOffsetVec)
        }

        val offsetCurve = when(curve){
            is Line -> {
                Line(curve.origin + currentEndOffsetVec, curve.direction)
            }
            is Circle -> {
                val radius = curve.radius

                val newRadius = if(bound.sense == Sense.Same){
                    radius + offset
                }else{
                    radius - offset
                }

                if(abs(newRadius) < 1e-5){
                    if(lastIter){
                        break
                    }else{
                        currentLoop = nextLoop
                        currentEdge = nextEdge
                        continue
                    }
                }else if(newRadius < 0.0){
                    Line.fromTo(currentStartVertex.point, currentEndVertex.point)
                }else{
                    Circle(curve.workplane, newRadius)
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

        val nextOffsetLoop = if(currentEndVertex === nextStartVertex){
            offsetLoop
        }else if(currentEndNormal.crossZ(nextStartNormal) * offset < 0.0){
            //no arc
            val first = Loop.wrap(Edge.line(currentEndVertex, nextStartVertex))
            offsetLoop.followedBy(first)
            first
        }else{
            //arc
            val workplane = Workplane(bound.end.point, Vec2.DirY, Vec2.DirX)
            val sense = if(offset > 0.0){
                Sense.Same
            }else{
                Sense.Opposite
            }

            val arc = Loop.wrap(Edge.arc(workplane, currentEndVertex, nextStartVertex, sense))
            offsetLoop.followedBy(arc)
            arc
        }

        lastLoop = nextOffsetLoop

        if(lastIter){
            break
        }

        currentLoop = nextLoop
        currentEdge = nextEdge
        currentStartVertex = nextStartVertex
    }

    if(lastLoop == null || initOffsetLoop == null){
        throw RuntimeException()
    }

    val lastEdge = lastLoop.edge.edge
    val firstEdge = initOffsetLoop.edge.edge

    val lastEdgeBound = lastEdge.bound
    val firstEdgeBound = firstEdge.bound

    val closedLastEdge = OrientedEdge(Edge(lastEdge.curve, EdgeBound(lastEdgeBound.start, firstEdgeBound.start, lastEdgeBound.sense)))
    lastLoop.edge = closedLastEdge

    lastLoop.followedBy(initOffsetLoop)
    return initOffsetLoop
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