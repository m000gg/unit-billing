package com.m000gg.billing.subscribers;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class ApplicationUserMapper {

    public void updateEntityFromDto(ApplicationUser entity, ApplicationUserEditDto dto) {
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setCountry(dto.getCountry());
        entity.setCity(dto.getCity());
        entity.setRegion(dto.getRegion());
        entity.setStreet(dto.getStreet());
        entity.setHouseNumber(dto.getHouseNumber());
        entity.setApartment(dto.getApartment());
        entity.setPostalCode(dto.getPostalCode());
    }

    public ApplicationUserEditDto toDto(ApplicationUser entity) {
        ApplicationUserEditDto dto = new ApplicationUserEditDto();
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setCountry(entity.getCountry());
        dto.setCity(entity.getCity());
        dto.setRegion(entity.getRegion());
        dto.setStreet(entity.getStreet());
        dto.setHouseNumber(entity.getHouseNumber());
        dto.setApartment(entity.getApartment());
        dto.setPostalCode(entity.getPostalCode());
        return dto;
    }

    public void registerUserFromDto(ApplicationUser newApplicationUser, ApplicationUserRegisterDto applicationUserRegisterDto, String encodedPassword){
        newApplicationUser.setFirstName(applicationUserRegisterDto.getFirstName());
        newApplicationUser.setLastName(applicationUserRegisterDto.getLastName());
        newApplicationUser.setEmail(applicationUserRegisterDto.getEmail());
        newApplicationUser.setPhone(applicationUserRegisterDto.getPhone());
        newApplicationUser.setCountry(applicationUserRegisterDto.getCountry());
        newApplicationUser.setCity(applicationUserRegisterDto.getCity());
        newApplicationUser.setRegion(applicationUserRegisterDto.getRegion());
        newApplicationUser.setStreet(applicationUserRegisterDto.getStreet());
        newApplicationUser.setHouseNumber(applicationUserRegisterDto.getHouseNumber());
        newApplicationUser.setApartment(applicationUserRegisterDto.getApartment());
        newApplicationUser.setPostalCode(applicationUserRegisterDto.getPostalCode());
        newApplicationUser.setBalance(BigDecimal.ZERO);
        newApplicationUser.setPassword(encodedPassword);
    }

    public AccountOverviewViewModel accountViewModelFromUser(ApplicationUser currentUser, AccountOverviewViewModel accountOverviewViewModel){
        accountOverviewViewModel.setBalance(currentUser.getBalance());
        accountOverviewViewModel.setFirstName(currentUser.getFirstName());
        accountOverviewViewModel.setLastName(currentUser.getLastName());
        accountOverviewViewModel.setEmail(currentUser.getEmail());
        accountOverviewViewModel.setCity(currentUser.getCity());
        accountOverviewViewModel.setCountry(currentUser.getCountry());
        accountOverviewViewModel.setRegion(currentUser.getRegion());
        accountOverviewViewModel.setStreet(currentUser.getStreet());
        accountOverviewViewModel.setCreatedAt(currentUser.getCreatedAt());
        accountOverviewViewModel.setHouseNumber(currentUser.getHouseNumber());
        accountOverviewViewModel.setApartment(currentUser.getApartment());
        accountOverviewViewModel.setPostalCode(currentUser.getPostalCode());
        accountOverviewViewModel.setPhone(currentUser.getPhone());
        return accountOverviewViewModel;
    }
}