package com.codecad.core.import.step

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.import.step.ast.StepFile
import com.codecad.core.import.step.ast.StepObject
import com.codecad.core.import.step.ast.*
import com.codecad.core.part.Context
import com.codecad.core.volume.Volume

class GeometricContext(val lengthFactor : Double, val angleUnit : Double)
val DefaultGeometricContext = GeometricContext(1.0, 1.0)

class StepImporter{
    private val vertices = hashMapOf<StepObject, Vertex<Vec3>>()
    private val edges = hashMapOf<StepObject, Edge<Vec3>>()
    private val edge2loop = hashMapOf<Edge<Vec3>, Loop<Vec3>>()
    private lateinit var geometricContext : GeometricContext

    fun importSIUnit(siUnit : StepObject) : Double{
        val lengthPrefix = when(siUnit.args[0]){
            StepString("MILLI") -> 1e-3
            StepString("KILO") -> 1e3
            else -> 1.0
        } * 1e3

        val lengthUnit = when(siUnit.args[1]){
            StepString("RADIAN") -> 1.0
            StepString("METRE") -> 1.0
            else -> 1.0
        }

        return lengthPrefix * lengthUnit
    }

    fun importGeometricContext(obj : StepDef) : GeometricContext{
        if(obj !is StepMultiObject) throw RuntimeException()
        val unitContext = obj.findObject("GLOBAL_UNIT_ASSIGNED_CONTEXT")!!
        val units = unitContext.args[0].asTuple()

        val lengthUnitObj = units.elems[0] as StepMultiObject
        val lengthUnit = lengthUnitObj.findObject("SI_UNIT")!!
        val lengthFactor = importSIUnit(lengthUnit)


        val angleUnitObj = units.elems[1] as StepMultiObject
        val angleUnit = angleUnitObj.findObject("SI_UNIT")!!
        val angleFactor = importSIUnit(angleUnit)

        return GeometricContext(lengthFactor, angleFactor)
    }

    fun setupGeometricContext(obj : StepDef){
        geometricContext = importGeometricContext(obj)
    }

    fun import(file : StepFile) : Context {
        val context = Context()

        for(geo in file.geometrics){
            setupGeometricContext(geo.geometricContext)
            for(styledItem in geo.styledItems){
                val solid = styledItem.brep
                if(solid.type == "MANIFOLD_SOLID_BREP"){
                    context.volumes.add(importSolid(solid))
                }
            }
        }

        return context
    }

    fun importSolid(solid: StepObject) : Volume {
        val shell = solid.shell
        if(shell.type == "CLOSED_SHELL"){
            return Volume(listOf(importClosedShell(shell)))
        }

        throw RuntimeException()
    }

    fun importClosedShell(shell: StepObject) : Shell {
        return Shell(shell.advancedFaces.map { importFace(it) })
    }

    fun importFace(advancedFace: StepObject) : Face {
        val faceBounds = advancedFace.faceBounds.map { importFaceBound(it) }
        val surface = advancedFace.surface(geometricContext)
        val face = Face(surface, faceBounds)

        return face
    }

    fun importFaceBound(faceBound : StepObject) : FaceBound {
        val edgeLoop = faceBound.edgeLoop

        val loop = Loop.of(edgeLoop.orientedEdges.map{
            val edgeCurve = it.edgeCurves
            val edge = createEdge(edgeCurve)

            val orientation = if(it.sense){
                EdgeOrientation.Forward
            }else{
                EdgeOrientation.Backward
            }

            OrientedEdge(edge, orientation)
        })

        for( loopSegment in loop ){
            val edge = loopSegment.edge.edge
            edge2loop.putIfAbsent(edge, loopSegment)?.let {
                assert(it.twin == null)
                assert(it.edge.edge === loopSegment.edge.edge)
                it.twin = loopSegment
                loopSegment.twin = it
            }
        }

        return FaceBound(loop, FaceBoundKind.OuterBound)
    }

    fun createVertex(obj : StepObject) : Vertex<Vec3>{
        return vertices.computeIfAbsent(obj){
            Vertex(obj.vec(geometricContext))
        }
    }

    fun createEdge(obj : StepObject) : Edge<Vec3>{
        return edges.computeIfAbsent(obj){
            val start = it.start
            val end = it.end

            val edgeBound = if(start === end){
                null
            }else{
                EdgeBound(createVertex(start), createVertex(end))
            }

            Edge(it.curve(geometricContext), edgeBound)
        }
    }
}