package com.codecad.core.gcode

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.Loop
import com.codecad.core.brep.OrientedEdge
import com.codecad.core.brep.Sense
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.length
import com.codecad.core.toolpath.ContourTree
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil

abstract class Command{
    var feed: Double? = null

    fun Double.format(scale: Int) = "%.${scale}f".format(Locale.US, this)

    abstract fun gcode(prev : Vec2) : String
}

class LineCommand(val target: Vec2, val depth: Double, val fast: Boolean) : Command(){
    override fun gcode(prev : Vec2): String {
        val code = if(fast){
            "G0"
        }else{
            "G1"
        }

        return "$code X${target.x.format(4)} Y${target.y.format(4)} Z${depth.format(4)}"
    }
}
class HelixCommand(val center: Vec2, val end: Vec2, val depth: Double, val sense : Sense) : Command(){
    override fun gcode(prev : Vec2): String {
        val code = if(sense == Sense.Same){
            "G3"
        }else{
            "G2"
        }

        return "$code X${end.x.format(4)} Y${end.y.format(4)} Z${depth.format(4)} I${center.x.format(4)} J${center.y.format(4)}"
    }
}

class Program{
    private val commands = arrayListOf<Command>()

    fun moveTo(target: Vec2, z : Double, fast: Boolean = false){
        commands.add(LineCommand(target, z, fast))
    }

    fun helixTo(center: Vec2, end: Vec2, z : Double, sense: Sense){
        commands.add(HelixCommand(center, end, z, sense))
    }

    fun gcode() : String{
        val sb = StringBuilder()
        for(commend in commands){
            sb.append(commend.gcode(Vec2.Zero)).append("\n")
        }
        return sb.toString()
    }
}

class ProgramGenerator(){
        private val program = Program()

        fun generate(contourTree: ContourTree, startHeight: Double, securityDistance : Double, endHeight: Double, increment: Double) : String{
            val bounds = contourTree.collect()
            assert(endHeight <= startHeight)

            val approachingHeight = startHeight + securityDistance
            val depth = abs(endHeight - startHeight)
            val cuts = ceil(depth / increment).toInt()

            for( bound in bounds ){
                val currentLoop = bound.loop
                val startEdge = currentLoop.edge.normalized()
                val startPoint = startEdge.bound.start.point

                program.moveTo(startPoint, approachingHeight, true)
                program.moveTo(startPoint, startHeight, true)

                for(cut in 0 until cuts){
                    val cutDepth = startHeight - cut * increment
                    generateHelix(currentLoop, cutDepth, increment)
                }

                generate(currentLoop, endHeight)
                program.moveTo(startPoint, approachingHeight, true)
            }

            return program.gcode()
        }

        fun generateHelix(currentLoop: Loop<Vec2>, startZ: Double, increment: Double){
            var currentZ = startZ
            val lengthFactor = increment / currentLoop.length() //desired depth

            for(loop in currentLoop){
                loop.edge.let {
                    val offset = it.edge.length() * lengthFactor
                    currentZ -= offset
                    generate(it, currentZ)
                }
            }
        }

        fun generate(currentLoop: Loop<Vec2>, currentZ: Double){
            for(loop in currentLoop){
                generate(loop.edge, currentZ)
            }
        }

        fun generate(edge: OrientedEdge<Vec2>, depth : Double){
            val edge = edge.normalized()
            val start = edge.bound.start.point
            val end = edge.bound.end.point
            when(val curve = edge.curve){
                is Line -> program.moveTo(end, depth)
                is Circle ->  program.helixTo(curve.workplane.origin - start, end, depth, edge.bound.sense)
            }
        }
    }