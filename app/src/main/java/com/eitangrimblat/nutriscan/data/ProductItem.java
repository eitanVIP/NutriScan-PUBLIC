package com.eitangrimblat.nutriscan.data;

/**
 * Wrapper for a {@link Product} including its unique database identifier.
 */
public class ProductItem {
    private Product product;
    private String id;

    /**
     * Initializes the product item with a product and its ID.
     */
    public ProductItem(Product product, String id) {
        this.product = product;
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
