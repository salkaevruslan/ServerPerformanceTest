package util;

import com.google.protobuf.InvalidProtocolBufferException;
import data.DataArray;
import data.DataArrayProto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class StreamUtils {

    public static DataArray readData(DataInputStream inputStream) throws IOException {
        int size = inputStream.readInt();
        DataArrayProto.DataArray response = DataArrayProto.DataArray.parseFrom(inputStream.readNBytes(size));
        return dataArrayFromProto(response);
    }

    public static void writeData(DataOutputStream outputStream, DataArray data) throws IOException {
        byte[] bytes = toByteArray(data);
        outputStream.writeInt(bytes.length);
        outputStream.write(bytes);
        outputStream.flush();
    }

    public static DataArray readData(ByteBuffer buffer) throws InvalidProtocolBufferException {
        DataArrayProto.DataArray response = DataArrayProto.DataArray.parseFrom(buffer);
        return dataArrayFromProto(response);
    }

    public static ByteBuffer toByteBuffer(DataArray data) {
        byte[] bytes = toByteArray(data);
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length + Integer.BYTES);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    private static DataArray dataArrayFromProto(DataArrayProto.DataArray response) {
        int id = response.getId();
        int[] data = response.getValueList().stream().mapToInt(i -> i).toArray();
        return new DataArray(id, data);
    }

    private static byte[] toByteArray(DataArray data) {
        DataArrayProto.DataArray request = DataArrayProto.DataArray.newBuilder().
                setId(data.getId()).
                addAllValue(() -> Arrays.stream(data.getValues()).iterator()).
                build();
        return request.toByteArray();
    }
}
