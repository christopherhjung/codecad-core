package com.codecad.core.mesh

import com.codecad.core.ast.primitive.ParamExpr
import com.codecad.core.ast.primitive.minus
import com.codecad.core.ast.primitive.times
import com.codecad.core.ast.vec.QuaternionExpr
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.face.entity.*
import com.codecad.core.face.entity.curve.Circle
import com.codecad.core.face.entity.curve.Line
import com.codecad.core.face.entity.surface.CylindricalSurface
import com.codecad.core.face.entity.surface.PlaneSurface
import com.codecad.core.volume.Volume
import org.poly2tri.Poly2Tri
import org.poly2tri.geometry.polygon.PolygonPoint
import org.poly2tri.triangulation.TriangulationPoint


class MeshGenerator {
    val edgePoints = HashMap<Edge, List<Vec3>>()
    val map = HashMap<Vec3, Int>()
    val indices = arrayListOf<Int>()
    val vertices = arrayListOf<Float>()

    fun createVertex(point : Vec3) : Int{
        return map.computeIfAbsent(point){
            map.size
        }
    }

    fun generate(volume: Volume) : Mesh{
        for(shell in volume.shells){
            generate(shell)
        }

        return Mesh(indices.toIntArray(), vertices.toFloatArray())
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
               if(bound.sense){
                   outline = sweepVertices(edgeLoop)
               }else{
                   holes.add(sweepVertices(edgeLoop))
               }
            }

            addTriangles(generateTriangles(projector, outline!!, holes))
        }else if(surface is CylindricalSurface){
/*
            val projector = CylinderProjector(surface.workplane.eval(), surface.radius)

            class CylindricalVertex(val edge: Edge, val vertexPoint : Vec3, val projPoint: Vec2) : Comparable<CylindricalVertex>{
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

            class CylindricalEvent(val vertex: CylindricalVertex) : Comparable<CylindricalEvent>{
                override fun compareTo(other: CylindricalEvent): Int {
                    val projPoint = vertex.projPoint
                    val otherProjPoint = other.vertex.projPoint
                    return when (projPoint.x) {
                        otherProjPoint.x -> projPoint.y.compareTo(otherProjPoint.y)
                        else -> projPoint.x.compareTo(otherProjPoint.x)
                    }
                }
            }

            val events = arrayListOf<CylindricalVertex>()
            val helper = arrayListOf<CylindricalVertex>()

            for(bound in face.bounds){
                val edgeLoop = bound.edgeLoop

                val loopVertices = arrayListOf<CylindricalVertex>()
                for(current in edgeLoop){
                    val vertices = sweepEdgeLoop(current)

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
                    val jumping = thetaDiff > Math.PI
                    val swap = (lhsVertex.projPoint.x > rhsVertex.projPoint.x) xor jumping

                    val (lhs, rhs) = if(swap){
                        Pair(rhsVertex, lhsVertex)
                    }else{
                        Pair(lhsVertex, rhsVertex)
                    }

                    if(jumping){
                        helper.add(rhs)
                    }

                    lhs.nextList.add(rhs)
                    rhs.prevList.add(lhs)
                }

                events.addAll(loopVertices)
            }

            events.sort()
            helper.sortBy { it.projPoint.y }
*/



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
            }
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
            vertices.addAll(sweepEdgeLoop(current))
        }

        return vertices
    }

    fun sweepEdgeLoop(edgeLoop: EdgeLoop) : List<Vec3>{
        val vertices = sweepEdge(edgeLoop.edge)

        return if(edgeLoop.orientation){
            vertices.reversed()
        }else{
            vertices
        }
    }

    fun sweepEdge(edge: Edge) : List<Vec3>{
        return edgePoints.computeIfAbsent(edge){
            sweepEdgeImpl(it)
        }
    }

    fun sweepEdgeImpl(edge: Edge) : List<Vec3>{
        val vertices = arrayListOf<Vec3>()
        val curve = edge.curve

        fun addPoint(point: Vec3){
            vertices.add(point)
        }

        when(curve){
            is Line -> {
                val bound = edge.bound
                if(bound != null){
                    addPoint(bound.start.point.eval())
                    //addPoint(bound.end.point.eval())
                }
            }

            is Circle -> {
                val workplane = curve.workplane
                val axisUp = workplane.axisUp

                val bound = edge.bound
                val paramExpr = ParamExpr(axisUp.world, 0.0)
                val world = workplane.world
                val rotatedPoint = if(bound != null){
                    val startPoint = bound.start.point.copy()
                    val endPoint = bound.end.point.copy()

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

                    workplane.unproject(rotatedPoint)
                }else{
                    val quaternion = QuaternionExpr.fromAxis(axisUp, (2.0 * Math.PI) * paramExpr )
                    val somePoint = workplane.unproject(curve.radius, workplane.world.Zero)
                    quaternion.rotate(workplane.origin, somePoint)
                }

                val count = 10
                for( angle in 0 until count ){
                    val theta = angle / count.toDouble()
                    paramExpr.value = theta
                    val point = rotatedPoint.eval()
                    addPoint(point)
                }
            }
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
        return pointMap[point]!!
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

