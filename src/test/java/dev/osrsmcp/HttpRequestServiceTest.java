package dev.osrsmcp;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class HttpRequestServiceTest {

    private static final String URL = "https://osrsmcp.retrodadson.com/api/death";

    private final CountDownLatch releaseServer = new CountDownLatch(1);
    private final CountDownLatch requestHandled = new CountDownLatch(1);
    private final AtomicReference<Request> seenRequest = new AtomicReference<>();
    private final AtomicReference<String> seenBody = new AtomicReference<>();
    private final AtomicReference<String> requestThread = new AtomicReference<>();
    private OkHttpClient client;

    /** A "server" that holds every request until the test releases it, so a blocking call would hang the test. */
    private OkHttpClient slowServer(boolean fail) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    requestThread.set(Thread.currentThread().getName());
                    seenRequest.set(chain.request());
                    Buffer buffer = new Buffer();
                    chain.request().body().writeTo(buffer);
                    seenBody.set(buffer.readUtf8());
                    try {
                        releaseServer.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    requestHandled.countDown();
                    if (fail) {
                        throw new IOException("server unreachable");
                    }
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create(MediaType.parse("application/json"), "{}"))
                            .build();
                })
                .build();
    }

    private HttpRequestService service(OkHttpClient httpClient) {
        client = httpClient;
        return new HttpRequestService(httpClient, new OsrsMcpConfig() {
        }, new Gson());
    }

    @After
    public void tearDown() {
        releaseServer.countDown();
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }

    @Test
    public void postAsyncReturnsBeforeTheServerResponds() throws Exception {
        HttpRequestService service = service(slowServer(false));

        long start = System.nanoTime();
        service.postAsync(URL, "my-token", new DeathEvent("Zezima", "Goblin"));
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue("postAsync blocked for " + elapsedMs + "ms", elapsedMs < 1000);

        releaseServer.countDown();
        assertTrue(requestHandled.await(5, TimeUnit.SECONDS));
        assertNotEquals(Thread.currentThread().getName(), requestThread.get());
    }

    @Test
    public void postAsyncSendsAuthAndJsonBody() throws Exception {
        HttpRequestService service = service(slowServer(false));
        releaseServer.countDown();

        service.postAsync(URL, "my-token", new DeathEvent("Zezima", "Goblin"));

        assertTrue(requestHandled.await(5, TimeUnit.SECONDS));
        assertEquals("POST", seenRequest.get().method());
        assertEquals("my-token", seenRequest.get().header("Authorization"));
        assertEquals(new Gson().toJson(new DeathEvent("Zezima", "Goblin")), seenBody.get());
    }

    @Test
    public void postAsyncSwallowsNetworkFailures() throws Exception {
        HttpRequestService service = service(slowServer(true));
        releaseServer.countDown();

        service.postAsync(URL, "my-token", new DeathEvent("Zezima", "Goblin"));

        assertTrue(requestHandled.await(5, TimeUnit.SECONDS));
    }
}
