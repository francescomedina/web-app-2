package it.polito.wa2.util.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.bson.types.ObjectId

class GsonUtils {
    companion object {
        val gson: Gson = GsonBuilder().registerTypeAdapter(ObjectId::class.java, ObjectIdTypeAdapter()).create()
    }
}