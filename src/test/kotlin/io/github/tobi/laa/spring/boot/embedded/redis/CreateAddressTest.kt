package io.github.tobi.laa.spring.boot.embedded.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Tests for createAddress()")
internal class CreateAddressTest {

    @Test
    @DisplayName("createAddress() should return bracketed address with port if host is an IPv6 address")
    fun ipv6Address_createAddress_shouldReturnBracketedAddr() {
        val host = "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
        val port = 6379
        val expected = "[$host]:$port"
        val actual = createAddress(host, port)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    @DisplayName("createAddress() should return unbracketed address with port if host is an IPv4 address")
    fun ipv4Address_createAddress_shouldReturnAddr() {
        val host = "192.168.178.1"
        val port = 6379
        val expected = "$host:$port"
        val actual = createAddress(host, port)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    @DisplayName("createAddress() should return unbracketed address with port if host is a domain name")
    fun domainName_createAddress_shouldReturnAddr() {
        val host = "zombo.com"
        val port = 6379
        val expected = "$host:$port"
        val actual = createAddress(host, port)
        assertThat(actual).isEqualTo(expected)
    }

}