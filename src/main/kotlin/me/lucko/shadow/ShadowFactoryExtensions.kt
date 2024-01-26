package me.lucko.shadow

import me.lucko.shadow.ShadowingStrategy.Unwrapper

inline fun <reified T : Shadow> ShadowFactory.shadow(handle: Any): T {
    return shadow(T::class.java, handle)
}

inline fun <reified T : Shadow> ShadowFactory.staticShadow(): T {
    return staticShadow(T::class.java)
}

inline fun <reified T : Shadow> ShadowFactory.constructShadow(vararg args: Any): T {
    return constructShadow(T::class.java, args)
}

inline fun <reified T : Shadow> ShadowFactory.constructShadow(unwrapper: Unwrapper, vararg args: Any): T {
    return constructShadow(T::class.java, unwrapper, args)
}

inline fun <reified T> ShadowFactory.getTargetClass(): Class<*> {
    return getTargetClass(T::class.java)
}
