import com.codecad.core.brep.Face
import com.codecad.core.export.ModelExport
import com.codecad.core.export.StepExport
import com.codecad.core.part.Context
import com.codecad.core.volume.Volume
import java.io.FileOutputStream

object ExportHelper {

    fun createExporter() : ModelExport{
        return StepExport()
    }

    fun saveStep(volume: Volume, name : String = "test"){

        val byteArray = createExporter().export(Context.of(volume))

        val outStream = FileOutputStream("$name.step")
        outStream.write(byteArray)
        outStream.close()
    }

    fun saveStep(face: Face, name : String = "test"){
        val byteArray = createExporter().export(Context.of(face))

        val outStream = FileOutputStream("$name.step")
        outStream.write(byteArray)
        outStream.close()
    }
}