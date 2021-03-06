package com.launchdarkly.android;

import android.net.Uri;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.launchdarkly.android.test.TestActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class DiagnosticEventProcessorTest {

    @Rule
    public final ActivityTestRule<TestActivity> activityTestRule =
            new ActivityTestRule<>(TestActivity.class, false, true);

    private MockWebServer mockEventsServer;

    @Before
    public void before() throws IOException {
        NetworkTestController.setup(activityTestRule.getActivity());
        mockEventsServer = new MockWebServer();
        mockEventsServer.start();
    }

    @After
    public void after() throws InterruptedException, IOException {
        NetworkTestController.enableNetwork();
        mockEventsServer.close();
    }

    @Test
    public void defaultDiagnosticRequest() throws InterruptedException {
        // Setup in background to prevent initial diagnostic event
        ForegroundTestController.setup(false);
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        LDConfig ldConfig = new LDConfig.Builder()
                .setMobileKey("test-mobile-key")
                .setEventsUri(Uri.parse(mockEventsServer.url("/mobile").toString()))
                .build();
        DiagnosticStore diagnosticStore = new DiagnosticStore(activityTestRule.getActivity().getApplication(), "test-mobile-key");
        DiagnosticEventProcessor diagnosticEventProcessor = new DiagnosticEventProcessor(ldConfig, "default", diagnosticStore, okHttpClient);

        DiagnosticEvent testEvent = new DiagnosticEvent("test-kind", System.currentTimeMillis(), diagnosticStore.getDiagnosticId());

        mockEventsServer.enqueue(new MockResponse());
        diagnosticEventProcessor.sendDiagnosticEventSync(testEvent);
        RecordedRequest r = mockEventsServer.takeRequest();
        assertEquals("POST", r.getMethod());
        assertEquals("/mobile/events/diagnostic", r.getPath());
        assertEquals("api_key test-mobile-key", r.getHeader("Authorization"));
        assertEquals("AndroidClient/" + BuildConfig.VERSION_NAME, r.getHeader("User-Agent"));
        assertEquals("application/json; charset=utf-8", r.getHeader("Content-Type"));
        assertEquals(GsonCache.getGson().toJson(testEvent), r.getBody().readUtf8());
    }

    @Test
    public void defaultDiagnosticRequestIncludingWrapper() throws InterruptedException {
        // Setup in background to prevent initial diagnostic event
        ForegroundTestController.setup(false);
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        LDConfig ldConfig = new LDConfig.Builder()
                .setMobileKey("test-mobile-key")
                .setEventsUri(Uri.parse(mockEventsServer.url("/mobile").toString()))
                .setWrapperName("ReactNative")
                .setWrapperVersion("1.0.0")
                .build();
        DiagnosticStore diagnosticStore = new DiagnosticStore(activityTestRule.getActivity().getApplication(), "test-mobile-key");
        DiagnosticEventProcessor diagnosticEventProcessor = new DiagnosticEventProcessor(ldConfig, "default", diagnosticStore, okHttpClient);

        DiagnosticEvent testEvent = new DiagnosticEvent("test-kind", System.currentTimeMillis(), diagnosticStore.getDiagnosticId());

        mockEventsServer.enqueue(new MockResponse());
        diagnosticEventProcessor.sendDiagnosticEventSync(testEvent);
        RecordedRequest r = mockEventsServer.takeRequest();
        assertEquals("POST", r.getMethod());
        assertEquals("/mobile/events/diagnostic", r.getPath());
        assertEquals("ReactNative/1.0.0", r.getHeader("X-LaunchDarkly-Wrapper"));
        assertEquals(GsonCache.getGson().toJson(testEvent), r.getBody().readUtf8());
    }

    @Test
    public void defaultDiagnosticRequestIncludingAdditionalHeaders() throws InterruptedException {
        // Setup in background to prevent initial diagnostic event
        ForegroundTestController.setup(false);
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        HashMap<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("Proxy-Authorization", "token");
        additionalHeaders.put("Authorization", "foo");
        LDConfig ldConfig = new LDConfig.Builder()
                .setMobileKey("test-mobile-key")
                .setEventsUri(Uri.parse(mockEventsServer.url("/mobile").toString()))
                .setAdditionalHeaders(additionalHeaders)
                .build();
        DiagnosticStore diagnosticStore = new DiagnosticStore(activityTestRule.getActivity().getApplication(), "test-mobile-key");
        DiagnosticEventProcessor diagnosticEventProcessor = new DiagnosticEventProcessor(ldConfig, "default", diagnosticStore, okHttpClient);

        DiagnosticEvent testEvent = new DiagnosticEvent("test-kind", System.currentTimeMillis(), diagnosticStore.getDiagnosticId());

        mockEventsServer.enqueue(new MockResponse());
        diagnosticEventProcessor.sendDiagnosticEventSync(testEvent);
        RecordedRequest r = mockEventsServer.takeRequest();
        assertEquals("POST", r.getMethod());
        assertEquals("/mobile/events/diagnostic", r.getPath());
        assertEquals("token", r.getHeader("Proxy-Authorization"));
        assertEquals("foo", r.getHeader("Authorization"));
        assertEquals(GsonCache.getGson().toJson(testEvent), r.getBody().readUtf8());
    }
}
