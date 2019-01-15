package com.example.voicerecognition.chatbot;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;

public class TTS implements TextToSpeech.OnInitListener {
    TextToSpeech textToSpeech;

    public TTS(Context context) {
        textToSpeech = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if(status != ERROR) {
            textToSpeech.setLanguage(Locale.KOREAN);
        }
    }

    public void speak(String speech) {
        textToSpeech.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void destroy() {
        textToSpeech.shutdown();
    }
}
