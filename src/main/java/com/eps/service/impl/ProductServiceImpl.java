package com.eps.service.impl;

import com.eps.entity.Product;
import com.eps.repository.ProductRepository;
import com.eps.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProductById(Long id) {
        if (id == null) {
            return null;
        }
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
    }

    @Override
    public Product updateProduct(Long id, Product product) {
        Product existing = getProductById(id);
        existing.setName(product.getName());
        existing.setPricePerProduct(product.getPricePerProduct());
        existing.setNumberOfQuantities(product.getNumberOfQuantities());
        existing.setDescription(product.getDescription());
        existing.setStatus(product.getStatus());
        existing.setDepartment(product.getDepartment());
        existing.setCategory(product.getCategory());
        return productRepository.save(existing);
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
