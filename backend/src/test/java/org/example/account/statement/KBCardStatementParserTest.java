package org.example.account.statement;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KBCardStatementParserTest {

    private final KBCardStatementParser parser = new KBCardStatementParser();

    @Test
    void 국민카드_엑셀_파일에서_일반_및_할부_거래_파싱() throws Exception {
        byte[] xlsx = buildSampleXlsx();
        List<ParsedTransaction> result = parser.parse(new ByteArrayInputStream(xlsx));

        assertThat(result).hasSize(2);

        ParsedTransaction first = result.get(0);
        assertThat(first.merchant()).isEqualTo("파리바게뜨 강남");
        assertThat(first.amount()).isEqualByComparingTo("18000");
        assertThat(first.isInstallment()).isFalse();
        assertThat(first.naturalKey()).startsWith("KB:");

        ParsedTransaction second = result.get(1);
        assertThat(second.merchant()).isEqualTo("쿠팡 결제");
        assertThat(second.amount()).isEqualByComparingTo("300000");
        assertThat(second.installmentMonths()).isEqualTo(3);
        assertThat(second.installmentSeq()).isEqualTo(1);
    }

    private byte[] buildSampleXlsx() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("국민카드내역");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("이용일");
            header.createCell(1).setCellValue("이용처");
            header.createCell(2).setCellValue("결제금액");
            header.createCell(3).setCellValue("할부정보");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("2026-06-10");
            r1.createCell(1).setCellValue("파리바게뜨 강남");
            r1.createCell(2).setCellValue("18,000");
            r1.createCell(3).setCellValue("일시");

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("2026-06-11");
            r2.createCell(1).setCellValue("쿠팡 결제");
            r2.createCell(2).setCellValue(300000);
            r2.createCell(3).setCellValue("1/3");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}
