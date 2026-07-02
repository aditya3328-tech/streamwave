package com.example

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.nio.charset.StandardCharsets

/**
 * Minimal blocking HTTP/1.1 plaintext server for tests (no MockWebServer dependency).
 * Handler receives (requestTarget, cookieHeaderValue) and returns a [Resp].
 */
class LocalUpstream(
    private val handler: (path: String, cookieHeader: String) -> Resp
) {
    data class Resp(val code: Int, val headers: List<Pair<String, String>>, val body: String)

    private val server = ServerSocket(0)
    val port: Int get() = server.localPort
    @Volatile private var running = true

    fun start() {
        Thread {
            while (running) {
                try {
                    server.accept().use { socket ->
                        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                        val requestLine = reader.readLine() ?: return@use
                        val target = requestLine.split(" ").getOrElse(1) { "/" }
                        var cookie = ""
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            if (line.startsWith("Cookie:", ignoreCase = true)) {
                                cookie = line.substringAfter(":").trim()
                            }
                        }
                        write(socket.getOutputStream(), handler(target, cookie))
                    }
                } catch (e: Exception) {
                    if (running) throw e
                }
            }
        }.apply { isDaemon = true }.start()
    }

    private fun write(out: OutputStream, resp: Resp) {
        val bytes = resp.body.toByteArray(StandardCharsets.UTF_8)
        val sb = StringBuilder("HTTP/1.1 ${resp.code} OK\r\n")
        resp.headers.forEach { (n, v) -> sb.append("$n: $v\r\n") }
        sb.append("Content-Type: application/json\r\n")
        sb.append("Content-Length: ${bytes.size}\r\n")
        sb.append("Connection: close\r\n\r\n")
        out.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    fun stop() {
        running = false
        try { server.close() } catch (_: Exception) {}
    }
}
