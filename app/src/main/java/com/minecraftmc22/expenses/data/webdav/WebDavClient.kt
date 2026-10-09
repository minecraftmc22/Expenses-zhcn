package com.minecraftmc22.expenses.data.webdav

import android.util.Base64
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal WebDAV access: the sync document is a single file that is read with GET and written
 * with PUT. Plain `HttpURLConnection` is enough for that, so no HTTP client was added.
 *
 * The target folder has to exist already; the server is expected to accept HTTP Basic auth,
 * which covers Nextcloud, ownCloud and friends with an app password.
 */
class WebDavClient(
    private val baseUrl: String,
    private val userName: String,
    private val password: String
) {

    /** Returns the document, or null when the server does not have it yet. */
    fun download(): String? {
        val connection = open(GET)

        return try {
            when (connection.responseCode) {
                HTTP_OK -> connection.inputStream.use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
                HTTP_NOT_FOUND -> null
                HTTP_UNAUTHORIZED -> throw IOException("The server rejected the credentials.")
                else -> throw IOException("The server answered ${connection.responseCode}.")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun upload(content: String) {
        val connection = open(PUT)
        connection.doOutput = true
        connection.setRequestProperty(CONTENT_TYPE_HEADER, JSON_MIME_TYPE)

        try {
            connection.outputStream.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            if (code !in HTTP_OK until HTTP_MULTIPLE_CHOICES) {
                throw IOException("The server answered $code.")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(method: String): HttpURLConnection {
        val connection = URL(documentUrl()).openConnection() as HttpURLConnection

        connection.requestMethod = method
        connection.connectTimeout = TIMEOUT
        connection.readTimeout = TIMEOUT
        connection.instanceFollowRedirects = true

        if (userName.isNotEmpty()) {
            val credentials = "$userName:$password".toByteArray(Charsets.UTF_8)
            val token = Base64.encodeToString(credentials, Base64.NO_WRAP)

            connection.setRequestProperty(AUTHORIZATION_HEADER, "Basic $token")
        }

        return connection
    }

    private fun documentUrl(): String {
        return "${baseUrl.trim().trimEnd('/')}/$FILE_NAME"
    }

    companion object {

        const val FILE_NAME = "expenses-sync.json"

        private const val GET = "GET"
        private const val PUT = "PUT"

        private const val TIMEOUT = 20000

        private const val HTTP_OK = 200
        private const val HTTP_MULTIPLE_CHOICES = 300
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_NOT_FOUND = 404

        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val CONTENT_TYPE_HEADER = "Content-Type"
        private const val JSON_MIME_TYPE = "application/json"
    }
}
