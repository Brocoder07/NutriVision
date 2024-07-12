package com.example.calorie_tracker_app

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class ImageAdapter(
    private val context: Context,
    private val images: List<ImageEntity>,
    private val onClick: (Uri) -> Unit,
    private val onDelete: (ImageEntity) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int {
        return images.size
    }

    override fun getItem(position: Int): Any {
        return images[position]
    }

    override fun getItemId(position: Int): Long {
        return images[position].id.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_image, parent, false)

        val imageView: ImageView = view.findViewById(R.id.image_view)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)

        val image = images[position]
        val imageUri = Uri.parse(image.imagePath)

        // Check if the URI is accessible
        try {
            context.contentResolver.openInputStream(imageUri)?.close()
            imageView.setImageURI(imageUri)
        } catch (e: Exception) {
            // If not, show an error image or a placeholder
            imageView.setImageResource(R.drawable.error_placeholder)
            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
        }

        imageView.setOnClickListener {
            onClick(imageUri)
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(context).apply {
                setTitle("Delete Image")
                setMessage("Are you sure you want to delete this image?")
                setPositiveButton("Yes") { _, _ -> onDelete(image) }
                setNegativeButton("No", null)
            }.show()
        }

        return view
    }
}