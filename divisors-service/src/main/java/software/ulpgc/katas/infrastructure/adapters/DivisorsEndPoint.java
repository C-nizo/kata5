package software.ulpgc.katas.infrastructure.adapters;

import io.javalin.http.Context;
import software.ulpgc.katas.architecture.operations.DivisorsCalculationOperation;

public class DivisorsEndPoint {
    private final DivisorsCalculationOperation operation;

    public DivisorsEndPoint(DivisorsCalculationOperation operation) {
        this.operation = operation;
    }

    public void execute(Context context) {
        try {
            int number = Integer.parseInt(context.pathParam("n"));
            String result = operation.execute(number);

            context.status(200)
                    .contentType("application/json")
                    .result(result);

        } catch (NumberFormatException e) {
            context.status(400)
                    .result("Introduce un valor entero para n.");

        } catch (IllegalArgumentException e) {
            context.status(400)
                    .result(e.getMessage());
        }
    }
}
