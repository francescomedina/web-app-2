package it.polito.wa2.order.outbox

import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface OutboxEventRepository : ReactiveMongoRepository<OutboxEvent?, String?>
