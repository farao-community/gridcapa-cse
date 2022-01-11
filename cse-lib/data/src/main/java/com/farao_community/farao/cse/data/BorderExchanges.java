/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static com.farao_community.farao.cse.data.DateTimeUtil.checkDate;
import static com.farao_community.farao.cse.data.DateTimeUtil.getVulcanusTime;

/*
 * Temporary recovery of border exchanges
 * from vulcanus file
 */
public final class BorderExchanges {
    private static final String REFERENCE_SHEET = "Sheet 31";
    private static final int DATE_ROW = 3;
    private static final int DATE_COL = 1;
    private static final int LABEL_ROW = 5;
    private static final int TIME_COL = 0;

    private BorderExchanges() {
        // Should not be instantiated
    }

    public static Map<String, Double> fromVulcanusFile(OffsetDateTime targetDateTime, InputStream vulcanusFile) throws IOException {
        HSSFWorkbook workbook = new HSSFWorkbook(vulcanusFile);
        HSSFSheet worksheet = workbook.getSheet(REFERENCE_SHEET);
        String vulcanusTime = getVulcanusTime(targetDateTime);

        LocalDate vulcanusDate = LocalDate.parse(
                worksheet.getRow(DATE_ROW).getCell(DATE_COL).getStringCellValue(),
                DateTimeFormatter.ofPattern("dd.MM.yyyy")
        );
        checkDate(vulcanusDate, targetDateTime);
        HSSFRow targetRow = getRow(worksheet, vulcanusTime);
        return getBordersFlowsFromRow(worksheet, targetRow);
    }

    private static HSSFRow getRow(HSSFSheet worksheet, String vulcanusTime) {
        for (int rowIndex = 0; rowIndex < worksheet.getPhysicalNumberOfRows(); rowIndex++) {
            if (worksheet.getRow(rowIndex) != null && worksheet.getRow(rowIndex).getCell(TIME_COL).getStringCellValue().equals(vulcanusTime)) {
                return worksheet.getRow(rowIndex);
            }
        }
        throw new IllegalStateException(String.format("Impossible to find the following time in the file : %s. Format error.", vulcanusTime));
    }

    private static Map<String, Double> getBordersFlowsFromRow(HSSFSheet worksheet, HSSFRow row) {
        Map<String, Double> bordersExchanges = new HashMap<>();
        for (int colIndex = 0; colIndex < row.getPhysicalNumberOfCells(); colIndex++) {
            String exchangeStr = worksheet.getRow(LABEL_ROW).getCell(colIndex).getStringCellValue();
            switch (exchangeStr) {
                case "CH > IT":
                    bordersExchanges.put("CH - IT", row.getCell(colIndex).getNumericCellValue());
                    break;
                case "FR > IT":
                    bordersExchanges.put("FR - IT", row.getCell(colIndex).getNumericCellValue());
                    break;
                case "IT > APG":
                    bordersExchanges.put("AT - IT", -row.getCell(colIndex).getNumericCellValue());
                    break;
                case "IT > SHB":
                    bordersExchanges.put("SI - IT", -row.getCell(colIndex).getNumericCellValue());
                    break;
                case "CH > FR":
                    bordersExchanges.put("CH - FR", row.getCell(colIndex).getNumericCellValue());
                    break;
                case "FR > DE":
                    bordersExchanges.put("FR - DE", row.getCell(colIndex).getNumericCellValue());
                    break;
                case "CH > DE+":
                    bordersExchanges.put("CH - DE", row.getCell(colIndex).getNumericCellValue());
                    break;
                default:
                    break;
            }
        }
        return bordersExchanges;
    }

}
