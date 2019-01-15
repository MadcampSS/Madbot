package com.example.voicerecognition.chatbot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.voicerecognition.MainActivity;

import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;

public class ChatBot {

    public static final String TAG = "ChatBot";

    private Context mContext;
    private AIDataService aiDataService;
    private AIRequest aiRequest;
    private TTS tts;

    public ChatBot(Context context) {
        mContext = context;

        final AIConfiguration config = new AIConfiguration("8aa82b7886364fe998410f9aa4bd4b6b",
                AIConfiguration.SupportedLanguages.Korean, AIConfiguration.RecognitionEngine.System);

        aiDataService = new AIDataService(config);

        aiRequest = new AIRequest();

        tts = new TTS(context);

    }

    public ChatBot setQuery(String query) {
        aiRequest.setQuery(query);
        return this;
    }

    @SuppressLint("StaticFieldLeak")
    public void requestTask() {
        new AsyncTask<AIRequest, Void, AIResponse>() {
            @Override
            protected AIResponse doInBackground(AIRequest... requests) {
                final AIRequest request = requests[0];
                try {
                    final AIResponse response = aiDataService.request(aiRequest);
                    return response;
                } catch (AIServiceException e) {
                }
                return null;
            }
            @Override
            protected void onPostExecute(AIResponse aiResponse) {
                if (aiResponse != null) {
                    // process aiResponse here
                    String speech = aiResponse.getResult().getFulfillment().getSpeech();
                    MainActivity.chatArrayAdapter.add("매드봇: " + speech);
                    Log.i(TAG, speech);
                    tts.speak(speech);
                }
            }
        }.execute(aiRequest);
    }

}
