package com.codecad.core

import com.codecad.common.Mesh
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StlExport {
    fun export(mesh: Mesh): ByteArray{
        val positions = mesh.points
        val indices = mesh.indices

        val count = indices.size / 3
        val buffer = ByteBuffer.allocate(84 + (4 * 4 * 3 + 2) * count)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(ByteArray(80))
        buffer.putInt(count)

        for (i in 0 until count) {
            buffer.putFloat(0.0f)
            buffer.putFloat(0.0f)
            buffer.putFloat(0.0f)
            for(j in 0 until 3){
                for(k in 0 until 3){
                    buffer.putFloat(positions[indices[i * 3 + j] * 3 + k])
                }
            }
            buffer.putShort(0.toShort())
        }

        buffer.rewind()
        return buffer.array()
    }
}
