package org.artemchik.newmrim.protocol.data

import org.artemchik.newmrim.protocol.MrimConstants

enum class UserStatus(val value: UInt, val xstatusType: String) {
    ONLINE(MrimConstants.STATUS_ONLINE, "STATUS_ONLINE"),
    AWAY(MrimConstants.STATUS_AWAY, "STATUS_AWAY"),
    DND(MrimConstants.STATUS_XSTATUS, "STATUS_DND"),
    CHAT(MrimConstants.STATUS_ONLINE, "status_chat"),
    INVISIBLE(MrimConstants.STATUS_INVISIBLE, "STATUS_ONLINE"),
    OFFLINE(MrimConstants.STATUS_OFFLINE, "");

    companion object {
        fun fromValue(value: UInt, xstatusType: String = ""): UserStatus {
            // Сначала проверяем по типу Xstatus, если он есть
            if (xstatusType.isNotEmpty()) {
                entries.find { it.xstatusType.equals(xstatusType, ignoreCase = true) }?.let { return it }
            }

            // Иначе по числовому значению
            return when (value) {
                MrimConstants.STATUS_INVISIBLE -> INVISIBLE
                MrimConstants.STATUS_AWAY -> AWAY
                MrimConstants.STATUS_ONLINE -> ONLINE
                MrimConstants.STATUS_XSTATUS -> DND
                MrimConstants.STATUS_OFFLINE -> OFFLINE
                else -> {
                    if ((value and MrimConstants.STATUS_XSTATUS) != 0u) DND
                    else ONLINE
                }
            }
        }
    }
}
