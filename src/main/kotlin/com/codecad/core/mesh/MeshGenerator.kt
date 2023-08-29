package com.codecad.core.mesh

import com.codecad.core.ast.primitive.ParamExpr
import com.codecad.core.ast.primitive.times
import com.codecad.core.ast.vec.QuaternionExpr
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.CylindricalSurface
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.rollover
import com.codecad.core.volume.Debugger
import com.codecad.core.volume.Volume
import org.poly2tri.Poly2Tri
import org.poly2tri.geometry.polygon.PolygonPoint
import org.poly2tri.triangulation.TriangulationPoint
import kotlin.math.abs


class MeshGenerator {
    val orientedEdgePoints = HashMap<OrientedEdge, List<Vec3>>()
    val edgePoints = HashMap<Edge, List<Vec3>>()

    val map = HashMap<Vec3, Int>()
    val indices = arrayListOf<Int>()
    val vertices = arrayListOf<Float>()

    fun createVertex(point : Vec3) : Int{
        return map.computeIfAbsent(point){
            map.size
        }
    }

    fun build() : Mesh{
        return Mesh(indices.toIntArray(), vertices.toFloatArray())
    }

    fun generate(volume: Volume){
        for(shell in volume.shells){
            generate(shell)
        }
    }

    fun generate(shell: Shell){
        for( face in shell.faces ){
            generate(face)
        }
    }

    fun generate(face: Face){
        val surface = face.surface

        if(surface is PlaneSurface){
            val projector = PlaneProjector(surface.workplane.eval())

            var outline : List<Vec3>? = null
            val holes = arrayListOf<List<Vec3>>()
            for(bound in face.bounds){
               val edgeLoop = bound.edgeLoop
               if(bound.sense == FaceBoundSense.Inside){
                   outline = sweepVertices(edgeLoop)
               }else{
                   holes.add(sweepVertices(edgeLoop))
               }
            }

            addTriangles(generateTriangles(projector, outline!!, holes))
        }else if(surface is CylindricalSurface){

            val projector = CylinderProjector(surface.workplane.eval(), surface.radius)

            class CylindricalVertex(val edge: OrientedEdge, val vertexPoint : Vec3, val projPoint: Vec2) : Comparable<CylindricalVertex>{
                lateinit var prev : CylindricalVertex
                lateinit var next : CylindricalVertex
                val nextList = arrayListOf<CylindricalVertex>()
                val prevList = arrayListOf<CylindricalVertex>()

                override fun compareTo(other: CylindricalVertex): Int {
                    return when (projPoint.x) {
                        other.projPoint.x -> projPoint.y.compareTo(other.projPoint.y)
                        else -> projPoint.x.compareTo(other.projPoint.x)
                    }
                }
            }

            val events = arrayListOf<CylindricalVertex>()
            val helper = arrayListOf<CylindricalVertex>()

            for(bound in face.bounds){
                val edgeLoop = bound.edgeLoop

                val loopVertices = arrayListOf<CylindricalVertex>()
                for(current in edgeLoop){
                    val vertices = sweepOrientedEdge(current.edge)

                    for( vertex in vertices ){
                        val projPoint = projector.project(vertex)
                        val cylVertex = CylindricalVertex(current.edge, vertex, projPoint)
                        loopVertices.add(cylVertex)
                    }
                }

                for( (lhsVertex, rhsVertex) in loopVertices.rollover() ){
                    lhsVertex.next = rhsVertex
                    rhsVertex.prev = lhsVertex

                    val thetaDiff = abs(lhsVertex.projPoint.x - rhsVertex.projPoint.x)
                    val wrapping = thetaDiff > Math.PI
                    val swap = (lhsVertex.compareTo(rhsVertex) == 1) xor wrapping

                    val (lhs, rhs) = if(swap){
                        Pair(rhsVertex, lhsVertex)
                    }else{
                        Pair(lhsVertex, rhsVertex)
                    }

                    if(wrapping){
                        helper.add(rhs)
                    }

                    lhs.nextList.add(rhs)
                    rhs.prevList.add(lhs)
                }

                events.addAll(loopVertices)
            }

            events.sort()
            helper.sort()

            println()
/*
            val firstBound = face.bounds.first()
            val lastBound = face.bounds.last()
            val firstIndices = sweepVertices(firstBound.edgeLoop)
            val secondIndices = sweepVertices(lastBound.edgeLoop)

            val size = firstIndices.size
            for( idx in firstIndices.indices ){
                val nextIdx = (idx + 1) % size

                addTriangle(
                    getIndex(firstIndices[idx]),
                    getIndex(secondIndices[idx]),
                    getIndex(firstIndices[nextIdx]),
                )

                addTriangle(
                    getIndex(secondIndices[idx]),
                    getIndex(firstIndices[nextIdx]),
                    getIndex(secondIndices[nextIdx]),
                )
            }*/
        }else{
            throw RuntimeException("No projector found")
        }
    }

    fun getIndex(point : Vec3) : Int{
        return map.computeIfAbsent(point){
            vertices.add(point.x.toFloat())
            vertices.add(point.y.toFloat())
            vertices.add(point.z.toFloat())
            map.size
        }
    }

    private fun addTriangle(a: Int, b: Int, c: Int){
        indices.add(a)
        indices.add(b)
        indices.add(c)
    }

    private fun addTriangles(triangleFaces: List<TriangleFace>){
        for(face in triangleFaces){
            addTriangle(
                getIndex(face.a),
                getIndex(face.b),
                getIndex(face.c)
            )
        }
    }

