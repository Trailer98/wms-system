package com.example.wms.admin.controller.base;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.service.SysDictService;
import com.example.wms.admin.view.dto.CreateSysDictItemRequest;
import com.example.wms.admin.view.dto.CreateSysDictTypeRequest;
import com.example.wms.admin.view.dto.SysDictItemResponse;
import com.example.wms.admin.view.dto.SysDictItemView;
import com.example.wms.admin.view.dto.SysDictTypeQuery;
import com.example.wms.admin.view.dto.SysDictTypeResponse;
import com.example.wms.admin.view.dto.UpdateSysDictItemRequest;
import com.example.wms.admin.view.dto.UpdateSysDictTypeRequest;
import com.example.wms.admin.view.dto.UpdateStatusRequest;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys-dicts")
public class SysDictController {

    private final SysDictService sysDictService;

    public SysDictController(SysDictService sysDictService) {
        this.sysDictService = sysDictService;
    }

    /**
     * Business read path: ENABLED-only, cached. Requires only {@code sys-dict:view}, granted to every
     * seeded role (see DataInitializer) — the RBAC model here has no "just logged in" tier, so this is
     * the minimum-viable stand-in for one, matching what any page rendering a stock movement needs.
     */
    @GetMapping("/batch")
    @RequiresPermission("sys-dict:view")
    public ApiResponse<Map<String, List<SysDictItemView>>> batch(@RequestParam String dictCodes) {
        List<String> codes = List.of(dictCodes.split(","));
        return ApiResponse.ok(sysDictService.getBatch(codes));
    }

    /** Admin listing: every status (ENABLED and DISABLED), for the dictionary management page. */
    @GetMapping("/items")
    @RequiresPermission("sys-dict:view")
    public ApiResponse<List<SysDictItemResponse>> items(@RequestParam String dictCode) {
        return ApiResponse.ok(sysDictService.listItemsForAdmin(dictCode));
    }

    @GetMapping("/types/page")
    @RequiresPermission("sys-dict:view")
    public ApiResponse<PageResponse<SysDictTypeResponse>> types(SysDictTypeQuery query) {
        return ApiResponse.ok(sysDictService.searchTypes(query));
    }

    @PostMapping("/types")
    @RequiresPermission("sys-dict:create")
    @SysOperationLog(operationType = "新增字典类型", content = "新增字典类型", module = "数据字典")
    public ApiResponse<SysDictTypeResponse> createType(@Valid @RequestBody CreateSysDictTypeRequest request) {
        return ApiResponse.ok(sysDictService.createType(request));
    }

    @PutMapping("/types/{id}")
    @RequiresPermission("sys-dict:update")
    @SysOperationLog(operationType = "编辑字典类型", content = "编辑字典类型", module = "数据字典")
    public ApiResponse<SysDictTypeResponse> updateType(@PathVariable Long id, @Valid @RequestBody UpdateSysDictTypeRequest request) {
        return ApiResponse.ok(sysDictService.updateType(id, request));
    }

    @PutMapping("/types/{id}/status")
    @RequiresPermission("sys-dict:disable")
    @SysOperationLog(operationType = "启停字典类型", content = "启停字典类型", module = "数据字典")
    public ApiResponse<SysDictTypeResponse> changeTypeStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return ApiResponse.ok(sysDictService.changeTypeStatus(id, request.status()));
    }

    @PostMapping("/items")
    @RequiresPermission("sys-dict:create")
    @SysOperationLog(operationType = "新增字典项", content = "新增字典项", module = "数据字典")
    public ApiResponse<SysDictItemResponse> createItem(@Valid @RequestBody CreateSysDictItemRequest request) {
        return ApiResponse.ok(sysDictService.createItem(request));
    }

    @PutMapping("/items/{id}")
    @RequiresPermission("sys-dict:update")
    @SysOperationLog(operationType = "编辑字典项", content = "编辑字典项", module = "数据字典")
    public ApiResponse<SysDictItemResponse> updateItem(@PathVariable Long id, @Valid @RequestBody UpdateSysDictItemRequest request) {
        return ApiResponse.ok(sysDictService.updateItem(id, request));
    }

    @PutMapping("/items/{id}/status")
    @RequiresPermission("sys-dict:disable")
    @SysOperationLog(operationType = "启停字典项", content = "启停字典项", module = "数据字典")
    public ApiResponse<SysDictItemResponse> changeItemStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return ApiResponse.ok(sysDictService.changeItemStatus(id, request.status()));
    }

    @DeleteMapping("/items/{id}")
    @RequiresPermission("sys-dict:delete")
    @SysOperationLog(operationType = "删除字典项", content = "删除字典项", module = "数据字典")
    public ApiResponse<Void> deleteItem(@PathVariable Long id) {
        sysDictService.deleteItem(id);
        return ApiResponse.ok();
    }
}
