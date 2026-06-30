package com.techmart.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "products")
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Double price;

    @Column(name = "warehouse_location")
    private String warehouseLocation;

    @Column(name = "category")
    private String category;

    public Product() {}

    public Product(String sku, String name, Integer stock, Double price, String warehouseLocation) {
        this.sku = sku;
        this.name = name;
        this.stock = stock;
        this.price = price;
        this.warehouseLocation = warehouseLocation;
        this.category = "General";
    }

    public Product(String sku, String name, Integer stock, Double price, String warehouseLocation, String category) {
        this.sku = sku;
        this.name = name;
        this.stock = stock;
        this.price = price;
        this.warehouseLocation = warehouseLocation;
        this.category = category;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getWarehouseLocation() {
        return warehouseLocation;
    }

    public void setWarehouseLocation(String warehouseLocation) {
        this.warehouseLocation = warehouseLocation;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
