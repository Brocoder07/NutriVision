package com.example.calorie_tracker_app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import kotlinx.coroutines.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import android.graphics.ImageDecoder
import android.os.Build
import androidx.annotation.RequiresApi
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var imageCapture: ImageCapture
    private lateinit var outputDirectory: File
    private lateinit var viewFinder: PreviewView
    private lateinit var imageView: ImageView
    private lateinit var tflite: Interpreter
    private lateinit var nutritionData: List<NutritionInfo>

    @RequiresApi(Build.VERSION_CODES.P)//Api 28+
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
                scope.launch { predictAndShowResults(it) }
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
    @RequiresApi(Build.VERSION_CODES.P)// Api 28 and above
    private val selectImageFromDatabaseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                imageView.setImageURI(it)
                imageView.visibility = ImageView.VISIBLE
                viewFinder.visibility = PreviewView.GONE
                scope.launch { predictAndShowResults(it) }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.P)//Api 28 and above
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder)
        imageView = findViewById(R.id.image_view)
        val btnCamera: ImageButton = findViewById(R.id.btn_camera)
        val btnAccessPictures: ImageButton = findViewById(R.id. btn_access_pictures)
        val btnRetrieveFromDatabase: ImageButton = findViewById(R.id.btn_retrieve_from_database)
        val btnResetCamera: ImageButton = findViewById(R.id.btn_reset_camera)

        //loading json and model
        tflite = loadModelFile()
        nutritionData = readJsonFile(this)

        //camera request perm
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCamera()
        }
        //listener for camera
        btnCamera.setOnClickListener {
            takePhoto()
        }
        //gallery retrieval
        btnAccessPictures.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageResultLauncher.launch(intent)
        }
        //db retrieval
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
            //Select back camera as a default
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
                @RequiresApi(Build.VERSION_CODES.P)
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    imageView.setImageURI(savedUri)
                    imageView.visibility = ImageView.VISIBLE
                    viewFinder.visibility = PreviewView.GONE
                    saveImageToDatabase(savedUri.toString())
                    scope.launch { predictAndShowResults(savedUri) }
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
    //Methods for model start from here
    private fun loadModelFile(): Interpreter {
        val fileDescriptor = assets.openFd("model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val buffer: ByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        return Interpreter(buffer)
    }
    private val scope = CoroutineScope(Dispatchers.Default)//Take off heavy load on Main thread to prevent frame drops

    @RequiresApi(Build.VERSION_CODES.P)//Api 28+
    private suspend fun predictAndShowResults(imageUri: Uri) {
        val source = ImageDecoder.createSource(this.contentResolver, imageUri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = true
        }
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 299, 299, true)
        val byteBuffer = convertBitmapToByteBuffer(scaledBitmap)
        val prediction = Array(1) { FloatArray(36) }
        tflite.run(byteBuffer, prediction)
        val predictedLabel = getLabel(prediction[0][0])
        withContext(Dispatchers.Main) {
            showNutritionInfo(predictedLabel)
        }
        Log.i(TAG, "Predicted label: ${predictedLabel}",null)
    }
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = 299
        val numChannels = 3
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * numChannels)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val value = intValues[pixel++]
                byteBuffer.putFloat(((value shr 16 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value and 0xFF) - 127.5f) / 127.5f)
            }
        }
        return byteBuffer
    }
    private fun getLabel(predictedValue: Float): String {
        return when (predictedValue.toInt()) {
            1 -> "apple" 2 -> "banana" 3 -> "beetroot" 4 -> "bell pepper" 5 -> "cabbage" 6 ->"capsicum"
            7 ->"carrot" 8 ->"cauliflower" 9 ->"chilli pepper" 10->"corn" 11->"cucumber" 12->"eggplant"
            13->"garlic" 14->"ginger" 15->"grapes" 16->"jalapeno" 17->"kiwi" 18->"lemon" 19->"lettuce"
            20->"mango" 21->"onion" 22->"orange" 23->"paprika" 24->"pear" 25->"peas" 26->"pineapple"
            27->"pomegranate" 28->"potato" 29->"radish" 30->"soy beans" 31->"spinach" 32->"sweetcorn"
            33->"sweetpotato" 34->"tomato" 35->"turnip" 36->"watermelon" else -> "Unknown"
        }
    }
    private fun showNutritionInfo(predictedLabel: String) {
        val nutritionInfo = nutritionData.find { it.name.equals(predictedLabel, ignoreCase = true) }
        if (nutritionInfo != null) {
            val dialogView = layoutInflater.inflate(R.layout.nutrition_popup, null)
            dialogView.findViewById<TextView>(R.id.foodName).text = nutritionInfo.name
            dialogView.findViewById<TextView>(R.id.calories).text = "Calories: ${nutritionInfo.calories}"
            dialogView.findViewById<TextView>(R.id.sodium).text = "Sodium: ${nutritionInfo.sodium}"
            dialogView.findViewById<TextView>(R.id.potassium).text = "Potassium: ${nutritionInfo.potassium}"
            dialogView.findViewById<TextView>(R.id.carbohydrates).text = "Carbohydrates: ${nutritionInfo.carbohydrates}"
            dialogView.findViewById<TextView>(R.id.protein).text = "Protein: ${nutritionInfo.protein}"
            AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            Toast.makeText(this, "Nutrition information not found for $predictedLabel", Toast.LENGTH_SHORT).show()
        }
    }
    companion object {
        private const val TAG = "MainActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
