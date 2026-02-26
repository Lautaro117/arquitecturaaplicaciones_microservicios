package com.uade.inventory.infrastructure.adapter.out.persistence;

import com.uade.inventory.domain.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toDomain(ProductJpaEntity entity) {
        return new Product(
                entity.getId(),
                entity.getName(),
                entity.getQuantity(),
                entity.getPrice()
        );
    }

    public ProductJpaEntity toJpaEntity(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity(
                product.getName(),
                product.getQuantity(),
                product.getPrice()
        );
        if (product.getId() != null) {
            entity.setId(product.getId());
        }
        return entity;
    }
}
