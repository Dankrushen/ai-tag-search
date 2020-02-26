package net.dankrushen.aitagsearch.conversion

import net.dankrushen.aitagsearch.conversion.basetypes.StringConverter
import org.agrona.DirectBuffer
import org.agrona.MutableDirectBuffer
import java.util.*

// Fake constructor to automatically provide a base array
@Suppress("FunctionName")
inline fun <reified T> ArrayConverter(converter: DirectBufferConverter<T>, valueLength: Int? = null) = ArrayConverter(converter, valueLength, emptyArray())

class ArrayConverter<T>(val converter: DirectBufferConverter<T>, val valueLength: Int? = null, private val baseArray: Array<T>) : DirectBufferConverter<Array<T>>() {
    companion object {
        val stringArrayConverter = ArrayConverter(StringConverter.converter)
    }

    override fun getLength(value: Array<T>): Int = value.size

    override fun getSize(value: Array<T>): Int {
        return if (valueLength != null) {
            value.sumBy { entry -> converter.getSize(entry) }
        } else {
            value.sumBy { entry -> converter.getSizeWithCount(entry) }
        }
    }

    override fun writeWithoutLength(directBuffer: MutableDirectBuffer, index: Int, value: Array<T>): Int {
        var bytesWritten = 0

        if (valueLength != null) {
            for (entry in value) {
                val curIndex = index + bytesWritten
                bytesWritten += converter.writeWithoutLength(directBuffer, curIndex, entry)
            }
        } else {
            for (entry in value) {
                val curIndex = index + bytesWritten
                bytesWritten += converter.write(directBuffer, curIndex, entry)
            }
        }

        return bytesWritten
    }

    override fun readWithoutLengthCount(directBuffer: DirectBuffer, index: Int, length: Int): Pair<Array<T>, Int> {
        var bytesRead = 0

        val entryList = ArrayList<T>(length)

        if (valueLength != null) {
            for (i in 0 until length) {
                val curIndex = index + bytesRead
                val results = converter.readWithoutLengthCount(directBuffer, curIndex, valueLength)

                entryList.add(results.first)
                bytesRead += results.second
            }
        } else {
            for (i in 0 until length) {
                val curIndex = index + bytesRead
                val results = converter.readCount(directBuffer, curIndex)

                entryList.add(results.first)
                bytesRead += results.second
            }
        }

        return Pair(entryList.toArray(baseArray), bytesRead)
    }

    override fun readWithoutLength(directBuffer: DirectBuffer, index: Int, length: Int): Array<T> {
        var bytesRead = 0

        val entryList = ArrayList<T>(length)

        if (valueLength != null) {
            for (i in 0 until length) {
                val curIndex = index + bytesRead
                val results = converter.readWithoutLengthCount(directBuffer, curIndex, valueLength)

                entryList.add(results.first)
                bytesRead += results.second
            }
        } else {
            for (i in 0 until length) {
                val curIndex = index + bytesRead
                val results = converter.readCount(directBuffer, curIndex)

                entryList.add(results.first)
                bytesRead += results.second
            }
        }

        return entryList.toArray(baseArray)
    }
}