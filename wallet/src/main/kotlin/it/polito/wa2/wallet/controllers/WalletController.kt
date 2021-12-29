package it.polito.wa2.wallet.controllers

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.util.jwt.JwtValidateUtils
import it.polito.wa2.wallet.domain.TransactionEntity
import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.dto.WalletDTO
import it.polito.wa2.wallet.services.WalletServiceImpl
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
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
     * @param walletId : id of the wallet
     * @return the requested wallet with the response status 200
     */
    @GetMapping("/{walletId}")
    suspend fun getWalletById(
        @PathVariable("walletId") walletId: ObjectId,
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
    @PostMapping
    fun createWallet(
        @RequestBody @Valid walletDTO: WalletDTO,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<Mono<WalletDTO>> {
        try {

            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // Ask the service to create the wallet. Throw an exception if the user can't access to such information
            val createdWallet = walletServiceImpl.createWallet(userInfoJWT, walletDTO.customerUsername)

            // Return a 201 with inside the wallet created
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
     * @param senderId : sender walletId (i.e. walletId where take the money to)
     * @param transactionDTO : information about the transaction ('amount' and 'receiverWalletId'). If the user create
     * the transaction the amount is negative (he spends money), if the admin create the transaction the amount is positive (he recharges money)
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

            // Save inside the DTO the senderId because is a pathVariable
            transactionDTO.senderWalletId = senderId

            // Ask the service to create the transaction. Throw an exception if the user can't access to such information
            val createdTransaction = walletServiceImpl.createTransaction(userInfoJWT, transactionDTO).awaitSingleOrNull()

            // Return a 201 with inside the transaction created
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }

    }

    /**
     * GET /wallets/{walletID}/transactions?from=<dateInMillis>&to=<dateInMillis>
     * Retrieve a list of transactions regarding a given wallet in a given time frame (from start to end)
     * @param senderId : sender walletId (i.e. walletId where take the money to)
     * @param start : start date in millis
     * @param end : end date in millis
     * @return the transaction of that walletId in that period
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

            // Ask the transactions to the service. Throw an exception if the user can't access to such information
            // FIXME: This must return a Flux<TransactionDTO> not entity
            val transactionsDTO = walletServiceImpl.getTransactionsByPeriod(
                userInfoJWT = userInfoJWT,
                walletId = senderId,
                start = Instant.ofEpochMilli(start),
                end = Instant.ofEpochMilli(end)
            )

            // Return a 200 with inside the transactions
            return ResponseEntity.status(HttpStatus.OK).body(transactionsDTO)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }

    }

    /**
     * GET /wallets/{walletID}/transactions/{transactionID}
     * Retrieves the details of a single transaction
     * @param walletId : id of the wallet that belongs to that transaction
     * @param transactionId : id of the transaction
     * @return the transaction with that transactionId
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

            // Ask the transaction to the service. Throw an exception if the user can't access to such information
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

