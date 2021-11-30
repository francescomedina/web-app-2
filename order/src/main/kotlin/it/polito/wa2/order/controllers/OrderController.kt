package it.polito.wa2.order.controllers

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.util.jwt.JwtValidateUtils
import it.polito.wa2.order.dto.OrderDTO
import it.polito.wa2.order.services.OrderServiceImpl
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import javax.validation.Valid


@RestController
@RequestMapping("/orders")
class OrderController(
    val orderServiceImpl: OrderServiceImpl,
    val jwtUtils: JwtValidateUtils
) {

    /**
     * GET /orders/{orderID}
     * Retrieve the wallet identified by OrderID
     * @param walletId : id of the order
     * @return the requested order with the response status 200
     */
    @GetMapping("/")
    suspend fun getOrders(
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<Flux<OrderDTO?>> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // Ask the wallet information to the service. Throw an exception if the user can't access to such information
            val orders = orderServiceImpl.getOrders(userInfoJWT)

            // Return a 200 with inside the order requested
            return ResponseEntity.status(HttpStatus.OK).body(orders)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }

    }

    /**
     * GET /orders/{orderID}
     * Retrieve the wallet identified by OrderID
     * @param walletId : id of the order
     * @return the requested order with the response status 200
     */
    @GetMapping("/{orderId}")
    suspend fun getOrderById(
        @PathVariable("orderId") orderId: ObjectId,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<OrderDTO> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // Ask the wallet information to the service. Throw an exception if the user can't access to such information
            val requestedOrder: OrderDTO = orderServiceImpl.getOrderById(userInfoJWT, orderId)

            // Return a 200 with inside the order requested
            return ResponseEntity.status(HttpStatus.OK).body(requestedOrder)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }

    }

    /**
     * POST /orders
     * Create a new wallet for a given customer. The wallet will have 0 money at the beginning.
     * @param walletDTO: Request Body with the id of the user (customer) that want to create the wallet
     * @return 201 (create) or an error message
     * */
    @PostMapping
    fun createOrder(
        @RequestBody @Valid orderDTO: OrderDTO,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<Mono<OrderDTO>> {
        try {

            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // Ask the service to create the wallet. Throw an exception if the user can't access to such information
            val createdOrder = orderServiceImpl.createOrder(userInfoJWT, orderDTO.customerUsername)

            // Return a 201 with inside the wallet created
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }
}
