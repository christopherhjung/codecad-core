package com.codecad.core.mesh

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.sketch.Unifier
import com.codecad.core.volume.Volume
import java.lang.Integer.min
import java.util.*
import kotlin.collections.HashSet

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

    fun edgeDirection(loop: Loop) : Vec3{
        val orientedEdge = loop.edge
        val edge = orientedEdge.edge
        val curve = edge.curve

        return if(curve is Line){
            val dir = curve.direction

            if(orientedEdge.orientation == EdgeOrientation.Forward){
                dir
            }else{
                dir.negate()
            }
        }else{
            (orientedEdge.end!!.point - orientedEdge.start!!.point).normalized()
        }
    }

    class MergeNode(val size : Int, val start : Loop, val end : Loop, val normal: Vec3?)
    private fun mergeCircularFaces(shell : Shell) : Shell{
        for( face in shell.faces ){
            val surface = face.surface
            if(surface !is PlaneSurface) continue

            for( bound in face.bounds ){
                trace(bound.loop)

                println("xx")
            }
        }

        return shell
    }

    private fun traceOld(loop: Loop){
        val visited = hashSetOf<Loop>()
        val worklist = LinkedList<MergeNode>()
        worklist.add(MergeNode(1, loop, loop, null))

        while(worklist.isNotEmpty()){
            val node = worklist.removeFirst()

            val prevSize = node.size
            val nextStart = if(prevSize == 5){
                node.start.next
            }else{
                node.start
            }
            val nextSize = min(prevSize + 1, 5)
            val lastNormal = node.normal
            val lastLoop = node.end

            val lastDir = edgeDirection(lastLoop)
            for(nextLoop in lastLoop.star()){
                if(!visited.add(nextLoop)) continue

                val nextDir = edgeDirection(nextLoop)
                val loopNormal = lastDir.cross(nextDir).normalized()

                val nextNormal = lastNormal ?: loopNormal
                worklist.add(MergeNode(nextSize, nextStart, nextLoop, nextNormal))
            }
        }
    }
}
