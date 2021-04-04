package dk.teamonline.table;

import java.util.ArrayList;
import java.util.List;

public class ExcelSheet {
    private final String name;
    private final String[] headers;
    private List<String[]> rows = new ArrayList<>();

    public ExcelSheet(String name, String[] headers) {
        this.name = name;
        this.headers = headers;
    }

    public void addRow(String... cells) {
        rows.add(cells);
    }

    public String getName() {
        return name;
    }

    public String[] getHeaders() {
        return headers;
    }

    public List<String[]> getRows() {
        return rows;
    }
}
