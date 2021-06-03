package util;

import java.util.ArrayList;

public class BubbleSorter {
    public static void sort(int[] data) {
        int n = data.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1 - i; j++) {
                if (data[j] > data[j + 1]) {
                    int c = data[j];
                    data[j] = data[j + 1];
                    data[j + 1] = c;
                }
            }
        }
    }
}
