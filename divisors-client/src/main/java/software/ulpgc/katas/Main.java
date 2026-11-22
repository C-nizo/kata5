package software.ulpgc.katas;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

    private static final String BASE_URL = "http://localhost:8080";

    static void main() throws InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            while (true) {
                int number = ThreadLocalRandom.current().nextInt(1, 101);

                check(client, "v1", number);
                check(client, "v2", number);

                Thread.sleep(1000);
            }
        }
    }

    private static void check(HttpClient client, String version, int number)
            throws InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/" + version + "/divisors/" + number))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            String node = response.headers()
                    .firstValue("X-Node")
                    .orElse("desconocido");

            System.out.println(
                    "[" + version + " | réplica=" + node + "] " +
                            "divisores(" + number + ") = " + response.body() +
                            " | HTTP " + response.statusCode()
            );

        } catch (IOException exception) {
            System.out.println(
                    "[" + version + "] divisores(" + number + ")" +
                            " | Error: " + exception.getMessage()
            );
        }
    }
}