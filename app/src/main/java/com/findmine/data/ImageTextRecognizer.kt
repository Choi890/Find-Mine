package com.findmine.data

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrAnalysis(
    val text: String,
    val parsed: ParsedMemoryText,
    val labels: List<ImageLabelCandidate> = emptyList(),
)

data class ImageLabelCandidate(
    val text: String,
    val confidence: Float,
)

class ImageTextRecognizer(
    private val context: Context,
) {
    // ML Kit clients are lazy so the app starts quickly and initializes models only when OCR is used.
    private val latinRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    private val koreanRecognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }
    private val imageLabeler by lazy {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.55f)
            .build()
        ImageLabeling.getClient(options)
    }

    suspend fun recognize(uriText: String): OcrAnalysis = withContext(Dispatchers.IO) {
        // Run Latin OCR, Korean OCR, and image labels together to give the parser richer context.
        val uri = Uri.parse(uriText)
        val image = InputImage.fromFilePath(context, uri)

        val latinText = latinRecognizer.process(image).await().text
        val koreanText = koreanRecognizer.process(image).await().text
        val labels = imageLabeler.process(image).await()
            .map { label ->
                ImageLabelCandidate(
                    text = label.text,
                    confidence = label.confidence,
                )
            }
            .sortedByDescending { it.confidence }
            .take(8)
        val mergedText = mergeRecognizedText(latinText, koreanText)
        val labelText = labels.joinToString(" ") { it.text }

        OcrAnalysis(
            text = mergedText,
            parsed = MemoryTextParser.parseOcrText("$mergedText\n$labelText"),
            labels = labels,
        )
    }

    private fun mergeRecognizedText(vararg texts: String): String =
        // Distinct trimmed lines prevent duplicated OCR blocks from biasing the memory parser.
        texts
            .flatMap { it.lines() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { error ->
            continuation.resumeWithException(error)
        }
    }
