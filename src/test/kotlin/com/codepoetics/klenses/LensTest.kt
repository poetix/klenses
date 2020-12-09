package com.codepoetics.klenses

import org.junit.Assert.*
import org.junit.Test

class LensTest {

    @Test
    fun basicExample() {
        data class Inner(val value: String)
        data class Outer(val outerValue: String, val inner: Inner?)

        val foo = Outer("foo", null)

        val outerValueLens = +Outer::outerValue
        val innerLens = Outer::inner orElse Inner("xyzzy")
        val innerValueLens = innerLens + Inner::value

        assertEquals("foo", outerValueLens(foo))
        assertEquals(Outer("quux", null), outerValueLens(foo, "quux"))
        assertEquals(Inner("xyzzy"), innerLens(foo))
        assertEquals(Outer("foo", Inner("frobnitz")), innerValueLens(foo, "frobnitz"))
        assertEquals(Outer("foo", Inner("XYZZY")), innerValueLens(foo) { toUpperCase() })
    }

    @Test
    fun lensesCompose(): Unit {
        data class Inner(val value: String)
        data class Outer(val outerValue: String, val inner: Inner?)

        val foo = Outer("foo", null)

        val innerLens = Outer::inner orElse Inner("xyzzy")
        val innerValueLens = innerLens + Inner::value

        assertEquals("foo", (Outer::outerValue)(foo))
        assertEquals(Outer("quux", null), Outer::outerValue.set(foo, "quux"))
        assertEquals(Inner("xyzzy"), innerLens(foo))
        assertEquals(Outer("foo", Inner("frobnitz")), innerValueLens(foo, "frobnitz"))
    }

    @Test
    fun extraProperties(): Unit {
        data class Foo(val value: String) {
            val uppercased = value.toUpperCase()
        }

        val foo = Foo("foo")
        assertEquals("BAR", Foo::value.set(foo, "bar").uppercased)

        try {
            val illegal = +Foo::uppercased
            fail("Illegal lens creation should throw an exception")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("not used in constructor"))
        }
    }

    @Test
    fun implicitConversion(): Unit {
        data class Inner(val value: String)
        data class Outer(val outerValue: String, val inner: Inner)

        val foo = Outer("foo", Inner("bar"))

        val propertyToLens: Lens<Outer, String> = +Outer::outerValue
        val propertyPlusProperty: Lens<Outer, String> = Outer::inner + Inner::value
        val lensPlusProperty: Lens<Outer, String> = +Outer::inner + Inner::value
        val propertyPlusLens: Lens<Outer, String> = Outer::inner + +Inner::value

        assertEquals("foo", Outer::outerValue.get(foo))
        assertEquals(Outer("baz", Inner("bar")), Outer::outerValue.set(foo, "baz"))
    }

    @Test
    fun updating(): Unit {
        data class Inner(val value: Int)
        data class Outer(val outerValue: String, val inner: Inner)

        val foo = Outer("foo", Inner(23))

        val innerValueLens = Outer::inner + Inner::value

        assertEquals(Outer("foo", Inner(46)), innerValueLens(foo) { times(2) })
    }

    @Test
    fun propertyMapper(): Unit {
        data class Inner(val value: Int)
        data class Outer(val outerValue: String, val inner: Inner)

        val foo = Outer("foo", Inner(23))

        val mapper = PropertyMapper.forKClass(Outer::class)

        val outerSetter = mapper.setterFor(Outer::outerValue)

        assertEquals(
            Outer("newFoo", Inner(23)),
            outerSetter(foo, "newFoo")
        )

        assertEquals(
            Outer("anotherFoo", Inner(23)),
            mapper.copy(foo, Outer::outerValue, "anotherFoo")
        )
    }

    @Test
    fun mapping() {
        data class Inner(val value: Int)
        data class Outer(val outerValue: String, val inners: List<Inner>)

        val foo = Outer("foo", listOf(Inner(1), Inner(2)))

        val innerLens = Outer::inners.lens()
        val innerValueLens = Inner::value.lens()

        val newOuter = innerLens.map(foo) {
            innerValueLens(this) { times(2) }
        }

        assertEquals(Outer("foo", listOf(Inner(2), Inner(4))), newOuter)
    }

}
