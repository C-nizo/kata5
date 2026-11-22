package software.ulpgc.katas.infrastructure;

import io.javalin.Javalin;
import software.ulpgc.katas.architecture.operations.DivisorsCalculationOperation;
import software.ulpgc.katas.infrastructure.adapters.ConsulServiceRegistrar;
import software.ulpgc.katas.infrastructure.adapters.DivisorsEndPoint;
import software.ulpgc.katas.infrastructure.adapters.JsonDivisorsSerializer;

import java.io.IOException;

public class Main {

    static void main(String[] args) throws IOException, InterruptedException {
        int port = Integer.parseInt(args[0]);
        String id = args[1];
        String serviceName = args[2];
        String consulAddress = args[3];

        DivisorsCalculationOperation operation = new DivisorsCalculationOperation(new JsonDivisorsSerializer());
        DivisorsEndPoint endPoint = new DivisorsEndPoint(operation);

        Javalin app = Javalin.create(config -> {
            config.routes.get("/divisors/{n}", endPoint::execute);
            config.routes.before(context -> context.header("X-Node", id));
            config.routes.get("/health", context -> context.result("OK"));
        }).start(port);

        try {
            new ConsulServiceRegistrar().register(consulAddress, id, serviceName, port);
            System.out.println("Instancia " + id + " registrada en Consul");
        } catch (IOException | InterruptedException | RuntimeException e) {
            app.stop();
            throw e;
        }
    }
}