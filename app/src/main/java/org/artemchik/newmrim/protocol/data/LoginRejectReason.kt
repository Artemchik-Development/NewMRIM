package org.artemchik.newmrim.protocol.data

import org.artemchik.newmrim.R

enum class LoginRejectReason(val serverText: String, val displayResId: Int) {
    INVALID_LOGIN("Invalid login", R.string.login_rej_invalid),
    DATABASE_ERROR("Database error", R.string.login_rej_db_error),
    ACCESS_DENIED("Access denied", R.string.login_rej_access_denied),
    BLACKLISTED_IP("Black-List IP", R.string.login_rej_blacklisted_ip),
    UNKNOWN("", R.string.login_rej_unknown);

    companion object {
        fun fromServerText(text: String): LoginRejectReason =
            entries.find { text.contains(it.serverText, ignoreCase = true) } ?: UNKNOWN
    }
}
