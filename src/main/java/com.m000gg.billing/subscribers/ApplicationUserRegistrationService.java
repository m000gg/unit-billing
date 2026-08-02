package com.m000gg.billing.subscribers;


import com.m000gg.billing.subscribers.exception.EmailAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ApplicationUserRegistrationService {

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomPasswordGenerator customPasswordGenerator;

    public String createNewApplicationUser(ApplicationUserRegisterDto applicationUserRegisterDto) {

        ApplicationUser newApplicationUser = new ApplicationUser();

        String email = applicationUserRegisterDto.getEmail();
        if (applicationUserRepository.existsByEmail(email)){
            throw new EmailAlreadyExistsException("User with this email already exists: " + email );
        }

        String generatedPassword = customPasswordGenerator.generatePassayPassword();
        newApplicationUser.setFirstName(applicationUserRegisterDto.getFirstName());
        newApplicationUser.setLastName(applicationUserRegisterDto.getLastName());
        newApplicationUser.setEmail(email);
        newApplicationUser.setPhone(applicationUserRegisterDto.getPhone());
        newApplicationUser.setCountry(applicationUserRegisterDto.getCountry());
        newApplicationUser.setCity(applicationUserRegisterDto.getCity());
        newApplicationUser.setRegion(applicationUserRegisterDto.getRegion());
        newApplicationUser.setStreet(applicationUserRegisterDto.getStreet());
        newApplicationUser.setHouseNumber(applicationUserRegisterDto.getHouseNumber());
        newApplicationUser.setApartment(applicationUserRegisterDto.getApartment());
        newApplicationUser.setPostalCode(applicationUserRegisterDto.getPostalCode());
        newApplicationUser.setBalance(BigDecimal.ZERO);
        newApplicationUser.setPassword(passwordEncoder.encode(generatedPassword));

        try {
            applicationUserRepository.save(newApplicationUser);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException("User with this email already exists: " + email);
        }

        return generatedPassword;
    }

    public Page<ApplicationUser> search(String search, Pageable pageable){
        return applicationUserRepository.search(search, pageable);
    }

}