package dk.teamonline.table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CommandLineTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineTable.class);
    private final String name;
    private String[] headers;
    private List<String[]> rows = new ArrayList<>();
    private final Map<Integer, Integer> columnLengths = new HashMap<>();
    private int maxWidth = 50;
    private boolean leftJustifiedRows = true;

    public CommandLineTable(String name) {
        this.name = name;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public void setHeaders(String... headers) {
        this.headers = headers;
    }

    public void addRow(String... cells) {
        rows.add(cells);
    }

    public void printTable() {
        this.rows = getWrappedRows();
        calculateColumnsLength(rows);
        final String horizontalLine = computeHorizontalLine();
        final String cellFormatting = computeCellFormatPattern();
        printHeaders(horizontalLine, cellFormatting);
        printBody(horizontalLine, cellFormatting);
    }

    private void printHeaders(String horizontalLine, String cellFormatting) {
        LOGGER.info(horizontalLine);
        LOGGER.info(String.format(cellFormatting, headers));
        LOGGER.info(horizontalLine);
    }

    private void printBody(String horizontalLine, String cellFormatting) {
        rows.forEach(cells ->  LOGGER.info(String.format(cellFormatting, cells)));
        if (!rows.isEmpty()) {
            LOGGER.info(horizontalLine);
        }
    }

    private List<String[]> getWrappedRows() {
        List<String[]> wrappedRows = new ArrayList<>();
        for (String[] row : rows) {
            // If any cell data is more than max width, then it will need extra row.
            boolean needExtraRow = false;
            // Count of extra split row.
            int splitRow = 0;
            do {
                needExtraRow = false;
                String[] newRow = new String[row.length];
                for (int i = 0; i < row.length; i++) {
                    String cell = row[i];
                    String[] splitValues = cell.split("\n");
                    if (splitValues.length > 1 && splitValues.length > splitRow) {
                        newRow[i] = splitValues[splitRow];
                        needExtraRow = true;
                        continue;
                    }
                    // If data is less than max width, use that as it is.
                    if (cell.length() < maxWidth) {
                        newRow[i] = splitRow == 0 ? cell : "";
                    } else if ((cell.length() > (splitRow * maxWidth))) {
                        // If data is more than max width, then crop data at maxWidth.
                        // Remaining cropped data will be part of next row.
                        int end = Math.min(cell.length(), ((splitRow * maxWidth) + maxWidth));
                        newRow[i] = cell.substring((splitRow * maxWidth), end);
                        needExtraRow = true;
                    } else {
                        newRow[i] = "";
                    }
                }
                wrappedRows.add(newRow);
                if (needExtraRow) {
                    splitRow++;
                }
            } while (needExtraRow);
        }
        return wrappedRows;
    }

    private void calculateColumnsLength(List<String[]> rows) {
        for (int i = 0; i < headers.length; i++) {
            calculateLengthForRow(headers[i], i);
        }

        rows.forEach(row -> Stream.iterate(0, (i -> i < row.length), (i -> ++i))
            .forEach(i -> calculateLengthForRow(row[i], i))
        );
    }

    private void calculateLengthForRow(String cell, int columnIndex) {
        columnLengths.putIfAbsent(columnIndex, 0);
        if (columnLengths.get(columnIndex) < cell.length()) {
            columnLengths.put(columnIndex, cell.length());
        }
    }

    private String computeHorizontalLine() {
        String line = columnLengths.values().stream().reduce("", (ln, columnLength) -> {
            String templn = Stream.iterate(0, (i -> i < columnLength), (i -> ++i))
                .reduce("+-", (ln1, b1) -> ln1 + "-", (a1, b1) -> a1 + b1);
            return ln + templn + "-";
        }, (a, b) -> a + b);
//        line = line + "+\n";
        return line;
    }

    private String computeCellFormatPattern() {
        final StringBuilder formatString = new StringBuilder("");
        String flag = leftJustifiedRows ? "-" : "";
        columnLengths.forEach((key, value) -> formatString.append("| %").append(flag).append(value).append("s "));
        formatString.append("|");
        return formatString.toString();
    }


    public static void main(String[] args) {
        CommandLineTable commandLineTableEx = new CommandLineTable("Template");
        commandLineTableEx.setHeaders("id", "First Name", "Last Name", "Age", "Profile");
        commandLineTableEx.addRow("1", "John", "Johnson", "45", "My name is John Johnson. My id is 1. My age is 45.");
        commandLineTableEx.addRow("2", "Tom", "", "35", "My name is Tom. My id is 2. My age is 35.");
        commandLineTableEx.addRow("3", "Rose", "Johnson Johnson Johnson Johnson Johnson Johnson Johnson Johnson Johnson Johnson", "22",
            "My name is Rose Johnson. My id is 3. My age is 22.");
        commandLineTableEx.addRow("4", "Jimmy", "Kimmel", "", "My name is Jimmy Kimmel. My id is 4. My age is not specified. "
            + "I am the host of the late night show. I am not fan of Matt Damon. ");
        commandLineTableEx.printTable();
    }
}
