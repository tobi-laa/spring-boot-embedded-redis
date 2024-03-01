package io.github.tobi.laa.spring.boot.embedded.redis

import org.apache.commons.validator.routines.InetAddressValidator

internal fun createAddress(host: String, port: Int): String {
    return if (InetAddressValidator.getInstance().isValidInet6Address((host))) {
        "[$host]:$port"
    } else {
        "$host:$port"
    }
}