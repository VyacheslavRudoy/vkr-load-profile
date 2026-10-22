package ru.rudoy.loadprofile.generator;

import java.util.List;

/** План теста: цель и запросы. */
public record JmeterPlan(String host, int port, List<HttpRequest> requests) {

    public JmeterPlan {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535: " + port);
        }
        requests = List.copyOf(requests);
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("plan needs at least one request");
        }
    }

    public record HttpRequest(String method, String path) {

        public HttpRequest {
            if (method == null || method.isBlank()) {
                throw new IllegalArgumentException("method must not be blank");
            }
            if (path == null || !path.startsWith("/")) {
                throw new IllegalArgumentException("path must start with '/': " + path);
            }
        }
    }
}
