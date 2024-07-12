package com.example.calorie_tracker_app

import android.content.Context
import org.json.JSONArray
import java.io.InputStream

data class NutritionInfo(
    val name: String,
    val calories: Int,
    val sodium: String,
    val potassium: String,
    val carbohydrates: String,
    val protein: String
)
fun readJsonFile(context: Context): List<NutritionInfo> {
    val inputStream: InputStream = context.assets.open("data.json")
    val json = inputStream.bufferedReader().use { it.readText() }
    val jsonArray = JSONArray(json)
    val nutritionList = mutableListOf<NutritionInfo>()

    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.getJSONObject(i)
        val nutritionInfo = NutritionInfo(
            jsonObject.getString("name"),
            jsonObject.getInt("calories"),
            jsonObject.getString("sodium"),
            jsonObject.getString("potassium"),
            jsonObject.getString("carbohydrates"),
            jsonObject.getString("protein")
        )
        nutritionList.add(nutritionInfo)
    }
    return nutritionList
}

