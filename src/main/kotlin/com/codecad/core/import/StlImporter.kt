package com.codecad.core.import

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.part.Context
import com.codecad.core.sketch.Unifier
import com.codecad.core.volume.Volume
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs


private class StlVertex(val vertex: Vertex, val idx: Int){
    val point get() = vertex.point
}

class VertexPair(val lhs: Vertex, val rhs: Vertex){
    override fun equals(other: Any?): Boolean {
        return this === other || other is VertexPair &&
                ( ( lhs == other.lhs && rhs == other.rhs )
                || ( lhs == other.rhs && rhs == other.lhs ) )
    }

    override fun hashCode(): Int {
        return System.identityHashCode(lhs) xor System.identityHashCode(rhs)
    }
}

class StlImporter : Importer {
    override fun import(buffer: ByteBuffer): Context{
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.position(80)
        val count = buffer.getInt()

        val vertices = HashMap<Vec3, StlVertex>()
        val edges = HashMap<Pair<StlVertex, StlVertex>, Edge>()
        val loops = HashMap<Pair<StlVertex, StlVertex>, Loop>()
        var idx = 0
        fun nextVec3() : Vec3{
            val x = buffer.getFloat()
            val y = buffer.getFloat()
            val z = buffer.getFloat()

            return Vec3(x.toDouble(),y.toDouble(),z.toDouble())
        }

        fun nextVertex() : StlVertex{
            val point = nextVec3()
            return vertices.computeIfAbsent(point){StlVertex(Vertex(point), idx++)}
        }

        fun createEdge(lhs: StlVertex, rhs: StlVertex) : Loop{
            val pair = if(lhs.idx < rhs.idx){
                Pair(lhs, rhs)
            }else{
                Pair(rhs, lhs)
            }

            var edge = edges[pair]
            val orientedEdge = if(edge == null){
                edge = Edge.line(lhs.vertex, rhs.vertex)
                edges[pair] = edge

                OrientedEdge(edge, EdgeOrientation.Forward)
            }else{
                OrientedEdge(edge, EdgeOrientation.Backward)
            }

            val loop = Loop(orientedEdge)

            val previous = loops[pair]
            if(previous != null){
                loop.twin = previous
                previous.twin = loop
            }else{
                loops[pair] = loop
            }

            return loop
        }

        val faces = arrayListOf<Face>()
        for (i in 0 until count) {
            var normal = nextVec3()
            val a = nextVertex()
            val b = nextVertex()
            val c = nextVertex()


            val abEdge = createEdge(a, b)
            val bcEdge = createEdge(b, c)
            val caEdge = createEdge(c, a)

            if(abs(normal.length() - 1.0) > 1e-5){
                normal = (b.point - a.point).cross(c.point - a.point).normalized()
            }

            val loop = Loop.combine(abEdge, bcEdge, caEdge)
            val center = (a.point + b.point + c.point) / 3.0
            val direction = if(abs(normal.x) < 1e-5){
                Vec3.DirectionX
            }else if(abs(normal.y) < 1e-5){
                Vec3.DirectionY
            }else if(abs(normal.z) < 1e-5){
                Vec3.DirectionZ
            }else{
                (a.point - center).normalized()
            }

            val workplane = Workplane(center, normal, direction)
            val face = Face(PlaneSurface(workplane), listOf(FaceBound(loop, FaceBoundKind.OuterBound)))

            faces.add(face)
            buffer.getShort()
        }

        val unifier = Unifier<Face>()

        faces.forEach{ lhsFace ->
            val lhsNode = unifier.get(lhsFace)
            val bound = lhsFace.bounds.first()
            bound.loop.forEach {
                val twin = it.twin ?: throw RuntimeException()

                val oppositeFace = twin.face
                val rhsNode = unifier.get(oppositeFace)
                unifier.unify(lhsNode, rhsNode)
            }
        }

        val shellLists = HashMap<Face, MutableList<Face>>()

        faces.forEach{
            val node = unifier.get(it)
            val parent = unifier.find(node)

            shellLists.computeIfAbsent(parent.value){
                arrayListOf()
            }.add(node.value)
        }

        val shells = shellLists.values.map { Shell(it) }
        val context = Context()
        context.volumes.add(Volume(shells))
        return context
    }
}
