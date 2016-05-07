package com.codepoetics.klenses

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1

class Iso<I, O>(val arrow: (I) -> O, val opArrow: (O) -> I) {
    val op: Iso<O, I> by lazy { Iso(opArrow, arrow) }
}

interface Lens<T, V> {

    companion object {
        fun <T, V> of(getter: (T) -> V, setter: (T, V) -> T): Lens<T, V> = object : Lens<T, V> {
            override fun get(t: T): V = getter(t)
            override fun set(t: T, v: V): T = setter(t, v)
        }
    }

    fun get(t: T): V
    fun set(t: T, v: V): T

    fun <V2> map(iso: Iso<V, V2>): Lens<T, V2> = Lens.of(
            { t -> iso.arrow(get(t)) },
            { t, v2 -> set(t, iso.opArrow(v2)) }
    )

    operator fun invoke(t: T): V = get(t)
    operator fun invoke(t: T, v: V): T = set(t, v)

    operator fun <V1> plus(next: Lens<V, V1>): Lens<T, V1> = Lens.of(
            { t -> next.get(this@Lens.get(t)) },
            { t, v -> this@Lens.set(t, next.set(this@Lens.get(t), v)) }
    )
}

infix fun <T, V> Lens<T, V?>.orElse(default: V): Lens<T, V> = Lens.of(
    { t ->  this.get(t) ?: default },
    { t, v ->  this.set(t, v) })

class PropertyMapper<T : Any>(
        val constructor: KFunction<T>,
        val members: Array<KCallable<*>>) {

    companion object {
        private val cache: ConcurrentMap<KClass<*>, PropertyMapper<*>> = ConcurrentHashMap()

        fun <T : Any> forKClass(kclass: KClass<T>): PropertyMapper<T> =
            cache.computeIfAbsent(kclass, { forKClassUncached(it) }) as PropertyMapper<T>

        private fun <T : Any >forKClassUncached(kclass: KClass<T>): PropertyMapper<T> {
            val propertiesByName = kclass.members.filter { it is KProperty1<*, *> }
                .map { it.name to it }
                .toMap()
            val constructor = kclass.constructors.find { it.parameters.size == propertiesByName.size }!!
            val members = constructor.parameters.map { propertiesByName[it.name!!]!! }.toTypedArray()
            return PropertyMapper(constructor, members)
        }
    }

    fun <V> copy(source: T, property: KCallable<*>, value: V): T = constructor.call(*members.map {
        if (it == property) value else it.call(source)
    }.toTypedArray())

    fun <V> setterFor(property: KProperty1<T, V>): (T, V) -> T = { t, v -> copy(t, property, v) }
}

inline fun <reified T : Any, V> KProperty1<T, V>.lens(): Lens<T, V> = Lens.of(
        this,
        PropertyMapper.forKClass(T::class).setterFor(this))
