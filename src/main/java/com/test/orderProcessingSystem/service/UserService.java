package com.test.orderProcessingSystem.service;

import com.test.orderProcessingSystem.config.CacheConfig;
import com.test.orderProcessingSystem.dto.AddressResponse;
import com.test.orderProcessingSystem.dto.CreateAddressRequest;
import com.test.orderProcessingSystem.dto.UpdateAddressRequest;
import com.test.orderProcessingSystem.entity.Address;
import com.test.orderProcessingSystem.entity.User;
import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import com.test.orderProcessingSystem.exception.BadRequestException;
import com.test.orderProcessingSystem.exception.ResourceNotFoundException;
import com.test.orderProcessingSystem.repository.AddressRepository;
import com.test.orderProcessingSystem.repository.OrderHistoryRepository;
import com.test.orderProcessingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
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

        // Orders reference addresses (shipping_address_id, NOT NULL), so they must be removed before
        // the addresses. Delete the user's orders first (cascading to each order's line items) and
        // flush, then delete the user — whose remaining addresses then cascade away cleanly.
        orderHistoryRepository.deleteAll(orderHistoryRepository.findByUser_UserId(userId));
        orderHistoryRepository.flush();

        userRepository.deleteById(userId);
        log.info("User account deleted userId={}", userId);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return addressRepository.findByUser_UserId(userId).stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + addressId + " for user: " + userId));

        // An address referenced by any order (kept for history, even cancelled/delivered) cannot be removed
        if (orderHistoryRepository.existsByShippingAddress_AddressId(addressId)) {
            throw new BadRequestException(
                    "Address cannot be deleted because it is used by one or more orders");
        }

        addressRepository.delete(address);
        log.info("Address deleted addressId={} userId={}", addressId, userId);
    }

    @Transactional
    public AddressResponse addAddress(Long userId, CreateAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Address address = new Address();
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setPostalCode(request.getPostalCode());
        address.setAddressType(request.getAddressType());
        address.setUser(user);

        Address saved = addressRepository.save(address);
        log.info("Address added addressId={} userId={}", saved.getAddressId(), userId);
        return toAddressResponse(saved);
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
        log.info("Address updated addressId={} userId={}", addressId, userId);
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
