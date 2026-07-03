package com.test.orderProcessingSystem.service;

import com.test.orderProcessingSystem.dto.AddressResponse;
import com.test.orderProcessingSystem.dto.UpdateAddressRequest;
import com.test.orderProcessingSystem.entity.Address;
import com.test.orderProcessingSystem.entity.User;
import com.test.orderProcessingSystem.entity.enums.AddressType;
import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import com.test.orderProcessingSystem.exception.BadRequestException;
import com.test.orderProcessingSystem.exception.ResourceNotFoundException;
import com.test.orderProcessingSystem.repository.AddressRepository;
import com.test.orderProcessingSystem.repository.OrderHistoryRepository;
import com.test.orderProcessingSystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private OrderHistoryRepository orderHistoryRepository;

    @InjectMocks
    private UserService userService;

    private static final Long USER_ID = 2L;
    private static final Long ADDRESS_ID = 1L;
    private static final List<OrderStatus> TERMINAL =
            List.of(OrderStatus.CANCELLED, OrderStatus.DELIVERED);

    // ---------- deleteUser ----------

    @Test
    void deleteUser_allOrdersTerminal_deletesById() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(orderHistoryRepository.existsByUser_UserIdAndOrderStatusNotIn(USER_ID, TERMINAL)).thenReturn(false);

        userService.deleteUser(USER_ID);

        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    void deleteUser_hasNonTerminalOrders_throwsAndDoesNotDelete() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(orderHistoryRepository.existsByUser_UserIdAndOrderStatusNotIn(USER_ID, TERMINAL)).thenReturn(true);

        assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not CANCELLED or DELIVERED");

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteUser_userNotFound_throwsAndSkipsOrderCheck() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, never()).deleteById(any());
        verify(orderHistoryRepository, never())
                .existsByUser_UserIdAndOrderStatusNotIn(anyLong(), eq(TERMINAL));
    }

    // ---------- updateAddress ----------

    @Test
    void updateAddress_success_updatesFieldsAndReturnsResponse() {
        User user = new User();
        user.setUserId(USER_ID);

        Address existing = new Address();
        existing.setAddressId(ADDRESS_ID);
        existing.setLine1("Old Line 1");
        existing.setCity("Old City");
        existing.setState("Old State");
        existing.setCountry("Old Country");
        existing.setPostalCode("000000");
        existing.setAddressType(AddressType.HOME);
        existing.setUser(user);

        UpdateAddressRequest request = new UpdateAddressRequest(
                "New Line 1", "New Line 2", "Bengaluru", "Karnataka", "India", "560001", AddressType.OFFICE);

        when(addressRepository.findByAddressIdAndUser_UserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(existing));
        when(addressRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        AddressResponse response = userService.updateAddress(USER_ID, ADDRESS_ID, request);

        assertThat(response.getAddressId()).isEqualTo(ADDRESS_ID);
        assertThat(response.getLine1()).isEqualTo("New Line 1");
        assertThat(response.getLine2()).isEqualTo("New Line 2");
        assertThat(response.getCity()).isEqualTo("Bengaluru");
        assertThat(response.getAddressType()).isEqualTo(AddressType.OFFICE);

        // entity itself was mutated with the new values
        assertThat(existing.getCity()).isEqualTo("Bengaluru");
        assertThat(existing.getPostalCode()).isEqualTo("560001");
        verify(addressRepository).save(existing);
    }

    @Test
    void updateAddress_notOwnedOrMissing_throws() {
        UpdateAddressRequest request = new UpdateAddressRequest(
                "New Line 1", null, "Bengaluru", "Karnataka", "India", "560001", AddressType.HOME);
        when(addressRepository.findByAddressIdAndUser_UserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateAddress(USER_ID, ADDRESS_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found");

        verify(addressRepository, never()).save(any());
    }
}
