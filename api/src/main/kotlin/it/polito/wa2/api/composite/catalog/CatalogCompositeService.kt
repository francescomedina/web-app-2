//package it.polito.wa2.api.composite.catalog
//
//import io.swagger.v3.oas.annotations.Operation
//import io.swagger.v3.oas.annotations.responses.ApiResponse
//import io.swagger.v3.oas.annotations.responses.ApiResponses
//import io.swagger.v3.oas.annotations.tags.Tag
//import it.polito.wa2.api.core.order.Order
//import org.springframework.http.HttpStatus
//import org.springframework.web.bind.annotation.*
//import reactor.core.publisher.Mono
//
//
//@Tag(name = "CatalogComposite", description = "REST API for composite catalog information.")
//interface CatalogCompositeService {
//
//    /**
//     * Sample usage, see below.
//     *
//     * curl -X POST $HOST:$PORT/product-composite \
//     * -H "Content-Type: application/json" --data \
//     * '{"productId":123,"name":"product 123","weight":123}'
//     *
//     * @param body A JSON representation of the new composite product
//     */
//    @Operation(
//        summary = "\${api.product-composite.create-composite-product.description}",
//        description = "\${api.product-composite.create-composite-product.notes}"
//    )
//    @ApiResponses(
//        value = [ApiResponse(
//            responseCode = "400",
//            description = "\${api.responseCodes.badRequest.description}"
//        ), ApiResponse(responseCode = "422", description = "\${api.responseCodes.unprocessableEntity.description}")]
//    )
//    @ResponseStatus(
//        HttpStatus.ACCEPTED
//    )
//    @PostMapping(value = ["/order-composite"], consumes = ["application/json"])
//    fun createOrder(@RequestBody body: Order?): Mono<Void?>?
//
//
//
//    /**
//     * Sample usage: "curl $HOST:$PORT/product-composite/1".
//     *
//     * @param productId Id of the product
//     * @return the composite product info, if found, else null
//     */
//    @Operation(
//        summary = "\${api.product-composite.get-composite-product.description}",
//        description = "\${api.product-composite.get-composite-product.notes}"
//    )
//    @ApiResponses(
//        value = [ApiResponse(
//            responseCode = "200",
//            description = "\${api.responseCodes.ok.description}"
//        ), ApiResponse(responseCode = "400", description = "\${api.responseCodes.badRequest.description}"), ApiResponse(
//            responseCode = "404",
//            description = "\${api.responseCodes.notFound.description}"
//        ), ApiResponse(responseCode = "422", description = "\${api.responseCodes.unprocessableEntity.description}")]
//    )
//    @GetMapping(value = ["/order-composite/{orderId}"], produces = ["application/json"])
//    fun getOrder(@PathVariable orderId: Int): Mono<Order?>?
//
//    /**
//     * Sample usage: "curl -X DELETE $HOST:$PORT/product-composite/1".
//     *
//     * @param productId Id of the product
//     */
//    @Operation(
//        summary = "\${api.product-composite.delete-composite-product.description}",
//        description = "\${api.product-composite.delete-composite-product.notes}"
//    )
//    @ApiResponses(
//        value = [ApiResponse(
//            responseCode = "400",
//            description = "\${api.responseCodes.badRequest.description}"
//        ), ApiResponse(responseCode = "422", description = "\${api.responseCodes.unprocessableEntity.description}")]
//    )
//    @ResponseStatus(
//        HttpStatus.ACCEPTED
//    )
//    @DeleteMapping(value = ["/order-composite/{orderId}"])
//    fun deleteOrder(@PathVariable orderId: Int): Mono<Void?>?
//}
