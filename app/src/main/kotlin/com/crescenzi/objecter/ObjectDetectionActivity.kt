package com.crescenzi.objecter

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.crescenzi.objecter.databinding.ActivityObjectDetectionBinding
import dalvik.annotation.optimization.FastNative
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/*
Implement Analyzer and pass it to setAnalyzer(Executor, ImageAnalysis.Analyzer)
 to receive images and perform custom processing by implementing the analyze(ImageProxy) function
 */
class ObjectDetectionActivity : AppCompatActivity(), ImageAnalysis.Analyzer {

    init {
        System.loadLibrary("objecter")
    }

    private val SCORE = 0.4

    private external fun initDetector(assetManager: AssetManager?)
    private external fun detect(bytes: ByteArray, width: Int, height: Int): FloatArray
    @FastNative
    private external fun destroyDetector()

    private lateinit var binding: ActivityObjectDetectionBinding

    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null

    private lateinit var rgbaFrame: ByteArray
    private val labelsMap = arrayListOf<String>()
    private val _paint = Paint()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityObjectDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDetector(assets)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            1
        )


        cameraExecutor = Executors.newSingleThreadExecutor()

        // init the paint for drawing the detections
        _paint.color = Color.RED
        _paint.style = Paint.Style.STROKE
        _paint.strokeWidth = 3f
        _paint.textSize = 50f
        _paint.textAlign = Paint.Align.LEFT

        // Set the detections drawings surface transparent
        binding.surfaceView.setZOrderOnTop(true)
        binding.surfaceView.holder.setFormat(PixelFormat.TRANSPARENT)
        binding.lblStatus.text = ""
        loadLabels()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            permissions.forEach {
                if(ContextCompat.checkSelfPermission(this,it)== PackageManager.PERMISSION_GRANTED) startCamera()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Frame rotation, it will be transformed by OpenCv
            val rotation = binding.viewFinder.display.rotation

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // ImageAnalysis
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(Size(768, 1024))
                .setTargetRotation(rotation)
                .setOutputImageRotationEnabled(true)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor, this)
                }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                LOG("use case binding failed ${exc.message}")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("SetTextI18n")
    override fun analyze(image: ImageProxy) {
        if (image.planes.isEmpty()) {return}

        val buffer = image.planes[0].buffer
        val size = buffer.capacity()
        if (!::rgbaFrame.isInitialized) {  // If it isn't initialized
            rgbaFrame = ByteArray(size)
        }

        buffer.position(0)
        buffer.get(rgbaFrame, 0, size)

        val start = System.currentTimeMillis()
        val res: FloatArray = detect( rgbaFrame, image.width, image.height)
        val span = System.currentTimeMillis() - start

        val canvas = binding.surfaceView.holder.lockCanvas()
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY)

            // Draw the detections, in our case there are only 3
            for (i in 0 until res[0].toInt()) {
                this.drawDetection(canvas, image.width, image.height, res, i)
            }

            binding.surfaceView.holder.unlockCanvasAndPost(canvas)
        }

        runOnUiThread {
            binding.lblStatus.text = "$span ms"
        }

        image.close()
    }

    private fun drawDetection(
        canvas: Canvas,
        frameWidth: Int,
        frameHeight: Int,
        detectionsArr: FloatArray,
        detectionIdx: Int
    ) {

        val pos = detectionIdx * 6 + 1
        val score = detectionsArr[pos + 0]
        val classId = detectionsArr[pos + 1]
        var xmin = detectionsArr[pos + 2]
        var ymin = detectionsArr[pos + 3]
        var xmax = detectionsArr[pos + 4]
        var ymax = detectionsArr[pos + 5]

        // Filter by score
        if (score < SCORE) return

        // detection coords are in frame coord system, convert to screen coords
        val scaleX = binding.viewFinder.width.toFloat() / frameWidth
        val scaleY = binding.viewFinder.height.toFloat() / frameHeight

        // The camera view offset on screen
        val xoff = 0 // viewFinder.left.toFloat()
        val yoff = 0 // viewFinder.top.toFloat()

        xmin = xoff + xmin * scaleX
        xmax = xoff + xmax * scaleX
        ymin = yoff + ymin * scaleY
        ymax = yoff + ymax * scaleY

        // Draw the rect
        val p = android.graphics.Path()
        p.moveTo(xmin, ymin)
        p.lineTo(xmax, ymin)
        p.lineTo(xmax, ymax)
        p.lineTo(xmin, ymax)
        p.lineTo(xmin, ymin)

        canvas.drawPath(p, _paint)

        // classId is zero-based (meaning class id 0 is class 1)
        val label = labelsMap[classId.toInt()]

        val txt = "%s (%.2f)".format(label, score)

        // Draw also the element name
        canvas.drawText(txt, xmin, ymin, _paint)
    }

    private fun loadLabels() {
        val labelsInput = this.assets.open("labels.txt")
        val br = BufferedReader(InputStreamReader(labelsInput))
        var line = br.readLine()
        while (line != null) {
            labelsMap.add(line)
            line = br.readLine()
        }

        br.close()
    }

    private fun LOG(value:String){
        Log.e("MY-LOG",value)
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyDetector()
    }
}