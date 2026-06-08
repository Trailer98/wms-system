package com.example.wms.admin.controller;

import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.InboundOrderService;
import com.example.wms.admin.view.dto.CreateInboundOrderRequest;
import com.example.wms.admin.view.dto.InboundOrderResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inbound-orders")
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;

    public InboundOrderController(InboundOrderService inboundOrderService) {
        this.inboundOrderService = inboundOrderService;
    }

    @PostMapping
    public ApiResponse<InboundOrderResponse> create(@Valid @RequestBody CreateInboundOrderRequest request) {
        return ApiResponse.ok(inboundOrderService.create(request));
    }

    @PostMapping("/{id}/receive")
    public ApiResponse<InboundOrderResponse> receive(@PathVariable Long id) {
        return ApiResponse.ok(inboundOrderService.receive(id));
    }
}
