package com.codepoetics.klenses

import org.junit.Assert.assertEquals
import org.junit.Test

class LensTest {

    @Test fun lensesCompose(): Unit {
        data class Inner(val value: String)
        data class Outer(val outerValue: String, val inner: Inner?)

        val foo = Outer("foo", null)

        val outerValueLens = Outer::outerValue.lens()
        val innerLens = Outer::inner.lens() orElse Inner("xyzzy")
        val valueLens = Inner::value.lens()
        val innerValueLens = innerLens + valueLens

        assertEquals("foo", outerValueLens(foo))
        assertEquals(Outer("quux", null), outerValueLens(foo, "quux"))
        assertEquals(Inner("xyzzy"), innerLens(foo))
        assertEquals(Outer("foo", Inner("frobnitz")), innerValueLens(foo, "frobnitz"))
    }
}
