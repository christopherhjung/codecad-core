package com.codecad.core.import.step

import com.codecad.core.brep.*
import com.codecad.core.import.step.ast.StepFile
import com.codecad.core.import.step.ast.StepObject
import com.codecad.core.import.step.ast.*
import com.codecad.core.part.Context
import com.codecad.core.volume.Volume

class StepImporter{
    private val vertices = hashMapOf<StepObject, Vertex>()
    private val edges = hashMapOf<StepObject, Edge>()

    fun import(file : StepFile) : Context {
        val context = Context()

        for(styledItem in file.styledItems){
            val solid = styledItem.brep
            if(solid.type == "MANIFOLD_SOLID_BREP"){
                context.volumes.add(importSolid(solid))
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
        val surface = advancedFace.surface
        return Face(surface, faceBounds)
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

        return FaceBound(loop, FaceBoundKind.OuterBound)
    }

    fun createVertex(obj : StepObject) : Vertex{
        return vertices.computeIfAbsent(obj){
            Vertex(obj.vec)
        }
    }

    fun createEdge(obj : StepObject) : Edge{
        return edges.computeIfAbsent(obj){
            val start = it.start
            val end = it.end

            val edgeBound = if(start === end){
                null
            }else{
                EdgeBound(createVertex(start), createVertex(end))
            }

            Edge(it.curve, edgeBound)
        }
    }
}