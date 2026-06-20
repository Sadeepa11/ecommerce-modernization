package com.techmart.ejb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShoppingCartSessionTest {

    @Mock
    private PlatformMetricsRegistry metrics;

    @InjectMocks
    private ShoppingCartSession cartSession;

    @BeforeEach
    public void setUp() {
        cartSession.init();
    }

    @Test
    public void testInit() {
        verify(metrics).incrementActiveSessions();
        verify(metrics).addLog(contains("ShoppingCartSession initialized"));
        assertTrue(cartSession.getCartItems().isEmpty());
    }

    @Test
    public void testSetAndGetCustomerName() {
        cartSession.setCustomerName("Alice");
        assertEquals("Alice", cartSession.getCustomerName());
    }

    @Test
    public void testAddItem() {
        cartSession.setCustomerName("Alice");
        cartSession.addItem("LAP-01", 2);

        Map<String, Integer> items = cartSession.getCartItems();
        assertEquals(1, items.size());
        assertEquals(2, items.get("LAP-01"));
        verify(metrics).addLog(contains("Item added to cart"));
    }

    @Test
    public void testRemoveItem() {
        cartSession.setCustomerName("Alice");
        cartSession.addItem("LAP-01", 2);
        cartSession.removeItem("LAP-01");

        Map<String, Integer> items = cartSession.getCartItems();
        assertTrue(items.isEmpty());
        verify(metrics).addLog(contains("Item removed from cart"));
    }

    @Test
    public void testClearCart() {
        cartSession.setCustomerName("Alice");
        cartSession.addItem("LAP-01", 2);
        cartSession.addItem("PHN-02", 1);
        cartSession.clearCart();

        Map<String, Integer> items = cartSession.getCartItems();
        assertTrue(items.isEmpty());
        verify(metrics).addLog(contains("Cart cleared"));
    }

    @Test
    public void testDestroy() {
        cartSession.destroy();
        verify(metrics).decrementActiveSessions();
        verify(metrics).addLog(contains("ShoppingCartSession destroyed"));
    }
}
