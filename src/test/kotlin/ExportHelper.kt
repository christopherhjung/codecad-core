import com.codecad.core.brep.Face
import com.codecad.core.export.StlExport
import com.codecad.core.mesh.MeshGenerator
import com.codecad.core.volume.Volume
import java.io.FileOutputStream

object ExportHelper {

    fun saveStl(volume: Volume){
        val meshGenerator = MeshGenerator()
        meshGenerator.generate(volume)
        val mesh = meshGenerator.build()

        val stlExport = StlExport()
        val byteArray = stlExport.export(mesh)

        val outStream = FileOutputStream("test.stl")
        outStream.write(byteArray)
        outStream.close()
    }

    fun saveStl(face: Face){
        val meshGenerator = MeshGenerator()
        meshGenerator.generate(face)
        val mesh = meshGenerator.build()

        val stlExport = StlExport()
        val byteArray = stlExport.export(mesh)

        val outStream = FileOutputStream("test.stl")
        outStream.write(byteArray)
        outStream.close()
    }
}