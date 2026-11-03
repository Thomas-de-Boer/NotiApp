package com.notiapp.fcm

/** Een bericht dat via het gezinskanaal (of rechtstreeks naar één telefoon) verstuurd wordt. */
data class FamilyMessage(
    val type: Type,
    val alertId: String,
    val senderId: String,
    val senderName: String,
    val text: String = "",
    /** Bij ACK: de id van de telefoon die het oorspronkelijke bericht stuurde. */
    val originalSenderId: String = "",
    /** Bij ALERT: namen van de ontvangers; leeg = iedereen. */
    val to: List<String> = emptyList(),
    /** Bij HELLO / HELLO_REPLY: het FCM-adres (token) van de afzender. */
    val token: String = "",
    val sentAt: Long = System.currentTimeMillis(),
) {
    enum class Type {
        /** Een gezinsbericht dat laat bellen. */
        ALERT,
        /** "Gezien": stuurt de ontvanger terug naar de afzender. */
        ACK,
        /** Bellen stoppen. */
        CANCEL,
        /** "Ik heb de app": een telefoon meldt zich aan bij het gezin. */
        HELLO,
        /** Antwoord op HELLO, rechtstreeks naar de nieuwe telefoon. */
        HELLO_REPLY,
        /** Stil controlebericht om te zien of een telefoon de app nog heeft. */
        PING,
    }

    fun toData(): Map<String, String> = mapOf(
        "type" to type.name,
        "alertId" to alertId,
        "senderId" to senderId,
        "senderName" to senderName,
        "text" to text,
        "originalSenderId" to originalSenderId,
        "to" to to.joinToString(SEPARATOR),
        "token" to token,
        "sentAt" to sentAt.toString(),
    )

    companion object {
        private const val SEPARATOR = "\u001F"

        fun fromData(data: Map<String, String>): FamilyMessage? {
            val type = runCatching { Type.valueOf(data["type"] ?: return null) }.getOrNull() ?: return null
            return FamilyMessage(
                type = type,
                alertId = data["alertId"] ?: return null,
                senderId = data["senderId"] ?: return null,
                senderName = data["senderName"].orEmpty(),
                text = data["text"].orEmpty(),
                originalSenderId = data["originalSenderId"].orEmpty(),
                to = data["to"]?.split(SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty(),
                token = data["token"].orEmpty(),
                sentAt = data["sentAt"]?.toLongOrNull() ?: System.currentTimeMillis(),
            )
        }
    }
}
