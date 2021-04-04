package dk.teamonline.utils;


import dk.teamonline.domain.ModuleSummary;
import dk.teamonline.table.ExcelSheet;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class ExcelTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelTransformer.class);
    private final Workbook workbook;
    private final CellStyle headerStyle;
    private final CellStyle cellStyle;
    private final CellStyle mergedcellStyle;

    public ExcelTransformer() {
        this.workbook = new XSSFWorkbook();
        this.cellStyle = getCellStyle(workbook);
        this.headerStyle = getHeaderCellStyle(workbook);
        this.mergedcellStyle = getMergedCellStyle(workbook);
    }

    private CellStyle getHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle getCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        return style;
    }

    private CellStyle getMergedCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER_SELECTION);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public void addSheetData(ExcelSheet table) {
        Sheet sheet = workbook.createSheet(table.getName());
        // headers
        String[] headers = table.getHeaders();
        int rowNumber = 0;
        Row headerRow = sheet.createRow(rowNumber);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(headers[i]);
        }
        // data
        for (int i = 0; i < table.getRows().size(); i++) {
            Row row = sheet.createRow(++rowNumber);
            String[] cellValues = table.getRows().get(i);
            // merge cells
            if (cellValues.length == 1) {
                CellRangeAddress cellRangeAddress = new CellRangeAddress(rowNumber, rowNumber, 0, headers.length - 1);
                sheet.addMergedRegion(cellRangeAddress);
                CellUtil.createCell(row, 0, cellValues[0], mergedcellStyle);
                continue;
            }
            int maxBreakLines = 1;
            for (int j = 0; j < cellValues.length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellStyle(cellStyle);
                String cellValue = cellValues[j];
                maxBreakLines = Math.max(cellValue.split("\n").length, maxBreakLines);
                cell.setCellValue(cellValue);
            }
            row.setHeightInPoints(maxBreakLines * sheet.getDefaultRowHeightInPoints());
        }
        adjustColumnWidth(sheet);
    }

    private void adjustColumnWidth(Sheet sheet) {
        Iterator<Cell> cellIterator = sheet.getRow(0).cellIterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            sheet.autoSizeColumn(cell.getColumnIndex());
        }
    }

    public void exportToFile(ModuleSummary moduleSummary) {
        moduleSummary.getSheets().forEach(this::addSheetData);
        Path excelFilePath = Paths.get("").normalize().toAbsolutePath().resolve("userTrackVerifier/domain/target")
            .resolve("userTrack-analyzer_" + moduleSummary.getModuleName() + ".xlsx");
        try {
            Files.createDirectories(excelFilePath.getParent());
            try (FileOutputStream outputStream = new FileOutputStream(excelFilePath.toFile())) {
                workbook.write(outputStream);
            }
        } catch (IOException e) {
            LOGGER.error("Unable to export excel to file", e);
        }
    }
}
