package it.polito.wa2.util.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bson.types.ObjectId
import java.io.IOException

class ObjectIdTypeAdapter : TypeAdapter<ObjectId?>() {

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): ObjectId {
        `in`.beginObject()
        assert("\$oid" == `in`.nextName())
        val objectId: String = `in`.nextString()
        `in`.endObject()
        return ObjectId(objectId)
    }

    @Throws(IOException::class)
    override fun write(out: JsonWriter?, value: ObjectId?) {
        out!!.beginObject()
            .name("\$oid")
            .value(value.toString())
            .endObject();
    }
}