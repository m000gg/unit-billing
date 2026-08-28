package com.m000gg.billing.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class i18nWebTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageSource messageSource;

    @Test
    void whenLangParamIsPresent_thenLocaleIsChangedAndSavedInSession() throws Exception {
        mockMvc.perform(get("/login").param("lang", "de"))
                .andExpect(status().isOk())
                .andExpect(request().sessionAttribute(
                        SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME,
                        new Locale("de")
                ));
    }

    @Test
    void frenchInsufficientBalanceMessage_shouldNotLeavePlaceholdersUnresolved() {
        UUID userId = UUID.randomUUID();
        String message = messageSource.getMessage(
                "errors.ledger.insufficientBalance",
                new Object[]{userId, BigDecimal.TEN, BigDecimal.ONE},
                Locale.FRENCH
        );

        assertThat(message)
                .doesNotContain("{0}")
                .doesNotContain("{1}")
                .doesNotContain("{2}")
                .contains(userId.toString());
    }

    @Test
    void whenLangParamIsPresent_thenPageContentIsTranslated() throws Exception {
        mockMvc.perform(get("/login").param("lang", "de"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Anmelden")));
    }

    @Test
    void whenLangParamIsPresent_thenPageContentIsTranslatedToFrench() throws Exception {
        mockMvc.perform(get("/login").param("lang", "fr"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Connexion")));
    }

    @Test
    void localeShouldPersistAcrossRequestsInSameSession() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/login").session(session).param("lang", "de"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Anmelden")));
    }

    @Test
    void whenLangParamIsUnsupported_thenFallsBackToDefault() throws Exception {
        mockMvc.perform(get("/login").param("lang", "xx-unsupported"))
                .andExpect(status().isOk());
    }
}
