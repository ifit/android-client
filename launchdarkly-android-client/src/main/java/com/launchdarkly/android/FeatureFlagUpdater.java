package com.launchdarkly.android;


import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class FeatureFlagUpdater {
    private static final String TAG = "LDFeatureFlagUpdater";
    private static final int MAX_CACHE_SIZE_BYTES = 500_000;
    private static FeatureFlagUpdater instance;

    private final LDConfig config;
    private final UserManager userManager;
    private final OkHttpClient client;

    static FeatureFlagUpdater init(Context context, LDConfig config, UserManager userManager) {
        instance = new FeatureFlagUpdater(context, config, userManager);
        return instance;
    }

    static FeatureFlagUpdater getInstance() {
        return instance;
    }

    private FeatureFlagUpdater(Context context, LDConfig config, UserManager userManager) {
        this.config = config;
        this.userManager = userManager;

        File cacheDir = context.getDir("launchdarkly_api_cache", Context.MODE_PRIVATE);
        deleteRecursive(cacheDir);
        Log.d(TAG, "Using cache at: " + cacheDir.getAbsolutePath());

        Cache cache = new Cache(cacheDir, MAX_CACHE_SIZE_BYTES);
        client = new OkHttpClient.Builder()
                .cache(cache)
                .build();
    }

    Future<Void> update() {
        final VeryBasicFuture doneFuture = new VeryBasicFuture();
        String uri = config.getBaseUri() + "/msdk/eval/users/" + userManager.getCurrentUser().getAsUrlSafeBase64();
        final Request request = config.getRequestBuilder()
                .url(uri)
                .build();

        Log.d(TAG, request.toString());
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Exception when updating flags.", e);
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response when retrieving Feature Flags:  " + response + " using url: " + request.url());
                    }
                    String body = response.body().string();
                    Log.d(TAG, body);
                    Log.d(TAG, "Cache hit count: " + client.cache().hitCount() + " Cache network Count: " + client.cache().networkCount());
                    Log.d(TAG, "Cache response: " + response.cacheResponse());
                    Log.d(TAG, "Network response: " + response.networkResponse());

                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(body).getAsJsonObject();
                    userManager.saveFlagSettingsForUser(jsonObject);
                    doneFuture.completed(null);
                } catch (Exception e) {
                    Log.e(TAG, "Exception when handling response for url: " + request.url(), e);
                    doneFuture.failed(e);
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }
            }
        });
        return doneFuture;
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
}