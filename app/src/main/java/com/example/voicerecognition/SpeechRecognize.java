package com.example.voicerecognition;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class SpeechRecognize implements RecognitionListener {

    private static final String TAG = "SpeechRecognize";

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private ChatBot chatBot;
    private EporConnection eporConnection;

    public static final int RESULT_SPEECH = 1000; // ReqCode for onActivityResult

    public SpeechRecognize(Activity activity, EporConnection eporConnection) {
        this.eporConnection = eporConnection;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        speechRecognizer.setRecognitionListener(this);
        chatBot = new ChatBot(activity.getApplicationContext());

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR"); //언어지정입니다.
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);   //검색을 말한 결과를 보여주는 갯수
    }

    public void startListen() {
        speechRecognizer.startListening(recognizerIntent);
    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {

    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        String resultOne = "인사";
        if (matches != null) {
            resultOne = matches.get(0);
            Log.e(TAG, "onResults text: " + resultOne);
            MainActivity.chatArrayAdapter.add("나:  " + resultOne);
            chatBot.setQuery(resultOne).requestTask();
            eporConnection.textToCommand(resultOne);
        }


//        for(int i = 0; i < matches.size() ; i++){
//            String result = matches.get(i);
//            Log.e("GoogleActivity", "onResults text : " + result);
//            chatBot.setQuery(result).requestTask();
//        }


    }

    @Override
    public void onError(int errorCode) {

        String message;

        switch (errorCode) {

            case SpeechRecognizer.ERROR_AUDIO:
                message = "오디오 에러";
                break;

            case SpeechRecognizer.ERROR_CLIENT:
                message = "클라이언트 에러";
                break;

            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "퍼미션없음";
                break;

            case SpeechRecognizer.ERROR_NETWORK:
                message = "네트워크 에러";
                break;

            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "네트웍 타임아웃";
                break;

            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "찾을수 없음";
                break;

            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "바쁘대";
                break;

            case SpeechRecognizer.ERROR_SERVER:
                message = "서버이상";
                break;

            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "말하는 시간초과";
                break;

            default:
                message = "알수없음";
                break;
        }

        Log.e("GoogleActivity", "SPEECH ERROR : " + message);
    }

    @Override
    public void onRmsChanged(float v) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }

    @Override
    public void onPartialResults(Bundle bundle) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {

    }
}


