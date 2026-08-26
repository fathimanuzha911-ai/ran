package com.mnpos.distribution.model;

import org.json.JSONObject;

public class CustomerLite {
    public int id;
    public String name;
    public String mobile;

    public static CustomerLite fromJson(JSONObject json) {
        CustomerLite c = new CustomerLite();
        c.id = json.optInt("id");
        c.name = json.optString("name", "Customer");
        c.mobile = json.optString("mobile", "");
        return c;
    }
}
