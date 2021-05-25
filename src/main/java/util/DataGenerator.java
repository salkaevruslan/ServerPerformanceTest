package util;

import java.util.ArrayList;
import java.util.Random;

public class DataGenerator {
    private static final Random random = new Random();

    public static ArrayList<Integer> gen(int n) {
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(random.nextInt());
        }
        return result;
    }
}
