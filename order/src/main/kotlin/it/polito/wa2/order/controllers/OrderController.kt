package it.polito.wa2.order.controllers

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.util.jwt.JwtValidateUtils
import it.polito.wa2.order.dto.OrderDTO
import it.polito.wa2.order.dto.PartiallyOrderDTO
import it.polito.wa2.order.services.OrderServiceImpl
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(OrderController::class.java)

    /**
     * GET /orders
     * Retrieve the list of all orders of the logged-in user
     * @return all the orders of that user with the response status 200
     */
    @GetMapping("/")
    suspend fun getOrders(
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<Flux<OrderDTO>> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // Ask the order information to the service. Throw an exception if the user can't access to such information
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
     * Retrieve the order identified by OrderID
     * @param orderId : id of the order
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

            // Ask the order information to the service. Throw an exception if the user can't access to such information
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
     * Create a new order for a given customer. The order will have 0 money at the beginning.
     * @param orderDTO: Request Body with the id of the user (customer) that want to create the order
     * @return 201 (create) or an error message
     * */
    @PostMapping
    suspend fun createOrder(
        @RequestBody @Valid orderDTO: OrderDTO,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<Mono<OrderDTO>> {
        try {

            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            val createdOrder = orderServiceImpl.createOrder(userInfoJWT, orderDTO).onErrorMap {
                logger.error("ERROR FUNCTION ${object{}.javaClass.enclosingMethod.name}: \n$it")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, it.message)
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }

    /**
     * PATCH /orders/{orderID}
     * @param orderID: id of the order to update
     * @param orderDTO:
     * @return the order updated
     */
    @PatchMapping("/{orderID}")
    suspend fun updatePartiallyOrder(
        @PathVariable orderID: String,
        @RequestBody @Valid orderDTO: PartiallyOrderDTO,
        @RequestHeader(name = "Authorization") jwtToken: String
    ) : ResponseEntity<OrderDTO> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

            // Ask the service to create a new order
            val updatedOrder = orderServiceImpl.updatePartiallyOrder(orderID,orderDTO)

            // Return a 200 with inside the order information
            return ResponseEntity.status(HttpStatus.CREATED).body(updatedOrder)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }catch (e: Exception){
            throw ResponseStatusException(HttpStatus.BAD_REQUEST,e.message)
        }
    }

    /**
     * DELETE /orders/{orderID}
     * @param orderId: the order to delete
     */
    @DeleteMapping("/{orderId}")
    suspend fun deleteOrder(
        @PathVariable("orderId") orderId: ObjectId,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<Mono<Void>> {
        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // Ask the service to delete that orderID. Throw an exception if the user can't access to such information
            val response = orderServiceImpl.deleteOrder(userInfoJWT, orderId).onErrorMap {
                logger.error("ERROR FUNCTION ${object{}.javaClass.enclosingMethod.name}: \n$it")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, it.message)
            }

            // Return a 200 with inside the order requested
            return ResponseEntity.status(HttpStatus.OK).body(response)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }

    }
}
