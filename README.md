# klenses
Lenses for Kotlin.

[![Maven Central](https://img.shields.io/maven-central/v/com.codepoetics/klenses.svg)](http://search.maven.org/#search%7Cga%7C1%7Cklenses)
[![Build Status](https://travis-ci.org/poetix/klenses.svg?branch=master)](https://travis-ci.org/poetix/klenses)

Lenses are property references with some extra abilities: they can also be used to create a copy of an object with the property set to a different value, and they compose to form pointers into nested objects.

```kotlin
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
assertEquals(Outer("foo", Inner("XYZZY")), innerValueLens(foo) { toUpperCase() })
```