    fun sweepVertices(edgeLoop: EdgeLoop) : List<Vec3>{
        val vertices = arrayListOf<Vec3>()
        for(current in edgeLoop){
            vertices.addAll(sweepOrientedEdge(current.edge))
        }

        return vertices
    }

    fun sweepOrientedEdge(edge: OrientedEdge) : List<Vec3>{
        return orientedEdgePoints.computeIfAbsent(edge){
            sweepOrientedEdgeImpl(it)
        }
    }

    fun sweepOrientedEdgeImpl(orientedEdge: OrientedEdge) : List<Vec3>{
        val edge = orientedEdge.edge
        val curve = edge.curve
        val bound = orientedEdge.bound

        val vertices = arrayListOf<Vec3>()

        fun addPoint(point: Vec3){
            vertices.add(point)
        }

        when(curve){
            is Line -> {
                if(bound != null){
                    addPoint(bound.start.point.eval())
                }
            }
            is Circle -> {
                val workplane = curve.workplane
                val axisUp = workplane.normal

                val paramExpr = ParamExpr(axisUp.world, 0.0)
                val world = workplane.world
                val startIdx : Int
                val rotatedPoint = if(bound != null){
                    val startPoint = bound.start.point.copy()
                    val endPoint = bound.end.point.copy()

                    addPoint(startPoint.eval())

                    val projStartPoint = workplane.project(startPoint)
                    val projEndPoint = workplane.project(endPoint)

                    val startTheta = projStartPoint.absoluteAngle()
                    val offsetPoint = projEndPoint.rotate(-startTheta)
                    val offsetTheta = offsetPoint.absoluteAngle()
                    val theta = startTheta + paramExpr * when(bound.sense){
                        Sense.CCW -> offsetTheta
                        Sense.CW  -> (offsetTheta - 2.0 * Math.PI)
                        else -> throw RuntimeException("Missing Sense")
                    }

                    val rotatedPoint2D = world.vec2(curve.radius, world.Zero)
                    val rotatedPoint = rotatedPoint2D.rotate(theta)
                    startIdx = 1
                    workplane.unproject(rotatedPoint)
                }else{
                    val quaternion = QuaternionExpr.fromAxis(axisUp, (2.0 * Math.PI) * paramExpr )
                    val somePoint = workplane.unproject(curve.radius, workplane.world.Zero)

                    startIdx = 0
                    quaternion.rotate(workplane.origin, somePoint)
                }

                val count = 10
                for( angle in startIdx until count ){
                    val theta = angle / count.toDouble()
                    paramExpr.value = theta
                    val point = rotatedPoint.eval()
                    addPoint(point)
                }
            }
            else -> throw RuntimeException("not implemented")
        }

        return vertices
    }
}


/*
class MeshGenerator {

    private val faces = mutableListOf<Int>()
    private val map = HashMap<Vec3, Int>()
    private fun index(point: Vec3) : Int{
        return map.computeIfAbsent(point){map.size}
    }

    fun add(volume: FacedVolume){
        for( face in volume.faces ){
            val triangles = face.generateTriangles()

            for( triangle in triangles ){
                for( pos in triangle.positions){
                    faces.add(index(pos))
                }
            }
        }
    }

    fun build() : Mesh{
        val meshPoints = FloatArray(map.size * 3)

        for( (point, index) in map.entries ){
            meshPoints[index * 3] = (point.x).toFloat()
            meshPoints[index * 3 + 1] = (point.y).toFloat()
            meshPoints[index * 3 + 2] = (point.z).toFloat()
        }

        val mesh = Mesh()
        mesh.points = meshPoints
        mesh.indices = faces.toIntArray()
        return mesh
    }
}*/




class TriangleFace(val a: Vec3, val b: Vec3, val c: Vec3)

fun generateTriangles(projector: Projector, outline: List<Vec3>, holes: List<List<Vec3>>) : List<TriangleFace>{
    if(outline.size < 3){
        return listOf()
    }

    val pointMap = HashMap<Vec2, Vec3>()
    fun createPoint(point: Vec3) : PolygonPoint {
        val projectPoint = projector.project(point)
        pointMap[projectPoint] = point
        return PolygonPoint(projectPoint.x, projectPoint.y, 0.0)
    }

    fun pointsToPolygon(points: List<Vec3>) : org.poly2tri.geometry.polygon.Polygon{
        val list = mutableListOf<PolygonPoint>()
        for( point in points ){
            list.add(createPoint(point))
        }
        return org.poly2tri.geometry.polygon.Polygon(list)
    }

    val parent = pointsToPolygon(outline)

    for( child in holes ){
        if(outline.size < 3){
            continue
        }

        parent.addHole(pointsToPolygon(child))
    }

    try{
        Poly2Tri.triangulate(parent)
    }catch (e: Exception){
        e.printStackTrace()
        throw e
    }

    val triangles = mutableListOf<TriangleFace>()
    fun createPoint(trianglePoint: TriangulationPoint) : Vec3 {
        val point = Vec2(trianglePoint.x, trianglePoint.y)
        val original = pointMap[point]
        return original!!
    }

    for( triangle in parent.triangles ){
        val points = triangle.points
        triangles.add(TriangleFace(
            createPoint(points[0]),
            createPoint(points[1]),
            createPoint(points[2])))
    }

    return triangles
}

