package com.eps.service.impl;

import com.eps.entity.AuditLog;
import com.eps.repository.AuditLogRepository;
import com.eps.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public AuditLog save(AuditLog log) {
        return auditLogRepository.save(log);
    }
}
