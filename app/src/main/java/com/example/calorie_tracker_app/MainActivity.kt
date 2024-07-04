package com.example.calorie_tracker_app

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var imageCapture: ImageCapture
    private lateinit var outputDirectory: File
    private lateinit var viewFinder: PreviewView
    private lateinit var imageView: ImageView

    private val pickImageResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent: Intent? = result.data
            val uri: Uri? = intent?.data
            uri?.let {
                imageView.setImageURI(it)
                imageView.visibility = ImageView.VISIBLE
                viewFinder.visibility = PreviewView.GONE
                showNutritionalInfo()
            }
        }
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is needed to take photos.", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder)
        imageView = findViewById(R.id.image_view)
        val btnCamera: ImageButton = findViewById(R.id.btn_camera)
        val btnAccessPictures: ImageButton = findViewById(R.id.btn_access_pictures)
        val btnRetrieveFromDatabase: ImageButton = findViewById(R.id.btn_retrieve_from_database)
        val btnResetCamera: ImageButton = findViewById(R.id.btn_reset_camera)

        //Request camera permissions
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                Toast.makeText(this, "Camera permission is needed to take photos.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        //Set up the listener for camera button
        btnCamera.setOnClickListener {
            takePhoto()
        }
        //Set up the listener for access pictures button
        btnAccessPictures.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageResultLauncher.launch(intent)
        }
        //Placeholder for retrieve from database button
        btnRetrieveFromDatabase.setOnClickListener {
            //To be implemented soon
        }
        //Set up the listener for reset camera button
        btnResetCamera.setOnClickListener {
            resetCameraView()
        }
        outputDirectory = getOutputDirectory()
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        },ContextCompat.getMainExecutor(this))
    }
    private fun takePhoto() {
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    imageView.setImageURI(savedUri)
                    imageView.visibility = ImageView.VISIBLE
                    viewFinder.visibility = PreviewView.GONE
                    showNutritionalInfo()
                }
            })
    }
    private fun resetCameraView() {
        imageView.setImageURI(null)
        imageView.visibility = ImageView.GONE
        viewFinder.visibility = PreviewView.VISIBLE
    }
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }
    private fun showNutritionalInfo() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.popup_info, null)

        builder.setView(dialogLayout)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        val fruitName: TextView = dialogLayout.findViewById(R.id.fruitName)
        val caloriesValue: TextView = dialogLayout.findViewById(R.id.caloriesValue)
        val fatValue: TextView = dialogLayout.findViewById(R.id.fatValue)
        val fiberValue: TextView = dialogLayout.findViewById(R.id.fiberValue)
        val proteinValue: TextView = dialogLayout.findViewById(R.id.proteinValue)
        val carbsValue: TextView = dialogLayout.findViewById(R.id.carbsValue)
        val cholesterolValue: TextView = dialogLayout.findViewById(R.id.cholesterolValue)

        //Populate with hardcoded values
        fruitName.text = "Nutritional Info for Apple"
        caloriesValue.text = "52"
        fatValue.text = "0.2g"
        fiberValue.text = "2.4g"
        proteinValue.text = "0.3g"
        carbsValue.text = "14g"
        cholesterolValue.text = "0mg"

        val dialog = builder.create()
        dialog.show()
    }
    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}
