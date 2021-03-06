package net.dankrushen.aitagsearch.database

import net.dankrushen.aitagsearch.comparison.CosineDistance
import net.dankrushen.aitagsearch.conversion.FloatVectorConverter
import net.dankrushen.aitagsearch.conversion.basetypes.StringConverter
import net.dankrushen.aitagsearch.database.query.BruteNearestNeighbour
import net.dankrushen.aitagsearch.database.transaction.ThreadLocalTransactionGenerator
import net.dankrushen.aitagsearch.datatypes.FloatVector
import net.dankrushen.aitagsearch.extensions.getFloatVector
import org.agrona.DirectBuffer
import org.lmdbjava.Env
import java.io.File

fun <T> time(action: String, block: () -> T): Pair<T, Long> {
    println("$action...")

    val start = System.currentTimeMillis()
    val result = block.invoke()
    val end = System.currentTimeMillis()

    val time = end - start
    println("$action took $time ms")

    return Pair(result, time)
}

const val bytesInGb = 1073741824L

fun main() {
    val env = DatabaseUtils.createEnv(File("C:\\Users\\Dankrushen\\Desktop\\NewDatabase"), bytesInGb * 2, 1)

    env.use {
        val db = TypedPairDatabase(env, "Test", StringConverter.converter, FloatVectorConverter.converter, ThreadLocalTransactionGenerator(env))

        db.use {
            //testDatabase(env, db)
            //println()
            //testWritingToDatabase(env, db, 1600000)
            //println()
            testNearestNeighbour(env, db, 150)
        }
    }
}

fun testDatabase(env: Env<DirectBuffer>, db: TypedPairDatabase<String, FloatVector>) {
    val demoKeyVector = Pair("test", FloatVector.zeros(128))

    for (i in 0 until demoKeyVector.second.dimension) {
        demoKeyVector.second[i] = i
    }

    println("Original: ")
    println("\tKey: ${demoKeyVector.first}")
    println("\tVector: ${demoKeyVector.second}")
    println("\tEncoded Vector: ${FloatVectorConverter.converter.toDirectBuffer(0, demoKeyVector.second).getFloatVector(0)}")

    var testKeyVectorResult: Pair<String, FloatVector>? = null

    db.txnWrite {
        db.putPair(it, demoKeyVector)
        it.commit()
    }

    db.txnRead {
        testKeyVectorResult = db.getPair(it, demoKeyVector.first)
    }

    if (testKeyVectorResult == null)
        throw Exception("KeyVector not found in database")

    println()
    println("Fetched: ")
    println("\tKey: ${testKeyVectorResult?.first ?: "null"}")
    println("\tVector: ${testKeyVectorResult?.second?.toString() ?: "null"}")

    println()
    println("Original and Fetched Equal: ${testKeyVectorResult?.second == demoKeyVector.second}")
}

fun testWritingToDatabase(env: Env<DirectBuffer>, db: TypedPairDatabase<String, FloatVector>, numVectors: Int) {
    time("Generating and inserting $numVectors random key vectors") {
        var txn = env.txnWrite()

        for (keyVectorNum in 0 until numVectors) {
            val keyVector = Pair("test${keyVectorNum}", FloatVector.zeros(128))

            for (i in 0 until keyVector.second.dimension) {
                keyVector.second[i] = Math.random()
            }

            db.putPair(txn, keyVector)

            if (keyVectorNum % 100 == 0) {
                txn.commit()
                txn.close()

                txn = env.txnWrite()
            }
        }

        txn.commit()
        txn.close()
    }
}

fun testNearestNeighbour(env: Env<DirectBuffer>, db: TypedPairDatabase<String, FloatVector>, numNeighbours: Int, sampleCount: Int = 50) {
    val times = LongArray(sampleCount)

    val testVector = FloatVector.ones(128)
    val nearestNeighbour = BruteNearestNeighbour(db, CosineDistance.measurer)

    db.txnRead {
        for (i in 0 until sampleCount) {
            times[i] = time("Fetching $numNeighbours nearest neighbour(s)") {
                val neighbours = nearestNeighbour.getNeighbours(it, testVector, numNeighbours)

                for (keyVectorDist in neighbours) {
                    println(keyVectorDist)
                }
            }.second
        }
    }

    println("Fetching $numNeighbours nearest neighbours took on average ${times.average()} ms over $sampleCount samples with a min of ${times.min()} ms and max of ${times.max()} ms")
}