package com.saeed.service;

import com.saeed.dto.CreateEmployeeRequest;
import com.saeed.dto.EmployeeResponse;
import com.saeed.dto.UpdateEmployeeRequest;
import com.saeed.exception.EmployeeNotFoundException;
import com.saeed.model.Employee;
import com.saeed.repository.EmployeeRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private static final String CACHE_NAME = "employees";

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Cacheable(value = CACHE_NAME, key = "#id")
    public EmployeeResponse getEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
        return toEmployeeResponse(employee);
    }

    @Cacheable(value = CACHE_NAME, key = "'all'")
    public List<EmployeeResponse> getAllEmployees() {
        return StreamSupport.stream(employeeRepository.findAll().spliterator(), false)
                .map(this::toEmployeeResponse)
                .toList();
    }

    @Caching(
        put = {@CachePut(value = CACHE_NAME, key = "#id")},
        evict = {@CacheEvict(value = CACHE_NAME, key = "'all'")}
    )
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
        
        employee.setName(request.getName());
        Employee updatedEmployee = employeeRepository.save(employee);
        return toEmployeeResponse(updatedEmployee);
    }
    
    @Caching(evict = {
        @CacheEvict(value = CACHE_NAME, key = "'all'")
    })
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        Employee employee = new Employee(request.getName());
        Employee savedEmployee = employeeRepository.save(employee);
        return toEmployeeResponse(savedEmployee);
    }

    @Caching(evict = {
        @CacheEvict(value = CACHE_NAME, key = "#id"),
        @CacheEvict(value = CACHE_NAME, key = "'all'")
    })
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        employeeRepository.deleteById(id);
    }

    private EmployeeResponse toEmployeeResponse(Employee employee) {
        return new EmployeeResponse(employee.getId(), employee.getName());
    }
}
