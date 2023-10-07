package com.codecad.core.sketch

import com.codecad.core.*
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.Edge
import com.codecad.core.brep.EdgeBound
import com.codecad.core.brep.Sense
import com.codecad.core.brep.Vertex
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.face.Event
import com.codecad.core.face.events

fun cutLines(edges: List<Edge<Vec2>>): List<Edge<Vec2>> {
    val events = events(edges)

    val sectionMap = HashMap<Edge<Vec2>, MutableList<Vertex<Vec2>>>()
    fun addSection(entity: Edge<Vec2>, pos : Vertex<Vec2> ){
        sectionMap.computeIfAbsent(entity){ mutableListOf() }.add(pos)
    }

    val actives = HashMap<Edge<Vec2>, Event>()
    for (event in events) {
        if (event.origin) {
            for (active in actives.values) {
                val intersectionPoints = Intersect.of(active.edge, event.edge)
                intersectionPoints.forEach {
                    val vertex = Vertex(it)
                    addSection(event.edge, vertex)
                    addSection(active.edge, vertex)
                }
            }

            actives[event.edge] = event
        } else {
            actives.remove(event.edge)
        }
    }

    val result = mutableListOf<Edge<Vec2>>()
    for( edge in edges ){
        val sections = sectionMap[edge]
        val curve = edge.curve
        if( sections != null ){
            when(curve){
                is Line -> {
                    val bound = edge.bound!!
                    sections.add(bound.start)
                    sections.add(bound.end)
                    sections.sortWith(DirectionVertexComparator(curve.direction))
                    for((lhs, rhs) in sections.zipWithNext()){
                        result.add(Edge(curve, EdgeBound(lhs, rhs, Sense.Same)))
                    }
                }
                is Circle -> {
                    val bound = edge.bound
                    val center = curve.workplane.origin
                    if(bound != null){
                        sections.add(bound.start)
                        sections.add(bound.end)
                        var comp : Comparator<Vertex<Vec2>> = RotaryVertexComparator(center, bound.start.point - center)
                        if(bound.sense == Sense.Opposite) comp = comp.reversed()
                        sections.sortWith(comp)
                        for((lhs, rhs) in sections.zipWithNext()){
                            result.add(Edge(curve, EdgeBound(lhs, rhs, bound.sense)))
                        }
                    }else{
                        sections.sortWith(RotaryVertexComparator(center))
                        for((lhs, rhs) in sections.rollover()){
                            result.add(Edge(curve, EdgeBound(lhs, rhs, Sense.Same)))
                        }
                    }

                }
            }
        }else{
            result.add(edge)
        }
    }

    return result
}
