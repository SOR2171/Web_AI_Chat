package com.github.sor2171.backend.entity

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

interface DataCopy {

    fun <T : Any> toViewObject(
        voClass: KClass<T>,
        otherProperties: Map<String, Any>
    ): T {
        try {
            val voProperties = voClass.declaredMemberProperties
            val vo = voClass.createInstance()
            for (voProperty in voProperties)
                copyFieldData(voProperty, otherProperties, vo)
            return vo
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(
                "${voClass.simpleName} has no fields.",
                e
            )
        }
    }

    private fun copyFieldData(
        voProperty: KProperty<*>,
        otherProperties: Map<String, Any>,
        vo: Any
    ) {
        try {
            val dtoProperty = this::class.memberProperties
                .firstOrNull { it.name == voProperty.name }
            val value = dtoProperty?.getter?.call(this)
                ?: otherProperties[voProperty.name]
                ?: throw RuntimeException(
                    "Property ${voProperty.name} not found in DTO or Map."
                )
            val field = voProperty.javaField!!
            field.isAccessible = true
            field.set(vo, value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}