package software.ulpgc.katas.architecture.domain;

import java.util.stream.IntStream;

public class DivisorsCalculator {
    public static int[] calculate(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("El valor debe ser mayor que cero.");
        }

        return IntStream.rangeClosed(1, number)
                .filter(i -> number % i == 0)
                .toArray();
    }
}
