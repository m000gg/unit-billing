package com.m000gg.billing.ledger;
import com.m000gg.billing.identity.Admin;
import com.m000gg.billing.identity.AdminRepository;
import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.Instant;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class LedgerIntegrationTest {
    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private MockMvc mockMvc;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private ApplicationUser user;
    private Admin admin;

    @BeforeEach
    void setUp() {
        user = new ApplicationUser();
        user.setFirstName("John");
        user.setLastName("Pork");
        user.setEmail("ledger_test_user@example.com");
        user.setBalance(BigDecimal.valueOf(100));
        user = applicationUserRepository.save(user);

        admin = new Admin();
        admin.setEmail("ledger_test_admin@example.com");
        admin.setPassword("test-password-hash");
        admin = adminRepository.save(admin);
    }

    @AfterEach
    void cleanUp() {
        ledgerEntryRepository.deleteAll();
        applicationUserRepository.deleteAll();
        adminRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "ledger_test_admin@example.com", roles = "ADMIN")
    void applyTopUp_Success_UpdatesBalanceAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/topup", user.getId())
                        .with(csrf())
                        .param("amount", "50")
                        .param("description", "Cash payment at office"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/profile/" + user.getId()));

        ApplicationUser updated = applicationUserRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(150));

        boolean entrySaved = ledgerEntryRepository.findAll().stream()
                .anyMatch(e -> e.getSubscriberId().equals(user.getId()) && e.getType() == EntryType.PAYMENT);
        assertThat(entrySaved).isTrue();
    }

    @Test
    @WithMockUser(username = "ledger_test_admin@example.com", roles = "ADMIN")
    void applyTopUp_NegativeAmount_RejectedWithInlineError() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/topup", user.getId())
                        .with(csrf())
                        .param("amount", "-50")
                        .param("description", "Should be rejected"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/topup"))
                .andExpect(model().attributeHasFieldErrors("topUpRequest", "amount"));

        ApplicationUser unchanged = applicationUserRepository.findById(user.getId()).orElseThrow();
        assertThat(unchanged.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @WithMockUser(username = "ledger_test_admin@example.com", roles = "ADMIN")
    void issueBill_ExceedsBalance_RejectedWithInlineError() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/bill", user.getId())
                        .with(csrf())
                        .param("amount", "150")
                        .param("description", "Too much"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bill"))
                .andExpect(model().attributeHasFieldErrors("billRequest", "amount"));

        ApplicationUser unchanged = applicationUserRepository.findById(user.getId()).orElseThrow();
        assertThat(unchanged.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void applyTopUp_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/topup", user.getId())
                        .with(csrf())
                        .param("amount", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void search_onlyReturnsEntriesForGivenSubscriber() {
        ApplicationUser otherUser = new ApplicationUser();
        otherUser.setFirstName("Jane");
        otherUser.setLastName("Doe");
        otherUser.setEmail("ledger_test_other_user@example.com");
        otherUser.setBalance(BigDecimal.valueOf(100));
        otherUser = applicationUserRepository.save(otherUser);

        LedgerEntry entryA = new LedgerEntry();
        entryA.setSubscriberId(user.getId());
        entryA.setAmount(new BigDecimal("10.00"));
        entryA.setType(EntryType.CHARGE);
        entryA.setCreatedAt(Instant.now());
        entryA.setDescription("A's charge");
        entryA.setSource(EntrySource.ADMIN);
        ledgerEntryRepository.save(entryA);

        LedgerEntry entryB = new LedgerEntry();
        entryB.setSubscriberId(otherUser.getId());
        entryB.setAmount(new BigDecimal("10.00"));
        entryB.setType(EntryType.CHARGE);
        entryB.setCreatedAt(Instant.now());
        entryB.setDescription("B's charge");
        entryB.setSource(EntrySource.ADMIN);
        ledgerEntryRepository.save(entryB);

        Pageable pageable = PageRequest.of(0, 10);
        Page<LedgerEntry> result = ledgerEntryRepository.search(user.getId(), null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSubscriberId()).isEqualTo(user.getId());
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("A's charge");
    }
}