package com.mnpos.distribution.model;

public class PickerOption {
    public final int id;
    public final String name;

    public PickerOption(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
