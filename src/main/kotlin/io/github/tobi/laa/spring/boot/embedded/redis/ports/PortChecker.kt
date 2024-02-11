package io.github.tobi.laa.spring.boot.embedded.redis.ports

import java.net.InetAddress
import javax.net.ServerSocketFactory

/**
 * This object is used to check if a port is available.
 */
internal object PortChecker {

    /**
     * Checks if the given port is available on `localhost`.
     * @param port The port to check.
     * @return `true` if the port is available, `false` otherwise.
     */
    internal fun available(port: Int): Boolean {
        try {
            val serverSocket = ServerSocketFactory.getDefault()
                .createServerSocket(port, 1, InetAddress.getByName("localhost"))
            serverSocket.close()
            return true
        } catch (ex: Exception) {
            return false
        }
    }
}