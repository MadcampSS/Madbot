package com.example.voicerecognition.realtime;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.nio.ByteBuffer;

public abstract class VisionProcessorBase<T> implements VisionImageProcessor {

    private ByteBuffer latestImage;

    private FrameMetadata latestImageMetaData;

    private ByteBuffer processingImage;

    private FrameMetadata processingMetaData;

    public VisionProcessorBase() {

    }

    @Override
    public synchronized void process(
            ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay
            graphicOverlay) {
        latestImage = data;
        latestImageMetaData = frameMetadata;
        if (processingImage == null && processingMetaData == null) {
            processLatestImage(graphicOverlay);
        }

    }

    @Override
    public synchronized void process(
            Bitmap bitmap, final GraphicOverlay
            graphicOverlay) {
        detectInVisionImage(null, FirebaseVisionImage.fromBitmap(bitmap), null,
                graphicOverlay);
    }

    private synchronized void processLatestImage(final GraphicOverlay graphicOverlay) {
        processingImage = latestImage;
        processingMetaData = latestImageMetaData;
        latestImage = null;
        latestImageMetaData = null;
        if (processingImage != null && processingMetaData != null) {
            processImage(processingImage, processingMetaData, graphicOverlay);
        }
    }

    private void processImage(
            ByteBuffer data, final FrameMetadata frameMetadata,
            final GraphicOverlay graphicOverlay) {
        FirebaseVisionImageMetadata metadata =
                new FirebaseVisionImageMetadata.Builder()
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setWidth(frameMetadata.getWidth())
                .setHeight(frameMetadata.getHeight())
                .setRotation(frameMetadata.getRotation())
                .build();

        Bitmap bitmap = BitmapUtils.getBitmap(data, frameMetadata);
        detectInVisionImage(
                bitmap, FirebaseVisionImage.fromByteBuffer(data, metadata), frameMetadata,
                graphicOverlay);
    }

    private void detectInVisionImage(
            final Bitmap originalCameraImage,
            FirebaseVisionImage image,
            final FrameMetadata metadata,
            final GraphicOverlay graphicOverlay) {
        detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<T>() {
                            @Override
                            public void onSuccess(T results) {
                                VisionProcessorBase.this.onSuccess(originalCameraImage, results,
                                        metadata,
                                        graphicOverlay);
                                processLatestImage(graphicOverlay);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                VisionProcessorBase.this.onFailure(e);
                            }
                        });

    }

    @Override
    public void stop() {

    }

    protected abstract Task<T> detectInImage(FirebaseVisionImage image);

    protected abstract void onSuccess(
            Bitmap originalCameraImage,
            T results,
            FrameMetadata frameMetadata,
            GraphicOverlay graphicOverlay);
    protected abstract void onFailure(Exception e);

}
