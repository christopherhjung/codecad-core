import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc


class Output {


    fun draw() {
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );

        val matrix = Mat(128, 128, CvType.CV_8SC3)


        // Drawing a line

        // Drawing a line
        Imgproc.line(
            matrix,  //Matrix obj of the image
            Point(10.0, 200.0),  //p1
            Point(300.0, 200.0),  //p2
            Scalar(0.0, 0.0, 255.0),  //Scalar object for color
            5 //Thickness of the line
        )

        Imgcodecs.imwrite("out.png", matrix)

    }
}
