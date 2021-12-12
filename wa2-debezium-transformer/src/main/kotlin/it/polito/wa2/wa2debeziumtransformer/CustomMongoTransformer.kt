package it.polito.wa2.wa2debeziumtransformer

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.connect.connector.ConnectRecord
import org.slf4j.LoggerFactory

inline fun <reified T> ObjectMapper.readValue(s: String): T = this.readValue(s, object : TypeReference<T>() {})


class CustomMongoTransformer<R : ConnectRecord<R>> :
    org.apache.kafka.connect.transforms.Transformation<R> {
    override fun apply(sourceRecord: R): R {
        var sourceRecord: R = sourceRecord
        val struct: org.apache.kafka.connect.data.Struct = sourceRecord.value() as org.apache.kafka.connect.data.Struct
        val databaseOperation: String = struct.getString("op")
        if ("c" == databaseOperation) {
            val encodedAfter: String = struct.getString("after")
            try {
                val after: com.fasterxml.jackson.databind.JsonNode = objectMapper.readTree(encodedAfter)
                val channel: String = after.get("channel").textValue()
                val messageKey: String = after.get("message_key").textValue()
                val payload: String = after.get("payload").textValue()
                val encodedHeaders: String = after.get("headers").textValue()
                val headers: org.apache.kafka.connect.header.Headers = sourceRecord.headers()
                try {
//                    val headersMap: Map<String?, String?> = objectMapper.readValue<HashMap<String, String>>(message.payload, typeRef)
                    val headersMap: Map<String,String> = objectMapper.readValue(encodedHeaders)
                    for ((key, value) in headersMap) headers.addString(
                    key, value
                    )
                } catch (ex: Exception) {
                    log.error("Can't decode headers column: ", ex)
                }
                sourceRecord = sourceRecord.newRecord(
                    channel,
                    null,
                    org.apache.kafka.connect.data.Schema.STRING_SCHEMA,
                    messageKey,
                    null,
                    payload,
                    sourceRecord.timestamp(),
                    headers
                )
            } catch (ex: Exception) {
                log.error("Can't decode the message payload: ", ex)
            }
        }
        return sourceRecord
    }

    override fun config(): org.apache.kafka.common.config.ConfigDef {
        return org.apache.kafka.common.config.ConfigDef()
    }

    override fun close() {}
    override fun configure(configs: Map<String?, *>?) {}

    companion object {
        private val log = LoggerFactory.getLogger(CustomMongoTransformer::class.java.name)
        private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper =
            com.fasterxml.jackson.databind.ObjectMapper()
    }
}
//package it.polito.wa2.wa2debeziumtransformer
//
//import org.apache.kafka.common.config.ConfigDef
//import org.apache.kafka.connect.connector.ConnectRecord
//import org.apache.kafka.connect.data.Schema
//import org.apache.kafka.connect.data.Struct
//import org.apache.kafka.connect.transforms.Transformation
//
///**
// * Created on 16/August/2021 By Author Eresh, Gorantla
// */
//class OutboxTransformer<R : ConnectRecord<R>?> :
//    Transformation<R> {
//    override fun apply(record: R): R {
//        var record = record
//        val kStruct = record!!.value() as Struct
//        val databaseOperation = kStruct.getString("op")
//        if ("c".equals(databaseOperation, ignoreCase = true)) {
//            val after = kStruct["after"] as Struct
//            val UUID = after.getString("id")
//            val payload = after.getString("payload")
//            val eventName = after.getString("event_name").toLowerCase()
//            val topic = eventName.toLowerCase()
//            val headers = record.headers()
//            headers.addString("eventId", UUID)
//
//            // Prepare the event to be published.
//            record = record.newRecord(
//                topic, null, Schema.STRING_SCHEMA, UUID,
//                null, payload, record.timestamp(), headers
//            )
//        }
//        return record
//    }
//
//    override fun config(): ConfigDef {
//        return ConfigDef()
//    }
//
//    override fun close() {}
//    override fun configure(configs: Map<String?, *>?) {}
//}