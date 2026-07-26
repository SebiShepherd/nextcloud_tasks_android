package com.nextcloud.tasks.data.network

import java.security.MessageDigest
import java.security.cert.X509Certificate

/** Colon-separated uppercase hex fingerprint of a certificate, e.g. `45:A4:5E:...`. */
internal fun sha256Hex(cert: X509Certificate): String = digestHex(cert, "SHA-256")

internal fun sha1Hex(cert: X509Certificate): String = digestHex(cert, "SHA-1")

private fun digestHex(
    cert: X509Certificate,
    algorithm: String,
): String =
    MessageDigest
        .getInstance(algorithm)
        .digest(cert.encoded)
        .joinToString(":") { "%02X".format(it) }
