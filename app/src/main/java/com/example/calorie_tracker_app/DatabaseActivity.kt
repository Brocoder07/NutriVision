package com.example.calorie_tracker_app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DatabaseActivity : AppCompatActivity() {

    private lateinit var gridView: GridView
    private lateinit var adapter: ImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database)

        gridView = findViewById(R.id.gridView)

        loadImages()
    }
    private fun loadImages() {
        val db = DatabaseClient.getInstance(applicationContext).getAppDatabase()
        val imageDao = db.imageDao()
        Thread {
            val images = imageDao.getAllImages()
            runOnUiThread {
                adapter = ImageAdapter(this, images, { imageUri ->
                    val resultIntent = Intent().apply {
                        data = imageUri
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }, { image ->
                    deleteImage(image)
                })
                gridView.adapter = adapter
            }
        }.start()
    }
    private fun deleteImage(image: ImageEntity) {
        val db = DatabaseClient.getInstance(applicationContext).getAppDatabase()
        val imageDao = db.imageDao()
        Thread {
            imageDao.deleteImage(image)
            runOnUiThread {
                loadImages() // Reload the images to refresh the UI
                Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }
}