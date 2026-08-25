package com.mnpos.distribution.model;

public class FieldSpec {
    public enum Type { TEXT, NUMBER, DATE, NOTES, BRANCH_PICKER, REP_PICKER }

    public final String label;
    public final String jsonKey;
    public final Type type;
    public final boolean required;

    public FieldSpec(String label, String jsonKey, Type type, boolean required) {
        this.label = label;
        this.jsonKey = jsonKey;
        this.type = type;
        this.required = required;
    }
}
