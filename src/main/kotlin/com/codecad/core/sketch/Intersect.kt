package com.codecad.core.sketch

import com.codecad.core.*
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.face.Event
import com.codecad.core.face.events


val Comp2D = Comparator.comparing<Vec2, Double> { it.x }.thenComparing(Comparator.comparing { it.y });
fun cutLines(curves: List<SketchEntity>): List<SketchEntity> {
    val events = events(curves)

    val sectionMap = HashMap<SketchEntity, MutableList<Vec2>>()
    fun addSection(entity: SketchEntity, pos : Vec2 ){
        sectionMap.computeIfAbsent(entity){ mutableListOf() }.add(pos)
    }

    val actives = HashMap<SketchEntity, Event>()
    for (event in events) {
        if (event.origin) {
            for (active in actives.values) {
                val intersectionPoints = Intersect.of(active.entity, event.entity)
                intersectionPoints.forEach {
                    addSection(event.entity, it)
                    addSection(active.entity, it)
                }
            }

            actives[event.entity] = event
        } else {
            actives.remove(event.entity)
        }
    }

    val result = mutableListOf<SketchEntity>()
    for( curve in curves ){
        val sections = sectionMap[curve]
        if( sections != null ){
            when(curve){
                is SketchLine -> {
                    sections.add(curve.p0)
                    sections.add(curve.p1)
                    sections.sortWith(Comp2D)
                    for((lhs, rhs) in sections.zipWithNext()){
                        result.add(SketchLine(lhs, rhs))
                    }
                }
                is SketchCircle -> {
                    sections.sortWith(RotaryVertexComparator(curve.center))
                    for((lhs, rhs) in sections.rollover()){
                        result.add(SketchArc(lhs, rhs, curve.center))
                    }
                }
                is SketchArc -> {
                    sections.add(curve.p0)
                    sections.add(curve.p1)
                    sections.sortWith(RotaryVertexComparator(curve.center, curve.p0 - curve.center))
                    for((lhs, rhs) in sections.zipWithNext()){
                        result.add(SketchArc(lhs, rhs, curve.center))
                    }
                }
            }
        }else{
            result.add(curve)
        }
    }

    return result
}
