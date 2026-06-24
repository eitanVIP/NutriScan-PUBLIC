package com.eitangrimblat.nutriscan.firebase;

import android.graphics.Bitmap;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.Part;
import com.google.firebase.ai.type.PublicPreviewAPI;
import com.google.firebase.ai.type.ResponseModality;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Utility class for interacting with Firebase AI (Gemini) for text and image generation.
 */
@PublicPreviewAPI
public class AI {
    private static final GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
        .generativeModel(
            "gemini-2.5-flash",
            new GenerationConfig.Builder()
                .setResponseMimeType("application/json")
                .build()
        );
    private static final GenerativeModelFutures model = GenerativeModelFutures.from(ai);
    private static final GenerativeModel imageAI = FirebaseAI.getInstance(GenerativeBackend.googleAI())
        .generativeModel(
            "gemini-2.5-flash-image",
            new GenerationConfig.Builder()
                .setResponseModalities(List.of(ResponseModality.IMAGE))
                .build()
        );
    private static final GenerativeModelFutures imageModel = GenerativeModelFutures.from(imageAI);
    private static final Executor executor = Executors.newSingleThreadExecutor();

    /**
     * Sends a raw content request to the text-based AI model.
     *
     * @param prompt             The content prompt to process.
     * @param onResponseListener Callback for the full AI response.
     */
    public static void sendRequest(Content prompt, OnCompleteRunnable<GenerateContentResponse> onResponseListener) {
        ListenableFuture<GenerateContentResponse> response = model.generateContent(prompt);
        Futures.addCallback(response, new FutureCallback<>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                onResponseListener.onComplete(result, true, null);
            }

            @Override
            public void onFailure(Throwable t) {
                onResponseListener.onComplete(null, false, t.getMessage());
            }
        }, executor);
    }

    /**
     * Sends a simple text message to the AI and receives a text response.
     *
     * @param message            The text message/prompt.
     * @param onResponseListener Callback for the response text.
     */
    public static void sendMessage(String message, OnCompleteRunnable<String> onResponseListener) {
        Content prompt = new Content.Builder().addText(message).build();
        sendRequest(prompt, (generateContentResponse, success, exception) -> {
            if (success)
                onResponseListener.onComplete(generateContentResponse.getText(), true, null);
            else
                onResponseListener.onComplete(null, false, exception);
        });
    }

    /**
     * Requests an image from the vision-based AI model.
     *
     * @param prompt             The descriptive prompt for the image.
     * @param onResponseListener Callback providing the generated Bitmap.
     */
    public static void sendRequestImage(String prompt, OnCompleteRunnable<Bitmap> onResponseListener) {
        Content p = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = imageModel.generateContent(p);

        Futures.addCallback(response, new FutureCallback<>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    Part part = result.getCandidates().get(0).getContent().getParts().get(0);

                    if (part instanceof com.google.firebase.ai.type.ImagePart) {
                        Bitmap bitmap = ((com.google.firebase.ai.type.ImagePart) part).getImage();
                        onResponseListener.onComplete(bitmap, true, null);
                    } else {
                        onResponseListener.onComplete(null, false, "AI did not return an image part.");
                    }
                } catch (Exception e) {
                    onResponseListener.onComplete(null, false, "Parsing error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                onResponseListener.onComplete(null, false, t.getMessage());
            }
        }, executor);
    }
}
