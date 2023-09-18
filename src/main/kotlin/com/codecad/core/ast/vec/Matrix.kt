package com.codecad.core.ast.vec

import kotlin.math.*

class Matrix(val numRows: Int, val numCols: Int, val arr : DoubleArray){
    operator fun get(row: Int, col: Int) : Double{
        return arr[row * numCols + col]
    }
    operator fun set(row: Int, col: Int, value: Double){
        arr[row * numCols + col] = value
    }

    fun matMul(other : Matrix) : Matrix{
        if (numCols != other.numRows) {
            throw IllegalArgumentException("The column count must match the row count of the argument matrix.")
        }

        val resultArray = DoubleArray(numRows * other.numCols)

        for (i in 0 until numRows) {
            for (j in 0 until other.numCols) {
                var sum = 0.0
                for (k in 0 until numCols) {
                    sum += this.arr[i * numCols + k] * other.arr[k * other.numCols + j]
                }
                resultArray[i * other.numCols + j] = sum
            }
        }

        return Matrix(numRows, other.numCols, resultArray)
    }

    operator fun times(other: Matrix): Matrix {
        if (numRows != other.numRows || numCols != other.numCols) {
            throw IllegalArgumentException("Matrix dimensions must match for subtraction")
        }

        val resultArr = DoubleArray(numRows * numCols)
        for (i in 0 until numRows) {
            for (j in 0 until numCols) {
                resultArr[i * numCols + j] = arr[i * numCols + j] * other.arr[i * numCols + j]
            }
        }
        return Matrix(numRows, numCols, resultArr)
    }

    operator fun minus(other: Matrix): Matrix {
        if (numRows != other.numRows || numCols != other.numCols) {
            throw IllegalArgumentException("Matrix dimensions must match for subtraction")
        }

        val resultArr = DoubleArray(numRows * numCols)
        for (i in 0 until numRows) {
            for (j in 0 until numCols) {
                resultArr[i * numCols + j] = arr[i * numCols + j] - other.arr[i * numCols + j]
            }
        }
        return Matrix(numRows, numCols, resultArr)
    }

    operator fun minusAssign(other: Matrix) {
        if (numRows != other.numRows || numCols != other.numCols) {
            throw IllegalArgumentException("Matrix dimensions must match for subtraction")
        }

        for (i in 0 until numRows) {
            for (j in 0 until numCols) {
                this[i, j] -= other[i, j]
            }
        }
    }

    operator fun minus(scalar: Double): Matrix {
        val resultArr = DoubleArray(numRows * numCols)
        for (i in 0 until numRows) {
            for (j in 0 until numCols) {
                resultArr[i * numCols + j] = arr[i * numCols + j] - scalar
            }
        }
        return Matrix(numRows, numCols, resultArr)
    }

    // Multiply the matrix by a scalar
    operator fun times(scalar: Double): Matrix {
        val resultArr = DoubleArray(numRows * numCols)
        for (i in 0 until numRows) {
            for (j in 0 until numCols) {
                resultArr[i * numCols + j] = arr[i * numCols + j] * scalar
            }
        }
        return Matrix(numRows, numCols, resultArr)
    }

    fun transpose(): Matrix {
        val transposedArr = DoubleArray(numRows * numCols)
        for (i in 0 until numRows) {
            for (j in 0 until numCols) {
                transposedArr[j * numRows + i] = get(i, j)
            }
        }
        return Matrix(numCols, numRows, transposedArr)
    }

    fun det(): Double {
        if (numRows != numCols) {
            throw IllegalArgumentException("The matrix must be square for determinant calculation.")
        }else if (numRows == 1) {
            return arr[0]
        }else if (numRows == 2) {
            return arr[0] * arr[3] - arr[1] * arr[2]
        }

        // Perform LU decomposition
        val luDecomposition = luDecompose()

        // Calculate determinant from the diagonal elements of U
        val uMatrix = luDecomposition.second
        var determinant = 1.0
        for (i in 0 until numRows) {
            determinant *= uMatrix.arr[i * numCols + i]
        }

        // Account for row swaps due to pivoting
        val permutationMatrix = luDecomposition.first
        var rowSwaps = 0
        for (i in 0 until numRows) {
            if (permutationMatrix.arr[i * numCols + i] == 0.0) {
                rowSwaps++
            }
        }

        return if (rowSwaps % 2 == 0) determinant else -determinant
    }

