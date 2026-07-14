package org.example.gestionrh.tcproject.Services;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

@Service
public class LlamaParseService {

    @Value("${llamaparse.api.key}")
    private String apiKey;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build();

    // Envoie un PDF à l'API LlamaParse, attend le traitement, et retourne le texte extrait en Markdown
    public String extractMarkdownFromPdf(File pdfFile) throws IOException, InterruptedException {
        // 1. Upload File
        String uploadUrl = "https://api.cloud.llamaindex.ai/api/parsing/upload";

        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", pdfFile.getName(),
                        RequestBody.create(pdfFile, MediaType.parse("application/pdf")))
                .build();

        Request uploadRequest = new Request.Builder()
                .url(uploadUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(formBody)
                .build();

        String jobId = null;
        try (Response response = httpClient.newCall(uploadRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("LlamaParse upload error " + response.code() + ": " + (response.body() != null ? response.body().string() : ""));
            }
            String respStr = response.body().string();
            // Extract job ID using simple JSON substring search
            String key = "\"id\":\"";
            int idx = respStr.indexOf(key);
            if (idx == -1) {
                key = "\"id\": \"";
                idx = respStr.indexOf(key);
            }
            if (idx != -1) {
                int endIdx = respStr.indexOf("\"", idx + key.length());
                if (endIdx != -1) {
                    jobId = respStr.substring(idx + key.length(), endIdx);
                }
            }
        }

        if (jobId == null) {
            throw new IOException("Could not parse Job ID from LlamaParse response.");
        }

        System.out.println("LlamaParse Job started: " + jobId + ". Waiting for completion...");

        // 2. Poll Status
        String statusUrl = "https://api.cloud.llamaindex.ai/api/parsing/job/" + jobId;
        boolean success = false;
        for (int i = 0; i < 60; i++) { // wait up to 60*5 = 300 seconds
            Thread.sleep(5000); // 5 seconds polling
            Request statusReq = new Request.Builder()
                    .url(statusUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(statusReq).execute()) {
                String respStr = response.body() != null ? response.body().string() : "";
                if (respStr.contains("\"status\":\"SUCCESS\"") || respStr.contains("\"status\": \"SUCCESS\"")) {
                    success = true;
                    break;
                } else if (respStr.contains("\"status\":\"ERROR\"") || respStr.contains("\"status\": \"ERROR\"")) {
                    throw new IOException("LlamaParse processing failed: " + respStr);
                }
            }
            System.out.println("Still processing...");
        }

        if (!success) {
            throw new IOException("LlamaParse processing timed out.");
        }

        System.out.println("LlamaParse processing complete. Fetching Markdown result...");

        // 3. Fetch Markdown Result
        String resultUrl = "https://api.cloud.llamaindex.ai/api/parsing/job/" + jobId + "/result/markdown";
        Request resultReq = new Request.Builder()
                .url(resultUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .get()
                .build();

        try (Response response = httpClient.newCall(resultReq).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("LlamaParse fetch result error " + response.code());
            }
            String respStr = response.body().string();
            // Extract markdown value
            return extractTextFromJson(respStr, "\"markdown\"");
        }
    }

    // Extrait une valeur texte d'un JSON brut en cherchant une clé spécifique (parsing manuel)
    private String extractTextFromJson(String json, String keyBase) {
        String key = keyBase + ":\"";
        int index = json.indexOf(key);
        if (index == -1) {
            key = keyBase + ": \"";
            index = json.indexOf(key);
        }
        if (index != -1) {
            int startQuote = json.indexOf("\"", index + key.length() - 1);
            if (startQuote != -1) {
                StringBuilder extracted = new StringBuilder();
                boolean escaped = false;
                for (int i = startQuote + 1; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (escaped) {
                        extracted.append(c);
                        escaped = false;
                    } else {
                        if (c == '\\') {
                            escaped = true;
                            extracted.append(c);
                        } else if (c == '"') {
                            break;
                        } else {
                            extracted.append(c);
                        }
                    }
                }
                return extracted.toString()
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            }
        }
        return "";
    }
}
