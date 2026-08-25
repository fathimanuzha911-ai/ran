package com.mnpos.distribution.data;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Thin wrapper around ONE shared, connection-pooled OkHttpClient.
 *
 * Why this matters vs the original app: the original opened a brand new
 * HttpURLConnection (and a brand new Thread) for every single call. With 20
 * users hitting the same server all day that means constant TCP+TLS
 * handshakes and no keep-alive. Here every request reuses a small pool of
 * warm connections, requests run on OkHttp's own bounded dispatcher (no
 * unbounded thread creation), and results are always delivered back on the
 * main thread so callers never need to think about threading.
 */
public final class ApiClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static volatile OkHttpClient client;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ApiClient() {}

    public interface JsonCallback {
        void onSuccess(JSONObject result);
        void onError(Exception error);
    }

    private static OkHttpClient client() {
        if (client == null) {
            synchronized (ApiClient.class) {
                if (client == null) {
                    client = new OkHttpClient.Builder()
                        .connectionPool(new ConnectionPool(6, 5, TimeUnit.MINUTES))
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .writeTimeout(20, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(true)
                        .build();
                }
            }
        }
        return client;
    }

    public static void get(String path, JsonCallback callback) {
        get(path, null, callback);
    }

    public static void get(String path, Map<String, String> query, JsonCallback callback) {
        enqueue(buildRequest("GET", path, query, null), callback);
    }

    public static void post(String path, JSONObject body, JsonCallback callback) {
        enqueue(buildRequest("POST", path, null, body), callback);
    }

    public static void put(String path, JSONObject body, JsonCallback callback) {
        enqueue(buildRequest("PUT", path, null, body), callback);
    }

    public static void delete(String path, JsonCallback callback) {
        enqueue(buildRequest("DELETE", path, null, null), callback);
    }

    private static Request buildRequest(String method, String path, Map<String, String> query, JSONObject body) {
        HttpUrl base = HttpUrl.parse(Session.get().url(path));
        if (base == null) throw new IllegalArgumentException("Invalid server URL");
        HttpUrl.Builder urlBuilder = base.newBuilder();
        if (query != null) {
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (entry.getValue() != null) urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }

        Request.Builder builder = new Request.Builder()
            .url(urlBuilder.build())
            .header("Accept", "application/json");

        String token = Session.get().authToken;
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        RequestBody requestBody = null;
        if (body != null || "POST".equals(method) || "PUT".equals(method)) {
            requestBody = RequestBody.create(body == null ? new JSONObject().toString() : body.toString(), JSON);
        }

        switch (method) {
            case "POST": builder.post(requestBody); break;
            case "PUT": builder.put(requestBody); break;
            case "DELETE":
                if (requestBody != null) builder.delete(requestBody); else builder.delete();
                break;
            default: builder.get();
        }
        return builder.build();
    }

    private static void enqueue(Request request, JsonCallback callback) {
        client().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    String text = r.body() == null ? "" : r.body().string();
                    if (text.trim().isEmpty()) {
                        JSONObject empty = new JSONObject();
                        empty.put("success", r.isSuccessful());
                        postSuccess(callback, empty);
                        return;
                    }
                    JSONObject json = new JSONObject(text);
                    if (!r.isSuccessful()) json.put("http_status", r.code());
                    postSuccess(callback, json);
                } catch (Exception e) {
                    postError(callback, e);
                }
            }
        });
    }

    private static void postSuccess(JsonCallback callback, JSONObject result) {
        if (callback != null) MAIN.post(() -> callback.onSuccess(result));
    }

    private static void postError(JsonCallback callback, Exception error) {
        if (callback != null) MAIN.post(() -> callback.onError(error));
    }

    /** Small helper for building query maps without a wall of put() calls. */
    public static Map<String, String> query(Object... kv) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i + 1] != null) map.put(String.valueOf(kv[i]), String.valueOf(kv[i + 1]));
        }
        return map;
    }
}
