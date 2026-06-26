package org.example.account.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SubscriptionConfigIntegrationTest {

    @Value("${applyhome.api-key:}")
    private String applyhomeApiKey;

    @Value("${lh.api-key:}")
    private String lhApiKey;

    @Test
    void 공공데이터_API_키가_정상적으로_주입된다() {
        assertThat(applyhomeApiKey)
                .as("Applyhome API key should be injected and not blank")
                .isNotBlank();
        
        assertThat(lhApiKey)
                .as("LH API key should be injected and not blank")
                .isNotBlank();
    }
}
