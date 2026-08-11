package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.dto.ManagerActionDto;
import com.eps.entity.PurchaseRequest;
import com.eps.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('MANAGER')")
@CrossOrigin(origins = "*")
public class ManagerController {

    private final PurchaseRequestService purchaseRequestService;

    @GetMapping("/pending-requests")
    public ResponseEntity<ApiResponseDto> getPendingRequests() {
        List<PurchaseRequest> pending = purchaseRequestService.getPendingRequests();
        ApiResponseDto response = ApiResponseDto.builder().success(true).message("Pending requests fetched").data(pending).build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<ApiResponseDto> approveRequest(@PathVariable Long id, @RequestBody ManagerActionDto dto, Principal principal) {
        String managerEmail = principal.getName();
        PurchaseRequest updated = purchaseRequestService.approveRequest(id, managerEmail, dto.getRemarks());
        ApiResponseDto response = ApiResponseDto.builder().success(true).message("Request approved").data(updated).build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<ApiResponseDto> rejectRequest(@PathVariable Long id, @RequestBody ManagerActionDto dto, Principal principal) {
        String managerEmail = principal.getName();
        PurchaseRequest updated = purchaseRequestService.rejectRequest(id, managerEmail, dto.getRemarks());
        ApiResponseDto response = ApiResponseDto.builder().success(true).message("Request rejected").data(updated).build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
