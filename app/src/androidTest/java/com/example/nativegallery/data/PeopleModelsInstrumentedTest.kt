package com.example.nativegallery.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.objdetect.FaceDetectorYN
import org.opencv.objdetect.FaceRecognizerSF
import java.io.File

@RunWith(AndroidJUnit4::class)
class PeopleModelsInstrumentedTest {
    @Test fun bundledModelsRunWithPackagedNativeRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(OpenCVLoader.initLocal())
        fun model(name: String): String {
            val file = File(context.cacheDir, "test-$name")
            context.assets.open("people/$name").use { input -> file.outputStream().use { input.copyTo(it) } }
            return file.absolutePath
        }
        val detector = FaceDetectorYN.create(model("yunet.onnx"), "", Size(320.0, 320.0))
        val recognizer = FaceRecognizerSF.create(model("sface.onnx"), "")
        val image = Mat.zeros(320, 320, CvType.CV_8UC3)
        val faces = Mat()
        val crop = Mat.zeros(112, 112, CvType.CV_8UC3)
        val vector = Mat()
        try {
            detector.detect(image, faces)
            assertEquals(0, faces.rows())
            recognizer.feature(crop, vector)
            assertEquals(128L, vector.total())
        } finally {
            image.release(); faces.release(); crop.release(); vector.release()
            File(context.cacheDir, "test-yunet.onnx").delete()
            File(context.cacheDir, "test-sface.onnx").delete()
        }
    }
}
