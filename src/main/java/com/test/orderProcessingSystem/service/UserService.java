package com.test.orderProcessingSystem.service;

import com.test.orderProcessingSystem.config.CacheConfig;
import com.test.orderProcessingSystem.dto.AddressResponse;
import com.test.orderProcessingSystem.dto.UpdateAddressRequest;
import com.test.orderProcessingSystem.entity.Address;
import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import com.test.orderProcessingSystem.exception.BadRequestException;
import com.test.orderProcessingSystem.exception.ResourceNotFoundException;
import com.test.orderProcessingSystem.repository.AddressRepository;
import com.test.orderProcessingSystem.repository.OrderHistoryRepository;
import com.test.orderProcessingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    // An account may be deleted only when every order has reached one of these terminal states
    private static final List<OrderStatus> TERMINAL_ORDER_STATUSES =
            List.of(OrderStatus.CANCELLED, OrderStatus.DELIVERED);

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final OrderHistoryRepository orderHistoryRepository;

    @Transactional
    @CacheEvict(value = CacheConfig.USER_DETAILS_CACHE, allEntries = true)
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        boolean hasNonTerminalOrders =
                orderHistoryRepository.existsByUser_UserIdAndOrderStatusNotIn(userId, TERMINAL_ORDER_STATUSES);
        if (hasNonTerminalOrders) {
            throw new BadRequestException(
                    "Account cannot be deleted while you have orders that are not CANCELLED or DELIVERED");
        }

        // Addresses are removed automatically via the ON DELETE CASCADE foreign key
        userRepository.deleteById(userId);
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, UpdateAddressRequest request) {
        Address address = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + addressId + " for user: " + userId));

        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setPostalCode(request.getPostalCode());
        address.setAddressType(request.getAddressType());

        Address saved = addressRepository.save(address);
        return toAddressResponse(saved);
    }

    private AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .line1(address.getLine1())
                .line2(address.getLine2())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .addressType(address.getAddressType())
                .build();
    }
}
