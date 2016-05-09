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
    fun update(t: T, update: V.() -> V): T = set(t, get(t).update())

    operator fun <V2> mod(iso: Iso<V, V2>): Lens<T, V2> = Lens.of(
            { t -> iso.arrow(get(t)) },
            { t, v2 -> set(t, iso.opArrow(v2)) }
    )

    operator fun invoke(t: T): V = get(t)
    operator fun invoke(t: T, v: V): T = set(t, v)
    operator fun invoke(t: T, update: V.() -> V): T = update(t, update)

    operator fun <V1> plus(next: Lens<V, V1>): Lens<T, V1> = Lens.of(
            { t -> next.get(get(t)) },
            { t, v -> set(t, next.set(get(t), v)) }
    )
}

// property.lens() = lens
inline fun <reified T : Any, V> KProperty1<T, V>.lens(): Lens<T, V> = Lens.of(
        this,
        PropertyMapper.forKClass(T::class).setterFor(this))

// +property = lens
inline operator fun <reified T : Any, V> KProperty1<T, V>.unaryPlus(): Lens<T, V> = this.lens()

// Lens + property = lens
inline operator fun <reified T : Any, reified V : Any, V1> Lens<T, V>.plus(next: KProperty1<V, V1>): Lens<T, V1> =
        this + next.lens()

// Property + property = lens
inline operator fun <reified T : Any, reified V : Any, V1> KProperty1<T, V>.plus(next: KProperty1<V, V1>): Lens<T, V1> =
        this.lens() + next.lens()

// Property + lens = lens
inline operator fun <reified T : Any, reified V : Any, V1> KProperty1<T, V>.plus(next: Lens<V, V1>): Lens<T, V1> =
        this.lens() + next

// Default value for nullable lens
infix fun <T, V> Lens<T, V?>.orElse(default: V): Lens<T, V> = Lens.of(
    { t ->  this.get(t) ?: default },
    { t, v ->  this.set(t, v) })

// Default value for nullable property
inline infix fun <reified T : Any, V> KProperty1<T, V?>.orElse(default: V): Lens<T, V> = this.lens() orElse default

// Modulo for property
inline operator fun <reified T : Any, V, V1> KProperty1<T, V>.mod(iso: Iso<V, V1>): Lens<T, V1> = this.lens() % iso

// Property.get
inline fun <reified T : Any, V> KProperty1<T, V>.get(t: T): V = this.lens().get(t)
inline operator fun <reified T : Any, V> KProperty1<T, V>.invoke(t: T): V = this.lens()(t)

// Property.set
inline fun <reified T : Any, V> KProperty1<T, V>.set(t: T, v: V): T = this.lens().set(t, v)
inline operator fun <reified T : Any, V> KProperty1<T, V>.invoke(t: T, v: V): T = this.lens()(t, v)

// Property.update
inline fun <reified T : Any, V> KProperty1<T, V>.update(t: T, noinline update: V.() -> V): T = this.lens().update(t, update)
inline operator fun <reified T : Any, V> KProperty1<T, V>.invoke(t: T, noinline update: V.() -> V): T = this.lens()(t, update)

class PropertyMapper<T : Any>(
        val constructor: KFunction<T>,
        val members: Array<KCallable<*>>,
        val updatableMembers: Set<KCallable<*>>) {

    companion object {
        private val cache: ConcurrentMap<KClass<*>, PropertyMapper<*>> = ConcurrentHashMap()

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> forKClass(kclass: KClass<T>): PropertyMapper<T> =
            cache.computeIfAbsent(kclass, { forKClassUncached(it) }) as PropertyMapper<T>

        private fun <T : Any >forKClassUncached(kclass: KClass<T>): PropertyMapper<T> {
            val propertiesByName = kclass.members.filter { it is KProperty1<*, *> }
                .map { it.name to it }
                .toMap()
            val propertyMappableConstructors = kclass.constructors.filter {
                propertiesByName.keys.containsAll(it.parameters.map { it.name!! }) }
            val constructor = propertyMappableConstructors.sortedBy { 0 - it.parameters.size }.first()
            val members = constructor.parameters.map { propertiesByName[it.name!!]!! }.toTypedArray()
            return PropertyMapper(constructor, members, members.toHashSet())
        }
    }

    fun <V> copy(source: T, property: KCallable<*>, value: V): T = constructor.call(*members.map {
        if (it == property) value else it.call(source)
    }.toTypedArray())

    fun <V> setterFor(property: KProperty1<T, V>): (T, V) -> T =
            if (updatableMembers.contains(property)) { t, v -> copy(t, property, v) }
            else throw IllegalArgumentException("Property $property not used in constructor $constructor")
}
