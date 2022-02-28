/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.data;

import com.farao_community.farao.commons.EICode;
import com.powsybl.iidm.network.Country;
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

import static com.farao_community.farao.cse.data.DateTimeUtil.*;

/**
 * @author Belgacem Najjari {@literal <belgacem.najjari at rte-france.com>}
 */
public final class CseReferenceExchanges {
    private static final String REFERENCE_SHEET = "Sheet 31";
    private static final String POSTFIX_VULCANUS_NAME_OLD_VERSION = "_96.xls";
    private static final int DATE_ROW = 3;
    private static final int DATE_COL = 1;
    private static final int LABEL_ROW = 5;
    private static final int TIME_COL = 0;

    private final Map<String, Double> exchanges;

    private CseReferenceExchanges(Map<String, Double> exchanges) {
        this.exchanges = exchanges;
    }

    public static CseReferenceExchanges fromVulcanusFile(OffsetDateTime targetDateTime, InputStream vulcanusFile, String vulcanusName) throws IOException {
        HSSFWorkbook workbook = new HSSFWorkbook(vulcanusFile);
        HSSFSheet worksheet = workbook.getSheet(REFERENCE_SHEET);
        String vulcanusTime = vulcanusName.toLowerCase().contains(POSTFIX_VULCANUS_NAME_OLD_VERSION) ? getVulcanusTimeFromVulcanusFileOldVersion(targetDateTime) : getVulcanusTimeFromVulcanusFileNewVersion(targetDateTime);
        LocalDate vulcanusDate = LocalDate.parse(
                worksheet.getRow(DATE_ROW).getCell(DATE_COL).getStringCellValue(),
                DateTimeFormatter.ofPattern("dd.MM.yyyy")
        );
        checkDate(vulcanusDate, targetDateTime);
        HSSFRow targetRow = getRow(worksheet, vulcanusTime);
        return new CseReferenceExchanges(getExchangesFromRow(worksheet, targetRow));
    }

    public Map<String, Double> getExchanges() {
        return new HashMap<>(exchanges);
    }

    double getExchange(Country country) {
        return exchanges.get(new EICode(country).getAreaCode());
    }

    private static HSSFRow getRow(HSSFSheet worksheet, String vulcanusTime) {
        for (int rowIndex = 0; rowIndex < worksheet.getPhysicalNumberOfRows(); rowIndex++) {
            if (worksheet.getRow(rowIndex) != null && worksheet.getRow(rowIndex).getCell(TIME_COL).getStringCellValue().equals(vulcanusTime)) {
                return worksheet.getRow(rowIndex);
            }
        }
        throw new IllegalStateException(String.format("Impossible to find the following time in the file : %s. Format error.", vulcanusTime));
    }

    private static Map<String, Double> getExchangesFromRow(HSSFSheet worksheet, HSSFRow row) {
        Map<String, Double> exchanges = new HashMap<>();
        for (int colIndex = 0; colIndex < row.getPhysicalNumberOfCells(); colIndex++) {
            String exchangeStr = worksheet.getRow(LABEL_ROW).getCell(colIndex).getStringCellValue();
            switch (exchangeStr) {
                case "CH > IT":
                    exchanges.put(new EICode(Country.CH).getAreaCode(), row.getCell(colIndex).getNumericCellValue());
                    break;
                case "FR > IT":
                    exchanges.put(new EICode(Country.FR).getAreaCode(), row.getCell(colIndex).getNumericCellValue());
                    break;
                case "IT > APG":
                    exchanges.put(new EICode(Country.AT).getAreaCode(), -row.getCell(colIndex).getNumericCellValue());
                    break;
                case "IT > SHB":
                    exchanges.put(new EICode(Country.SI).getAreaCode(), -row.getCell(colIndex).getNumericCellValue());
                    break;
                default:
                    break;
            }
        }
        return exchanges;
    }

}

