package util;

import java.util.ArrayList;

public class BubbleSorter {
    public static void sort(ArrayList<Integer> data) {
        int n = data.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1 - i; j++) {
                if (data.get(j) > data.get(j + 1)) {
                    int c = data.get(j);
                    data.set(j, data.get(j + 1));
                    data.set(j + 1, c);
                }
            }
        }
    }
}
