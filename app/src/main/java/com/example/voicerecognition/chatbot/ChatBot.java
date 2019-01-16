package com.example.voicerecognition.chatbot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.example.voicerecognition.MainActivity;
import com.example.voicerecognition.epor.EporConnection;

import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

public class ChatBot {

    public static final String TAG = "ChatBot";

    private Context mContext;
    private AIDataService aiDataService;
    private AIRequest aiRequest;
    private TTS tts;
    private EporConnection eporConnection;

    public ChatBot(Context context, EporConnection eporConnection) {
        mContext = context;

        final AIConfiguration config = new AIConfiguration("8aa82b7886364fe998410f9aa4bd4b6b",
                AIConfiguration.SupportedLanguages.Korean, AIConfiguration.RecognitionEngine.System);

        aiDataService = new AIDataService(config);

        aiRequest = new AIRequest();

        tts = new TTS(context);

        this.eporConnection = eporConnection;

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
                    final Result result = aiResponse.getResult();
                    String speech = result.getFulfillment().getSpeech();
                    String intentName =  result.getMetadata().getIntentName();

                    Toast.makeText(mContext, intentName, Toast.LENGTH_SHORT).show();

                    eporConnection.sendCommand(intentName);

                    MainActivity.chatArrayAdapter.add("매드봇: " + speech);
                    Log.i(TAG, speech);
                    tts.speak(speech);
                }
            }
        }.execute(aiRequest);
    }

}
