package com.vinayakit.bms.security;

import com.vinayakit.bms.entity.Customer;
import com.vinayakit.bms.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtService implements Converter<Jwt, AbstractAuthenticationToken> {

    private final CustomerRepository customerRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // 1. Extract email from Google JWT claims
        String email = jwt.getClaimAsString("email");

        if (email == null) {
            log.warn("Security: JWT rejected - email claim is missing");
            throw new JwtException(
                    "Invalid token: email claim is missing. Access denied."
            );
        }

        // 2. Check email is verified by Google
        Boolean emailVerified = jwt.getClaim("email_verified");
        if (emailVerified == null || !emailVerified) {
            log.warn("Security: JWT rejected - email not verified for {}", email);
            throw new JwtException(
                    "Invalid token: email claim is not verified. Access denied."
            );
        }

        // 3. Look up customer in DB - reject if not registered
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Security: Access denied - email not registered: {}", email);
                    return new JwtException(
                            "Access denied: this email is not registered in the system"
                    );
                });

        // 4. Check KYC status - block REJECTED customers
        if (customer.getKycStatus() == Customer.KycStatus.REJECTED) {
            log.warn("Security: Access denied - KYC rejected for {}", email);
            throw new JwtException(
                    "Access denied: your KYC verification was rejected"
            );
        }

        // 5. Null safety on role
        if (customer.getUserRole() == null) {
            log.error("Security: Customer {} has null role in DB - defaulting to CUSTOMER", email);
            return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), email);
        }

        // 6. Build authentication with correct role
        String role = "ROLE_" + customer.getUserRole().name();
        log.info("Security: Authenticated {} with role {}", email, role);

        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(role)), email);
    }
}
