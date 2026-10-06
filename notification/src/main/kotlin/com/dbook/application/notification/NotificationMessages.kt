package com.dbook.application.notification

import com.dbook.domain.notification.NotificationType

data class NotificationContent(
    val title: String,
    val body: String,
)

// What each event says to the customer, in Portuguese. The payload already carries what is needed (the flight, the
// amount), so rendering asks nobody anything.
object NotificationMessages {
    fun contentFor(event: NotificationEvent): NotificationContent {
        val flight = event.data["title"]?.toString() ?: "sua viagem"
        return when (event.type) {
            NotificationType.BOOKING_CONFIRMED ->
                NotificationContent("Reserva confirmada", "Sua reserva de $flight foi confirmada. Boa viagem!")
            NotificationType.BOOKING_EXPIRED ->
                NotificationContent(
                    "Reserva expirada",
                    "Sua reserva de $flight expirou porque o pagamento não foi feito a tempo. O assento foi liberado.",
                )
            NotificationType.BOOKING_CANCELLED_BY_STAFF ->
                NotificationContent(
                    "Reserva cancelada",
                    "Sua reserva de $flight foi cancelada pela nossa equipe. Se tiver dúvidas, fale com o suporte.",
                )
            NotificationType.REFUND_COMPLETED ->
                NotificationContent(
                    "Reembolso concluído",
                    "O reembolso de ${money(event.data["amount"])} da reserva de $flight foi concluído.",
                )
            NotificationType.FLIGHT_CHANGED -> flightChanged(event, flight)
            NotificationType.PRICE_ALERT ->
                NotificationContent(
                    "Alerta de preço",
                    "O voo $flight em ${date(event.data["date"])} está por ${money(event.data["price"])}, " +
                        "dentro do seu alvo de ${money(event.data["targetPrice"])}.",
                )
        }
    }

    private fun flightChanged(
        event: NotificationEvent,
        flight: String,
    ): NotificationContent {
        val changes = (event.data["changes"] as? List<*>).orEmpty().mapNotNull { CHANGE_NAMES[it] }
        val what = if (changes.isEmpty()) "os dados do voo" else changes.joinToString(", ")
        return NotificationContent(
            "Seu voo mudou",
            "O voo $flight teve alteração em: $what. Confira os novos horários na sua viagem.",
        )
    }

    // "2027-01-15" -> "15/01"
    private fun date(value: Any?): String {
        val parts = value?.toString().orEmpty().split('-')
        return if (parts.size == ISO_DATE_PARTS) "${parts[2]}/${parts[1]}" else ""
    }

    // "100.00" -> "R$ 100,00"
    private fun money(amount: Any?): String = "R$ ${amount?.toString()?.replace('.', ',') ?: "0,00"}"

    private const val ISO_DATE_PARTS = 3

    private val CHANGE_NAMES =
        mapOf(
            "flightNumber" to "número do voo",
            "origin" to "origem",
            "destination" to "destino",
            "departureTime" to "horário de partida",
            "arrivalTime" to "horário de chegada",
        )
}
