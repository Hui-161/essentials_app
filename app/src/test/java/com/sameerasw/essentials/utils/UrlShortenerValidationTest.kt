package com.sameerasw.essentials.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlShortenerValidationTest {
    @Test
    fun webAddressesFromTheClipboardAreRecognized() {
        assertTrue(UrlShortener.looksLikeWebUrl("https://example.com/a?b=c"))
        assertTrue(UrlShortener.looksLikeWebUrl("http://example.com"))
        assertTrue(UrlShortener.looksLikeWebUrl("example.com/path"))
        assertTrue(UrlShortener.looksLikeWebUrl("  sub.example.org  "))
    }

    @Test
    fun otherClipboardContentIsNotOffered() {
        assertFalse(UrlShortener.looksLikeWebUrl(null))
        assertFalse(UrlShortener.looksLikeWebUrl(""))
        assertFalse(UrlShortener.looksLikeWebUrl("name@example.com"))
        assertFalse(UrlShortener.looksLikeWebUrl("https://user:secret@example.com"))
        assertFalse(UrlShortener.looksLikeWebUrl("one two.three"))
        assertFalse(UrlShortener.looksLikeWebUrl("123456"))
        assertFalse(UrlShortener.looksLikeWebUrl("ftp://example.com"))
        assertFalse(UrlShortener.looksLikeWebUrl("example."))
    }

    @Test
    fun shortenerDomainMustBeAnHttpsOrigin() {
        assertTrue(UrlShortener.isValidDomain(UrlShortener.DEFAULT_DOMAIN))
        assertTrue(UrlShortener.isValidDomain("https://short.example.com/"))
        assertTrue(UrlShortener.isValidDomain("https://short.example.com:8443"))
        assertFalse(UrlShortener.isValidDomain("http://short.example.com"))
        assertFalse(UrlShortener.isValidDomain("short.example.com"))
        assertFalse(UrlShortener.isValidDomain("https://"))
        assertFalse(UrlShortener.isValidDomain("https://user@short.example.com"))
        assertFalse(UrlShortener.isValidDomain("https://short.example.com/api?x=1"))
        assertFalse(UrlShortener.isValidDomain(null))
    }
}
