package com.vinayakit.bms.controller;

import com.vinayakit.bms.entity.Customer;
import com.vinayakit.bms.exception.ResourceNotFoundException;
import com.vinayakit.bms.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerRepository.findAll());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{customerId}")
    public ResponseEntity<Customer> getCustomerById(
            @PathVariable UUID customerId) {
        return ResponseEntity.ok(
                customerRepository.findById(customerId)
                        .orElseThrow(() -> new com.vinayakit.bms.exception
                                .ResourceNotFoundException(
                                "Customer not found: " + customerId))
        );
    }

    @GetMapping("/me")
    public ResponseEntity<Customer> getMe(Principal principal) {
        String email = principal.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new com.vinayakit.bms.exception
                        .ResourceNotFoundException("Customer not found: " + email));
        return ResponseEntity.ok(customer);
    }
}
