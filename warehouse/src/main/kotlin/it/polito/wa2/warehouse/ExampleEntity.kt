package it.polito.wa2.warehouse

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "example", schema = "public")
class ExampleEntity {
        @Id
        val id: UUID = UUID.randomUUID()
        val data: String?

        constructor() {
                this.data = null
        }

        constructor(data: String?) {
                this.data = data
        }
}

