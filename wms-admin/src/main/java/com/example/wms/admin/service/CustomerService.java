package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.common.BusinessException;
import com.example.wms.admin.model.entity.Customer;
import com.example.wms.admin.model.mapper.CustomerMapper;
import com.example.wms.admin.view.dto.CreateCustomerRequest;
import com.example.wms.admin.view.dto.CustomerQuery;
import com.example.wms.admin.view.dto.CustomerResponse;
import com.example.wms.common.common.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomerService {

    private final CustomerMapper customerMapper;

    public CustomerService(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (customerMapper.selectCount(Wrappers.lambdaQuery(Customer.class).eq(Customer::getCode, request.code())) > 0) {
            throw new BusinessException("customer code already exists");
        }
        Customer customer = new Customer(
                request.code(),
                request.name(),
                request.contactName(),
                request.contactPhone(),
                request.address()
        );
        customerMapper.insert(customer);
        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(CustomerQuery query) {
        Page<Customer> page = customerMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(Customer.class)
                        .like(StringUtils.hasText(query.getCode()), Customer::getCode, query.getCode())
                        .like(StringUtils.hasText(query.getName()), Customer::getName, query.getName())
                        .orderByAsc(Customer::getCode)
        );

        return PageResponse.from(page, CustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public Customer getById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException("customer not found");
        }
        return customer;
    }
}
