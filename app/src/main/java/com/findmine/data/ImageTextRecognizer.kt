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
    // 이 클래스는 사진 한 장을 받아서 텍스트 OCR, 이미지 라벨링, 메모 파싱까지 한 번에 수행한다.
    // ML Kit 클라이언트는 처음 사용할 때만 생성해서 앱 시작 속도를 늦추지 않도록 한다.
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
        // 1. URI 문자열을 실제 이미지로 읽는다.
        // 2. 영어/숫자 계열 OCR과 한국어 OCR을 둘 다 실행해 누락을 줄인다.
        // 3. 이미지 라벨도 함께 추출해서, 사진 안에 글자가 적어도 메모 후보를 만들 수 있게 한다.
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
        // 같은 문장이 두 OCR 엔진에서 중복으로 나오면 검색 점수와 파싱 결과가 치우칠 수 있다.
        // 그래서 줄 단위로 공백을 정리하고 중복을 제거한 뒤 하나의 OCR 텍스트로 합친다.
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
