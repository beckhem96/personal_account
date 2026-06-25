package org.example.account.statement;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.account.domain.CardCompany;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ShinhanCardStatementParser implements CardStatementParser {

    private static final List<String> DATE_KEYWORDS = List.of("이용일자", "거래일자", "거래일", "승인일", "이용일");
    private static final List<String> MERCHANT_KEYWORDS = List.of("가맹점명", "이용가맹점", "가맹점", "이용처");
    private static final List<String> AMOUNT_KEYWORDS = List.of("이용금액", "거래금액", "금액", "승인금액", "결제금액");
    private static final List<String> INSTALLMENT_KEYWORDS = List.of("할부", "할부개월", "할부정보", "할부기간");

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    private static final Pattern INSTALLMENT_FRACTION = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");
    private static final Pattern INSTALLMENT_MONTHS = Pattern.compile("할부\\s*(\\d+)\\s*개월");
    private static final Pattern AMOUNT_CLEAN = Pattern.compile("[^0-9.\\-]");

    @Override
    public CardCompany company() {
        return CardCompany.SHINHAN;
    }

    @Override
    public List<ParsedTransaction> parse(InputStream xlsx) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(xlsx)) {
            Sheet sheet = workbook.getSheetAt(0);
            HeaderIndex header = findHeader(sheet);
            if (header == null) {
                throw new IllegalArgumentException("신한카드 명세서 형식을 인식할 수 없습니다.");
            }

            List<ParsedTransaction> result = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int r = header.rowIndex + 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    ParsedTransaction parsed = parseRow(row, header);
                    if (parsed != null) {
                        result.add(parsed);
                    }
                } catch (Exception e) {
                    log.warn("신한카드 명세서 {}행 파싱 실패: {}", r + 1, e.getMessage());
                }
            }
            return result;
        }
    }

    private ParsedTransaction parseRow(Row row, HeaderIndex header) {
        LocalDate date = readDate(row.getCell(header.dateCol));
        String merchant = readString(row.getCell(header.merchantCol));
        BigDecimal amount = readAmount(row.getCell(header.amountCol));

        if (date == null || merchant == null || merchant.isBlank() || amount == null || amount.signum() == 0) {
            return null;
        }

        Integer months = null;
        Integer seq = null;
        if (header.installmentCol >= 0) {
            String raw = readString(row.getCell(header.installmentCol));
            int[] parsed = parseInstallment(raw);
            if (parsed != null) {
                seq = parsed[0];
                months = parsed[1];
            }
        }

        String naturalKey = buildNaturalKey(date, merchant, amount, months);
        return new ParsedTransaction(date, merchant.trim(), amount, null, months, seq, naturalKey);
    }

    private String buildNaturalKey(LocalDate date, String merchant, BigDecimal amount, Integer installmentMonths) {
        String monthsPart = installmentMonths == null ? "1" : String.valueOf(installmentMonths);
        return String.format("SHINHAN:%s:%s:%s:%s", date, merchant.trim(), amount.toPlainString(), monthsPart);
    }

    private int[] parseInstallment(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();

        if (trimmed.contains("일시불") || trimmed.equals("0") || trimmed.equals("일시")) {
            return null;
        }

        Matcher m = INSTALLMENT_FRACTION.matcher(trimmed);
        if (m.find()) {
            int seq = Integer.parseInt(m.group(1));
            int months = Integer.parseInt(m.group(2));
            return months > 1 ? new int[]{seq, months} : null;
        }

        Matcher m2 = INSTALLMENT_MONTHS.matcher(trimmed);
        if (m2.find()) {
            int months = Integer.parseInt(m2.group(1));
            return months > 1 ? new int[]{1, months} : null;
        }

        try {
            int months = Integer.parseInt(trimmed.replaceAll("[^0-9]", ""));
            return months > 1 ? new int[]{1, months} : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private HeaderIndex findHeader(Sheet sheet) {
        int lastRow = Math.min(sheet.getLastRowNum(), 30);
        for (int r = 0; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            Map<String, Integer> columns = new HashMap<>();
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                if (cell == null) continue;
                String value = readString(cell);
                if (value != null && !value.isBlank()) {
                    columns.put(value.trim(), c);
                }
            }

            Integer dateCol = matchColumn(columns, DATE_KEYWORDS);
            Integer merchantCol = matchColumn(columns, MERCHANT_KEYWORDS);
            Integer amountCol = matchColumn(columns, AMOUNT_KEYWORDS);
            Integer installmentCol = matchColumn(columns, INSTALLMENT_KEYWORDS);

            if (dateCol != null && merchantCol != null && amountCol != null) {
                return new HeaderIndex(r, dateCol, merchantCol, amountCol, installmentCol == null ? -1 : installmentCol);
            }
        }
        return null;
    }

    private Integer matchColumn(Map<String, Integer> columns, List<String> keywords) {
        for (Map.Entry<String, Integer> entry : columns.entrySet()) {
            String key = entry.getKey();
            for (String keyword : keywords) {
                if (key.contains(keyword)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private LocalDate readDate(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
        } catch (Exception ignored) {
        }

        String raw = readString(cell);
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();

        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(trimmed, fmt);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String readString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                if (v == Math.floor(v) && !Double.isInfinite(v)) {
                    yield String.valueOf((long) v);
                }
                yield String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        yield String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        yield null;
                    }
                }
            }
            default -> null;
        };
    }

    private BigDecimal readAmount(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String raw = readString(cell);
        if (raw == null || raw.isBlank()) return null;
        String cleaned = AMOUNT_CLEAN.matcher(raw.trim()).replaceAll("");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) return null;
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record HeaderIndex(int rowIndex, int dateCol, int merchantCol, int amountCol, int installmentCol) {
    }
}
