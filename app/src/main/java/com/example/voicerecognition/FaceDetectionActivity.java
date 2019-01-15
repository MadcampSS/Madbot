package com.example.voicerecognition;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.voicerecognition.realtime.DrawView;
import com.example.voicerecognition.realtime.VisionImage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabel;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabelDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;

import java.util.List;

public class FaceDetectionActivity extends AppCompatActivity {

    VisionImage visionImage;
    TextView textView;
    DrawView imageView;
    TextView labelView;

    int w, h;
    int viewWidth ;
    int viewHeight;

    Task<List<FirebaseVisionFace>> result;
    Task<List<FirebaseVisionCloudLabel>> labelResult;

    // High-accuracy landmark detection and face classification
    FirebaseVisionFaceDetectorOptions highAccuracyOpts =
            new FirebaseVisionFaceDetectorOptions.Builder()
                    .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                    // .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                    .enableTracking()
                    .build();

    // Real-time contour detection of multiple faces
    FirebaseVisionFaceDetectorOptions realTimeOpts =
            new FirebaseVisionFaceDetectorOptions.Builder()
                    .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                    .build();

    FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
            .getVisionFaceDetector(highAccuracyOpts);


    FirebaseVisionCloudDetectorOptions options =
            new FirebaseVisionCloudDetectorOptions.Builder()
                    .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                    .setMaxResults(15)
                    .build();

    FirebaseVisionCloudLabelDetector labelDetector = FirebaseVision.getInstance()
            .getVisionCloudLabelDetector();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detection);

        textView = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        imageView = (DrawView) findViewById(R.id.imageView);
        labelView = (TextView) findViewById(R.id.labelView);
        labelView.setMovementMethod(new ScrollingMovementMethod());


        Intent intent = getIntent();
        String imagePath = intent.getStringExtra("imgPath");

        Glide.with(this).load(imagePath).into(imageView);

        visionImage = new VisionImage();

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        w = bitmap.getWidth();
        h = bitmap.getHeight();

        FirebaseVisionImage firebaseVisionImage = visionImage.getFirebaseVisionImage(bitmap);

        analyzeFace(firebaseVisionImage);


    }

    private void showResult() {
        List<FirebaseVisionFace> faceList = result.getResult();
        assert faceList != null;
        for(FirebaseVisionFace face: faceList) {

            String faceAttribute = "Tracking ID: " + face.getTrackingId()
                    + "\n Left Eye Open: " + face.getLeftEyeOpenProbability()
                    + "\n Right Eye Open: " + face.getRightEyeOpenProbability()
                    + "\n Smiling Probability: " + face.getSmilingProbability() + "\n\n";

            textView.append(faceAttribute);

            viewWidth = imageView.getWidth();
            viewHeight = imageView.getHeight();

            Rect boundingBox = face.getBoundingBox();

            float widthRate = (float) viewWidth / (float) w;
            float heightRate = (float) viewHeight / (float) h;


            int left = Math.round(boundingBox.left * widthRate);
            int top = Math.round(boundingBox.top * heightRate);
            int right = Math.round(boundingBox.right * widthRate);
            int bottom = Math.round(boundingBox.bottom * heightRate);

            Rect resizedRect = new Rect(left, top, right, bottom);

            imageView.addRect(resizedRect);
        }
    }

    private void showLabelingResult() {
        List<FirebaseVisionCloudLabel> labelList = labelResult.getResult();

        for (FirebaseVisionCloudLabel label: labelList) {
            String text = label.getLabel();
            float confidence = label.getConfidence();

            String labelAttribute = text + ": " + confidence + "%\n";

            labelView.append(labelAttribute);
        }

    }

    private void analyzeFace(FirebaseVisionImage image) {

       result = detector.detectInImage(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        // Task completed successfully
                                        // ...
//                                        for (FirebaseVisionFace face : faces) {
//                                            Rect bounds = face.getBoundingBox();
//                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
//
//                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees
//
//                                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
//                                            // nose available):
//                                            FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
//                                            if (leftEar != null) {
//                                                FirebaseVisionPoint leftEarPos = leftEar.getPosition();
//                                            }
//
//                                            // If contour detection was enabled:
//                                            List<FirebaseVisionPoint> leftEyeContour =
//                                                    face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints();
//
//
//                                            List<FirebaseVisionPoint> upperLipBottomContour =
//                                                    face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints();
//
//
//                                            // If classification was enabled:
//                                            if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
//                                                float smileProb = face.getSmilingProbability();
//                                            }
//                                            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
//                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
//                                            }
//
//                                            // If face tracking was enabled:
//                                            if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
//                                                int id = face.getTrackingId();
//                                            }
//
//                                            Log.i("FaceDetectionLog", "Succeed!");
//
//                                        }

                                        showResult();
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...

                                        Log.i("FaceDetectionLog", "Failed!");
                                    }
                                });


        labelResult = labelDetector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<FirebaseVisionCloudLabel>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionCloudLabel> labels) {
                                // Task completed successfully
                                // ...
                                showLabelingResult();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                            }
                        });
    }

}


