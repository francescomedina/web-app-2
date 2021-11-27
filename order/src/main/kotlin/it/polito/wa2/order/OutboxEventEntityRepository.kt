package it.polito.wa2.order

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OutboxEventEntityRepository : JpaRepository<OutboxEventEntity, UUID>
