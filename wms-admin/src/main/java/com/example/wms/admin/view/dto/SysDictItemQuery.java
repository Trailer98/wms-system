package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Admin listing (all statuses) of the items under one dict type; deliberately not paginated — a dict type's item count is small. */
@Getter
@Setter
public class SysDictItemQuery {

    @NotBlank
    private String dictCode;
}
