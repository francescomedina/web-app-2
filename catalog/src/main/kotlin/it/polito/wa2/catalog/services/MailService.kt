package it.polito.wa2.catalog.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface MailService {
    fun sendMessage(toMail: String, subject: String, mailBody: String)
}

@Service
@Transactional
class MailServiceImpl(
    val emailSender: JavaMailSender
): MailService {
    @Value("\${spring.mail.username}")
    val username: String = ""

    override fun sendMessage(toMail: String, subject: String, mailBody: String) {
        val message = SimpleMailMessage()
        message.setFrom(username)
        message.setTo(toMail)
        message.setSubject(subject)
        message.setText(mailBody)
        emailSender.send(message)
    }
}