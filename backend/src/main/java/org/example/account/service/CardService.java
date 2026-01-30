package org.example.account.service;

import lombok.RequiredArgsConstructor;
import org.example.account.domain.Card;
import org.example.account.dto.CardRequest;
import org.example.account.dto.CardResponse;
import org.example.account.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;

    @Transactional
    public CardResponse createCard(CardRequest request) {
        Card card = new Card(request.name(), request.type());
        return CardResponse.from(cardRepository.save(card));
    }

    public List<CardResponse> getAllCards() {
        return cardRepository.findAll().stream()
                .map(CardResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCard(Long id) {
        cardRepository.deleteById(id);
    }
}
