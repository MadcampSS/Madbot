package com.example.voicerecognition;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

public class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 4.0f;
    private static final float ID_TEXT_SIZE = 30.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private int facing;

    private final Paint facePositionPaint;
    private final Paint idPaint;
    private final Paint boxPaint;

    private volatile FirebaseVisionFace firebaseVisionFace;

    public FaceGraphic(GraphicOverlay overlay, FirebaseVisionFace face, int facing) {
        super(overlay);

        firebaseVisionFace = face;
        this.facing = facing;
        final int selectedColor = Color.WHITE;

        facePositionPaint = new Paint();
        facePositionPaint.setColor(selectedColor);

        idPaint = new Paint();
        idPaint.setColor(selectedColor);

        boxPaint = new Paint();
        boxPaint.setColor(selectedColor);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    @Override
    public void draw(Canvas canvas) {
        FirebaseVisionFace face = firebaseVisionFace;
        if (face == null) {
            return;
        }

        float x = translateX(face.getBoundingBox().centerX());
        float y = translateY(face.getBoundingBox().centerY());

        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);
        canvas.drawText("id: " + face.getTrackingId(), x + ID_X_OFFSET, y + ID_Y_OFFSET, idPaint);
        canvas.drawText(
                "happiness: " + String.format("%.2f", face.getSmilingProbability()),
                x + ID_X_OFFSET * 3,
                y - ID_Y_OFFSET,
                idPaint);
        if (facing == CameraSource.CAMERA_FACING_FRONT) {
            canvas.drawText(
                    "right eye: " + String.format("%.2f", face.getRightEyeOpenProbability()),
                    x - ID_X_OFFSET,
                    y,
                    idPaint);
            canvas.drawText(
                    "left eye: " + String.format("%.2f", face.getLeftEyeOpenProbability()),
                    x + ID_X_OFFSET * 6,
                    y,
                    idPaint);
        } else {
            canvas.drawText(
                    "left eye: " + String.format("%.2f", face.getLeftEyeOpenProbability()),
                    x - ID_X_OFFSET,
                    y,
                    idPaint);
            canvas.drawText(
                    "right eye: " + String.format("%.2f", face.getRightEyeOpenProbability()),
                    x + ID_X_OFFSET * 6,
                    y,
                    idPaint);
        }

        float xOffset = scaleX(face.getBoundingBox().width() / 2.0f);
        float yOffset = scaleY(face.getBoundingBox().height() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, boxPaint);

        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.MOUTH_BOTTOM);
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.LEFT_CHEEK);
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.LEFT_EAR);
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.MOUTH_LEFT);
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.LEFT_EYE);
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.NOSE_BASE);
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.RIGHT_CHEEK);
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.RIGHT_EAR);
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.RIGHT_EYE);
        drawLandmarkPosition(canvas, face, FirebaseVisionFaceLandmark.MOUTH_RIGHT);
    }

    private void drawLandmarkPosition(Canvas canvas, FirebaseVisionFace face, int landmarkID) {
        FirebaseVisionFaceLandmark landmark = face.getLandmark(landmarkID);
        if (landmark != null) {
            FirebaseVisionPoint point = landmark.getPosition();
            canvas.drawCircle(
                    translateX(point.getX()),
                    translateY(point.getY()),
                    10f, idPaint);
        }
    }
}
