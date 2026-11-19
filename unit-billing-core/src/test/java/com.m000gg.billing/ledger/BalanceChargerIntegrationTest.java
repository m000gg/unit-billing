/*
 * Copyright 2026 Vladyslav Livandovskyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m000gg.billing.ledger;

import com.m000gg.billing.identity.Admin;
import com.m000gg.billing.identity.AdminRepository;
import com.m000gg.billing.subscribers.ApplicationUser;
import com.m000gg.billing.subscribers.ApplicationUserRepository;
import com.m000gg.plugin.api.command.balance.BalanceCharger;
import com.m000gg.plugin.api.command.balance.ChargeRequest;
import com.m000gg.plugin.api.command.balance.ChargeResult;
import com.m000gg.plugin.api.command.balance.ChargeStatus;
import com.m000gg.plugin.api.event.BalanceChanged;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@RecordApplicationEvents
public class BalanceChargerIntegrationTest {

    @Autowired
    private BalanceCharger balanceCharger;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ApplicationEvents events;

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
        user = createUser("charger_test_user@example.com", "100.00");

        admin = new Admin();
        admin.setEmail("charger_test_admin@example.com");
        admin.setPassword("test-password-hash");
        admin = adminRepository.save(admin);
    }

    @AfterEach
    void cleanUp() {
        ledgerEntryRepository.deleteAllInBatch();
        applicationUserRepository.deleteAll();
        adminRepository.deleteAll();
    }

    @Test
    void charge_Success_DeductsBalanceAndCreatesChargeEntry() {
        String key = key();

        ChargeResult result = balanceCharger.charge(request(user.getId(), "30.00", key));

        assertThat(result.status()).isEqualTo(ChargeStatus.SUCCESS);
        assertThat(result.ledgerEntryId()).isNotNull();
        assertThat(balanceOf(user)).isEqualByComparingTo("70.00");

        LedgerEntry entry = ledgerEntryRepository.findByIdempotencyKey(key).orElseThrow();
        assertThat(entry.getId()).isEqualTo(result.ledgerEntryId());
        assertThat(entry.getType()).isEqualTo(EntryType.CHARGE);
        assertThat(entry.getSource()).isEqualTo(EntrySource.PLUGIN);
        assertThat(entry.getPerformedByAdmin()).isNull();
        assertThat(entry.getSubscriberId()).isEqualTo(user.getId());
        assertThat(entry.getAmount()).isEqualByComparingTo("30.00");
        assertThat(entry.getUserCurrency()).isEqualTo("USD");
        assertThat(events.stream(BalanceChanged.class).count()).isEqualTo(1);
    }

    @Test
    void charge_AmountEqualToBalance_Succeeds() {
        ChargeResult result = balanceCharger.charge(request(user.getId(), "100.00", key()));

        assertThat(result.status()).isEqualTo(ChargeStatus.SUCCESS);
        assertThat(balanceOf(user)).isEqualByComparingTo("0.00");
    }

    @Test
    void charge_InsufficientFunds_ReturnsStatusAndChangesNothing() {
        String key = key();

        ChargeResult result = balanceCharger.charge(request(user.getId(), "150.00", key));

        assertThat(result.status()).isEqualTo(ChargeStatus.INSUFFICIENT_FUNDS);
        assertThat(result.ledgerEntryId()).isNull();
        assertThat(balanceOf(user)).isEqualByComparingTo("100.00");
        assertThat(ledgerEntryRepository.findByIdempotencyKey(key)).isEmpty();
        assertThat(entriesOf(user)).isZero();
        assertThat(events.stream(BalanceChanged.class).count()).isZero();
    }

    @Test
    void charge_InsufficientFundsThenRetryAfterTopUp_Succeeds() {
        String key = key();
        ChargeRequest request = request(user.getId(), "150.00", key);

        assertThat(balanceCharger.charge(request).status()).isEqualTo(ChargeStatus.INSUFFICIENT_FUNDS);

        ApplicationUser reloaded = applicationUserRepository.findById(user.getId()).orElseThrow();
        reloaded.setBalance(new BigDecimal("200.00"));
        applicationUserRepository.save(reloaded);

        ChargeResult retry = balanceCharger.charge(request);

        assertThat(retry.status()).isEqualTo(ChargeStatus.SUCCESS);
        assertThat(balanceOf(user)).isEqualByComparingTo("50.00");
    }

    @Test
    void charge_RepeatedKey_ReturnsSameEntryAndChargesOnce() {
        ChargeRequest request = request(user.getId(), "30.00", key());

        ChargeResult first = balanceCharger.charge(request);
        ChargeResult second = balanceCharger.charge(request);

        assertThat(second.status()).isEqualTo(ChargeStatus.SUCCESS);
        assertThat(second.ledgerEntryId()).isEqualTo(first.ledgerEntryId());
        assertThat(balanceOf(user)).isEqualByComparingTo("70.00");
        assertThat(entriesOf(user)).isEqualTo(1);
        assertThat(events.stream(BalanceChanged.class).count()).isEqualTo(1);
    }

    @Test
    void charge_SameKeyDifferentAmount_Throws() {
        String key = key();
        balanceCharger.charge(request(user.getId(), "30.00", key));

        assertThatThrownBy(() -> balanceCharger.charge(request(user.getId(), "40.00", key)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(balanceOf(user)).isEqualByComparingTo("70.00");
        assertThat(entriesOf(user)).isEqualTo(1);
    }

    @Test
    void charge_SameKeyDifferentSubscriber_Throws() {
        ApplicationUser other = createUser("charger_test_other@example.com", "100.00");
        String key = key();
        balanceCharger.charge(request(user.getId(), "30.00", key));

        assertThatThrownBy(() -> balanceCharger.charge(request(other.getId(), "30.00", key)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(balanceOf(other)).isEqualByComparingTo("100.00");
    }

    @Test
    void charge_UnknownSubscriber_Throws() {
        assertThatThrownBy(() -> balanceCharger.charge(request(UUID.randomUUID(), "30.00", key())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void charge_ConcurrentRequests_NeverOverdrawBalance() throws Exception {
        int tasks = 10;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<ChargeResult>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                ChargeRequest request = request(user.getId(), "30.00", key());
                futures.add(executor.submit(() -> {
                    start.await();
                    return balanceCharger.charge(request);
                }));
            }
            start.countDown();

            long successes = 0;
            long rejected = 0;
            for (Future<ChargeResult> future : futures) {
                ChargeResult result = future.get(30, TimeUnit.SECONDS);
                if (result.status() == ChargeStatus.SUCCESS) {
                    successes++;
                } else {
                    rejected++;
                }
            }

            assertThat(successes).isEqualTo(3);
            assertThat(rejected).isEqualTo(7);
            assertThat(balanceOf(user)).isEqualByComparingTo("10.00");
            assertThat(entriesOf(user)).isEqualTo(3);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void charge_ConcurrentRequestsWithSameKey_ChargeOnce() throws Exception {
        int tasks = 5;
        ChargeRequest request = request(user.getId(), "30.00", key());
        ExecutorService executor = Executors.newFixedThreadPool(tasks);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<ChargeResult>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return balanceCharger.charge(request);
                }));
            }
            start.countDown();

            Set<UUID> entryIds = new java.util.HashSet<>();
            for (Future<ChargeResult> future : futures) {
                ChargeResult result = future.get(30, TimeUnit.SECONDS);
                assertThat(result.status()).isEqualTo(ChargeStatus.SUCCESS);
                entryIds.add(result.ledgerEntryId());
            }

            assertThat(entryIds).hasSize(1);
            assertThat(entriesOf(user)).isEqualTo(1);
            assertThat(balanceOf(user)).isEqualByComparingTo("70.00");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void refund_OfPluginCharge_ReturnsMoney() {
        ChargeResult charge = balanceCharger.charge(request(user.getId(), "30.00", key()));

        RefundRequestDto refund = new RefundRequestDto();
        refund.setOriginalEntryId(charge.ledgerEntryId());
        refund.setAmount(new BigDecimal("30.00"));
        refund.setAmountInBaseCurrency(new BigDecimal("30.00"));
        refund.setExchangeRate(BigDecimal.ONE);
        refund.setUserCurrency("USD");
        refund.setBaseCurrency("USD");
        refund.setDescription("Refund of a plugin charge");

        ledgerService.applyRefund(refund,
                applicationUserRepository.findById(user.getId()).orElseThrow(), admin);

        assertThat(balanceOf(user)).isEqualByComparingTo("100.00");
        assertThat(ledgerEntryRepository
                .existsByOriginalEntryIdAndType(charge.ledgerEntryId(), EntryType.REFUND)).isTrue();
    }

    private ApplicationUser createUser(String email, String balance) {
        ApplicationUser u = new ApplicationUser();
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmail(email);
        u.setBalance(new BigDecimal(balance));
        u.setUserCurrency("USD");
        return applicationUserRepository.save(u);
    }

    private ChargeRequest request(UUID subscriberId, String amount, String key) {
        return new ChargeRequest(subscriberId, new BigDecimal(amount), key, "Test charge");
    }

    private String key() {
        return "test-plugin:" + UUID.randomUUID();
    }

    private BigDecimal balanceOf(ApplicationUser u) {
        return applicationUserRepository.findById(u.getId()).orElseThrow().getBalance();
    }

    private long entriesOf(ApplicationUser u) {
        return ledgerEntryRepository.findAll().stream()
                .filter(e -> e.getSubscriberId().equals(u.getId()))
                .count();
    }
}
