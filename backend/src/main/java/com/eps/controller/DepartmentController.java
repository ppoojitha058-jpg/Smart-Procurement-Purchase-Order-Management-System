package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.dto.DepartmentDto;
import com.eps.entity.Department;
import com.eps.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<ApiResponseDto> createDepartment(@RequestBody DepartmentDto dto) {
        Department department = modelMapper.map(dto, Department.class);
        Department created = departmentService.createDepartment(department);
        DepartmentDto responseDto = modelMapper.map(created, DepartmentDto.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.builder().success(true).message("Department created").data(responseDto).build());
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto> getAllDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        List<DepartmentDto> dtos = departments.stream()
                .map(department -> modelMapper.map(department, DepartmentDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Departments fetched").data(dtos).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto> getDepartmentById(@PathVariable Long id) {
        Department department = departmentService.getDepartmentById(id);
        DepartmentDto dto = modelMapper.map(department, DepartmentDto.class);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Department fetched").data(dto).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto> updateDepartment(@PathVariable Long id, @RequestBody DepartmentDto dto) {
        Department department = modelMapper.map(dto, Department.class);
        Department updated = departmentService.updateDepartment(id, department);
        DepartmentDto responseDto = modelMapper.map(updated, DepartmentDto.class);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Department updated").data(responseDto).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Department deleted").build());
    }
}
