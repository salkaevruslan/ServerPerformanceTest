package util;

import data.DataArray;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class StreamUtils {

    public static DataArray readData(DataInputStream inputStream) throws IOException {
        int id = inputStream.readInt();
        int size = inputStream.readInt();
        int[] data = new int[size];
        for (int i = 0; i < size; i++) {
            data[i] = inputStream.readInt();
        }
        return new DataArray(id, data);
    }

    public static void writeData(DataOutputStream outputStream, DataArray data) throws IOException {
        outputStream.writeInt(data.getId());
        outputStream.writeInt(data.getLength());
        for (int x : data.getValues()) {
            outputStream.writeInt(x);
        }
    }

}
