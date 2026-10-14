package com.example.dominograndprix.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Fecha civil (año, mes, día) sin dependencias de plataforma.
 *
 * No se usa `java.time.LocalDate` a proposito: exige API 26 en Android y este proyecto tiene
 * minSdk 24, asi que habria que activar el desugaring de librerias (una dependencia y
 * configuracion de build extra) solo para calcular un numero de dia. Con aritmetica entera se
 * consigue lo mismo, funciona en cualquier version y se puede probar en un test unitario puro.
 */
@Serializable(with = CalendarDateSerializer::class)
data class CalendarDate(val year: Int, val month: Int, val day: Int) {

    /**
     * Numero de dia consecutivo desde el 1 de enero del año 1 (el "dia juliano" de toda la
     * vida, en version corta). Dos fechas iguales dan el mismo numero, y fechas consecutivas
     * dan numeros consecutivos: es lo que hace falta para una semilla diaria.
     */
    val dayNumber: Int
        get() {
            val a = (14 - month) / 12
            val y = year + 4800 - a
            val m = month + 12 * a - 3
            return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        }

    /** Representacion estable para mostrar y para depurar: 2026-03-14. */
    override fun toString(): String =
        "%04d-%02d-%02d".format(year, month, day)

    companion object {
        /**
         * Fecha de hoy segun el reloj del dispositivo, en hora LOCAL.
         *
         * Se usa [java.util.Calendar] porque es lo unico disponible desde API 1; queda aqui
         * encapsulado para que el resto del codigo trabaje con [CalendarDate] y se pueda probar
         * sin tocar el reloj.
         */
        fun today(): CalendarDate {
            val calendar = java.util.Calendar.getInstance()
            return CalendarDate(
                year = calendar.get(java.util.Calendar.YEAR),
                month = calendar.get(java.util.Calendar.MONTH) + 1,
                day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }
    }
}

/**
 * Serializa la fecha como tres campos con nombre en lugar de como un entero opaco, para que el
 * JSON guardado se pueda leer y depurar a simple vista.
 */
object CalendarDateSerializer : KSerializer<CalendarDate> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CalendarDate") {
        element<Int>("year")
        element<Int>("month")
        element<Int>("day")
    }

    override fun serialize(encoder: Encoder, value: CalendarDate) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeIntElement(descriptor, 0, value.year)
        composite.encodeIntElement(descriptor, 1, value.month)
        composite.encodeIntElement(descriptor, 2, value.day)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): CalendarDate {
        val composite = decoder.beginStructure(descriptor)
        var year = 0
        var month = 1
        var day = 1
        // Se leen los tres campos por indice, admitiendo que lleguen en cualquier orden.
        while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> year = composite.decodeIntElement(descriptor, 0)
                1 -> month = composite.decodeIntElement(descriptor, 1)
                2 -> day = composite.decodeIntElement(descriptor, 2)
                else -> throw kotlinx.serialization.SerializationException("Campo inesperado: $index")
            }
        }
        composite.endStructure(descriptor)
        return CalendarDate(year, month, day)
    }
}
