package com.m000gg.billing.subscribers;


import com.m000gg.billing.subscribers.exception.EmailAlreadyExistsException;
import com.m000gg.billing.subscribers.exception.EmailAlreadyTakenException;
import com.m000gg.billing.subscribers.exception.UserAlreadyDeletedException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ApplicationUserManagementService {

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationUserMapper applicationUserMapper;

    @Autowired
    private CustomPasswordGenerator customPasswordGenerator;

    @Transactional
    public String createNewApplicationUser(ApplicationUserRegisterDto applicationUserRegisterDto) {

        ApplicationUser newApplicationUser = new ApplicationUser();

        String email = applicationUserRegisterDto.getEmail();
        if (applicationUserRepository.existsByEmail(email)){
            throw new EmailAlreadyExistsException("User with this email already exists: " + email );
        }
        String generatedPassword = customPasswordGenerator.generatePassayPassword();
        String encodedPassword = passwordEncoder.encode(generatedPassword);
        applicationUserMapper.registerUserFromDto(newApplicationUser, applicationUserRegisterDto, encodedPassword);

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

    public ApplicationUser findApplicationUserById(UUID id){
        return applicationUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
    }

    public ApplicationUserEditDto findApplicationUserDtoById(UUID id){
        return applicationUserMapper.toDto(findApplicationUserById(id));
    }

    @Transactional
    public void editApplicationUserProfile(UUID id, ApplicationUserEditDto dataToChange){
        ApplicationUser user = findApplicationUserById(id);

        if (!user.getEmail().equals(dataToChange.getEmail())
                && applicationUserRepository.existsByEmail(dataToChange.getEmail())) {
            throw new EmailAlreadyTakenException("This email is already taken by another user: " + dataToChange.getEmail());
        }

        applicationUserMapper.updateEntityFromDto(user, dataToChange);

        applicationUserRepository.save(user);
    }

    @Transactional
    public void deleteApplicationUserProfile(UUID id){
        ApplicationUser applicationUser = findApplicationUserById(id);
        if (!applicationUser.getDeleted()){
            applicationUser.setDeleted(true);
            applicationUserRepository.save(applicationUser);
        } else {
            throw new UserAlreadyDeletedException("This user with id: " + id + " is already deleted.");}
    }

}