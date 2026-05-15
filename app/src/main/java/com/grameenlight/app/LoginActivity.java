package com.grameenlight.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class LoginActivity extends Activity {
    private SessionManager sessionManager;
    private PanchayatAuthManager authManager;
    private LoginView loginView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(7, 21, 17));
        window.setNavigationBarColor(Color.rgb(7, 21, 17));

        sessionManager = new SessionManager(this);
        authManager = new PanchayatAuthManager();

        if (sessionManager.isAuthenticated()) {
            startMainActivity();
            return;
        }

        loginView = new LoginView(this);
        setContentView(loginView);
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showPanchayatLogin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Panchayat Login");
        builder.setMessage("Enter Panchayat ID and Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        layout.setPadding(padding, padding, padding, padding);

        EditText idInput = new EditText(this);
        idInput.setHint("Panchayat ID");
        idInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(idInput);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);

        builder.setView(layout);
        builder.setPositiveButton("Login", (dialog, which) -> {
            String panchayatId = idInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            if (panchayatId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            authenticatePanchayat(panchayatId, password);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void authenticatePanchayat(String panchayatId, String password) {
        if (!authManager.isConfigured()) {
            // Offline fallback — only used if Firebase is completely unavailable
            if ("panchayat".equals(panchayatId) && "admin".equals(password)) {
                sessionManager.setAuthenticated(panchayatId, "Demo Panchayat", "demo-village", "panchayat");
                startMainActivity();
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        authManager.authenticate(panchayatId, password, new PanchayatAuthManager.AuthCallback() {
            @Override
            public void onSuccess(String id, String name) {
                sessionManager.setAuthenticated(id, name, "demo-village", "panchayat");
                Toast.makeText(LoginActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                startMainActivity();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class LoginView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] citizenRect = new float[4];
        private final float[] panchayatRect = new float[4];

        LoginView(Activity activity) {
            super(activity);
            setFocusable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            
            // Premium deep background
            paint.setColor(Color.rgb(10, 15, 25));
            canvas.drawRect(0, 0, w, h, paint);
            
            // Floating background orbs (Mesh gradient effect)
            paint.setShader(new android.graphics.RadialGradient(w * 0.8f, h * 0.2f, dp(250), Color.argb(60, 0, 208, 142), Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP));
            canvas.drawCircle(w * 0.8f, h * 0.2f, dp(250), paint);
            
            paint.setShader(new android.graphics.RadialGradient(w * 0.2f, h * 0.8f, dp(300), Color.argb(50, 130, 80, 255), Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP));
            canvas.drawCircle(w * 0.2f, h * 0.8f, dp(300), paint);
            paint.setShader(null);

            // Title
            paint.setColor(Color.WHITE); // Crisp white
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(44));
            paint.setFakeBoldText(true);
            paint.setShadowLayer(dp(20), 0, dp(5), Color.argb(80, 180, 255, 220)); // Soft ambient glow
            canvas.drawText("Grameen-Light", w / 2f, dp(120), paint);
            paint.clearShadowLayer();
            paint.setFakeBoldText(false);

            // Subtitle
            paint.setColor(Color.rgb(170, 185, 200));
            paint.setTextSize(dp(16));
            canvas.drawText("Smart Village Illumination", w / 2f, dp(155), paint);

            float buttonWidth = dp(260);
            float buttonHeight = dp(64);
            float left = (w - buttonWidth) / 2f;
            float citizenTop = h / 2f - dp(50);
            float panchayatTop = h / 2f + dp(35);

            // Citizen Button (Glassmorphism + Neon accent)
            paint.setColor(Color.argb(80, 35, 45, 60)); // Brighter glass backdrop
            paint.setShadowLayer(dp(30), 0, dp(15), Color.argb(140, 0, 0, 0));
            canvas.drawRoundRect(left, citizenTop, left + buttonWidth, citizenTop + buttonHeight, dp(32), dp(32), paint);
            paint.clearShadowLayer();
            
            // Neon accent border
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.argb(180, 76, 210, 140));
            canvas.drawRoundRect(left, citizenTop, left + buttonWidth, citizenTop + buttonHeight, dp(32), dp(32), paint);
            paint.setStyle(Paint.Style.FILL);

            paint.setColor(Color.rgb(220, 255, 230)); 
            paint.setTextSize(dp(18));
            paint.setFakeBoldText(true);
            canvas.drawText("Continue as Citizen", w / 2f, citizenTop + dp(38), paint);

            // Panchayat Button
            paint.setColor(Color.argb(80, 35, 45, 60)); // Brighter glass backdrop
            paint.setShadowLayer(dp(30), 0, dp(15), Color.argb(140, 0, 0, 0));
            canvas.drawRoundRect(left, panchayatTop, left + buttonWidth, panchayatTop + buttonHeight, dp(32), dp(32), paint);
            paint.clearShadowLayer();

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.argb(180, 180, 100, 255));
            canvas.drawRoundRect(left, panchayatTop, left + buttonWidth, panchayatTop + buttonHeight, dp(32), dp(32), paint);
            paint.setStyle(Paint.Style.FILL);

            paint.setColor(Color.rgb(230, 200, 255));
            canvas.drawText("Panchayat Login", w / 2f, panchayatTop + dp(38), paint);
            paint.setFakeBoldText(false);

            citizenRect[0] = left;
            citizenRect[1] = citizenTop;
            citizenRect[2] = left + buttonWidth;
            citizenRect[3] = citizenTop + buttonHeight;
            panchayatRect[0] = left;
            panchayatRect[1] = panchayatTop;
            panchayatRect[2] = left + buttonWidth;
            panchayatRect[3] = panchayatTop + buttonHeight;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_DOWN) {
                return true;
            }
            float x = event.getX();
            float y = event.getY();
            if (contains(citizenRect, x, y)) {
                sessionManager.setAuthenticated("", "Citizen", "demo-village", "citizen");
                startMainActivity();
                return true;
            }
            if (contains(panchayatRect, x, y)) {
                showPanchayatLogin();
                return true;
            }
            return true;
        }

        private boolean contains(float[] rect, float x, float y) {
            return x >= rect[0] && x <= rect[2] && y >= rect[1] && y <= rect[3];
        }
    }
}
