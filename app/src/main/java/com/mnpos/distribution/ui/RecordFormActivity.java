package com.mnpos.distribution.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mnpos.distribution.R;
import com.mnpos.distribution.data.ApiClient;
import com.mnpos.distribution.data.Catalog;
import com.mnpos.distribution.data.Options;
import com.mnpos.distribution.model.FieldSpec;
import com.mnpos.distribution.model.PickerOption;
import com.mnpos.distribution.model.RecordSpec;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a create-form on the fly from RecordSpec.createFields, so adding a
 * "create" capability to a new distribution screen only means adding
 * FieldSpec entries in Catalog - not writing a new form Activity.
 *
 * NOTE: the exact JSON keys posted (location_id, sales_rep_id, amount, ...)
 * are inferred from the field names used elsewhere in the existing app.
 * Double check these against your backend's actual validation rules before
 * relying on this in production - I don't have visibility into the server
 * source, only the mobile client that calls it.
 */
public class RecordFormActivity extends Activity {
    private RecordSpec spec;
    private LinearLayout container;
    private Button submitButton;
    private final Map<String, View> fieldViews = new HashMap<>();
    private final Map<String, List<PickerOption>> pickerOptions = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_form);

        String key = getIntent().getStringExtra("specKey");
        spec = Catalog.RECORDS.get(key);
        if (spec == null || spec.createFields == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.screenTitle)).setText("New " + trimTrailingS(spec.title));
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        container = findViewById(R.id.formContainer);
        submitButton = findViewById(R.id.submitButton);
        submitButton.setOnClickListener(v -> submit());

        buildFields();
    }

    private void buildFields() {
        for (FieldSpec field : spec.createFields) {
            TextView label = new TextView(this);
            label.setText(field.label + (field.required ? " *" : ""));
            label.setTextColor(getResources().getColor(R.color.pos_muted));
            label.setTextSize(12);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = dp(12);
            container.addView(label, labelParams);

            View input = buildInput(field);
            LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(field.type == FieldSpec.Type.NOTES ? 90 : 48));
            inputParams.topMargin = dp(4);
            container.addView(input, inputParams);
            fieldViews.put(field.jsonKey, input);
        }
    }

    private View buildInput(FieldSpec field) {
        switch (field.type) {
            case BRANCH_PICKER: {
                Spinner spinner = new Spinner(this);
                spinner.setBackgroundResource(R.drawable.bg_input);
                Options.loadBranches(options -> bindSpinner(spinner, field.jsonKey, options));
                return spinner;
            }
            case REP_PICKER: {
                Spinner spinner = new Spinner(this);
                spinner.setBackgroundResource(R.drawable.bg_input);
                Options.loadReps(options -> bindSpinner(spinner, field.jsonKey, options));
                return spinner;
            }
            case NUMBER: {
                EditText input = plainInput();
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                return input;
            }
            case DATE: {
                EditText input = plainInput();
                input.setHint("YYYY-MM-DD");
                return input;
            }
            case NOTES: {
                EditText input = plainInput();
                input.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                input.setMinLines(3);
                return input;
            }
            default: {
                return plainInput();
            }
        }
    }

    private EditText plainInput() {
        EditText input = new EditText(this);
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        return input;
    }

    private void bindSpinner(Spinner spinner, String jsonKey, List<PickerOption> options) {
        pickerOptions.put(jsonKey, options);
        ArrayAdapter<PickerOption> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void submit() {
        try {
            JSONObject body = new JSONObject();
            for (FieldSpec field : spec.createFields) {
                View view = fieldViews.get(field.jsonKey);
                if (view instanceof Spinner) {
                    Spinner spinner = (Spinner) view;
                    Object selected = spinner.getSelectedItem();
                    int id = selected instanceof PickerOption ? ((PickerOption) selected).id : 0;
                    if (field.required && id <= 0) {
                        toast(field.label + " is required.");
                        return;
                    }
                    if (id > 0) body.put(field.jsonKey, id);
                } else if (view instanceof EditText) {
                    String text = ((EditText) view).getText().toString().trim();
                    if (field.required && text.isEmpty()) {
                        toast(field.label + " is required.");
                        return;
                    }
                    if (!text.isEmpty()) {
                        if (field.type == FieldSpec.Type.NUMBER) {
                            body.put(field.jsonKey, Double.parseDouble(text));
                        } else {
                            body.put(field.jsonKey, text);
                        }
                    }
                }
            }
            if (spec.branchFilter && !body.has("location_id")) {
                // fall back to nothing - server will use the user's default branch
            }

            submitButton.setEnabled(false);
            String endpoint = spec.createEndpoint != null ? spec.createEndpoint : spec.endpoint;
            ApiClient.post(endpoint, body, new ApiClient.JsonCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    submitButton.setEnabled(true);
                    boolean success = response.optBoolean("success", !response.has("http_status"));
                    if (success) {
                        toast(spec.title + " saved.");
                        finish();
                    } else {
                        toast(response.optString("message", "Save failed."));
                    }
                }

                @Override
                public void onError(Exception error) {
                    submitButton.setEnabled(true);
                    toast(error.getMessage() == null ? "Save failed." : error.getMessage());
                }
            });
        } catch (NumberFormatException nfe) {
            toast("Please enter a valid number.");
        } catch (Exception e) {
            toast(e.getMessage() == null ? "Something went wrong." : e.getMessage());
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String trimTrailingS(String label) {
        return label.endsWith("s") ? label.substring(0, label.length() - 1) : label;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
