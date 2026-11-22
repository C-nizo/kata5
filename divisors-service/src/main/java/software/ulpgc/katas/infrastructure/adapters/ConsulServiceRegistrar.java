package software.ulpgc.katas.infrastructure.adapters;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class ConsulServiceRegistrar {
    public void register(String consulAddress, String id, String serviceName, int port)
            throws IOException, InterruptedException {

        String json = registrationJson(id, serviceName, port);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(consulAddress + "/v1/agent/service/register"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() / 100 != 2) {
                throw new IOException(
                        "Consul rechazó el registro: HTTP " + response.statusCode()
                );
            }
        }
    }

    private String registrationJson(String id, String serviceName, int port) {
        String host = "host.docker.internal";

        return new Gson().toJson(Map.of(
                "ID", id,
                "Name", serviceName,
                "Address", host,
                "Port", port,
                "Check", Map.of(
                        "HTTP", "http://" + host + ":" + port + "/health",
                        "Interval", "5s",
                        "Timeout", "2s"
                )
        ));
    }
}