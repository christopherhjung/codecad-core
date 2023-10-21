package com.codecad.core.sketch

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import kotlin.math.abs

fun offsetFace(sourceFace : SketchFace, offset: Double) : FaceTree{
    if(offset == 0.0) return nestContours(sourceFace.bounds)
    val rawOffsetFace = offsetFaceRaw(sourceFace, offset)
    val edges = rawOffsetFace.bounds.flatMap { bound -> bound.loop.map { it.edge.edge } }
    val cutEdges = cutLines(edges)
    val loops = connectVerticesMirrored(cutEdges)
    val offsetFace = generateFaces(loops)
    val tree = nestContours(offsetFace.bounds)
    return tree//SketchFace((tree.children + tree.children.flatMap { it.children }).map { it.bound })
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
    var currentLoop = initLoop
    var currentEdge = currentLoop.edge.normalized()

    val initNormal = startNormal(currentEdge)
    val initBound = currentEdge.bound
    var currentStartVertex = Vertex(initBound.start.point + initNormal * offset)
    val edges = arrayListOf<Edge<Vec2>>()

    fun addEdge(edge: Edge<Vec2>){
        edges.add(edge)
    }

    while( true ){
        val nextLoop = currentLoop.next
        val nextEdge = nextLoop.edge.normalized()
        val nextBound = nextEdge.bound

        val bound = currentEdge.bound
        val curve = currentEdge.curve

        val currentEndNormal = endNormal(currentEdge)
        val nextStartNormal = startNormal(nextEdge)

        val currentEndOffsetVec = currentEndNormal * offset
        val nextStartVertex = Vertex(nextBound.start.point + nextStartNormal * offset)
        val normalDiff = (currentEndNormal - nextStartNormal).squaredLength()

        val currentEndVertex = if(normalDiff < 1e-5){
            nextStartVertex
        }else{
            Vertex(bound.end.point + currentEndOffsetVec)
        }

        val offsetCurve = when(curve){
            is Line -> Line(curve.origin + currentEndOffsetVec, curve.direction)
            is Circle -> {
                val newRadius = curve.radius + if(bound.sense == Sense.Same){
                    offset
                }else{
                    -offset
                }

                if(newRadius < 1e-5){
                    null
                }else{
                    Circle(curve.workplane, newRadius)
                }
            }
            else -> throw RuntimeException()
        }

        if(offsetCurve != null){
            addEdge(
                Edge(offsetCurve,
                    EdgeBound(
                        currentStartVertex,
                        currentEndVertex,
                        bound.sense
                    )
                )
            )
        }else{
            addEdge(Edge.line(currentStartVertex, bound.end))
            addEdge(Edge(curve.invert(), bound.invert()))
            addEdge(Edge.line(bound.start, currentEndVertex))
        }

        if(currentEndVertex !== nextStartVertex){
            if(currentEndNormal.crossZ(nextStartNormal) * offset < 0.0){
                //no arc
                addEdge(Edge.line(currentEndVertex, nextBound.start))
                addEdge(Edge.line(nextBound.start, nextStartVertex))
            }else{
                //arc
                val workplane = Workplane(bound.end.point, Vec2.DirY, Vec2.DirX)
                val sense = if(offset > 0.0){
                    Sense.Same
                }else{
                    Sense.Opposite
                }

                addEdge(Edge.arc(workplane, currentEndVertex, nextStartVertex, sense))
            }
        }

        if(initLoop === nextLoop){
            break
        }

        currentLoop = nextLoop
        currentEdge = nextEdge
        currentStartVertex = nextStartVertex
    }

    return finishOffsetLoop(edges)
}

fun finishOffsetLoop(edges: List<Edge<Vec2>>) : Loop<Vec2>{
    val iterator = edges.iterator()
    if(!iterator.hasNext()) throw RuntimeException("One is required")

    val firstEdge = iterator.next()
    val firstLoop = Loop(OrientedEdge(firstEdge))
    var prevEdgeLoop = firstLoop
    val firstEdgeBound = firstEdge.bound
    var prevEdgeBound = firstEdgeBound
    var currentEdgeLoop = firstLoop

    while(iterator.hasNext()){
        val currentEdge = iterator.next()
        val currentEdgeBound = currentEdge.bound

        val nextEdgeBoundFix = EdgeBound(prevEdgeBound.end, currentEdgeBound.end, currentEdgeBound.sense)

        val closedLastEdge = OrientedEdge(Edge(currentEdge.curve, nextEdgeBoundFix))
        currentEdgeLoop = Loop(closedLastEdge)

        prevEdgeBound = currentEdgeBound
        prevEdgeLoop.followedBy(currentEdgeLoop)
        prevEdgeLoop = currentEdgeLoop
    }

    val lastEdge = currentEdgeLoop.edge.edge
    currentEdgeLoop.edge = OrientedEdge(Edge(lastEdge.curve, EdgeBound(prevEdgeBound.start, firstEdgeBound.start, prevEdgeBound.sense)))
    currentEdgeLoop.followedBy(firstLoop)
    return firstLoop
}
