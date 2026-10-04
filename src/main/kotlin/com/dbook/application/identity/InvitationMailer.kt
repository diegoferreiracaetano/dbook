package com.dbook.application.identity

import com.dbook.domain.identity.EmailDeliveryException
import com.dbook.domain.identity.EmailSender
import com.dbook.domain.identity.StaffInvitation
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class AdminPortalLinks(
    val baseUrl: String,
) {
    fun acceptInvitation(token: String) = "$baseUrl/accept-invite?token=$token"
}

@Service
class InvitationMailer(
    private val emailSender: EmailSender,
    private val portalLinks: AdminPortalLinks,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // A delivery failure must not undo the invitation (already committed): it can be sent again.
    fun send(
        invitation: StaffInvitation,
        token: String,
    ) {
        try {
            emailSender.send(invitation.email, SUBJECT, body(invitation, token))
        } catch (ex: EmailDeliveryException) {
            log.warn("Could not send invitation {}: it can be sent again", invitation.id, ex)
        }
    }

    private fun body(
        invitation: StaffInvitation,
        token: String,
    ) = """
        Olá,

        Você foi convidado(a) para a equipe do DBook com o papel ${invitation.role.name}.
        Para criar a sua conta, abra o link abaixo. Ele vale por ${StaffInvitation.VALIDITY.toHours()} horas
        e só pode ser usado uma vez:

        ${portalLinks.acceptInvitation(token)}

        Se você não esperava este convite, ignore esta mensagem.
        """.trimIndent()

    private companion object {
        const val SUBJECT = "Convite para a equipe do DBook"
    }
}
