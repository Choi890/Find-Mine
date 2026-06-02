package com.findmine.data

import kotlin.math.sqrt

object LocalTextEmbedding {
    private const val Dimensions = 96

    fun similarity(query: String, record: MemoryRecord): Float {
        val queryVector = vectorize(query)
        val recordVector = vectorize(
            listOf(record.itemName, record.location, record.note, record.tags)
                .joinToString(" "),
        )
        return cosine(queryVector, recordVector)
    }

    private fun vectorize(text: String): FloatArray {
        val normalized = MemoryTextParser.normalizeForEmbedding(text)
        val vector = FloatArray(Dimensions)

        if (normalized.isBlank()) return vector

        normalized
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .forEach { token ->
                addFeature(vector, token, 1.8f)
                token.windowed(2, 1, partialWindows = false)
                    .forEach { addFeature(vector, it, 1.0f) }
                token.windowed(3, 1, partialWindows = false)
                    .forEach { addFeature(vector, it, 1.3f) }
            }

        return vector
    }

    private fun addFeature(vector: FloatArray, feature: String, weight: Float) {
        val index = (feature.hashCode() and Int.MAX_VALUE) % vector.size
        vector[index] += weight
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var aNorm = 0f
        var bNorm = 0f

        for (index in a.indices) {
            dot += a[index] * b[index]
            aNorm += a[index] * a[index]
            bNorm += b[index] * b[index]
        }

        if (aNorm == 0f || bNorm == 0f) return 0f
        return dot / (sqrt(aNorm) * sqrt(bNorm))
    }
}
