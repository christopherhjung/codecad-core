package com.codecad.core.sketch

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.Edge
import com.codecad.core.brep.EdgeBound
import com.codecad.core.brep.Sense
import com.codecad.core.brep.Vertex
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.face.Event
import com.codecad.core.face.events
import com.codecad.core.utils.Unifier

fun cutLines(edges: List<Edge<Vec2>>): List<Edge<Vec2>> {
    val unifier = Unifier<Vertex<Vec2>>()

    val events = events(edges)

    val sectionMap = HashMap<Edge<Vec2>, MutableList<Vertex<Vec2>>>()
    fun addSection(entity: Edge<Vec2>, pos : Vertex<Vec2> ){
        sectionMap.computeIfAbsent(entity){ mutableListOf() }.add(pos)
    }

    val actives = HashMap<Edge<Vec2>, Event>()
    for (event in events) {
        if (event.origin) {
            val eventEdge = event.edge
            for (active in actives.values) {
                val activeEdge = active.edge
                val intersectionPoints = Intersect.of(activeEdge, eventEdge)
                intersectionPoints.forEach {
                    val activeBound = activeEdge.bound
                    val eventBound = eventEdge.bound
                    val vertex = Vertex(it)

                    val vertexNode = unifier.get(vertex)

                    fun unify(boundVertex: Vertex<Vec2>){
                        if(boundVertex.point.near(it, EPSILON)){
                            unifier.unify(vertexNode, boundVertex)
                        }
                    }

                    unify(activeBound.start)
                    unify(activeBound.end)
                    unify(eventBound.start)
                    unify(eventBound.end)

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
        var sections = sectionMap[edge]
        if( sections != null ){
            val bound = edge.bound
            val comp = createComparator(edge)
            val start = unifier.parent(bound.start)
            val end = unifier.parent(bound.end)

            sections = sections
                .map { unifier.parent(it) }
                .filter { it !== start && it !== end }
                .toMutableList()

            sections.sortWith(comp)
            if(edge.bound.isUnbounded()){
                sections.add(sections.first())
            }else{
                sections.add(0, start)
                sections.add(end)
            }

            sections
                .distinctWithNext()
                .zipWithNext()
                .forEach{ (lhs, rhs) ->
                result.add(Edge(edge.curve, EdgeBound(lhs, rhs, bound.sense)))
            }
        }else{
            result.add(edge)
        }
    }

    return result
}

inline fun <T> Iterable<T>.distinctWithNext(): List<T> {
    val list = ArrayList<T>()
    var last : T? = null
    for (e in this) {
        if(e !== last){
            list.add(e)
        }else{
            println("s")
        }

        last = e
    }
    return list
}

private fun createComparator(edge : Edge<Vec2>) : Comparator<Vertex<Vec2>>{
    return when(val curve = edge.curve){
        is Line -> DirectionVertexComparator(curve.direction)
        is Circle -> {
            val bound = edge.bound
            val center = curve.workplane.origin
            val rotComp = RotaryVertexComparator(center, bound.start.point - center)
            if(bound.sense == Sense.Same){
                rotComp
            }else{
                rotComp.reversed()
            }
        }
        else -> throw NotImplementedError()
    }
}