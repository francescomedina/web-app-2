//package it.polito.wa2.api.core.warehouse
//
//import it.polito.wa2.api.core.order.Order
//import org.springframework.web.bind.annotation.GetMapping
//import org.springframework.web.bind.annotation.PathVariable
//import reactor.core.publisher.Mono
//
//
//interface WarehouseService {
//    fun createWarehouse(body: Warehouse?): Mono<Warehouse?>?
//
//    /**
//     * Sample usage: "curl $HOST:$PORT/product/1".
//     *
//     * @param walletId Id of the product
//     * @return the product, if found, else null
//     */
//    @GetMapping(value = ["/order/{warehouseId}"], produces = ["application/json"])
//    fun getWarehouse(@PathVariable warehouseId: Int): Mono<Warehouse?>?
//
//    fun checkAvailability(order: Order?): Boolean
//
//    fun decrementQuantity(order: Order?): Boolean
//
//    fun deleteWarehouse(orderId: Int): Mono<Void?>?
//}