    fun luDecompose(): Pair<Matrix, Matrix> {
        val rowSize = numRows
        val colSize = numCols

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
        require(row in 0 until numRows) { "Row index out of bounds" }
        require(col in 0 until numCols) { "Column index out of bounds" }

        val newArr = DoubleArray((numRows - 1) * (numCols - 1))
        var destIdx = 0

        for (i in 0 until numRows) {
            if (i != row) {
                for (j in 0 until numCols) {
                    if (j != col) {
                        newArr[destIdx++] = get(i, j)
                    }
                }
            }
        }

        return Matrix(numRows - 1, numCols - 1, newArr)
    }

    fun withoutColumn(col: Int): Matrix {
        require(col in 0 until numCols) { "Column index out of bounds" }

        val newArr = DoubleArray(numRows * (numCols - 1))
        var destIdx = 0
        for (i in 0 until numRows) {
            for (j in 0 until numCols) {
                if (j != col) {
                    newArr[destIdx++] = this[i, j]
                }
            }
        }

        return Matrix(numRows, numCols - 1, newArr)
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
        val copyMat = Matrix(numRows, numCols, copy)

        // Perform Gaussian elimination to calculate the inverse
        for (i in 0 until numRows) {
            // Find the pivot element
            val pivotIdx = findPivotElement(copy, i)

            // Swap rows if necessary
            if (pivotIdx != i) {
                copyMat.swapRows(i, pivotIdx)
                identity.swapRows(i, pivotIdx)
            }

            // Scale the pivot row
            val pivotValue = copy[i * numCols + i]
            for (j in 0 until numCols) {
                copy[i * numCols + j] /= pivotValue
                identity.arr[i * numCols + j] /= pivotValue
            }

            // Eliminate other rows
            for (k in 0 until numRows) {
                if (k != i) {
                    val factor = copy[k * numCols + i]
                    for (j in 0 until numCols) {
                        copy[k * numCols + j] -= factor * copy[i * numCols + j]
                        identity.arr[k * numCols + j] -= factor * identity.arr[i * numCols + j]
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
        val identityArr = DoubleArray(numRows * numCols) { if (it % (numRows + 1) == 0) 1.0 else 0.0 }
        return Matrix(numRows, numCols, identityArr)
    }

    private fun findPivotElement(matrix: DoubleArray, rowIndex: Int): Int {
        var maxIdx = rowIndex
        var maxValue = abs(matrix[rowIndex * numCols + rowIndex])

        for (i in rowIndex + 1 until numRows) {
            val value = abs(matrix[i * numCols + rowIndex])
            if (value > maxValue) {
                maxIdx = i
                maxValue = value
            }
        }

        return maxIdx
    }

    private fun swapRows(row1: Int, row2: Int) {
        for (col in 0 until numCols) {
            val temp = this[row1, col]
            this[row1, col] = this[row2, col]
            this[row2, col] = temp
        }
    }



    fun qrFactorization(): Pair<Matrix, Matrix> {
        val A = Matrix(numRows, numCols, arr.copyOf()) // Make a copy of the matrix

        val Q = Matrix(numRows, numCols, DoubleArray(numRows * numCols))
        val R = Matrix(numRows, numCols, DoubleArray(numRows * numCols))

        for (k in 0 until numRows) {
            // Calculate the k-th column of Q
            val Ak = A.extractColumn(k)
            val qk = Ak.gramSchmidt(Q)

            // Update the k-th column of Q and R
            Q.updateColumn(k, qk)
            R.updateColumn(k, qk * Ak)
        }

        return Pair(Q, R)
    }

    // Helper function to extract a column as a vector
    fun extractColumn(col: Int): Matrix {
        val colData = DoubleArray(numRows)
        for (i in 0 until numRows) {
            colData[i] = arr[i * numCols + col]
        }
        return Matrix(numRows, 1, colData)
    }

    // Helper function to update a column in the matrix
    fun updateColumn(col: Int, colData: Matrix) {
        for (i in 0 until numRows) {
            arr[i * numCols + col] = colData.arr[i]
        }
    }

    // Gram-Schmidt orthogonalization of a vector
    fun gramSchmidt(Q: Matrix): Matrix {
        val v = Matrix(numRows, 1, arr.copyOf()) // Make a copy of the vector
        for (i in 0 until Q.numCols) {
            val qi = Q.extractColumn(i)
            val dotProduct = qi.dotProduct(v)
            v -= qi * dotProduct
        }
        v.normalize()
        return v
    }

    // Dot product of two vectors
    fun dotProduct(other: Matrix): Double {
        var result = 0.0
        for (i in 0 until numRows) {
            result += arr[i] * other.arr[i]
        }
        return result
    }

    // Normalize a vector
    fun normalize() {
        val length = Math.sqrt(arr.sumOf { it * it })
        for (i in 0 until numRows) {
            arr[i] /= length
        }
    }

    fun printMatrix() {
        for (i in 0 until numRows) {
            for (j in 0 until numCols) {
                print("${arr[i * numCols + j]}\t")
            }
            println()
        }
    }

    fun trace(): Double {
        if (numRows != numCols) {
            throw IllegalArgumentException("Matrix must be square to calculate the trace.")
        }

        var traceSum = 0.0
        for (i in 0 until numRows) {
            traceSum += this[i, i]
        }

        return traceSum
    }

    fun eigenvalues(): DoubleArray {
        if (numRows != 3 || numCols != 3) {
            throw IllegalArgumentException("Matrix must be 3x3 to compute eigenvalues.")
        }

        val detA = det()
        val trace = trace()

        // Solve the characteristic polynomial: λ^3 - trace(λ^2) + (trace^2 - detA)λ - detA = 0
        val coefficients = doubleArrayOf(1.0, -trace, (trace * trace - detA), -detA)
        val eigenvalues = solveCubicEquation(coefficients)

        return eigenvalues
    }

    fun rank(): Int {
        // Create a copy of the matrix to avoid modifying the original
        val copy = Matrix(numRows, numCols, arr.copyOf())

        var rank = 0 // Initialize the rank to 0

        for (col in 0 until numCols) {
            // Find the first row with a non-zero element in the current column
            var pivotRow = -1
            for (row in rank until numRows) {
                if (copy[row, col] != 0.0) {
                    pivotRow = row
                    break
                }
            }

            if (pivotRow != -1) {
                // Swap the current row with the pivot row
                copy.swapRows(rank, pivotRow)

                // Scale the pivot row to make the pivot element 1
                val pivotElement = copy[rank, col]
                for (j in col until numCols) {
                    copy[rank, j] /= pivotElement
                }

                // Eliminate non-zero elements below the pivot
                for (row in 0 until numRows) {
                    if (row != rank && copy[row, col] != 0.0) {
                        val factor = copy[row, col]
                        for (j in col until numCols) {
                            copy[row, j] -= factor * copy[rank, j]
                        }
                    }
                }

                rank++
            }
        }

        return rank
    }

    companion object{
        fun identity(size : Int) : Matrix{
            val arr = DoubleArray(size * size)

            for( row in 0 until size ){
                for( col in 0 until size ){
                    arr[row * size + col] = if(row == col){
                        1.0
                    }else{
                        0.0
                    }
                }
            }

            return Matrix(size, size, arr)
        }
    }
}


fun cubeRoot(x: Double): Double {
    return if (x < 0) -(-x).pow(1.0 / 3.0) else x.pow(1.0 / 3.0)
}


private fun solveCubicEquation(coefficients: DoubleArray): DoubleArray {
    val a = coefficients[1] / coefficients[0]
    val b = coefficients[2] / coefficients[0]
    val c = coefficients[3] / coefficients[0]

    val p = b - a * a / 3.0
    val q = 2.0 * a * a * a / 27.0 - a * b / 3.0 + c
    val discriminant = q * q / 4.0 + p * p * p / 27.0

    if (discriminant > 0) {
        // One real root and two complex roots
        val sqrtD = sqrt(discriminant)
        val u = cubeRoot(-q / 2.0 + sqrtD)
        val v = cubeRoot(-q / 2.0 - sqrtD)
        val realRoot = u + v - a / 3.0
        return doubleArrayOf(realRoot)
    } else if (discriminant == 0.0) {
        // Three real roots, at least two equal
        val u = cubeRoot(-q / 2.0)
        val realRoot1 = 2.0 * u - a / 3.0
        val realRoot2 = -u - a / 3.0
        return doubleArrayOf(realRoot1, realRoot2)
    } else {
        // Three distinct real roots
        val phi = acos(-q / (2.0 * sqrt(-p * p * p / 27.0)))
        val sqrtP = sqrt(-p / 3.0)
        val root1 = 2.0 * sqrtP * cos(phi / 3.0) - a / 3.0
        val root2 = 2.0 * sqrtP * cos((phi + 2.0 * Math.PI) / 3.0) - a / 3.0
        val root3 = 2.0 * sqrtP * cos((phi + 4.0 * Math.PI) / 3.0) - a / 3.0
        return doubleArrayOf(root1, root2, root3)
    }
}