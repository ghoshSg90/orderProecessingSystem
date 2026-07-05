package com.test.orderProcessingSystem.repository;

import com.test.orderProcessingSystem.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Optional<Address> findByAddressIdAndUser_UserId(Long addressId, Long userId);

    List<Address> findByUser_UserId(Long userId);
}
