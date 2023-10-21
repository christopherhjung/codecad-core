package com.codecad.core.toolpath

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.FaceBound
import com.codecad.core.brep.SketchFace
import com.codecad.core.debug.DebugPrinter
import com.codecad.core.sketch.BoundedFaceTree
import com.codecad.core.sketch.FaceTree
import com.codecad.core.sketch.offsetFace
import java.util.*
import kotlin.collections.ArrayList

open class ContourTree(val children : List<ContourTree> = Collections.emptyList()){
    open fun print(printer: DebugPrinter){
        children.forEach { it.print(printer) }
    }
    fun collect() : List<FaceBound<Vec2>>{
        val list = arrayListOf<FaceBound<Vec2>>()
        collect(list)
        return list
    }

    protected open fun collect(list: MutableList<FaceBound<Vec2>>){
        children.forEach { it.collect(list) }
    }
}

class BoundedContourTree(val tree : BoundedFaceTree, children : List<ContourTree> = Collections.emptyList()) : ContourTree(children)
{
    override fun print(printer: DebugPrinter) {
        printer.add(tree.toSketchFace())
        super.print(printer)
    }
    override fun collect(list: MutableList<FaceBound<Vec2>>){
        super.collect(list)
        list.add(tree.bound)
        tree.children.forEach{list.add(it.bound)}
    }
}

fun smoothOffset(face : SketchFace, offset: Double, factor: Double) : FaceTree {
        val first = offsetFace(face, offset + factor).toSketchFace()
        return offsetFace(first, -factor)
    }

fun buildContourTree(face : SketchFace, offset: Double, nextOffset: Double, round : Double) : ContourTree{
    val firstOffset = smoothOffset(face, offset, -round)

    val trees = ArrayList<ContourTree>()
    for( offsetBound in firstOffset.children ){
        val children = ArrayList<ContourTree>()
        val tree = BoundedContourTree(offsetBound, children)

        if(offsetFace(offsetBound.toSketchFace(), nextOffset).children.isNotEmpty()){
            children.add(buildContourTree(offsetBound.toSketchFace(), nextOffset, nextOffset, round))
            trees.add(tree)
        }
    }

    if(trees.size == 1){
        return trees.first()
    }

    return ContourTree(trees)
}