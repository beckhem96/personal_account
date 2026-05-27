package org.example.account.statement;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HanaCardStatementParserTest {

    private final HanaCardStatementParser parser = new HanaCardStatementParser();

    @Test
    void 할부_표기_파싱_분수형식() {
        assertThat(parser.parseInstallment("3/12")).containsExactly(3, 12);
        assertThat(parser.parseInstallment(" 1 / 6 ")).containsExactly(1, 6);
    }

    @Test
    void 할부_표기_파싱_텍스트형식() {
        assertThat(parser.parseInstallment("할부 3개월")).containsExactly(1, 3);
        assertThat(parser.parseInstallment("할부6개월")).containsExactly(1, 6);
    }

    @Test
    void 일시불은_null_반환() {
        assertThat(parser.parseInstallment("일시불")).isNull();
        assertThat(parser.parseInstallment("0")).isNull();
        assertThat(parser.parseInstallment("")).isNull();
        assertThat(parser.parseInstallment(null)).isNull();
    }

    @Test
    void 엑셀_파일에서_일반_거래_파싱() throws Exception {
        byte[] xlsx = buildSampleXlsx();
        List<ParsedTransaction> result = parser.parse(new ByteArrayInputStream(xlsx));

        assertThat(result).hasSize(3);

        ParsedTransaction first = result.get(0);
        assertThat(first.merchant()).isEqualTo("스타벅스 강남점");
        assertThat(first.amount()).isEqualByComparingTo("5800");
        assertThat(first.isInstallment()).isFalse();
        assertThat(first.naturalKey()).startsWith("HANA:");

        ParsedTransaction installment = result.get(2);
        assertThat(installment.merchant()).isEqualTo("LG전자");
        assertThat(installment.installmentMonths()).isEqualTo(3);
        assertThat(installment.installmentSeq()).isEqualTo(1);
    }

    @Test
    void 빈_행과_헤더가_없으면_예외() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row r = sheet.createRow(0);
            r.createCell(0).setCellValue("아무 컬럼");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            try {
                parser.parse(new ByteArrayInputStream(out.toByteArray()));
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).contains("형식");
                return;
            }
            throw new AssertionError("예외가 발생해야 합니다.");
        }
    }

    private byte[] buildSampleXlsx() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("이용내역");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("이용일");
            header.createCell(1).setCellValue("이용가맹점");
            header.createCell(2).setCellValue("이용금액");
            header.createCell(3).setCellValue("할부개월");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("2026-04-15");
            r1.createCell(1).setCellValue("스타벅스 강남점");
            r1.createCell(2).setCellValue("5,800");
            r1.createCell(3).setCellValue("일시불");

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("2026-04-16");
            r2.createCell(1).setCellValue("GS25 역삼점");
            r2.createCell(2).setCellValue(12400);
            r2.createCell(3).setCellValue("");

            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("2026-04-20");
            r3.createCell(1).setCellValue("LG전자");
            r3.createCell(2).setCellValue("900,000");
            r3.createCell(3).setCellValue("1/3");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    @SuppressWarnings("unused")
    private BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
