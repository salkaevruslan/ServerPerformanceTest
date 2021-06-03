package data;

public class DataArray {
    private final int id;
    private final int[] values;

    public DataArray(int id, int[] values) {
        this.id = id;
        this.values = values;
    }

    public int getId() {
        return id;
    }

    public int[] getValues() {
        return values;
    }

    public int getLength() {
        return values.length;
    }
}
