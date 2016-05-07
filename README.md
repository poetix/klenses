# klenses
Lenses for Kotlin.

[![Maven Central](https://img.shields.io/maven-central/v/com.codepoetics/klenses.svg)](http://search.maven.org/#search%7Cga%7C1%7Cklenses)
[![Build Status](https://travis-ci.org/poetix/klenses.svg?branch=master)](https://travis-ci.org/poetix/klenses)

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
```
