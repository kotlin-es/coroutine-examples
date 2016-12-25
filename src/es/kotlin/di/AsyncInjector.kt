package es.kotlin.di

import es.kotlin.async.Promise
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.await

@Target(AnnotationTarget.CLASS)
annotation class Prototype

@Target(AnnotationTarget.CLASS)
annotation class Singleton

class AsyncInjector {
	private val instances = hashMapOf<Class<*>, Any?>()

	suspend inline fun <reified T : Any> get() = await(getAsync(T::class.java))

	inline fun <reified T : Any> getAsync(): Promise<T> = getAsync(T::class.java)
	inline fun <reified T : Any> map(instance: T): AsyncInjector = map(T::class.java, instance)

	init {
		map<AsyncInjector>(this)
	}

	fun <T : Any?> map(clazz: Class<T>, instance: T): AsyncInjector {
		instances[clazz] = instance as Any
		return this
	}

	@Suppress("UNCHECKED_CAST")
	fun <T : Any?> getAsync(clazz: Class<T>): Promise<T> = async {
		if (instances.containsKey(clazz) || clazz.getAnnotation(Singleton::class.java) != null) {
			if (!instances.containsKey(clazz)) {
				val instance = await(createAsync(clazz))
				instances[clazz] = instance
			}
			instances[clazz]!! as T
		} else {
			await(createAsync(clazz))
		}
	}

	@Suppress("UNCHECKED_CAST")
	fun <T : Any?> createAsync(clazz: Class<T>): Promise<T> = async {
		val constructor = clazz.declaredConstructors.first()
		val promises = arrayListOf<Promise<out Any>>()
		val out = arrayListOf<Any>()
		for (paramType in constructor.parameterTypes) {
			promises += getAsync(paramType)
		}
		for (prom in promises) {
			out += await(prom)
		}
		val instance = constructor.newInstance(*out.toTypedArray()) as T
		if (instance is AsyncDependency) {
			try {
				instance.init()
			} catch (e: Throwable) {
				println("AsyncInjector (${e.message}):")
				e.printStackTrace()
				throw e
			}
		}
		instance
	}
}

interface AsyncDependency {
	suspend fun init(): Unit
}