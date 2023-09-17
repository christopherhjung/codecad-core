package com.codecad.core.mesh

import com.codecad.core.brep.*
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.sketch.Unifier
import com.codecad.core.volume.Volume

object Solidify {
    fun solidify(volume: Volume) : Volume{
        val volume = mergePlaneFaces(volume)

        return volume
    }

    fun mergePlaneFaces(volume: Volume) : Volume{
        val shells = arrayListOf<Shell>()
        for( shell in volume.shells ){
            val faceUnifier = Unifier<Face>()

            for( face in shell.faces ){
                val faceSurface = face.surface
                if(faceSurface !is PlaneSurface) continue

                val faceNode = faceUnifier.get(face)

                for( bound in face.bounds ){
                    for( loop in bound.loop ){
                        val twinFace = loop.twin!!.face
                        val twinFaceSurface = twinFace.surface
                        if(twinFaceSurface !is PlaneSurface) continue

                        val faceNormal = faceSurface.workplane.normal
                        val twinFaceNormal = twinFaceSurface.workplane.normal

                        val normalDiff = (faceNormal - twinFaceNormal).length()

                        if(normalDiff < 1e-10){
                            val twinFaceNode = faceUnifier.get(twinFace)
                            faceUnifier.unify(faceNode, twinFaceNode)
                        }
                    }
                }
            }

            val planeFaces = hashMapOf<Face, HashSet<Face>>()

            for( face in shell.faces ){
                val faceNode = faceUnifier.get(face)
                val parentNode = faceUnifier.find(faceNode)
                planeFaces.computeIfAbsent(parentNode.value){ hashSetOf() }.add(face)
            }

            val faces = arrayListOf<Face>()
            for( (parentFace, group) in planeFaces.entries ){
                faces.add(mergeFace(parentFace, group))
            }

            shells.add(Shell(faces))
        }

        return Volume(shells)
    }

    fun mergeFace(parentFace : Face, faces: Set<Face>) : Face{
        val loops = hashSetOf<Loop>()

        for( face in faces ){
            for(bound in face.bounds){
                for( loop in bound.loop ){
                    val twinFace = loop.twin!!.face

                    if(!faces.contains(twinFace)){
                        loops.add(loop)
                    }
                }
            }
        }

        val prevs = loops.associateBy { it.edge.end!! }
        val nexts = loops.associateBy { it.edge.start!! }

        val bounds = arrayListOf<FaceBound>()
        val mergeFace = Face(parentFace.surface, bounds)

        for( loop in loops ){
            val edge = loop.edge
            val prev = prevs[edge.start]
            val next = nexts[edge.end]

            loop.prev = prev!!
            loop.next = next!!
            loop.face = mergeFace
        }

        val faceBoundLoops = arrayListOf<Loop>()
        while(loops.isNotEmpty()){
            val loop = loops.first()
            faceBoundLoops.add(loop)
            loops.removeAll(loop)
        }

        val max = faceBoundLoops.maxBy { it.computeArea() }
        for( faceBoundLoop in faceBoundLoops ){
            val faceBoundKind = if(faceBoundLoop === max){
                FaceBoundKind.OuterBound
            }
            else
            {
                FaceBoundKind.InnerBound
            }
            val faceBound = FaceBound(faceBoundLoop, faceBoundKind)
            bounds.add(faceBound)
        }

        return mergeFace
    }

}