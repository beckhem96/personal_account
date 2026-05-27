package org.example.account.statement;

import org.example.account.domain.CardCompany;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class CardStatementParserRegistry {

    private final Map<CardCompany, CardStatementParser> parsers = new HashMap<>();

    public CardStatementParserRegistry(List<CardStatementParser> parserList) {
        for (CardStatementParser parser : parserList) {
            parsers.put(parser.company(), parser);
        }
    }

    public Optional<CardStatementParser> get(CardCompany company) {
        if (company == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(parsers.get(company));
    }

    public Set<CardCompany> supportedCompanies() {
        return parsers.keySet();
    }
}
