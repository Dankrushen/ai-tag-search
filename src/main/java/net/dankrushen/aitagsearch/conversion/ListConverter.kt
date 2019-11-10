package net.dankrushen.aitagsearch.conversion

import org.agrona.DirectBuffer
import org.agrona.MutableDirectBuffer

class ListConverter<T>(val converter: DirectBufferConverter<T>) : DirectBufferConverter<List<T>>() {
    companion object {
        val floatListConverter = ListConverter(FloatConverter.converter)
        val intListConverter = ListConverter(IntConverter.converter)
        val stringListConverter = ListConverter(StringConverter.converter)
    }

    override fun getLength(value: List<T>): Int {
        return value.size
    }

    override fun getSize(value: List<T>): Int {
        return value.sumBy { string -> converter.getSizeWithCount(string) }
    }

    override fun writeWithoutLength(directBuffer: MutableDirectBuffer, index: Int, value: List<T>): Int {
        var bytesWritten = 0

        for (entry in value) {
            bytesWritten += converter.write(directBuffer, index + bytesWritten, entry)
        }

        return bytesWritten
    }

    override fun readWithoutLengthCount(directBuffer: DirectBuffer, index: Int, length: Int): Pair<MutableList<T>, Int> {
        var bytesRead = 0

        val entryList = mutableListOf<T>()

        for (i in 0 until length) {
            val results = converter.readCount(directBuffer, index + bytesRead)
            entryList.add(results.first)
            bytesRead += results.second
        }

        return Pair(entryList, bytesRead)
    }

    override fun readWithoutLength(directBuffer: DirectBuffer, index: Int, length: Int): MutableList<T> {
        var bytesRead = 0

        val entryList = mutableListOf<T>()

        for (i in 0 until length) {
            val results = converter.readCount(directBuffer, index + bytesRead)
            entryList.add(results.first)
            bytesRead += results.second
        }

        return entryList
    }
}