package com.example.calorie_tracker_app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var imageCapture: ImageCapture
    private lateinit var outputDirectory: File
    private lateinit var viewFinder: PreviewView
    private lateinit var imageView: ImageView

    private val pickImageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent: Intent? = result.data
            val uri: Uri? = intent?.data
            uri?.let {
                imageView.setImageURI(it)
                imageView.visibility = ImageView.VISIBLE
                viewFinder.visibility = PreviewView.GONE
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                saveImageToDatabase(it.toString())
            }
        }
    }
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    private val selectImageFromDatabaseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                imageView.setImageURI(it)
                imageView.visibility = ImageView.VISIBLE
                viewFinder.visibility = PreviewView.GONE
            }
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

        //Camera permission request
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCamera()
        }
        //listener for camera button
        btnCamera.setOnClickListener {
            takePhoto()
        }
        //listener for gallery
        btnAccessPictures.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageResultLauncher.launch(intent)
        }
        btnRetrieveFromDatabase.setOnClickListener {
            val intent = Intent(this, DatabaseActivity::class.java)
            selectImageFromDatabaseLauncher.launch(intent)
        }
        btnResetCamera.setOnClickListener {
            resetCameraView()
        }
        outputDirectory = getOutputDirectory()
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            //Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            //Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder().build()
            //Select back camera as default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                //Unbind use cases before rebinding
                cameraProvider.unbindAll()
                //Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    private fun takePhoto() {
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg")
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
                    saveImageToDatabase(savedUri.toString())
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
    private fun saveImageToDatabase(imagePath: String) {
        val db = DatabaseClient.getInstance(applicationContext).getAppDatabase()
        val imageDao = db.imageDao()
        Thread {
            imageDao.insertImage(ImageEntity(imagePath = imagePath))
        }.start()
    }
    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}