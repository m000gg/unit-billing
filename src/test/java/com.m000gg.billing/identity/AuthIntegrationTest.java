package com.m000gg.billing.identity;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void login_success() throws Exception {
        mockMvc.perform(post("/login/process")
                        .param("username", "john.doe@example.com")
                        .param("password", "password123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/"));
    }

    @Test
    public void login_failed() throws Exception {
        mockMvc.perform(post("/login/process")
                        .param("username", "john.doe@example.com")
                        .param("password", "12345")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/login?error"));
    }
}
