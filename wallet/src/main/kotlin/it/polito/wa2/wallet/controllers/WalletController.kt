package it.polito.wa2.wallet.controllers

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.util.jwt.JwtValidateUtils
import it.polito.wa2.wallet.domain.TransactionEntity
import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.dto.WalletDTO
import it.polito.wa2.wallet.services.WalletServiceImpl
import org.bson.types.ObjectId
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.stream.Collectors
import javax.validation.ConstraintViolationException
import javax.validation.Valid


@RestController
@RequestMapping("/wallets")
class WalletController(
    val walletServiceImpl: WalletServiceImpl,
    val jwtUtils: JwtValidateUtils
) {

    /**
     * GET /wallets/{walletID}
     * Retrieve the wallet identified by WalletID
     *
     * @return the requested wallet with the response status 200
     */
    @GetMapping("/{walletId}")
    suspend fun getWalletById(
        @PathVariable("walletId") walletId: String,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<WalletDTO> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // Ask the wallet information to the service. Throw an exception if the user can't access to such information
            val requestedWallet: WalletDTO = walletServiceImpl.getWalletById(userInfoJWT, walletId)

            // Return a 200 with inside the wallet requested
            return ResponseEntity.status(HttpStatus.OK).body(requestedWallet)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }

    }

    /**
     * POST /wallets
     * Create a new wallet for a given customer. The wallet will have 0 money at the beginning.
     * @param walletDTO: Request Body with the id of the user (customer) that want to create the wallet
     * @return 201 (create) or an error message
     * */
    @PostMapping()
    fun createWallet(
        @RequestBody @Valid walletDTO: WalletDTO,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<Mono<WalletDTO>> {
        try {

            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            val createdWallet = walletServiceImpl.createWallet(userInfoJWT, walletDTO.customerUsername)

            // Return a 200 with inside the wallet requested
            return ResponseEntity.status(HttpStatus.CREATED).body(createdWallet)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }

    }

    /**
     * POST /wallets/{walletID}/transactions
     * Create a transaction taking the amount of money set in the body from the given wallet (@param walletId) and
     * transferring it to a second walletID defined in the body.
     * @return the created transaction
     */
    @PostMapping("/{walletId}/transactions")
    suspend fun createTransaction(
        @PathVariable("walletId") senderId: ObjectId,
        @RequestBody @Valid transactionDTO: TransactionDTO,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<TransactionDTO> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            transactionDTO.senderWalletId = senderId
            val createdTransaction = walletServiceImpl.createTransaction(userInfoJWT, transactionDTO)

            // Return a 201 with inside the transaction created
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }

    }

    /**
     * GET /wallets/{walletID}/transactions?from=<dateInMillis>&to=<dateInMillis>
     * Retrieve a list of transactions regarding a given wallet in a given time frame
     */
    @GetMapping("/{walletId}/transactions")
    suspend fun getTransactionsByPeriod(
        @PathVariable("walletId") senderId: ObjectId,
        @RequestParam("from") start: Long, @RequestParam("to") end: Long,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<Flux<TransactionEntity?>> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            val transactionDTO = walletServiceImpl.getTransactionsByPeriod(
                userInfoJWT = userInfoJWT,
                walletId = senderId,
                start = Instant.ofEpochMilli(start),
                end = Instant.ofEpochMilli(end)
            )

            // Return a 200 with inside the transactions
            return ResponseEntity.status(HttpStatus.OK).body(transactionDTO)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }

    }

    /**
     * GET /wallets/{walletID}/transactions/{transactionID}
     * Retrieves the details of a single transaction
     */
    @GetMapping("/{walletId}/transactions/{transactionId}")
    suspend fun getTransaction(
        @PathVariable("walletId") walletId: ObjectId,
        @PathVariable("transactionId") transactionId: ObjectId,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<TransactionDTO> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            val transactionDTO = walletServiceImpl.getTransactionByIdAndWalletId(
                userInfoJWT = userInfoJWT,
                transactionId = transactionId,
                walletId = walletId
            )

            // Return a 200 with inside the transaction requested
            return ResponseEntity.status(HttpStatus.OK).body(transactionDTO)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }

    }


}

// Exception class used for send to the controller the correct message and status to generate an appropriate http message
data class ErrorResponse(val status: HttpStatus, val errorMessage: String) : Throwable()

data class Profile(val username: String)

@ControllerAdvice
class ValidationHandler {

    // This is the exception launched by @Valid annotation. It will catch and format the errors inside
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleException(e: WebExchangeBindException): ResponseEntity<List<String?>> {
        val errors = e.bindingResult
            .allErrors
            .stream()
            .map { obj: ObjectError -> obj.defaultMessage }
            .collect(Collectors.toList())
        return ResponseEntity.badRequest().body(errors)
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException::class, TypeMismatchException::class)
    fun handleException(e: TypeMismatchException): ResponseEntity<Map<String, String>>? {
        val errorResponse: MutableMap<String, String> = HashMap()
        errorResponse["message"] = e.localizedMessage
        errorResponse["status"] = HttpStatus.BAD_REQUEST.toString()
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }
}

