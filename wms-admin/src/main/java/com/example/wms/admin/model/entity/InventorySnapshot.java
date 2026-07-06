package com.example.wms.admin.model.entity;

public record InventorySnapshot(int onHandQty, int lockedQty, int frozenQty) {

    public int availableQty() {
        return onHandQty - lockedQty - frozenQty;
    }
}
