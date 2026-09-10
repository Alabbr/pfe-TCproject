package org.example.gestionrh.tcproject.Services;

import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class MachineLearningService {

    private static final String PYTHON_API_URL = System.getenv("ML_API_URL") != null ? System.getenv("ML_API_URL") : "http://ml-service:5000/predict_single";
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    public static class ModelResult {
        public double slope;
        public double intercept;
        public double r2; // Coefficient of determination (R2 score)
        
        public double predict(double x) {
            return slope * x + intercept;
        }
    }

    /**
     * Calls the Python Microservice for ML prediction via HTTP.
     * @param data A list of Maps containing "year" (Number) and "value" (Number)
     * @return ModelResult with computed slope, intercept, and R2 score.
     */
    public ModelResult trainModel(List<Map<String, Object>> data) {
        int n = data.size();
        if (n < 2) {
            ModelResult result = new ModelResult();
            result.slope = 0.0;
            result.intercept = n == 1 ? ((Number) data.get(0).get("value")).doubleValue() : 0.0;
            result.r2 = 1.0;
            return result;
        }

        // Construction manuelle du JSON pour éviter le problème de dépendance Jackson
        StringBuilder jsonBuilder = new StringBuilder("[");
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> point = data.get(i);
            jsonBuilder.append("{\"year\":").append(point.get("year"))
                       .append(",\"value\":").append(point.get("value")).append("}");
            if (i < data.size() - 1) jsonBuilder.append(",");
        }
        jsonBuilder.append("]");

        Request request = new Request.Builder()
                .url(PYTHON_API_URL)
                .post(RequestBody.create(jsonBuilder.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                
                // Parsing manuel du JSON retourné par Python
                ModelResult result = new ModelResult();
                result.slope = extractDouble(responseBody, "\"slope\":");
                result.intercept = extractDouble(responseBody, "\"intercept\":");
                result.r2 = extractDouble(responseBody, "\"r2\":");
                return result;
            } else {
                System.err.println("Erreur API Python: " + response.code());
            }
        } catch (Exception e) {
            System.err.println("Impossible de contacter le serveur Python sur " + PYTHON_API_URL);
            e.printStackTrace();
        }

        // Fallback en cas d'erreur de communication avec Python
        ModelResult fallback = new ModelResult();
        fallback.slope = 0.0;
        fallback.intercept = 0.0;
        fallback.r2 = 0.0;
        return fallback;
    }

    private double extractDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return 0.0;
        int start = idx + key.length();
        // Skip whitespace if any
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || Character.isWhitespace(c)) {
                break;
            }
            end++;
        }
        try {
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
