package com.dbook.application.identity

import com.dbook.domain.identity.EmailDeliveryException
import com.dbook.domain.identity.EmailSender
import com.dbook.domain.identity.User
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * The two mails of account recovery. A delivery failure never undoes what was committed: the user asks for another.
 * The reset mail leaves **on another thread**: whether an account exists must not show in how long the answer takes.
 */
@Service
class AccountMailer(
    private val emailSender: EmailSender,
    private val customerLinks: CustomerAppLinks,
    private val portalLinks: AdminPortalLinks,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendVerification(
        user: User,
        token: String,
    ) = deliver(
        user,
        VERIFICATION_SUBJECT,
        """
        Olá, ${user.name},

        Para confirmar o seu e-mail no DBook, abra o link abaixo. Ele vale por 48 horas e só pode ser usado uma vez:

        ${customerLinks.verifyEmail(token)}

        Se você não criou uma conta, ignore esta mensagem.
        """.trimIndent(),
    )

    @Async("mailExecutor")
    fun sendPasswordReset(
        user: User,
        token: String,
    ) {
        val link = if (user.role.isStaff) portalLinks.resetPassword(token) else customerLinks.resetPassword(token)
        deliver(
            user,
            RESET_SUBJECT,
            """
            Olá, ${user.name},

            Recebemos um pedido para escolher uma nova senha. Para continuar, abra o link abaixo. Ele vale por
            1 hora e só pode ser usado uma vez; ao usá-lo, todas as suas sessões são encerradas:

            $link

            Se não foi você, ignore esta mensagem: a sua senha continua a mesma.
            """.trimIndent(),
        )
    }

    private fun deliver(
        user: User,
        subject: String,
        body: String,
    ) {
        try {
            emailSender.send(user.email, subject, body)
        } catch (ex: EmailDeliveryException) {
            log.warn("Could not send '{}' to user {}: it can be asked again", subject, user.id, ex)
        }
    }

    private companion object {
        const val VERIFICATION_SUBJECT = "Confirme o seu e-mail no DBook"
        const val RESET_SUBJECT = "Escolha uma nova senha no DBook"
    }
}
