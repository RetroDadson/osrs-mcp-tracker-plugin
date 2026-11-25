package dev.osrsmcp;

import com.google.gson.Gson;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLiteProperties;
import okhttp3.*;

@Slf4j
@Singleton
public class HttpRequestService {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String USER_AGENT =
            "OsrsMcpTracker/" + PluginVersion.get() + " " + "RuneLite/" + RuneLiteProperties.getVersion();
    private static final String PUBLIC_BASE_URL = "https://osrsmcp.retrodadson.com";

    private final OkHttpClient okHttpClient;
    private final OsrsMcpConfig config;
    private final Gson gson;

    @Inject
    HttpRequestService(OkHttpClient okHttpClient, OsrsMcpConfig config, Gson gson) {
        this.okHttpClient = okHttpClient;
        this.config = config;
        this.gson = gson;
    }

    public HttpResponse get(String url, String authToken) {
        Request request = buildRequest(url, authToken).get().build();

        return executeRequest(request, "GET", url, null);
    }

    public HttpResponse post(String url, String authToken, Object requestBody) {
        String requestJson = gson.toJson(requestBody);
        RequestBody body = RequestBody.create(JSON, requestJson);

        Request request = buildRequest(url, authToken).post(body).build();

        return executeRequest(request, "POST", url, requestJson);
    }

    /**
     * Fire-and-forget POST on OkHttp's dispatcher thread. Use this from event subscribers, which run on
     * the client thread: a blocking call there would freeze the game until the server responds or times out.
     */
    public void postAsync(String url, String authToken, Object requestBody) {
        String requestJson = gson.toJson(requestBody);
        RequestBody body = RequestBody.create(JSON, requestJson);

        Request request = buildRequest(url, authToken).post(body).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException ex) {
                log.warn("POST {} failed: {}", url, ex.toString());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    logRequest("POST", url, requestJson, r, readBodySafe(r));
                }
            }
        });
    }

    private Request.Builder buildRequest(String url, String authToken) {
        Request.Builder requestBuilder = new Request.Builder().url(url).header("User-Agent", USER_AGENT);

        if (isInternalUrl(url)) {
            if (authToken != null && !authToken.trim().isEmpty()) {
                requestBuilder.header("Authorization", authToken);
            }
            requestBuilder.header("Accept", "application/json");
        }

        return requestBuilder;
    }

    private boolean isInternalUrl(String url) {
        return url.startsWith(getBaseUrl());
    }

    private HttpResponse executeRequest(Request request, String method, String url, String requestBody) {
        Call call = okHttpClient.newCall(request);

        try (Response response = call.execute()) {
            String responseBody = readBodySafe(response);
            logRequest(method, url, requestBody, response, responseBody);
            return new HttpResponse(response.isSuccessful(), response.code(), responseBody);
        } catch (IOException ex) {
            log.warn("{} {} failed: {}", method, url, ex.toString());
            return new HttpResponse(false, -1, ex.getMessage());
        }
    }

    private void logRequest(String method, String url, String requestBody, Response response, String responseBody) {
        if (!log.isDebugEnabled()) {
            return;
        }

        switch (method) {
            case "GET":
                log.debug("GET {} -> {}\nResponse: {}", url, response.code(), responseBody);
                break;
            case "POST":
                log.debug("POST {}\nRequest: {}\nResponse({}): {}", url, requestBody, response.code(), responseBody);
                break;
            default:
                log.debug("{} {} -> {}\nResponse: {}", method, url, response.code(), responseBody);
        }
    }

    private static String readBodySafe(Response response) {
        try {
            ResponseBody responseBody = response.body();
            return responseBody != null ? responseBody.string() : "<no body>";
        } catch (Exception e) {
            return "<unavailable: " + e.getMessage() + ">";
        }
    }

    public String getBaseUrl() {
        String baseUrlOverride = config.baseUrlOverride().trim();
        if (!baseUrlOverride.isEmpty()) {
            return baseUrlOverride;
        }
        return PUBLIC_BASE_URL;
    }

    @Getter
    public static class HttpResponse {
        private final boolean successful;
        private final int code;
        private final String body;

        public HttpResponse(boolean successful, int code, String body) {
            this.successful = successful;
            this.code = code;
            this.body = body;
        }
    }
}
