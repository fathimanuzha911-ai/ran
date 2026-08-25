package com.mnpos.distribution.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mnpos.distribution.R;
import com.mnpos.distribution.data.ApiClient;
import com.mnpos.distribution.data.Session;

import org.json.JSONArray;
import org.json.JSONObject;

public class LoginActivity extends Activity {
    private EditText serverUrlInput;
    private EditText emailInput;
    private EditText passwordInput;
    private TextView loginStatus;
    private Button loginButton;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        serverUrlInput = findViewById(R.id.serverUrlInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginStatus = findViewById(R.id.loginStatus);
        loginButton = findViewById(R.id.loginButton);
        progress = findViewById(R.id.loginProgress);

        Session session = Session.get();
        if (session.isLoggedIn()) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }
        if (!session.baseUrl.isEmpty()) serverUrlInput.setText(session.baseUrl);

        loginButton.setOnClickListener(v -> login());
    }

    private void login() {
        String serverUrl = serverUrlInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (serverUrl.isEmpty() || email.isEmpty() || password.isEmpty()) {
            loginStatus.setText("Enter server, username, and password.");
            return;
        }

        Session session = Session.get();
        session.baseUrl = serverUrl;
        setLoading(true);

        try {
            JSONObject body = new JSONObject();
            body.put("username", email);
            body.put("email", email);
            body.put("password", password);

            ApiClient.post("/api/mobile/login", body, new ApiClient.JsonCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    boolean success = response.optBoolean("success", response.has("token"));
                    if (!success) {
                        fail(response.optString("message", "Login failed. Check your server URL and credentials."));
                        return;
                    }
                    applyLoginResponse(response, serverUrl);
                    setLoading(false);
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                }

                @Override
                public void onError(Exception error) {
                    fail(error.getMessage() == null ? "Could not reach server." : error.getMessage());
                }
            });
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void applyLoginResponse(JSONObject response, String serverUrl) {
        Session session = Session.get();
        session.baseUrl = serverUrl;
        session.authToken = response.optString("token", response.optString("access_token", ""));

        JSONArray locations = response.optJSONArray("locations");
        if (locations != null && locations.length() > 0) {
            JSONObject location = locations.optJSONObject(0);
            if (location != null) {
                session.locationId = location.optInt("id", 0);
                session.locationName = location.optString("name", "Default Location");
            }
        }

        JSONObject user = response.optJSONObject("user");
        session.userName = user == null ? email() : user.optString("name", user.optString("username", email()));
        session.userMobile = user == null ? "" : user.optString("mobile", user.optString("phone", ""));
        session.roleName = user == null ? "User" : user.optString("role_name", "User");
        session.businessName = response.optString("business_name", "Business");

        session.permissions.clear();
        addPermissions(session, response.optJSONArray("permissions"));
        addPermissions(session, response.optJSONArray("mobile_permissions"));

        session.save(this);
    }

    private void addPermissions(Session session, JSONArray permissions) {
        if (permissions == null) return;
        for (int i = 0; i < permissions.length(); i++) {
            String p = permissions.optString(i, "").trim();
            if (!p.isEmpty()) session.permissions.add(p);
        }
    }

    private String email() {
        return emailInput.getText().toString().trim();
    }

    private void fail(String message) {
        setLoading(false);
        loginStatus.setText(message == null ? "Login failed." : message);
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) loginStatus.setText("");
    }
}
