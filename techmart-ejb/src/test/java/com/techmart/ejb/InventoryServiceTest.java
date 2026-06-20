package com.techmart.ejb;

import com.techmart.model.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {

    @Mock
    private EntityManager em;

    @Mock
    private PlatformMetricsRegistry metrics;

    @InjectMocks
    private InventoryService inventoryService;

    private Product sampleProduct;

    @BeforeEach
    public void setUp() {
        sampleProduct = new Product("TEST-01", "Test Product", 50, 10.00, "Warehouse A");
    }

    @Test
    public void testGetAllProducts() {
        List<Product> products = new ArrayList<>();
        products.add(sampleProduct);

        TypedQuery<Product> query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Product.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(products);

        List<Product> result = inventoryService.getAllProducts();
        assertEquals(1, result.size());
        assertEquals("TEST-01", result.get(0).getSku());
    }

    @Test
    public void testGetProductBySku_Success() {
        TypedQuery<Product> query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Product.class))).thenReturn(query);
        when(query.setParameter(eq("sku"), anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(sampleProduct);

        Product result = inventoryService.getProductBySku("TEST-01");
        assertNotNull(result);
        assertEquals("TEST-01", result.getSku());
    }

    @Test
    public void testGetProductBySku_NotFound() {
        TypedQuery<Product> query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Product.class))).thenReturn(query);
        when(query.setParameter(eq("sku"), anyString())).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new RuntimeException("No product found"));

        Product result = inventoryService.getProductBySku("TEST-NOT-FOUND");
        assertNull(result);
    }

    @Test
    public void testCheckAvailability_Available() {
        TypedQuery<Product> query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Product.class))).thenReturn(query);
        when(query.setParameter(eq("sku"), anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(sampleProduct);

        boolean isAvailable = inventoryService.checkAvailability("TEST-01", 10);
        assertTrue(isAvailable);
    }

    @Test
    public void testCheckAvailability_Unavailable() {
        TypedQuery<Product> query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Product.class))).thenReturn(query);
        when(query.setParameter(eq("sku"), anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(sampleProduct);

        boolean isAvailable = inventoryService.checkAvailability("TEST-01", 60);
        assertFalse(isAvailable);
    }

    @Test
    public void testDeductStock_Success() {
        TypedQuery<Product> query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Product.class))).thenReturn(query);
        when(query.setParameter(eq("sku"), anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(sampleProduct);

        boolean success = inventoryService.deductStock("TEST-01", 10);
        assertTrue(success);
        assertEquals(40, sampleProduct.getStock());
        verify(em).merge(sampleProduct);
        verify(metrics).addLog(contains("Stock deducted"));
    }

    @Test
    public void testDeductStock_Failure() {
        TypedQuery<Product> query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Product.class))).thenReturn(query);
        when(query.setParameter(eq("sku"), anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(sampleProduct);

        boolean success = inventoryService.deductStock("TEST-01", 60);
        assertFalse(success);
        assertEquals(50, sampleProduct.getStock());
        verify(em, never()).merge(any());
        verify(metrics).addLog(contains("Failed to deduct stock"));
    }
}
