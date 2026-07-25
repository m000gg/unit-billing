package com.m000gg.billing.identity;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private ApplicationUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/client/login", "/login/process", "/css/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/client/login")
                        .loginProcessingUrl("/login/process")
                        .defaultSuccessUrl("/client/", true)
                        .permitAll()
                )

                .logout( logout -> logout
                        .logoutUrl("/client/logout")
                        .logoutSuccessUrl("/client/login")
                        .permitAll()

                )

                .userDetailsService(userDetailsService);


        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
