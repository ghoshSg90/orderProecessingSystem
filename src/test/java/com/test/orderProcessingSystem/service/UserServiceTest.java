package com.test.orderProcessingSystem.service;

import com.test.orderProcessingSystem.dto.AddressResponse;
import com.test.orderProcessingSystem.dto.CreateAddressRequest;
import com.test.orderProcessingSystem.dto.UpdateAddressRequest;
import com.test.orderProcessingSystem.entity.Address;
import com.test.orderProcessingSystem.entity.OrderHistory;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.inOrder;
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
    void deleteUser_allOrdersTerminal_deletesOrdersThenUser() {
        List<OrderHistory> orders = List.of(new OrderHistory());
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(orderHistoryRepository.existsByUser_UserIdAndOrderStatusNotIn(USER_ID, TERMINAL)).thenReturn(false);
        when(orderHistoryRepository.findByUser_UserId(USER_ID)).thenReturn(orders);

        userService.deleteUser(USER_ID);

        // orders (and their line items) are removed before the user, then the user delete cascades to addresses
        InOrder inOrder = inOrder(orderHistoryRepository, userRepository);
        inOrder.verify(orderHistoryRepository).deleteAll(orders);
        inOrder.verify(orderHistoryRepository).flush();
        inOrder.verify(userRepository).deleteById(USER_ID);
    }

    @Test
    void deleteUser_hasNonTerminalOrders_throwsAndDoesNotDelete() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(orderHistoryRepository.existsByUser_UserIdAndOrderStatusNotIn(USER_ID, TERMINAL)).thenReturn(true);

        assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not CANCELLED or DELIVERED");

        verify(userRepository, never()).deleteById(any());
        verify(orderHistoryRepository, never()).deleteAll(any());
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

    // ---------- listAddresses ----------

    @Test
    void listAddresses_success_returnsMappedAddresses() {
        Address a1 = new Address();
        a1.setAddressId(1L);
        a1.setLine1("45 Lake View Road");
        a1.setCity("Bengaluru");
        a1.setAddressType(AddressType.HOME);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(addressRepository.findByUser_UserId(USER_ID)).thenReturn(List.of(a1));

        List<AddressResponse> result = userService.listAddresses(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAddressId()).isEqualTo(1L);
        assertThat(result.get(0).getCity()).isEqualTo("Bengaluru");
    }

    @Test
    void listAddresses_userNotFound_throws() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> userService.listAddresses(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ---------- deleteAddress ----------

    @Test
    void deleteAddress_notReferenced_deletes() {
        Address address = new Address();
        address.setAddressId(ADDRESS_ID);
        when(addressRepository.findByAddressIdAndUser_UserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(orderHistoryRepository.existsByShippingAddress_AddressId(ADDRESS_ID)).thenReturn(false);

        userService.deleteAddress(USER_ID, ADDRESS_ID);

        verify(addressRepository).delete(address);
    }

    @Test
    void deleteAddress_referencedByOrder_throwsAndDoesNotDelete() {
        Address address = new Address();
        address.setAddressId(ADDRESS_ID);
        when(addressRepository.findByAddressIdAndUser_UserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(orderHistoryRepository.existsByShippingAddress_AddressId(ADDRESS_ID)).thenReturn(true);

        assertThatThrownBy(() -> userService.deleteAddress(USER_ID, ADDRESS_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("used by one or more orders");

        verify(addressRepository, never()).delete(any());
    }

    @Test
    void deleteAddress_notOwnedOrMissing_throws() {
        when(addressRepository.findByAddressIdAndUser_UserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteAddress(USER_ID, ADDRESS_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found");

        verify(addressRepository, never()).delete(any());
    }

    // ---------- addAddress ----------

    @Test
    void addAddress_success_savesAndReturnsResponse() {
        User user = new User();
        user.setUserId(USER_ID);
        CreateAddressRequest request = new CreateAddressRequest(
                "45 Lake View Road", "Near City Park", "Bengaluru", "Karnataka", "India", "560001", AddressType.HOME);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
            Address a = inv.getArgument(0);
            a.setAddressId(99L);
            return a;
        });

        AddressResponse response = userService.addAddress(USER_ID, request);

        assertThat(response.getAddressId()).isEqualTo(99L);
        assertThat(response.getLine1()).isEqualTo("45 Lake View Road");
        assertThat(response.getCity()).isEqualTo("Bengaluru");
        assertThat(response.getAddressType()).isEqualTo(AddressType.HOME);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        // the new address is linked to the owning user
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void addAddress_userNotFound_throwsAndDoesNotSave() {
        CreateAddressRequest request = new CreateAddressRequest(
                "45 Lake View Road", null, "Bengaluru", "Karnataka", "India", "560001", AddressType.HOME);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.addAddress(USER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(addressRepository, never()).save(any());
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
