package pl.halovisuals.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 2D drawing helpers. Rounded shapes are built from real geometry (triangle fans) with a 1-device-pixel
 * feathered fringe = true anti-aliasing at any GUI scale (no stair-stepped corners).
 */
public final class Draw {
    public static final int YELLOW = 0xFFFFD84D, ORANGE = 0xFFFF8A1F, WHITE = 0xFFFFFFFF;
    public static final int GLASS = 0xB0261E26, GRAY = 0xFFB9B2A6;

    /** Global alpha multiplier (ClickGUI fade) and shared glow pulse (0..1). */
    public static float alphaMul = 1f, pulse = 0.5f;

    private static final Vector3f TMP = new Vector3f();

    private Draw() {}

    public static int a(int c, int alpha) { return (c & 0xFFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24); }

    private static int mulA(int c) { return alphaMul >= 0.999f ? c : a(c, (int) (((c >>> 24) & 255) * alphaMul)); }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int A = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int R = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int G = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int B = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (A << 24) | (R << 16) | (G << 8) | B;
    }

    public static int accent(float t) { return lerp(YELLOW, ORANGE, t); }

    /** Device pixels per drawing unit (window scale factor x current matrix scale). */
    private static float dev(DrawContext c) {
        c.getMatrices().peek().getPositionMatrix().getScale(TMP);
        return (float) Math.max(1.0, MinecraftClient.getInstance().getWindow().getScaleFactor() * TMP.x);
    }

    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int col) { rrectG(c, x, y, w, h, r, col, col); }

    /** Anti-aliased rounded rectangle with horizontal colour gradient (colL -> colR). */
    public static void rrectG(DrawContext c, float x, float y, float w, float h, float r, int colL, int colR) {
        if (w <= 0 || h <= 0) return;
        colL = mulA(colL); colR = mulA(colR);
        r = Math.max(0f, Math.min(r, Math.min(w, h) / 2f));
        Matrix4f m = c.getMatrices().peek().getPositionMatrix();
        VertexConsumer vc = c.getVertexConsumers().getBuffer(RenderLayer.getGui());
        float d = dev(c), fw = 1.0f / d;
        if (r < 0.4f) {
            quad(vc, m, x, y, colL, x + w, y, colR, x + w, y + h, colR, x, y + h, colL);
            // fringe
            int el = colL & 0xFFFFFF, er = colR & 0xFFFFFF;
            quad(vc, m, x, y - fw, el, x + w, y - fw, er, x + w, y, colR, x, y, colL);
            quad(vc, m, x, y + h, colL, x + w, y + h, colR, x + w, y + h + fw, er, x, y + h + fw, el);
            quad(vc, m, x - fw, y, el, x, y, colL, x, y + h, colL, x - fw, y + h, el);
            quad(vc, m, x + w, y, colR, x + w + fw, y, er, x + w + fw, y + h, er, x + w, y + h, colR);
            c.draw();
            return;
        }
        int seg = Math.max(5, Math.min(16, (int) (r * d / 2f)));
        int n = 4 * (seg + 1);
        float[] px = new float[n], py = new float[n], nx = new float[n], ny = new float[n];
        float[][] cs = {{x + r, y + r, 180}, {x + w - r, y + r, 270}, {x + w - r, y + h - r, 0}, {x + r, y + h - r, 90}};
        int k = 0;
        for (float[] cc : cs) {
            for (int s = 0; s <= seg; s++) {
                double ang = Math.toRadians(cc[2] + 90.0 * s / seg);
                nx[k] = (float) Math.cos(ang); ny[k] = (float) Math.sin(ang);
                px[k] = cc[0] + nx[k] * r; py[k] = cc[1] + ny[k] * r;
                k++;
            }
        }
        float cx0 = x + w / 2f, cy0 = y + h / 2f;
        int cmid = lerp(colL, colR, 0.5f);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            int ci = lerp(colL, colR, (px[i] - x) / w), cj = lerp(colL, colR, (px[j] - x) / w);
            quad(vc, m, cx0, cy0, cmid, px[i], py[i], ci, px[j], py[j], cj, px[j], py[j], cj);
            quad(vc, m, px[i], py[i], ci, px[j], py[j], cj, px[j] + nx[j] * fw, py[j] + ny[j] * fw, cj & 0xFFFFFF, px[i] + nx[i] * fw, py[i] + ny[i] * fw, ci & 0xFFFFFF);
        }
        c.draw();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, int c0, float x1, float y1, int c1,
                             float x2, float y2, int c2, float x3, float y3, int c3) {
        vc.vertex(m, x0, y0, 0f).color(c0);
        vc.vertex(m, x1, y1, 0f).color(c1);
        vc.vertex(m, x2, y2, 0f).color(c2);
        vc.vertex(m, x3, y3, 0f).color(c3);
    }

    /** Horizontal gradient rectangle (one quad). */
    public static void gradH(DrawContext c, float x0, float y0, float x1, float y1, int colL, int colR) {
        Matrix4f m = c.getMatrices().peek().getPositionMatrix();
        VertexConsumer vc = c.getVertexConsumers().getBuffer(RenderLayer.getGui());
        quad(vc, m, x0, y0, colL, x0, y1, colL, x1, y1, colR, x1, y0, colR);
        c.draw();
    }

    /** Anti-aliased arc stroke. a0/sweep in degrees (0 = right, -90 = top). */
    public static void arc(DrawContext c, float cx, float cy, float r, float thick, float a0, float sweep, int col) {
        if (Math.abs(sweep) < 0.1f) return;
        col = mulA(col);
        Matrix4f m = c.getMatrices().peek().getPositionMatrix();
        VertexConsumer vc = c.getVertexConsumers().getBuffer(RenderLayer.getGui());
        float fw = 1.0f / dev(c), e = col & 0xFFFFFF;
        int edge = (int) e;
        int n = Math.max(12, (int) (Math.abs(sweep) / 360f * 2 * Math.PI * r / 1.2));
        float r0 = r - thick / 2 - fw, r1 = r - thick / 2, r2 = r + thick / 2, r3 = r + thick / 2 + fw;
        for (int i = 0; i < n; i++) {
            double ang0 = Math.toRadians(a0 + sweep * i / n), ang1 = Math.toRadians(a0 + sweep * (i + 1) / n);
            float c0 = (float) Math.cos(ang0), s0 = (float) Math.sin(ang0), c1 = (float) Math.cos(ang1), s1 = (float) Math.sin(ang1);
            quad(vc, m, cx + c0 * r0, cy + s0 * r0, edge, cx + c0 * r1, cy + s0 * r1, col, cx + c1 * r1, cy + s1 * r1, col, cx + c1 * r0, cy + s1 * r0, edge);
            quad(vc, m, cx + c0 * r1, cy + s0 * r1, col, cx + c0 * r2, cy + s0 * r2, col, cx + c1 * r2, cy + s1 * r2, col, cx + c1 * r1, cy + s1 * r1, col);
            quad(vc, m, cx + c0 * r2, cy + s0 * r2, col, cx + c0 * r3, cy + s0 * r3, edge, cx + c1 * r3, cy + s1 * r3, edge, cx + c1 * r2, cy + s1 * r2, col);
        }
        c.draw();
    }

    /** Glass panel: soft pulsing golden glow + white border + smoky body (all anti-aliased). */
    public static void panel(DrawContext c, int x, int y, int w, int h, int r) {
        for (int k = 3; k >= 1; k--) rrect(c, x - k * 2, y - k * 2, w + 4 * k, h + 4 * k, r + k * 2, a(YELLOW, (int) (3 + 6 * pulse)));
        rrect(c, x - 1, y - 1, w + 2, h + 2, r + 1, a(WHITE, 75));
        rrect(c, x, y, w, h, r, GLASS);
    }

    public static int width(String s) { return MinecraftClient.getInstance().textRenderer.getWidth(s); }

    public static void text(DrawContext c, String s, int x, int y, int col) {
        col = mulA(col);
        if (((col >>> 24) & 255) <= 3) return;
        c.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, col, true);
    }

    public static void ctext(DrawContext c, String s, int cx, int y, int col) { text(c, s, cx - width(s) / 2, y, col); }

    /** Gradient progress bar (yellow -> orange). */
    public static void bar(DrawContext c, int x, int y, int w, int h, float frac) {
        rrect(c, x, y, w, h, h / 2, a(WHITE, 40));
        float f = Math.max(0f, Math.min(1f, frac));
        if (f > 0.01f) rrectG(c, x, y, Math.max(h, w * f), h, h / 2f, accent(0f), accent(f));
    }

    /** Circle outline, progress 0..1 starting at the top, clockwise (smooth arc). */
    public static void ring(DrawContext c, float cx, float cy, float r, float progress, int col) {
        arc(c, cx, cy, r, 1.6f, -90f, 360f * Math.max(0f, Math.min(1f, progress)), col);
    }
}
