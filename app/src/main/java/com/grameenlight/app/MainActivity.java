package com.grameenlight.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.room.Room;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private GrameenDb db;
    private PoleDao dao;
    private FirebaseReporter firebaseReporter;
    private FirebaseSyncManager syncManager;
    private GrameenView grameenView;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(7, 21, 17));
        window.setNavigationBarColor(Color.rgb(7, 21, 17));

        sessionManager = new SessionManager(this);

        // Check if user is authenticated, if not go back to login
        if (!sessionManager.isAuthenticated()) {
            startLoginActivity();
            return;
        }

        db = Room.databaseBuilder(this, GrameenDb.class, "grameen-light.db")
                .allowMainThreadQueries()
                .addMigrations(GrameenDb.MIGRATION_1_2)
                .build();
        dao = db.poleDao();
        firebaseReporter = new FirebaseReporter();
        syncManager = new FirebaseSyncManager(dao);
        seedPoles();

        grameenView = new GrameenView(this);
        setContentView(grameenView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (syncManager != null) {
            syncManager.startSync(() -> {
                if (grameenView != null) {
                    grameenView.reload();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (syncManager != null) {
            syncManager.stopSync();
        }
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void seedPoles() {
        if (dao.poleCount() > 0) {
            return;
        }
        float[][] points = {
                {0.18f, 0.20f}, {0.42f, 0.16f}, {0.70f, 0.22f},
                {0.26f, 0.40f}, {0.54f, 0.38f}, {0.82f, 0.42f},
                {0.16f, 0.66f}, {0.40f, 0.70f}, {0.68f, 0.64f}, {0.86f, 0.76f}
        };
        String[] lanes = {
                "Temple Road", "School Junction", "Water Tank Lane", "Anganwadi Street",
                "Market Bend", "Panchayat Gate", "Canal Walk", "Bus Stop Line",
                "Health Post Road", "North Hamlet"
        };
        for (int i = 0; i < points.length; i++) {
            PoleEntity pole = new PoleEntity();
            pole.id = i + 1;
            pole.x = points[i][0];
            pole.y = points[i][1];
            pole.lane = lanes[i];
            pole.status = i == 4 ? PoleStatus.FUSED : i == 7 ? PoleStatus.BURNING_DAY : PoleStatus.WORKING;
            pole.tracker = i == 4 ? "Assigned" : i == 7 ? "Fixed" : "Healthy";
            pole.complaintId = pole.status.equals(PoleStatus.WORKING) ? "" : makeComplaintId(pole.id);
            pole.updatedAt = System.currentTimeMillis() - (long) i * 3200000L;
            dao.upsertPole(pole);
        }
    }

    private static String makeComplaintId(int poleId) {
        String stamp = new SimpleDateFormat("MMddHHmm", Locale.US).format(new Date());
        return "GL-" + stamp + "-P" + String.format(Locale.US, "%02d", poleId);
    }

    private final class GrameenView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF mapRect = new RectF();
        private final RectF themeRect = new RectF();
        private final RectF adminRect = new RectF();
        private final RectF sheetRect = new RectF();
        private final RectF workingRect = new RectF();
        private final RectF fusedRect = new RectF();
        private final RectF dayRect = new RectF();
        private final RectF closeRect = new RectF();
        private final List<PoleEntity> poles = new ArrayList<>();
        private final List<ReportEntity> reports = new ArrayList<>();
        private boolean nightMode = true;
        private PoleEntity selectedPole;
        private String lastComplaint = "";
        private String cloudStatus = syncManager.isConfigured()
                ? "Firebase: connecting..."
                : "Firebase: add google-services.json to enable sync";

        // Panchayat console scroll state
        private float trackerScrollOffset = 0f;
        private float touchDownY = 0f;
        private float touchDownScrollOffset = 0f;
        private boolean isTouchScrolling = false;

        GrameenView(Activity activity) {
            super(activity);
            setFocusable(true);
            reload();
        }

        void reload() {
            poles.clear();
            poles.addAll(dao.getPoles());
            reports.clear();
            reports.addAll(dao.getRecentReports());
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            drawBackground(canvas, w, h);
            drawHeader(canvas, w);
            drawEnergy(canvas, w);
            drawMap(canvas, w, h);
            drawTracker(canvas, w, h);
            boolean isPanchayat = sessionManager.isPanchayat();
            if (selectedPole != null && !isPanchayat) {
                drawReportSheet(canvas, w, h);
            }
        }

        private void drawBackground(Canvas canvas, int w, int h) {
            int top = nightMode ? Color.rgb(10, 15, 25) : Color.rgb(245, 248, 250);
            int bottom = nightMode ? Color.rgb(2, 5, 10) : Color.rgb(235, 240, 245);
            paint.setShader(new LinearGradient(0, 0, 0, h, top, bottom, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);

            // Ambient glowing orbs (Mesh gradient effect)
            paint.setShader(new RadialGradient(w * 0.8f, h * 0.1f, w * 0.6f,
                    nightMode ? Color.argb(50, 76, 210, 140) : Color.argb(70, 110, 230, 160),
                    Color.TRANSPARENT, Shader.TileMode.CLAMP));
            canvas.drawCircle(w * 0.8f, h * 0.1f, w * 0.6f, paint);
            paint.setShader(null);
            
            paint.setShader(new RadialGradient(w * 0.1f, h * 0.4f, w * 0.5f,
                    nightMode ? Color.argb(30, 180, 100, 255) : Color.argb(40, 200, 140, 255),
                    Color.TRANSPARENT, Shader.TileMode.CLAMP));
            canvas.drawCircle(w * 0.1f, h * 0.4f, w * 0.5f, paint);
            paint.setShader(null);
        }

        private void drawHeader(Canvas canvas, int w) {
            paint.setShader(null);
            paint.setColor(nightMode ? Color.WHITE : Color.rgb(20, 30, 40));
            paint.setTextSize(dp(24)); // Reduced title size to avoid overlap
            paint.setFakeBoldText(true);
            canvas.drawText("Grameen-Light", dp(22), dp(80), paint);
            paint.setFakeBoldText(false);

            paint.setColor(nightMode ? Color.rgb(170, 185, 200) : Color.rgb(100, 110, 120));
            paint.setTextSize(dp(12)); // Reduced subtitle size
            canvas.drawText("Smart Village Illumination", dp(23), dp(102), paint);

            // Day/Night Button - Glassmorphism
            themeRect.set(w - dp(96), dp(57), w - dp(20), dp(91));
            paint.setColor(nightMode ? Color.argb(40, 255, 255, 255) : Color.argb(200, 255, 255, 255));
            paint.setShadowLayer(dp(10), 0, dp(5), Color.argb(40, 0, 0, 0));
            canvas.drawRoundRect(themeRect, dp(17), dp(17), paint);
            paint.clearShadowLayer();
            
            paint.setColor(nightMode ? Color.rgb(255, 230, 100) : Color.rgb(20, 30, 40));
            paint.setTextSize(dp(15));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            canvas.drawText(nightMode ? "DAY" : "NIGHT", themeRect.centerX(), themeRect.centerY() + dp(5), paint);
            paint.setFakeBoldText(false);
            paint.setTextAlign(Paint.Align.LEFT);

            // LOGOUT Button
            boolean isPanchayat = sessionManager.isPanchayat();
            adminRect.set(w - dp(192), dp(57), w - dp(104), dp(91));
            paint.setColor(Color.argb(30, 255, 50, 50));
            paint.setShadowLayer(dp(10), 0, dp(5), Color.argb(40, 0, 0, 0));
            canvas.drawRoundRect(adminRect, dp(17), dp(17), paint);
            paint.clearShadowLayer();
            
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.5f));
            paint.setColor(Color.argb(100, 255, 100, 100));
            canvas.drawRoundRect(adminRect, dp(17), dp(17), paint);
            paint.setStyle(Paint.Style.FILL);

            paint.setColor(Color.rgb(255, 120, 120));
            paint.setTextSize(dp(13));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            canvas.drawText("LOGOUT", adminRect.centerX(), adminRect.centerY() + dp(5), paint);
            paint.setFakeBoldText(false);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        private void drawEnergy(Canvas canvas, int w) {
            RectF card = new RectF(dp(18), dp(122), w - dp(18), dp(224));
            
            // Glassmorphism Card
            paint.setColor(nightMode ? Color.argb(180, 20, 25, 35) : Color.argb(240, 255, 255, 255));
            paint.setShadowLayer(dp(25), 0, dp(15), Color.argb(40, 0, 0, 0));
            canvas.drawRoundRect(card, dp(24), dp(24), paint);
            paint.clearShadowLayer();
            
            // Subtle border
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(nightMode ? Color.argb(30, 255, 255, 255) : Color.argb(50, 0, 0, 0));
            canvas.drawRoundRect(card, dp(24), dp(24), paint);
            paint.setStyle(Paint.Style.FILL);

            paint.setColor(nightMode ? Color.rgb(170, 185, 200) : Color.rgb(100, 110, 120));
            paint.setTextSize(dp(14));
            canvas.drawText("Energy saved this month", card.left + dp(20), card.top + dp(28), paint);

            int dayReports = Math.max(dao.countReportsByStatus(PoleStatus.BURNING_DAY), 1);
            int units = 18 + dayReports * 6;
            paint.setColor(nightMode ? Color.rgb(110, 230, 160) : Color.rgb(0, 180, 110));
            paint.setTextSize(dp(34));
            paint.setFakeBoldText(true);
            canvas.drawText(units + " kWh", card.left + dp(20), card.top + dp(68), paint);
            paint.setFakeBoldText(false);

            RectF bar = new RectF(card.left + dp(160), card.top + dp(46), card.right - dp(20), card.top + dp(58));
            rounded(canvas, bar, nightMode ? Color.argb(50, 255, 255, 255) : Color.argb(30, 0, 0, 0), dp(6));
            RectF fill = new RectF(bar.left, bar.top, bar.left + bar.width() * Math.min(0.88f, units / 90f), bar.bottom);
            rounded(canvas, fill, nightMode ? Color.rgb(110, 230, 160) : Color.rgb(0, 180, 110), dp(6));

            paint.setColor(nightMode ? Color.rgb(120, 135, 150) : Color.rgb(140, 150, 160));
            paint.setTextSize(dp(11));
            canvas.drawText("Daytime burning reports converted to visible savings", card.left + dp(20), card.bottom - dp(16), paint);
        }

        private void drawMap(Canvas canvas, int w, int h) {
            mapRect.set(dp(18), dp(242), w - dp(18), Math.min(h - dp(242), dp(560)));
            
            // Glassmorphism Map Card
            paint.setColor(nightMode ? Color.argb(160, 15, 20, 25) : Color.argb(240, 255, 255, 255));
            paint.setShadowLayer(dp(20), 0, dp(10), Color.argb(30, 0, 0, 0));
            canvas.drawRoundRect(mapRect, dp(26), dp(26), paint);
            paint.clearShadowLayer();
            
            // Subtle border
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(nightMode ? Color.argb(30, 255, 255, 255) : Color.argb(40, 0, 0, 0));
            canvas.drawRoundRect(mapRect, dp(26), dp(26), paint);
            paint.setStyle(Paint.Style.FILL);

            // Clip inner drawings (paths/houses) to map rounded corners
            canvas.save();
            Path clipPath = new Path();
            clipPath.addRoundRect(mapRect, dp(26), dp(26), Path.Direction.CW);
            canvas.clipPath(clipPath);

            drawVillagePaths(canvas);
            drawHouses(canvas);
            canvas.restore(); // restore clipping

            paint.setTextSize(dp(12));
            for (PoleEntity pole : poles) {
                float x = mapRect.left + mapRect.width() * pole.x;
                float y = mapRect.top + mapRect.height() * pole.y;
                drawPole(canvas, pole, x, y);
            }

            paint.setColor(nightMode ? Color.rgb(200, 220, 240) : Color.rgb(20, 40, 60));
            paint.setTextSize(dp(16));
            paint.setFakeBoldText(true);
            canvas.drawText("Village Pole Map", mapRect.left + dp(20), mapRect.top + dp(32), paint);
            paint.setFakeBoldText(false);

            drawLegend(canvas);
        }

        private void drawVillagePaths(Canvas canvas) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(dp(18));
            paint.setColor(nightMode ? Color.rgb(26, 61, 54) : Color.rgb(211, 224, 195));
            Path road = new Path();
            road.moveTo(mapRect.left + mapRect.width() * 0.08f, mapRect.top + mapRect.height() * 0.28f);
            road.cubicTo(mapRect.left + mapRect.width() * 0.32f, mapRect.top + mapRect.height() * 0.16f,
                    mapRect.left + mapRect.width() * 0.52f, mapRect.top + mapRect.height() * 0.48f,
                    mapRect.left + mapRect.width() * 0.90f, mapRect.top + mapRect.height() * 0.37f);
            canvas.drawPath(road, paint);
            canvas.drawLine(mapRect.left + mapRect.width() * 0.18f, mapRect.top + mapRect.height() * 0.76f,
                    mapRect.left + mapRect.width() * 0.90f, mapRect.top + mapRect.height() * 0.70f, paint);
            canvas.drawLine(mapRect.left + mapRect.width() * 0.32f, mapRect.top + mapRect.height() * 0.18f,
                    mapRect.left + mapRect.width() * 0.22f, mapRect.top + mapRect.height() * 0.78f, paint);

            paint.setStrokeWidth(dp(2));
            paint.setColor(nightMode ? Color.argb(80, 246, 220, 120) : Color.argb(90, 44, 111, 79));
            canvas.drawPath(road, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawHouses(Canvas canvas) {
            int[] colors = nightMode
                    ? new int[]{Color.rgb(27, 74, 62), Color.rgb(44, 68, 51), Color.rgb(62, 55, 36)}
                    : new int[]{Color.rgb(183, 220, 194), Color.rgb(230, 220, 163), Color.rgb(183, 210, 221)};
            for (int i = 0; i < 9; i++) {
                float x = mapRect.left + mapRect.width() * (0.12f + (i % 3) * 0.27f + (i > 5 ? 0.08f : 0));
                float y = mapRect.top + mapRect.height() * (0.46f + (i / 3) * 0.16f);
                paint.setColor(colors[i % colors.length]);
                RectF home = new RectF(x, y, x + dp(24), y + dp(18));
                rounded(canvas, home, paint.getColor(), dp(4));
                paint.setColor(nightMode ? Color.rgb(255, 210, 91) : Color.rgb(111, 88, 51));
                canvas.drawRect(x + dp(15), y + dp(7), x + dp(20), y + dp(14), paint);
            }
        }

        private void drawPole(Canvas canvas, PoleEntity pole, float x, float y) {
            int color = colorForStatus(pole.status);
            paint.setColor(nightMode ? Color.argb(120, 255, 255, 255) : Color.argb(90, 0, 0, 0));
            paint.setStrokeWidth(dp(2));
            canvas.drawLine(x, y + dp(16), x, y + dp(39), paint);
            paint.setColor(Color.argb(55, Color.red(color), Color.green(color), Color.blue(color)));
            canvas.drawCircle(x, y, dp(20), paint);
            paint.setColor(color);
            canvas.drawCircle(x, y, dp(10), paint);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(9));
            paint.setFakeBoldText(true);
            canvas.drawText(String.valueOf(pole.id), x, y + dp(4), paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setFakeBoldText(false);
        }

        private void drawLegend(Canvas canvas) {
            float x = mapRect.left + dp(18);
            float y = mapRect.bottom - dp(24);
            legendItem(canvas, x, y, colorForStatus(PoleStatus.WORKING), "Working");
            legendItem(canvas, x + dp(104), y, colorForStatus(PoleStatus.FUSED), "Fused");
            legendItem(canvas, x + dp(188), y, colorForStatus(PoleStatus.BURNING_DAY), "Day burn");
        }

        private void legendItem(Canvas canvas, float x, float y, int color, String label) {
            paint.setColor(color);
            canvas.drawCircle(x, y - dp(4), dp(5), paint);
            paint.setColor(nightMode ? Color.rgb(178, 214, 203) : Color.rgb(53, 91, 72));
            paint.setTextSize(dp(10));
            canvas.drawText(label, x + dp(10), y, paint);
        }

        private void drawTracker(Canvas canvas, int w, int h) {
            float top = mapRect.bottom + dp(16);
            boolean isPanchayat = sessionManager.isPanchayat();

            // ── Section title ──────────────────────────────────────────────
            paint.setColor(nightMode ? Color.WHITE : Color.rgb(20, 40, 60));
            paint.setTextSize(dp(20));
            paint.setFakeBoldText(true);
            canvas.drawText(isPanchayat ? "Panchayat Console" : "Repair Tracker", dp(22), top + dp(22), paint);
            paint.setFakeBoldText(false);
            if (isPanchayat) {
                paint.setColor(nightMode ? Color.rgb(150, 170, 190) : Color.rgb(80, 100, 120));
                paint.setTextSize(dp(11));
                canvas.drawText("Scroll & tap a complaint to take action", dp(22), top + dp(38), paint);
            }

            List<ReportEntity> list = reports.isEmpty() ? sampleTracker() : reports;

            if (isPanchayat) {
                // ── Panchayat: scrollable full list ────────────────────────
                float listTop = top + dp(48);
                float listBottom = h - dp(28);
                float itemH = dp(60);
                int total = list.size();

                float maxScroll = Math.max(0f, total * itemH - (listBottom - listTop));
                trackerScrollOffset = Math.max(0f, Math.min(trackerScrollOffset, maxScroll));

                canvas.save();
                canvas.clipRect(0, listTop, w, listBottom);

                for (int i = 0; i < total; i++) {
                    ReportEntity report = list.get(i);
                    float y = listTop + i * itemH - trackerScrollOffset;
                    if (y + itemH < listTop || y > listBottom) continue;

                    RectF row = new RectF(dp(18), y, w - dp(18), y + dp(50));
                    
                    // Glassmorphism Row
                    int rowBg = "Fixed".equals(report.tracker)
                            ? (nightMode ? Color.argb(160, 20, 60, 40) : Color.argb(220, 230, 255, 240))
                            : (nightMode ? Color.argb(120, 25, 35, 45) : Color.argb(230, 255, 255, 255));
                    
                    paint.setColor(rowBg);
                    paint.setShadowLayer(dp(10), 0, dp(4), Color.argb(30, 0, 0, 0));
                    canvas.drawRoundRect(row, dp(16), dp(16), paint);
                    paint.clearShadowLayer();
                    
                    // subtle stroke
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(1));
                    paint.setColor(nightMode ? Color.argb(30, 255, 255, 255) : Color.argb(40, 0, 0, 0));
                    canvas.drawRoundRect(row, dp(16), dp(16), paint);
                    paint.setStyle(Paint.Style.FILL);

                    paint.setColor(colorForStatus(report.status));
                    canvas.drawCircle(row.left + dp(20), row.centerY(), dp(8), paint);

                    paint.setColor(nightMode ? Color.WHITE : Color.rgb(20, 40, 60));
                    paint.setTextSize(dp(12));
                    paint.setFakeBoldText(true);
                    canvas.drawText(report.complaintId, row.left + dp(38), row.centerY() - dp(4), paint);
                    paint.setFakeBoldText(false);

                    paint.setColor(nightMode ? Color.rgb(170, 185, 200) : Color.rgb(100, 110, 120));
                    paint.setTextSize(dp(10));
                    canvas.drawText("Pole " + report.poleId + "  ·  " + report.tracker,
                            row.left + dp(38), row.centerY() + dp(14), paint);

                    drawStatusPill(canvas, row.right - dp(104), row.top + dp(14), report.status);
                }
                canvas.restore();

                float totalH = total * itemH;
                float visibleH = listBottom - listTop;
                if (totalH > visibleH) {
                    float barH = (visibleH / totalH) * visibleH;
                    float barTop = listTop + (trackerScrollOffset / totalH) * visibleH;
                    paint.setColor(nightMode ? Color.argb(80, 255, 255, 255) : Color.argb(60, 0, 0, 0));
                    RectF track = new RectF(w - dp(6), listTop, w - dp(3), listBottom);
                    canvas.drawRoundRect(track, dp(2), dp(2), paint);
                    paint.setColor(nightMode ? Color.argb(180, 180, 100, 255) : Color.argb(180, 100, 80, 200));
                    RectF thumb = new RectF(w - dp(6), barTop, w - dp(3), barTop + barH);
                    canvas.drawRoundRect(thumb, dp(2), dp(2), paint);
                }
            } else {
                int count = Math.min(3, list.size());
                for (int i = 0; i < count; i++) {
                    ReportEntity report = list.get(i);
                    float y = top + dp(40 + i * 56);
                    RectF row = new RectF(dp(18), y, w - dp(18), y + dp(46));
                    
                    paint.setColor(nightMode ? Color.argb(120, 25, 35, 45) : Color.argb(230, 255, 255, 255));
                    paint.setShadowLayer(dp(10), 0, dp(4), Color.argb(30, 0, 0, 0));
                    canvas.drawRoundRect(row, dp(16), dp(16), paint);
                    paint.clearShadowLayer();
                    
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(1));
                    paint.setColor(nightMode ? Color.argb(30, 255, 255, 255) : Color.argb(40, 0, 0, 0));
                    canvas.drawRoundRect(row, dp(16), dp(16), paint);
                    paint.setStyle(Paint.Style.FILL);

                    paint.setColor(colorForStatus(report.status));
                    canvas.drawCircle(row.left + dp(20), row.centerY(), dp(8), paint);
                    
                    paint.setColor(nightMode ? Color.WHITE : Color.rgb(20, 40, 60));
                    paint.setTextSize(dp(12));
                    paint.setFakeBoldText(true);
                    canvas.drawText(report.complaintId, row.left + dp(38), row.centerY() - dp(4), paint);
                    paint.setFakeBoldText(false);
                    
                    paint.setColor(nightMode ? Color.rgb(170, 185, 200) : Color.rgb(100, 110, 120));
                    paint.setTextSize(dp(10));
                    canvas.drawText("Pole " + report.poleId + " - " + report.tracker, row.left + dp(38), row.centerY() + dp(14), paint);
                    drawStatusPill(canvas, row.right - dp(104), row.top + dp(12), report.status);
                }
            }

            // ── Bottom status bar ──────────────────────────────────────────
            paint.setColor(nightMode ? Color.rgb(134, 188, 171) : Color.rgb(82, 112, 95));
            paint.setTextSize(dp(10));
            canvas.drawText(cloudStatus, dp(22), h - dp(5), paint);
        }

        private List<ReportEntity> sampleTracker() {
            List<ReportEntity> samples = new ArrayList<>();
            for (PoleEntity pole : poles) {
                if (!PoleStatus.WORKING.equals(pole.status)) {
                    ReportEntity report = new ReportEntity();
                    report.poleId = pole.id;
                    report.status = pole.status;
                    report.tracker = pole.tracker;
                    report.complaintId = pole.complaintId;
                    samples.add(report);
                }
            }
            return samples;
        }

        private void drawStatusPill(Canvas canvas, float x, float y, String status) {
            RectF pill = new RectF(x, y, x + dp(88), y + dp(22));
            int base = colorForStatus(status);
            rounded(canvas, pill, Color.argb(45, Color.red(base), Color.green(base), Color.blue(base)), dp(11));
            paint.setColor(base);
            paint.setTextSize(dp(9));
            paint.setFakeBoldText(true);
            String label = status.equals(PoleStatus.BURNING_DAY) ? "DAY BURN" : status.toUpperCase(Locale.US);
            canvas.drawText(label, pill.left + dp(9), pill.centerY() + dp(4), paint);
            paint.setFakeBoldText(false);
        }

        private void drawReportSheet(Canvas canvas, int w, int h) {
            paint.setColor(Color.argb(180, 0, 0, 0)); // darker backdrop
            canvas.drawRect(0, 0, w, h, paint);
            sheetRect.set(dp(14), h - dp(310), w - dp(14), h - dp(14));
            
            // Glassmorphism Sheet
            paint.setColor(nightMode ? Color.argb(245, 245, 250, 239) : Color.argb(235, 15, 25, 30));
            paint.setShadowLayer(dp(30), 0, dp(-10), Color.argb(60, 0, 0, 0));
            canvas.drawRoundRect(sheetRect, dp(32), dp(32), paint);
            paint.clearShadowLayer();

            paint.setColor(nightMode ? Color.rgb(20, 40, 60) : Color.WHITE);
            paint.setTextSize(dp(24));
            paint.setFakeBoldText(true);
            canvas.drawText("Pole " + selectedPole.id + " - " + selectedPole.lane, sheetRect.left + dp(24), sheetRect.top + dp(44), paint);
            paint.setFakeBoldText(false);
            paint.setColor(nightMode ? Color.rgb(100, 120, 140) : Color.rgb(170, 185, 200));
            paint.setTextSize(dp(13));
            canvas.drawText("Tap once to send report and generate complaint ID", sheetRect.left + dp(24), sheetRect.top + dp(66), paint);

            closeRect.set(sheetRect.right - dp(52), sheetRect.top + dp(22), sheetRect.right - dp(22), sheetRect.top + dp(52));
            rounded(canvas, closeRect, nightMode ? Color.rgb(230, 240, 230) : Color.rgb(30, 45, 55), dp(15));
            paint.setColor(nightMode ? Color.rgb(50, 70, 90) : Color.rgb(200, 220, 240));
            paint.setTextSize(dp(18));
            paint.setFakeBoldText(true);
            canvas.drawText("X", closeRect.left + dp(9), closeRect.top + dp(21), paint);
            paint.setFakeBoldText(false);

            float top = sheetRect.top + dp(90);
            workingRect.set(sheetRect.left + dp(20), top, sheetRect.right - dp(20), top + dp(50));
            fusedRect.set(sheetRect.left + dp(20), top + dp(64), sheetRect.right - dp(20), top + dp(114));
            dayRect.set(sheetRect.left + dp(20), top + dp(128), sheetRect.right - dp(20), top + dp(178));
            reportButton(canvas, workingRect, PoleStatus.WORKING, "Light is working tonight", "Healthy");
            reportButton(canvas, fusedRect, PoleStatus.FUSED, "Bulb fused or stays dark", "Assigned");
            reportButton(canvas, dayRect, PoleStatus.BURNING_DAY, "Burning during daylight", "Assigned");
        }

        private void reportButton(Canvas canvas, RectF rect, String status, String label, String tracker) {
            int color = colorForStatus(status);
            paint.setColor(Color.argb(nightMode ? 40 : 60, Color.red(color), Color.green(color), Color.blue(color)));
            canvas.drawRoundRect(rect, dp(18), dp(18), paint);
            
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.5f));
            paint.setColor(Color.argb(nightMode ? 80 : 120, Color.red(color), Color.green(color), Color.blue(color)));
            canvas.drawRoundRect(rect, dp(18), dp(18), paint);
            paint.setStyle(Paint.Style.FILL);

            paint.setColor(color);
            canvas.drawCircle(rect.left + dp(26), rect.centerY(), dp(10), paint);
            
            paint.setColor(nightMode ? Color.rgb(20, 40, 60) : Color.WHITE);
            paint.setTextSize(dp(15));
            paint.setFakeBoldText(true);
            canvas.drawText(label, rect.left + dp(48), rect.centerY() - dp(4), paint);
            paint.setFakeBoldText(false);
            
            paint.setColor(nightMode ? Color.rgb(100, 120, 140) : Color.rgb(170, 185, 200));
            paint.setTextSize(dp(12));
            canvas.drawText("Tracker: " + tracker, rect.left + dp(48), rect.centerY() + dp(14), paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            boolean isPanchayat = sessionManager.isPanchayat();
            int action = event.getAction();

            // ── Panchayat scroll: track finger drag in the complaint list ──
            if (isPanchayat && action == MotionEvent.ACTION_DOWN) {
                touchDownY = y;
                touchDownScrollOffset = trackerScrollOffset;
                isTouchScrolling = false;
                return true;
            }
            if (isPanchayat && action == MotionEvent.ACTION_MOVE) {
                float dy = touchDownY - y;
                if (Math.abs(dy) > dp(6)) isTouchScrolling = true;
                if (isTouchScrolling) {
                    trackerScrollOffset = touchDownScrollOffset + dy;
                    invalidate();
                }
                return true;
            }

            // ── All tap logic on ACTION_UP ─────────────────────────────────
            if (action != MotionEvent.ACTION_UP) return true;

            // If we were scrolling, don't fire a tap
            if (isTouchScrolling) {
                isTouchScrolling = false;
                return true;
            }

            if (themeRect.contains(x, y)) {
                nightMode = !nightMode;
                invalidate();
                return true;
            }
            if (adminRect.contains(x, y)) {
                // Both citizen and panchayat can log out
                sessionManager.clearSession();
                startLoginActivity();
                return true;
            }
            if (selectedPole != null && !isPanchayat) {
                if (closeRect.contains(x, y) || !sheetRect.contains(x, y)) {
                    selectedPole = null;
                    invalidate();
                    return true;
                }
                if (workingRect.contains(x, y)) {
                    submitReport(PoleStatus.WORKING, "Healthy");
                } else if (fusedRect.contains(x, y)) {
                    submitReport(PoleStatus.FUSED, "Assigned");
                } else if (dayRect.contains(x, y)) {
                    submitReport(PoleStatus.BURNING_DAY, "Assigned");
                }
                return true;
            }
            if (!isPanchayat) {
                for (PoleEntity pole : poles) {
                    float px = mapRect.left + mapRect.width() * pole.x;
                    float py = mapRect.top + mapRect.height() * pole.y;
                    float dx = x - px;
                    float dy = y - py;
                    if (dx * dx + dy * dy <= dp(28) * dp(28)) {
                        selectedPole = pole;
                        invalidate();
                        return true;
                    }
                }
            }
            if (isPanchayat && handleAdminTrackerTap(x, y)) {
                return true;
            }
            return true;
        }

        private boolean handleAdminTrackerTap(float x, float y) {
            float listTop = mapRect.bottom + dp(60);   // matches drawTracker listTop
            float itemH   = dp(52);
            int h = getHeight();
            float listBottom = h - dp(28);
            List<ReportEntity> list = reports.isEmpty() ? sampleTracker() : reports;
            for (int i = 0; i < list.size(); i++) {
                float rowTop = listTop + i * itemH - trackerScrollOffset;
                float rowBot = rowTop + dp(42);
                if (rowTop > listBottom || rowBot < listTop) continue;  // off-screen
                RectF row = new RectF(dp(18), rowTop, getWidth() - dp(18), rowBot);
                if (row.contains(x, y)) {
                    showComplaintDialog(list.get(i));
                    return true;
                }
            }
            return false;
        }

        private void showComplaintDialog(ReportEntity report) {
            PoleEntity pole = dao.getPoleById(report.poleId);
            String lane = pole != null ? pole.lane : "Unknown location";

            String statusLabel = PoleStatus.FUSED.equals(report.status) ? "Fused / Dark"
                    : PoleStatus.BURNING_DAY.equals(report.status) ? "Burning in Daylight"
                    : "Working";

            String msg = "Complaint ID: " + report.complaintId
                    + "\nPole #" + report.poleId + "  ·  " + lane
                    + "\nIssue: " + statusLabel
                    + "\nCurrent status: " + report.tracker;

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Complaint Details");
            builder.setMessage(msg);

            // Only offer meaningful transitions
            if (!"Assigned".equals(report.tracker)) {
                builder.setNeutralButton("Mark Assigned", (d, w) -> {
                    applyAdminUpdate(report, "Assigned", report.status);
                });
            }
            if (!"Fixed".equals(report.tracker)) {
                builder.setPositiveButton("Mark Fixed ✓", (d, w) -> {
                    applyAdminUpdate(report, "Fixed", PoleStatus.WORKING);
                });
            }
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void applyAdminUpdate(ReportEntity report, String nextTracker, String nextPoleStatus) {
            PoleEntity pole = dao.getPoleById(report.poleId);
            if (pole == null) return;
            long now = System.currentTimeMillis();

            report.tracker = nextTracker;
            pole.tracker   = nextTracker;
            pole.status    = nextPoleStatus;
            pole.updatedAt = now;
            dao.updateReportTracker(report.complaintId, nextTracker);
            dao.upsertPole(pole);

            cloudStatus = "Updating: " + report.complaintId + " → " + nextTracker;
            firebaseReporter.updateTracker(
                    report.complaintId, report.poleId,
                    nextPoleStatus, nextTracker, now,
                    success -> {
                        cloudStatus = success
                                ? "Firebase: update synced ✓"
                                : "Firebase: saved locally, will sync";
                        invalidate();
                    });
            reload();
        }

        // updateAdminStatus replaced by applyAdminUpdate above (keeps code DRY)

        private void submitReport(String status, String tracker) {
            long now = System.currentTimeMillis();
            String complaint = makeComplaintId(selectedPole.id);
            selectedPole.status = status;
            selectedPole.tracker = tracker;
            selectedPole.complaintId = complaint;
            selectedPole.updatedAt = now;
            dao.upsertPole(selectedPole);

            ReportEntity report = new ReportEntity();
            report.complaintId = complaint;   // String PK — no autoGenerate
            report.poleId = selectedPole.id;
            report.status = status;
            report.tracker = tracker;
            report.createdAt = now;
            dao.upsertReport(report);

            lastComplaint = complaint;
            cloudStatus = "Firebase: sending report...";
            firebaseReporter.upload(selectedPole, report, success -> {
                cloudStatus = success ? "Firebase: report synced to Panchayat dashboard" : "Firebase: saved locally, waiting for setup/network";
                invalidate();
            });
            selectedPole = null;
            reload();
        }

        private int colorForStatus(String status) {
            if (PoleStatus.FUSED.equals(status)) {
                return Color.rgb(255, 92, 92);
            }
            if (PoleStatus.BURNING_DAY.equals(status)) {
                return Color.rgb(255, 204, 67);
            }
            return Color.rgb(0, 208, 142);
        }

        private void rounded(Canvas canvas, RectF rect, int color, float radius) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }

        private int dpInt(float value) {
            return Math.round(dp(value));
        }

    }
}
