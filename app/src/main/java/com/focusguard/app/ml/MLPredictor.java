package com.focusguard.app.ml;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import com.focusguard.app.service.TrackingPolicy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class MLPredictor {
    private static final String[] FEATURES = {"app_id", "hour_of_day", "time_spent_mins"};
    private static MLPredictor instance;
    private final float[] scale = new float[3];
    private final float[] offset = new float[3];
    private Interpreter interpreter;

    private MLPredictor(Context context) {
        try {
            JSONObject metadata = new JSONObject(new String(read(context, "scaler_params.json"),
                    StandardCharsets.UTF_8));
            byte[] model = read(context, "distraction_model.tflite");
            String digest = Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(model));
            if (!digest.equals(metadata.getString("model_sha256_base64"))) {
                throw new IOException("Model metadata checksum mismatch");
            }
            for (int i = 0; i < FEATURES.length; i++) {
                if (!FEATURES[i].equals(metadata.getJSONArray("feature_names").getString(i))) {
                    throw new IOException("Unexpected model input order");
                }
                scale[i] = (float) metadata.getJSONArray("scale").getDouble(i);
                offset[i] = (float) metadata.getJSONArray("min").getDouble(i);
                if (!Float.isFinite(scale[i]) || !Float.isFinite(offset[i])) {
                    throw new IOException("Invalid preprocessing values");
                }
            }
            ByteBuffer buffer = ByteBuffer.allocateDirect(model.length).order(ByteOrder.nativeOrder());
            buffer.put(model);
            ((java.nio.Buffer) buffer).rewind();
            interpreter = new Interpreter(buffer);
        } catch (Exception | LinkageError error) {
            Log.i("FocusGuardML", "Using duration-based nudges: " + error.getMessage());
        }
    }

    public static synchronized MLPredictor getInstance(Context context) {
        if (instance == null) instance = new MLPredictor(context.getApplicationContext());
        return instance;
    }

    public synchronized float predictRisk(int appId, long seconds, int hour) {
        if (interpreter == null) return TrackingPolicy.durationRisk(seconds);
        float[] input = {appId, hour, seconds / 60f};
        for (int i = 0; i < input.length; i++) {
            input[i] = Math.max(0f, Math.min(1f, input[i] * scale[i] + offset[i]));
        }
        float[][] output = new float[1][1];
        try {
            interpreter.run(new float[][]{input}, output);
            if (Float.isFinite(output[0][0]) && output[0][0] >= 0f && output[0][0] <= 1f) {
                return output[0][0];
            }
        } catch (RuntimeException | LinkageError error) {
            Log.w("FocusGuardML", "Model inference failed", error);
        }
        try {
            interpreter.close();
        } catch (RuntimeException | LinkageError error) {
            Log.w("FocusGuardML", "Model cleanup failed", error);
        }
        interpreter = null;
        return TrackingPolicy.durationRisk(seconds);
    }

    private static byte[] read(Context context, String asset) throws IOException {
        try (InputStream source = context.getAssets().open(asset);
                ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[4096];
            int length;
            while ((length = source.read(bytes)) != -1) result.write(bytes, 0, length);
            return result.toByteArray();
        }
    }
}
