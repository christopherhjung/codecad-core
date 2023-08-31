import com.codecad.core.brep.Face
import com.codecad.core.export.ModelExport
import com.codecad.core.export.StepExport
import com.codecad.core.export.StlExport
import com.codecad.core.mesh.MeshGenerator
import com.codecad.core.part.Context
import com.codecad.core.volume.Volume
import java.io.FileOutputStream

object ExportHelper {

    fun createExporter() : ModelExport{
        return StepExport()
    }

    fun saveStl(volume: Volume, name : String = "test.step"){

        val byteArray = createExporter().export(Context.of(volume))

        val outStream = FileOutputStream(name)
        outStream.write(byteArray)
        outStream.close()
    }

    fun saveStl(face: Face, name : String = "test.step"){
        val byteArray = createExporter().export(Context.of(face))

        val outStream = FileOutputStream(name)
        outStream.write(byteArray)
        outStream.close()
    }
}