package com.notiapp.fcm

/** Een bericht dat via het gezinskanaal verstuurd wordt. */
data class FamilyMessage(
    val type: Type,
    val alertId: String,
    val senderId: String,
    val senderName: String,
    val text: String = "",
    val sentAt: Long = System.currentTimeMillis(),
) {
    enum class Type { ALERT, ACK, CANCEL }

    fun toData(): Map<String, String> = mapOf(
        "type" to type.name,
        "alertId" to alertId,
        "senderId" to senderId,
        "senderName" to senderName,
        "text" to text,
        "sentAt" to sentAt.toString(),
    )

    companion object {
        fun fromData(data: Map<String, String>): FamilyMessage? {
            val type = runCatching { Type.valueOf(data["type"] ?: return null) }.getOrNull() ?: return null
            return FamilyMessage(
                type = type,
                alertId = data["alertId"] ?: return null,
                senderId = data["senderId"] ?: return null,
                senderName = data["senderName"].orEmpty(),
                text = data["text"].orEmpty(),
                sentAt = data["sentAt"]?.toLongOrNull() ?: System.currentTimeMillis(),
            )
        }
    }
}
