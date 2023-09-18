package com.codecad.core.mesh

import com.codecad.core.brep.*
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.export.StlExport
import com.codecad.core.part.Context
import com.codecad.core.sketch.Unifier
import com.codecad.core.volume.Volume
import java.io.FileOutputStream

object Solidify {
    fun solidify(volume: Volume) : Volume{
        return Volume(volume.shells
            .map { mergePlaneFaces(it) }
            .map { mergeCircularFaces(it) })
    }
/*
    fun mergePlaneFaces(volume: Volume) : Volume{
        return Volume(volume.shells.map { mergePlaneFaces(it) })
    }*/

    private fun mergePlaneFaces(shell: Shell) : Shell{
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

                    if(normalDiff < 1e-3){
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
            faces.add(if(group.size == 1){
                parentFace
            }else{
                mergeFace(parentFace, group)
            })
        }

        return Shell(faces)
    }

    private fun mergeFace(parentFace : Face, faces: Set<Face>) : Face{
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

        val faceBoundLoops = arrayListOf<Loop>()
        while(loops.isNotEmpty()){
            var currentLoop = loops.first()
            val initLoop = currentLoop
            var nextLoop = currentLoop.next
            faceBoundLoops.add(currentLoop)
            while(true){
                if(loops.remove(nextLoop)){
                    currentLoop.next = nextLoop
                    nextLoop.prev = currentLoop
                    if(nextLoop === initLoop){
                        break
                    }

                    currentLoop = nextLoop
                    nextLoop = nextLoop.next
                }else{
                    val twinLoop = nextLoop.twin!!
                    nextLoop = twinLoop.next
                }
            }
        }

        val bounds = arrayListOf<FaceBound>()
        val mergeFace = Face(parentFace.surface, bounds)

        val max = faceBoundLoops.maxBy { it.computeArea() }
        for( faceBoundLoop in faceBoundLoops ){
            val faceBoundKind = if(faceBoundLoop === max){
                FaceBoundKind.OuterBound
            }else{
                FaceBoundKind.InnerBound
            }
            bounds.add(FaceBound(faceBoundLoop, faceBoundKind))
        }

        return mergeFace
    }

    private fun mergeCircularFaces(shell : Shell) : Shell{
        for( face in shell.faces ){
            for( bound in face.bounds ){
                for( loop in bound.loop ){
                    val nearFace = loop.twin!!.face
                    val edge = loop.edge



                }
            }
        }

        return shell
    }
}
