package software.ulpgc.katas.architecture.operations;

import software.ulpgc.katas.architecture.domain.DivisorsCalculator;
import software.ulpgc.katas.architecture.ports.DivisorsSerializer;

public class DivisorsCalculationOperation {
    private final DivisorsSerializer serializer;

    public DivisorsCalculationOperation(DivisorsSerializer serializer) {
        this.serializer = serializer;
    }

    public String execute(int number) {
        return serializer.serialize(DivisorsCalculator.calculate(number));
    }
}
