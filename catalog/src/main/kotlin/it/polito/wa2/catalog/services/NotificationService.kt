package it.polito.wa2.catalog.services

import it.polito.wa2.catalog.persistence.EmailVerificationToken
import it.polito.wa2.catalog.persistence.EmailVerificationTokenRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.RuntimeException
import java.util.*

interface NotificationService {
    suspend fun createEmailVerificationToken(expiryDate: Date, username: String): EmailVerificationToken
}

@Service
@Transactional
class NotificationServiceImpl(var emailVerificationTokenRepository: EmailVerificationTokenRepository): NotificationService {

    /**
     * Create a EmailVerificationToken
     */
    override suspend fun createEmailVerificationToken(expiryDate: Date, username: String): EmailVerificationToken {

        val emailVerificationToken = EmailVerificationToken(
            expiryDate,
            UUID.randomUUID().toString(),
            username
        )

        val notificationService = emailVerificationTokenRepository.save(emailVerificationToken).awaitSingleOrNull()
        notificationService?.let {
            return it
        }

        throw RuntimeException("Error notification service")

    }
}