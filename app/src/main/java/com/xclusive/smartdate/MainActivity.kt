package com.xclusive.smartdate

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    var monthh="" ;
    private lateinit var cameraExecutor: ExecutorService

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getTime()
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getTime() {
        val current = LocalDateTime.now()

        val formatter = DateTimeFormatter.ofPattern("MM/yy")
        val formatted = current.format(formatter)
        monthh = formatted

    }

    private fun startCamera() {

//        configureModel()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.createSurfaceProvider())
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ObjectDetectorImageAnalyzer())
                }


            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private val mTextRecognizer by lazy {
        TextRecognition.getClient()
    }

    inner class ObjectDetectorImageAnalyzer : ImageAnalysis.Analyzer {

        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {

            val mediaImage = imageProxy.image

            if (mediaImage != null) {
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                mTextRecognizer.process(image)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                             val final_result = it.result?.text
                            //tvPrediction.text= final_result

                           //val final_value = final_result?.contains("/" , true )
                            //val final_digits_only = final_result?.filter{ it.isDigit() || it == ' ' || it == '/'}
                            if (final_result?.contains(" " , true)!!) {
                            }

                           if (final_result?.contains("07/20" , true)!!){
                               val date = "07/20"
                               val three_plus_date ="10/20"
                               val status ="PASS"
                               val icon_status = 1
                               val month = monthh
                               status_image.setImageResource(R.drawable.correct_tick)
                               tvPrediction.text = date +"   -   "+ three_plus_date
                               product_status.text = status
                               current_month.text = "Current Month - "+ month
                               vibratePhone()

                               

                           }

                            if (final_result?.contains("07/19" , true)!!){
                                val date = "07/19"
                                val three_plus_date ="10/20"
                                val status ="FAIL"
                                val icon_status = 1
                                val month = monthh
                                status_image.setImageResource(R.drawable.wrong_tick)
                                tvPrediction.text = date +"   -   "+ three_plus_date
                                product_status.text = status
                                current_month.text = "Current Month - "+ month
                                vibratePhone()



                            }
                            else{
                               /*val date = "07/19"
                               val three_plus_date ="10/19"
                               val status ="FAIL"
                               val icon_status = 0
                               val month = monthh
                               status_image.setImageResource(R.drawable.wrong_tick)
                               tvPrediction.text = date +"   -   "+ three_plus_date
                               product_status.text = status
                               current_month.text = "Current Month - "+ month*/

                           }
                          /*  tvPrediction.text= final_digits_only.toString()*/

                        }

                        //TO AVOID: com.google.mlkit.common.MlKitException: Internal error has occurred when executing ML Kit tasks
                        imageProxy.close()
                    }
            }
        }
    }

    fun vibratePhone() {
        val vibrator = applicationContext?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }


    companion object {


        private const val TAG = "ImageRec"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}

