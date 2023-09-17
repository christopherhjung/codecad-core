package com.codecad.core.mesh

import com.codecad.core.brep.*
import com.codecad.core.brep.surface.PlaneSurface
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

        return Shell(listOf())
    }

}

class Matrix(val rowSize: Int, val colSize: Int, val arr : DoubleArray){
    operator fun get(row: Int, col: Int) : Double{
        return arr[row * colSize + col]
    }

    operator fun times(other : Matrix) : Matrix{
        if (colSize != other.rowSize) {
            throw IllegalArgumentException("The column count must match the row count of the argument matrix.")
        }

        val resultArray = DoubleArray(rowSize * other.colSize)

        for (i in 0 until rowSize) {
            for (j in 0 until other.colSize) {
                var sum = 0.0
                for (k in 0 until colSize) {
                    sum += this.arr[i * colSize + k] * other.arr[k * other.colSize + j]
                }
                resultArray[i * other.colSize + j] = sum
            }
        }

        return Matrix(rowSize, other.colSize, resultArray)
    }

    fun transpose(): Matrix {
        val transposedArr = DoubleArray(rowSize * colSize)
        for (i in 0 until rowSize) {
            for (j in 0 until colSize) {
                transposedArr[j * rowSize + i] = get(i, j)
            }
        }
        return Matrix(colSize, rowSize, transposedArr)
    }

    fun det(): Double {
        if (rowSize != colSize) {
            throw IllegalArgumentException("The matrix must be square for determinant calculation.")
        }else if (rowSize == 1) {
            return arr[0]
        }else if (rowSize == 2) {
            return arr[0] * arr[3] - arr[1] * arr[2]
        }

        // Perform LU decomposition
        val luDecomposition = luDecompose()

        // Calculate determinant from the diagonal elements of U
        val uMatrix = luDecomposition.second
        var determinant = 1.0
        for (i in 0 until rowSize) {
            determinant *= uMatrix.arr[i * colSize + i]
        }

        // Account for row swaps due to pivoting
        val permutationMatrix = luDecomposition.first
        var rowSwaps = 0
        for (i in 0 until rowSize) {
            if (permutationMatrix.arr[i * colSize + i] == 0.0) {
                rowSwaps++
            }
        }

        return if (rowSwaps % 2 == 0) determinant else -determinant
    }

    fun luDecompose(): Pair<Matrix, Matrix> {
        val rowSize = rowSize
        val colSize = colSize

        val luMatrix = arr.copyOf()
        val permutationMatrix = DoubleArray(rowSize * colSize) { 0.0 }

        for (i in 0 until rowSize) {
            permutationMatrix[i * colSize + i] = 1.0
        }

        for (k in 0 until rowSize - 1) {
            for (i in k + 1 until rowSize) {
                val factor = luMatrix[i * colSize + k] / luMatrix[k * colSize + k]
                luMatrix[i * colSize + k] = factor
                for (j in k + 1 until colSize) {
                    luMatrix[i * colSize + j] -= factor * luMatrix[k * colSize + j]
                }
            }
        }

        return Pair(Matrix(rowSize, colSize, permutationMatrix), Matrix(rowSize, colSize, luMatrix))
    }

    fun subMatrix(row: Int, col: Int): Matrix {
        require(row in 0 until rowSize) { "Row index out of bounds" }
        require(col in 0 until colSize) { "Column index out of bounds" }

        val newArr = DoubleArray((rowSize - 1) * (colSize - 1))
        var destIdx = 0

        for (i in 0 until rowSize) {
            if (i != row) {
                for (j in 0 until colSize) {
                    if (j != col) {
                        newArr[destIdx++] = get(i, j)
                    }
                }
            }
        }

        return Matrix(rowSize - 1, colSize - 1, newArr)
    }

    fun withoutColumn(col: Int): Matrix {
        require(col in 0 until colSize) { "Column index out of bounds" }

        val newArr = DoubleArray(rowSize * (colSize - 1))
        var destIdx = 0
        for (i in 0 until rowSize) {
            for (j in 0 until colSize) {
                if (j != col) {
                    newArr[destIdx++] = this[i, j]
                }
            }
        }

        return Matrix(rowSize, colSize - 1, newArr)
    }

    fun sq() : Matrix{
        return this * transpose()
    }

    fun inverse(): Matrix {
        // Check if the matrix is invertible
        require(isInvertible()) { "Matrix is not invertible." }

        // Create an identity matrix of the same size
        val identity = createIdentityMatrix()

        // Copy the original matrix to avoid modifying it
        val copy = arr.copyOf()

        // Perform Gaussian elimination to calculate the inverse
        for (i in 0 until rowSize) {
            // Find the pivot element
            val pivotIdx = findPivotElement(copy, i)

            // Swap rows if necessary
            if (pivotIdx != i) {
                swapRows(copy, i, pivotIdx)
                swapRows(identity.arr, i, pivotIdx)
            }

            // Scale the pivot row
            val pivotValue = copy[i * colSize + i]
            for (j in 0 until colSize) {
                copy[i * colSize + j] /= pivotValue
                identity.arr[i * colSize + j] /= pivotValue
            }

            // Eliminate other rows
            for (k in 0 until rowSize) {
                if (k != i) {
                    val factor = copy[k * colSize + i]
                    for (j in 0 until colSize) {
                        copy[k * colSize + j] -= factor * copy[i * colSize + j]
                        identity.arr[k * colSize + j] -= factor * identity.arr[i * colSize + j]
                    }
                }
            }
        }

        return identity
    }

    private fun isInvertible(): Boolean {
        // Check if the determinant is nonzero
        val determinant = det()
        return abs(determinant) > 1e-10
    }

    private fun createIdentityMatrix(): Matrix {
        val identityArr = DoubleArray(rowSize * colSize) { if (it % (rowSize + 1) == 0) 1.0 else 0.0 }
        return Matrix(rowSize, colSize, identityArr)
    }

    private fun findPivotElement(matrix: DoubleArray, rowIndex: Int): Int {
        var maxIdx = rowIndex
        var maxValue = abs(matrix[rowIndex * colSize + rowIndex])

        for (i in rowIndex + 1 until rowSize) {
            val value = abs(matrix[i * colSize + rowIndex])
            if (value > maxValue) {
                maxIdx = i
                maxValue = value
            }
        }

        return maxIdx
    }

    private fun swapRows(matrix: DoubleArray, row1: Int, row2: Int) {
        for (i in 0 until colSize) {
            val temp = matrix[row1 * colSize + i]
            matrix[row1 * colSize + i] = matrix[row2 * colSize + i]
            matrix[row2 * colSize + i] = temp
        }
    }
}