package com.codepoetics.oktarine

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface Arrow<I, O> {
    fun arrow(i: I): O
}

interface OpArrow<I, O> {
    fun opArrow(o: O): I
}

interface Iso<I, O> : Arrow<I, O>, OpArrow<I, O> {

    companion object {
        fun <I, O> of(arrow: Arrow<I, O>, opArrow: OpArrow<I, O>): Iso<I, O> =
                object : Arrow<I, O> by arrow, OpArrow<I, O> by opArrow, Iso<I, O>
    }
}

interface Lens<T, V> {
    fun get(t: T): V
    fun set(t: T, v: V): T

    operator fun invoke(t: T): V = get(t)
    operator fun invoke(t: T, v: V): T = set(t, v)

    operator fun <V1> plus(next: Lens<V, V1>): Lens<T, V1> = object : Lens<T, V1> {
        override fun get(t: T): V1 = next.get(this@Lens.get(t))

        override fun set(t: T, v: V1): T = this@Lens.set(t, next.set(this@Lens.get(t), v))
    }
}

infix fun <T, V> Lens<T, V?>.orElse(default: V): Lens<T, V> {
    val self = this
    return object : Lens<T, V> {
        override fun get(t: T): V = self.get(t) ?: default
        override fun set(t: T, v: V): T = self.set(t, v)
    }
}

data class KPropertyLens<T : Any, V>(val kclass: KClass<T>, val property: KProperty1<T, V>) : Lens<T, V> {
    override fun set(t: T, v: V): T {
        val propertyValues = kclass.members.filter { it is KProperty1<*, *> }
                .map { if (it.name.equals(property.name)) it.name to v else it.name to it.call(t) }
                .toMap()
        val constructor = kclass.constructors.find { it.parameters.size == propertyValues.size }!!
        val args = constructor.parameters.map { propertyValues[it.name] }.toTypedArray()
        return constructor.call(*args)
    }

    override fun get(t: T): V = property.get(t)
}

inline fun <reified T : Any, V> KProperty1<T, V>.lens() = KPropertyLens(T::class, this)

data class Inner(val ping: String)
data class Foo(val bar: String, val baz: Inner?)

fun main(argv: Array<String>): Unit {
    val foo = Foo("baz", null)

    val barLens = Foo::bar.lens()
    val bazLens = Foo::baz.lens() orElse Inner("pang")
    val pingLens = Inner::ping.lens()
    val bazPingLens = bazLens + pingLens

    val foo2 = barLens(foo, "quux")
    val foo3 = bazPingLens(foo2, "pong")

    println(bazLens(foo))
    println(foo)
    println(foo2)
    println(foo3)
}
