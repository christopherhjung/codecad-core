package com.codecad.core.mesh

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Curve
import com.codecad.core.brep.curve.Ellipse
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.regression.Regression
import com.codecad.core.utils.Unifier
import com.codecad.core.volume.Volume
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.math.abs
import kotlin.math.sin

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
            val parentNode = faceNode.find()
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

    val deletedFaces = hashSetOf<Face>()
    private fun mergeFace(parentFace : Face, faces: Set<Face>) : Face{
        val loops = hashSetOf<Loop<Vec3>>()
        deletedFaces.addAll(faces)

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

        val faceBoundLoops = arrayListOf<Loop<Vec3>>()
        while(loops.isNotEmpty()){
            var currentLoop = loops.first()
            val initLoop = currentLoop
            var nextLoop = currentLoop.next
            faceBoundLoops.add(currentLoop)
            while(true){
                if(loops.remove(nextLoop)){
                    currentLoop.followedBy(nextLoop)
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

        val bounds = arrayListOf<FaceBound<Vec3>>()
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

        mergeFace.finish()
        return mergeFace
    }

    fun direction(loop: Loop<Vec3>) : Vec3{
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

    private fun mergeCircularFaces(shell : Shell) : Shell{
        for( face in shell.faces ){
            val surface = face.surface
            if(surface !is PlaneSurface) continue

            traceFace(face)

            /*
            for( bound in face.bounds ){
                for( loop in bound.loop ){
                    traceEdge(loop)
                }
            }*/
        }

        for( (loop, curve) in loop2Curve ){
            val twinCurves = loop2Curve[loop.twin!!]
            println("test")
        }

        return shell
    }

    class FaceFitNode(val face: Face, val faces : List<Face>, val normal: Vec3, val axis: Vec3 = Vec3.Zero){
        val size get() = faces.size

        fun expand(face : Face, nextNormal: Vec3, nextAxis : Vec3) : FaceFitNode{
            val faces = ArrayList<Face>(faces.size + 1)
            faces.addAll(this.faces)
            faces.add(face)
            return FaceFitNode(face, faces, nextNormal,  axis + nextAxis)
        }
    }

    private fun traceFace(face: Face){
        val surface = face.surface as PlaneSurface
        val worklist = LinkedList<FaceFitNode>()
        worklist.add(FaceFitNode(face, arrayListOf(face), surface.workplane.normal))

        while(worklist.isNotEmpty()){
            val node = worklist.removeFirst()
            val lastNormal = node.normal
            val lastFace = node.face
            val lastAxis = node.axis

            for( bound in lastFace.bounds ){
                if(!bound.loop.validate()){
                    println("error")
                }
                for( loop in bound.loop ){
                    val currentFace = loop.twin!!.face
                    if(node.faces.contains(currentFace)) continue
                    val currentSurface = currentFace.surface as PlaneSurface
                    val currentNormal = currentSurface.workplane.normal

                    val angle = Vec3.angle(lastNormal, currentNormal) / Math.PI * 180.0
                    val axis = lastNormal.cross(currentNormal)
                    if(axis.length() > MaxTurn || lastAxis.dot(axis) < 0.0 ){
                        continue
                    }

                    if(lastAxis.length() > 1e-5 && axis.length() >= 1e-5){
                        val axisDeviation = Vec3.angle(axis, lastAxis) / Math.PI * 180.0
                        if(axisDeviation > 10.0) continue
                    }


                    val nextNode = node.expand(currentFace, currentNormal, axis)

                    if(nextNode.size == 5){
                        fitFace(nextNode)
                    }else{
                        worklist.add(nextNode)
                    }
                }
            }
        }
    }

    private fun fitFace(node: FaceFitNode){
        println(node.axis.normalized())
    }


    val MaxTurn = sin(Math.PI / 6.0)
    private fun getVertices(loops: List<Loop<Vec3>>) : List<Vertex<Vec3>>{
        return loops.map { it.edge.start!! } + loops.last().edge.end!!
    }

    class EdgeFitNode(val loops : List<Loop<Vec3>>, val end : Loop<Vec3>, val dir: Vec3, val normal: Vec3?){
        val size get() = loops.size

        fun expand(nextLoop : Loop<Vec3>, nextDir : Vec3, nextNormal : Vec3) : EdgeFitNode{
            val loops = ArrayList<Loop<Vec3>>(loops.size + 1)
            loops.addAll(this.loops)
            loops.add(nextLoop)

            return EdgeFitNode(loops, nextLoop, nextDir, normal ?: nextNormal)
        }
    }

    private fun traceEdge(loop: Loop<Vec3>){
        val worklist = LinkedList<EdgeFitNode>()
        worklist.add(EdgeFitNode(listOf(loop), loop, direction(loop),null))

        while(worklist.isNotEmpty()){
            val node = worklist.removeFirst()
            val lastNormal = node.normal
            val lastDir = node.dir
            for(nextLoop in node.end.star()){
                val nextDir = direction(nextLoop)
                val turn = lastDir.cross(nextDir)
                if(turn.length() > MaxTurn) continue

                val loopNormal = turn.normalized()
                if(lastNormal != null){
                    val normalDiff = (loopNormal - lastNormal).length()
                    if(normalDiff > 1e-3) continue
                }

                val nextNode = node.expand(nextLoop, nextDir, loopNormal)

                if(nextNode.size == 5){
                    fitEdge(nextNode)
                }else{
                    worklist.add(nextNode)
                }
            }
        }
    }

    val loop2Curve = hashMapOf<Loop<Vec3>, MutableList<Curve<Vec3>>>()
    fun fitEdge(node : EdgeFitNode){
        val vertices = getVertices(node.loops)
        val points3d = vertices.map { it.point }

        val origin = points3d.first()
        val normal = node.normal!!
        val x = direction(node.loops.first())
        val workplane = Workplane(origin, normal, x)

        val points = vertices.map { workplane.project2d(it.point) }
        val ellipse = Regression.matchEllipse(points, 1e-3) ?: return

        val center = workplane.unproject(ellipse.center)
        val dir3d = workplane.unprojectDir(ellipse.direction)
        val major = ellipse.major
        val minor = ellipse.minor

        val curveWorkplane = Workplane(center, normal, dir3d)

        val curve = if(abs(major-minor) < 1e-3){
            Circle(curveWorkplane, (major+minor)/2)
        }else{
            Ellipse(curveWorkplane, major, minor)
        }

        //println("center: $center,dir: $dir3d,major: $major,minor: $minor")

        for( loop in node.loops ){
            loop2Curve.computeIfAbsent(loop) {
                arrayListOf()
            }.add(curve)
        }
    }
}
