package com.findmine.data

import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale

object OnDeviceSpeechInput {
    fun recognizerIntent(prompt: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
}
