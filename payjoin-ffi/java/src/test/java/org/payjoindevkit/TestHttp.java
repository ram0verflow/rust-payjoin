package org.payjoindevkit;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * HTTPS client that trusts only the directory's rcgen test certificate and
 * proxies every request through the local OHTTP relay.
 */
public final class TestHttp implements AutoCloseable {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient client;

    public TestHttp(TestServices services) {
        Objects.requireNonNull(services, "services");
        try {
            X509Certificate cert =
                    (X509Certificate)
                            CertificateFactory.getInstance("X.509")
                                    .generateCertificate(
                                            new ByteArrayInputStream(services.cert()));
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("directory", cert);
            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            URI relay = URI.create(services.ohttpRelayUrl());
            int port = relay.getPort() == -1 ? 80 : relay.getPort();

            this.client =
                    HttpClient.newBuilder()
                            .sslContext(sslContext)
                            .proxy(
                                    ProxySelector.of(
                                            new InetSocketAddress(relay.getHost(), port)))
                            .connectTimeout(TIMEOUT)
                            .build();
        } catch (Exception e) {
            throw new IllegalStateException("failed to build directory HTTP client", e);
        }
    }

    /**
     * POST {@code request} and return the response body. All OHTTP directory
     * traffic is POST; there is no GET path in the v2 integration test.
     */
    public byte[] post(Request request) {
        Objects.requireNonNull(request, "request");
        try {
            HttpRequest httpRequest =
                    HttpRequest.newBuilder(URI.create(request.url()))
                            .timeout(TIMEOUT)
                            .header("Content-Type", request.contentType())
                            .POST(HttpRequest.BodyPublishers.ofByteArray(request.body()))
                            .build();
            HttpResponse<byte[]> response =
                    client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "HTTP " + response.statusCode() + " from " + request.url());
            }
            return response.body();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("POST " + request.url() + " failed", e);
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
