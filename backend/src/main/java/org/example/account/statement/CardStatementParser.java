package org.example.account.statement;

import org.example.account.domain.CardCompany;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface CardStatementParser {

    CardCompany company();

    List<ParsedTransaction> parse(InputStream xlsx) throws IOException;
}
