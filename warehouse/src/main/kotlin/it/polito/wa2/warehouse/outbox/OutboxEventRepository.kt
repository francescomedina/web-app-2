package it.polito.wa2.warehouse.outbox

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface OutboxEventRepository : ReactiveMongoRepository<OutboxEvent?, String?>
