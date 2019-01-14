package com.example.voicerecognition;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.Policy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class CameraSource {
    public static final int CAMERA_FACING_BACK = Camera.CameraInfo.CAMERA_FACING_BACK;

    public static final int CAMERA_FACING_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;

    public static final int NUMBER_OF_PROCESSING_THREAD = 1;

    private static final String TAG = "CameraSource";

    private static final int DUMMY_TEXTURE_NAME = 100;

    private static final float ASPECT_RATIO_TOLERANCE = 0.01f;

    protected Activity activity;

    private Camera camera;

    protected int facing = CAMERA_FACING_BACK;

    private int rotation;

    private Size previewSize;


    private final float requestedFps = 60.0f;
    private final int requestedPreviewWidth = 480;
    private final int requestedPreviewHeight = 360;
    private final boolean requestedAutoFocus = true;


    private SurfaceTexture dummySurfaceTexture;
    private final GraphicOverlay graphicOverlay;


    private boolean usingSurfaceTexture;

    private List<Thread> processingThreads;

//    private Thread processingThreadAlpha;
//    private Thread processingThreadBravo;

    private final FrameProcessingRunnable processingRunnable;

    private final Object processorLock = new Object();

    private VisionImageProcessor frameProcessor;

    private final Map<byte[], ByteBuffer> bytesToByteBuffer = new IdentityHashMap<>();

    public CameraSource(Activity activity, GraphicOverlay overlay) {
        this.activity = activity;
        graphicOverlay = overlay;
        graphicOverlay.clear();
        processingRunnable = new FrameProcessingRunnable();

        if (Camera.getNumberOfCameras() == 1) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(0, cameraInfo);
            facing = cameraInfo.facing;
        }
    }

    public void release() {
        synchronized (processorLock) {
            stop();
            processingRunnable.release();
            cleanScreen();

            if (frameProcessor != null) {
                frameProcessor.stop();
            }
        }
    }

    public synchronized CameraSource start() throws IOException {
        if (camera != null) {
            return this;
        }

        camera = createCamera();
        dummySurfaceTexture = new SurfaceTexture(DUMMY_TEXTURE_NAME);
        camera.setPreviewTexture(dummySurfaceTexture);
        usingSurfaceTexture = true;
        camera.startPreview();

        processingThreads = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_PROCESSING_THREAD; i++) {
            Thread processingThread = new Thread(processingRunnable);
            processingThread.setName(String.valueOf(i));
            processingThreads.add(processingThread);
        }
//
//        Thread processingThreadAlpha = new Thread(processingRunnable);
//        processingThreadAlpha.setName("Thread ALPHA");
//        processingThreads.add(processingThreadAlpha);
//
//        Thread processingThreadBravo = new Thread(processingRunnable);
//        processingThreadBravo.setName("Thread BRAVO");
//        processingThreads.add(processingThreadBravo);
//
//        Thread processingThreadCharlie = new Thread(processingRunnable);
//        processingThreadCharlie.setName("Thread Charlie");
//        processingThreads.add(processingThreadCharlie);
//
//        Thread processingThreadDelta = new Thread(processingRunnable);
//        processingThreadDelta.setName("Thread Delta");
//        processingThreads.add(processingThreadDelta);

        processingRunnable.setActive(true);

        for (Thread processingThread : processingThreads) {
            processingThread.start();
        }

        return this;
    }

