package com.example.calorie_tracker_app

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ImageDao {
    @Query("SELECT * FROM images")
    fun getAllImages(): List<ImageEntity>

    @Insert
    fun insertImage(image: ImageEntity)

    @Delete
    fun deleteImage(image: ImageEntity)
}
