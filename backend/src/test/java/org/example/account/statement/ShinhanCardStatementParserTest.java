package org.example.account.statement;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShinhanCardStatementParserTest {

    private final ShinhanCardStatementParser parser = new ShinhanCardStatementParser();

    @Test
    void 신한카드_엑셀_파일에서_일반_및_할부_거래_파싱() throws Exception {
        byte[] xlsx = buildSampleXlsx();
        List<ParsedTransaction> result = parser.parse(new ByteArrayInputStream(xlsx));

        assertThat(result).hasSize(2);

        ParsedTransaction first = result.get(0);
        assertThat(first.merchant()).isEqualTo("스타벅스 명동");
        assertThat(first.amount()).isEqualByComparingTo("5400");
        assertThat(first.isInstallment()).isFalse();
        assertThat(first.naturalKey()).startsWith("SHINHAN:");

        ParsedTransaction second = result.get(1);
        assertThat(second.merchant()).isEqualTo("하이마트");
        assertThat(second.amount()).isEqualByComparingTo("1200000");
        assertThat(second.installmentMonths()).isEqualTo(6);
        assertThat(second.installmentSeq()).isEqualTo(1);
    }

    private byte[] buildSampleXlsx() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("신한카드이용내역");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("이용일자");
            header.createCell(1).setCellValue("가맹점명");
            header.createCell(2).setCellValue("이용금액");
            header.createCell(3).setCellValue("할부개월");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("2026-06-01");
            r1.createCell(1).setCellValue("스타벅스 명동");
            r1.createCell(2).setCellValue("5,400");
            r1.createCell(3).setCellValue("일시불");

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("2026-06-02");
            r2.createCell(1).setCellValue("하이마트");
            r2.createCell(2).setCellValue(1200000);
            r2.createCell(3).setCellValue("1/6");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}
