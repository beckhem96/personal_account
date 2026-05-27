package org.example.account.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType type;

    @Enumerated(EnumType.STRING)
    private CardCompany company;

    public Card(String name, CardType type) {
        this.name = name;
        this.type = type;
    }

    public Card(String name, CardType type, CardCompany company) {
        this.name = name;
        this.type = type;
        this.company = company;
    }

    public void update(String name, CardType type) {
        this.name = name;
        this.type = type;
    }

    public void update(String name, CardType type, CardCompany company) {
        this.name = name;
        this.type = type;
        this.company = company;
    }
}
