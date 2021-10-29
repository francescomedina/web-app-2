package it.polito.wa2.wallet.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/wallets")
class WalletController {

    /**
     * GET /wallets/{walletID}
     * Retrieve the wallet identified by WalletID
     */
    @GetMapping("/{walletId}")
    fun getWalletById(@PathVariable("walletId") walletId: Long ): String {

        return "Hai chiesto $walletId"

    }

    /**
     * POST /wallets
     * Create a new wallet for a given customer
     */
    @PostMapping()
    fun createWallet(){

    }

    /**
     * POST /wallets/{walletID}/transactions
     * Adds a new transaction to the wallet identified by walletID
     */
    @PostMapping("/{walletId}/transactions")
    fun createTransaction(){

    }

    /**
     * POST /wallets/{walletID}/transactions?from=<dateInMillis>&to=<dateInMillis>
     * Retrieve a list of transactions regarding a given wallet in a given time frame
     */
    @GetMapping("/{walletId}/transactions")
    fun getTransactionByInterval(){

    }

    /**
     * GET /wallets/{walletID}/transactions/{transactionID}
     * Retrieves the details of a single transaction
     */
    @GetMapping("/{walletId}/transactions/{transactionId}")
    fun getTransaction(){

    }

}