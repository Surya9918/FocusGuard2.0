package com.focusguard.app.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * MLPredictor — TFLite Inference Engine
 * ════════════════════════════════════════
 * Handles loading the TFLite model and performing real-time risk predictions.
 */
public class MLPredictor {
    private static final String TAG = "MLPredictor";
    private static final String MODEL_FILE = "distraction_model.tflite";
    private static MLPredictor sInstance;

    private Interpreter mInterpreter;

    private MLPredictor(Context context) {
        try {
            mInterpreter = new Interpreter(loadModelFile(context));
            Log.d(TAG, "TFLite Model loaded successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error loading TFLite model.", e);
        }
    }

    public static synchronized MLPredictor getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MLPredictor(context);
        }
        return sInstance;
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Predicts the distraction risk score (0.0 to 1.0).
     * 
     * @param appId The mapped category ID of the app.
     * @param durationSeconds Time spent in the current session.
     * @param hourOfDay Current hour (0-23).
     * @return Risk score.
     */
    public float predictRisk(int appId, long durationSeconds, int hourOfDay) {
        if (mInterpreter == null) return 0.0f;

        // Model expects input: [app_id, duration, hour]
        float[][] input = {{ (float)appId, (float)durationSeconds, (float)hourOfDay }};
        float[][] output = new float[1][1];

        mInterpreter.run(input, output);
        return output[0][0];
    }
}
