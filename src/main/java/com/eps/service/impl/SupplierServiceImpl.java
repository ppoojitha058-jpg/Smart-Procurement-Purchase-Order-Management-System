package com.eps.service.impl;

import com.eps.entity.Supplier;
import com.eps.repository.SupplierRepository;
import com.eps.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Override
    public Supplier createSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    @Override
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    @Override
    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id).orElseThrow(() -> new RuntimeException("Supplier not found"));
    }

    @Override
    public Supplier updateSupplier(Long id, Supplier supplier) {
        Supplier existing = getSupplierById(id);
        existing.setName(supplier.getName());
        existing.setPhone(supplier.getPhone());
        existing.setAddress(supplier.getAddress());
        existing.setEmail(supplier.getEmail());
        existing.setGstNumber(supplier.getGstNumber());
        existing.setStatus(supplier.getStatus());
        existing.setRating(supplier.getRating());
        existing.setFeedback(supplier.getFeedback());
        existing.setProduct(supplier.getProduct());
        return supplierRepository.save(existing);
    }

    @Override
    public void deleteSupplier(Long id) {
        supplierRepository.deleteById(id);
    }
}
