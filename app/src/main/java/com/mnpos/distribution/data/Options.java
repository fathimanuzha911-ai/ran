package com.mnpos.distribution.data;

import com.mnpos.distribution.model.PickerOption;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Shared loader for the branch / sales-rep dropdowns used across several screens. */
public final class Options {
    private Options() {}

    public static void loadBranches(Consumer<List<PickerOption>> onLoaded) {
        load("/api/mobile/locations", "All branches", onLoaded);
    }

    public static void loadReps(Consumer<List<PickerOption>> onLoaded) {
        load("/api/mobile/sales-reps", "All sales reps", onLoaded);
    }

    private static void load(String endpoint, String allLabel, Consumer<List<PickerOption>> onLoaded) {
        List<PickerOption> options = new ArrayList<>();
        options.add(new PickerOption(0, allLabel));
        ApiClient.get(endpoint, new ApiClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray data = response.optJSONArray("data");
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject row = data.optJSONObject(i);
                        if (row != null) options.add(new PickerOption(row.optInt("id"), row.optString("name", "—")));
                    }
                }
                onLoaded.accept(options);
            }

            @Override
            public void onError(Exception error) {
                onLoaded.accept(options);
            }
        });
    }
}
