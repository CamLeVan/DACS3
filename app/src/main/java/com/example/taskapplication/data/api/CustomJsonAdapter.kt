package com.example.taskapplication.data.api

import android.util.Log
import com.example.taskapplication.data.api.response.PersonalTaskListResponse
import com.example.taskapplication.data.api.response.PersonalTaskResponse
import com.example.taskapplication.data.api.response.PaginationMeta
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import java.lang.reflect.Type

/**
 * Adapter tùy chỉnh để xử lý phản hồi JSON từ API
 * Xử lý trường hợp khi API trả về mảng thay vì đối tượng hoặc ngược lại
 */
class PersonalTaskListResponseAdapter : JsonDeserializer<PersonalTaskListResponse> {
    private val TAG = "PersonalTaskListResponseAdapter"
    
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): PersonalTaskListResponse {
        if (json == null) {
            Log.w(TAG, "JSON element is null")
            return PersonalTaskListResponse(emptyList(), null)
        }
        
        try {
            // Nếu json là một đối tượng, xử lý bình thường
            if (json.isJsonObject) {
                val jsonObject = json.asJsonObject
                
                // Kiểm tra xem có trường "data" không
                if (jsonObject.has("data")) {
                    val dataElement = jsonObject.get("data")
                    
                    // Nếu data là một mảng, xử lý bình thường
                    if (dataElement.isJsonArray) {
                        val gson = Gson()
                        val tasks = gson.fromJson(dataElement, Array<PersonalTaskResponse>::class.java).toList()
                        
                        // Xử lý meta nếu có
                        val meta = if (jsonObject.has("meta")) {
                            gson.fromJson(jsonObject.get("meta"), PaginationMeta::class.java)
                        } else {
                            null
                        }
                        
                        return PersonalTaskListResponse(tasks, meta)
                    } else {
                        Log.w(TAG, "data field is not an array")
                        return PersonalTaskListResponse(emptyList(), null)
                    }
                } else {
                    // Tạo một đối tượng mới với trường "data" là một mảng rỗng
                    Log.w(TAG, "JSON object does not have 'data' field, creating empty response")
                    return PersonalTaskListResponse(emptyList(), null)
                }
            } 
            // Nếu json là một mảng, tạo một đối tượng mới với trường "data" là mảng đó
            else if (json.isJsonArray) {
                Log.w(TAG, "Expected object but got array, wrapping array in object")
                val gson = Gson()
                val tasks = gson.fromJson(json, Array<PersonalTaskResponse>::class.java).toList()
                return PersonalTaskListResponse(tasks, null)
            }
            
            Log.e(TAG, "Unexpected JSON format: $json")
            return PersonalTaskListResponse(emptyList(), null)
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing PersonalTaskListResponse", e)
            return PersonalTaskListResponse(emptyList(), null)
        }
    }
}
