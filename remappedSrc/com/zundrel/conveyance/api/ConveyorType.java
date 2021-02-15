package com.zundrel.conveyance.api;

import net.minecraft.util.StringIdentifiable;

public enum ConveyorType implements StringIdentifiable {
    NORMAL("normal"),
    VERTICAL("vertical"),
    DOWN_VERTICAL("down_vertical");

    String name;

    ConveyorType(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return name;
    }
}
