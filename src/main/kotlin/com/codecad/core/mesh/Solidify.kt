package com.codecad.core.mesh

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.regression.Regression
import com.codecad.core.sketch.Unifier
import com.codecad.core.volume.Volume
import kotlin.math.abs

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

        var mergedOrigin = Vec3.Zero
        for( face in faces ){
            for(bound in face.bounds){
                for( loop in bound.loop ){
                    val twinFace = loop.twin!!.face

                    if(!faces.contains(twinFace)){
                        loops.add(loop)
                    }
                }
            }

            val surface = face.surface
            if(surface is PlaneSurface){
                mergedOrigin += surface.workplane.origin
            }
        }

        mergedOrigin /= faces.size

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

        val parentSurface = parentFace.surface as PlaneSurface
        val workplane = parentSurface.workplane.withOrigin(mergedOrigin)
        val mergedSurface = PlaneSurface(workplane)

        val bounds = arrayListOf<FaceBound>()
        val mergeFace = Face(mergedSurface, bounds)

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
            val surface = face.surface
            if(surface !is PlaneSurface) continue

            for( bound in face.bounds ){
                for( loop in bound.loop ){
                    val nearFace = loop.twin!!.face
                    val edge = loop.edge

                    val points = loop.nextPoints(5)
                        .map { surface.workplane.project2d(it) }

                    if(points.size == 5){
                        println("ssss")

                        val (center, direction, a, b) = Regression.fitEllipse(points)!!
                        val (centerCircle, radius) = Regression.fitCircle(points)!!

                        if(abs(a - b) < 1e-3){
                            println("circle!")
                        }

                        var center3D = surface.workplane.unproject(center)
                        var direction3d = surface.workplane.unprojectDir(direction)

                        println("center: $center, direction: $direction, a: $a, b: $b")
                        println("--")
                    }
                }
            }
        }

        return shell
    }
}