//    public synchronized CameraSource start(SurfaceHolder surfaceHolder) throws IOException {
//        if (camera != null) {
//            return this;
//        }
//
//        camera = createCamera();
//        camera.setPreviewDisplay(surfaceHolder);
//        camera.startPreview();
//
//        processingThread = new Thread(processingRunnable);
//        processingRunnable.setActive(true);
//        processingThread.start();
//
//        usingSurfaceTexture = false;
//        return this;
//    }

    public synchronized void stop() {
        processingRunnable.setActive(false);

        for (Thread processingThread : processingThreads) {
            if(processingThread != null) {
                try {
                    processingThread.join();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Frame processing thread interrupted on release");
                }
                processingThread = null;
            }
        }

        processingThreads.clear();


        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallbackWithBuffer(null);
            try {
                if (usingSurfaceTexture) {
                    camera.setPreviewTexture(null);
                } else {
                    camera.setPreviewDisplay(null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear camera preview: " + e);
            }
            camera.release();
            camera = null;
        }

        bytesToByteBuffer.clear();
    }


    public synchronized void setFacing(int facing) throws IllegalAccessException {
        if ((facing != CAMERA_FACING_BACK) && (facing != CAMERA_FACING_FRONT)) {
            throw new IllegalAccessException("Invalid camera: " + facing);
        }
        this.facing = facing;
    }

    public Size getPreviewSize() {
        return previewSize;
    }

    public int getCameraFacing() {
        return facing;
    }

    private Camera createCamera() throws IOException {
        int requestedCameraId = getIdForRequestedCamera(facing);
        if (requestedCameraId == -1) {
            throw new IOException("Could not find requested camera");
        }

        Camera camera = Camera.open(requestedCameraId);

        SizePair sizePair = selecteSizePair(camera, requestedPreviewWidth, requestedPreviewHeight);
        if (sizePair == null) {
            throw new IOException("Could not find suitable preview size");
        }

        Size pictureSize = sizePair.pictureSize();
        previewSize = sizePair.previewSize();

        int[] previewFpsRange = selectPreviewFpsRange(camera, requestedFps);
        if (previewFpsRange == null) {
            throw new IOException("Could not find suitable preview frames per second range");
        }

        Camera.Parameters parameters = camera.getParameters();

        if (pictureSize != null) {
            parameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        }
        parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
        parameters.setPreviewFpsRange(
                previewFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                previewFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
        parameters.setPreviewFormat(ImageFormat.NV21);

        setRotation(camera, parameters, requestedCameraId);

        if (requestedAutoFocus) {
            if (parameters
                    .getSupportedFocusModes()
                    .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else {
                Log.i(TAG, "Camera auto focus is not supported");
            }
        }

        camera.setParameters(parameters);

        camera.setPreviewCallbackWithBuffer(new CameraPreviewCallback());
        camera.addCallbackBuffer(createPreviewBuffer(previewSize));
        camera.addCallbackBuffer(createPreviewBuffer(previewSize));
        camera.addCallbackBuffer(createPreviewBuffer(previewSize));
        camera.addCallbackBuffer(createPreviewBuffer(previewSize));

        return camera;
    }

    private static int getIdForRequestedCamera(int facing) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == facing) {
                return i;
            }
        }
        return -1;
    }


    private static SizePair selecteSizePair(Camera camera, int desiredWidth, int desiredHeight) {
        List<SizePair> validPreviewSizes = generateValidPreviewSizeList(camera);

        SizePair selectedPair = null;
        int minDiff = Integer.MAX_VALUE;
        for (SizePair sizePair : validPreviewSizes) {
            Size size = sizePair.previewSize();
            int diff =
                    Math.abs(size.getWidth() - desiredWidth) + Math.abs(size.getHeight() - desiredHeight);
            if (diff < minDiff) {
                selectedPair = sizePair;
                minDiff = diff;
            }
        }

        return selectedPair;
    }

    private static class SizePair {
        private final Size preview;
        private Size picture;

        SizePair(
                android.hardware.Camera.Size previewSize,
                android.hardware.Camera.Size pictureSize) {
            preview = new Size(previewSize.width, previewSize.height);
            if (pictureSize != null) {
                picture = new Size(pictureSize.width, pictureSize.height);
            }
        }

        Size previewSize() {
            return preview;
        }

        Size pictureSize() {
            return picture;
        }
    }

    private static List<SizePair> generateValidPreviewSizeList(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> supportedPreviewSizes =
                parameters.getSupportedPreviewSizes();
        List<Camera.Size> supportedPictureSizes =
                parameters.getSupportedPreviewSizes();

        List<SizePair> validPreviewSizes = new ArrayList<>();
        for (android.hardware.Camera.Size previewSize : supportedPreviewSizes) {
            float previewAspectRatio = (float) previewSize.width / (float) previewSize.height;

            for (android.hardware.Camera.Size pictureSize : supportedPictureSizes) {
                float pictureAspectRatio = (float) pictureSize.width / (float) pictureSize.height;
                if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
                    validPreviewSizes.add(new SizePair(previewSize, pictureSize));
                    break;
                }
            }
        }

        if (validPreviewSizes.size() == 0) {
            Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size");
            for (android.hardware.Camera.Size previewSize : supportedPreviewSizes) {
                validPreviewSizes.add(new SizePair(previewSize, null));
            }
        }

        return validPreviewSizes;
    }


    private static int[] selectPreviewFpsRange(Camera camera, float desiredPreviewFps) {
        int desiredPreviewFpsScaled = (int) (desiredPreviewFps * 1000.0f);


        int[] selectedFpsRange = null;
        int minDiff = Integer.MAX_VALUE;

        List<int[]> previewFpsRangeList = camera.getParameters().getSupportedPreviewFpsRange();
        for (int[] range : previewFpsRangeList) {
            int deltaMin = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
            int deltaMax = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];

            int diff = Math.abs(deltaMin) + Math.abs(deltaMax);
            if (diff < minDiff) {
                selectedFpsRange = range;
                minDiff = diff;
            }
        }

        return selectedFpsRange;
    }

    private void setRotation(Camera camera, Camera.Parameters parameters, int cameraId) {
        WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        int degrees = 0;
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                Log.e(TAG, "BAD ROTATION VALUE " + rotation);
        }

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        int angle;
        int displayAngle;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            angle = (cameraInfo.orientation + degrees) % 360;
            displayAngle = (360 - angle) % 360;
        } else {
            angle = (cameraInfo.orientation - degrees + 360) % 360;
            displayAngle = angle;
        }

        this.rotation = angle / 90;

        camera.setDisplayOrientation(displayAngle);
        parameters.setRotation(angle);
    }

    private byte[] createPreviewBuffer(Size previewSize) {
        int bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21);
        long sizeInBits = (long) previewSize.getHeight() * previewSize.getWidth() * bitsPerPixel;
        int bufferSize = (int) Math.ceil(sizeInBits / 8.0d) + 1;

        byte[] byteArray = new byte[bufferSize];
        ByteBuffer buffer= ByteBuffer.wrap(byteArray);
        if (!buffer.hasArray() || (buffer.array() != byteArray)) {
            throw new IllegalStateException("Failed to create valid buffer for camera source");
        }

        bytesToByteBuffer.put(byteArray, buffer);
        return byteArray;
    }

    private class CameraPreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            processingRunnable.setNextFrame(data, camera);
        }
    }

    public void setMachineLearningFrameProcessor(VisionImageProcessor processor) {
        synchronized (processorLock) {
            cleanScreen();
            if (frameProcessor != null) {
                frameProcessor.stop();
            }
            frameProcessor = processor;
        }
    }

    private boolean assertThreadRelease() {
        for (Thread processingThread : processingThreads) {
            if (processingThread.getState() != Thread.State.TERMINATED)
                return false;
        }
        return true;
    }

    private class FrameProcessingRunnable implements Runnable {
        private final Object lock = new Object();
        private boolean active = true;


        private ByteBuffer pendingFrameData;

        FrameProcessingRunnable() {}

        void release() { assert (assertThreadRelease()); }

        void setActive(boolean active) {
            synchronized (lock) {
                this.active = active;
                lock.notifyAll();
            }
        }

        void setNextFrame(byte[] data, Camera camera) {
            synchronized (lock) {
                if (pendingFrameData != null) {
                    camera.addCallbackBuffer(pendingFrameData.array());
                    pendingFrameData = null;
                }

                if(!bytesToByteBuffer.containsKey(data)) {
                    Log.d(
                            TAG,
                            "Skipping frame. Could not find ByteBuffer associated with the image"
                                    + "data from the camera.");
                    return;
                }

                pendingFrameData = bytesToByteBuffer.get(data);

                lock.notifyAll();
            }
        }
        @Override
        public void run() {
            ByteBuffer data;

            while(true) {
                synchronized (lock) {
                    while (active && (pendingFrameData == null)) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Frame processing loop terminated.", e);
                            return;
                        }
                    }

                    if(!active) {
                        return;
                    }

                    data = pendingFrameData;
                    pendingFrameData = null;

                }

                try {
                    synchronized (processorLock) {
                        String threadName = Thread.currentThread().getName();
                        Log.d(TAG, "Process an image: Thread " + threadName);
                        frameProcessor.process(
                                data,
                                new FrameMetadata.Builder()
                                .setWidth(previewSize.getWidth())
                                .setHeight(previewSize.getHeight())
                                .setRotation(rotation)
                                .setCameraFacing(facing)
                                .build(),
                                graphicOverlay);
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Exception thrown from receiver", t);
                } finally {
                    camera.addCallbackBuffer(data.array());
                }
            }
        }
    }

    private void cleanScreen() { graphicOverlay.clear(); }
}



