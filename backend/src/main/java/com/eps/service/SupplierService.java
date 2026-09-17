package com.eps.service;

import com.eps.entity.Supplier;

import java.util.List;

public interface SupplierService {
    Supplier createSupplier(Supplier supplier);
    List<Supplier> getAllSuppliers();
    Supplier getSupplierById(Long id);
    Supplier updateSupplier(Long id, Supplier supplier);
    void deleteSupplier(Long id);
}
