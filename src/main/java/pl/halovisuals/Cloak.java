package pl.halovisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 * Halo Cloak: a cloth-simulated cape (verlet) for the local player. The texture (crystal facets, golden trim,
 * Halo Visuals logo) is generated procedurally with Java2D; a second translucent "shine" layer sweeps over it.
 */
public final class Cloak {
    private static final Identifier BASE = Identifier.of("halovisuals", "cloak_base");
    private static final Identifier SHINE = Identifier.of("halovisuals", "cloak_shine");
    private static final int LIGHT = 15728880;
    private static final int NX = 8, NY = 14;
    private static final double WIDTH = 0.64, LENGTH = 1.05;

    private static boolean ready, failed, init;
    private static final double[] x = new double[NX * NY], y = new double[NX * NY], z = new double[NX * NY];
    private static final double[] px = new double[NX * NY], py = new double[NX * NY], pz = new double[NX * NY];
    private static double acc;
    private static double bX, bZ; // body "back" direction (unit)
    private static final Random rnd = new Random();

    private Cloak() {}

    public static boolean failed() { return failed; }
    public static void reset() { init = false; acc = 0; }

    // ------------------------------------------------------------------ textures
    private static void ensureTextures() {
        if (ready || failed) return;
        try {
            System.setProperty("java.awt.headless", "true");
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.getTextureManager().registerTexture(BASE, new NativeImageBackedTexture(NativeImage.read(new ByteArrayInputStream(makeBase()))));
            mc.getTextureManager().registerTexture(SHINE, new NativeImageBackedTexture(NativeImage.read(new ByteArrayInputStream(makeShine()))));
            ready = true;
        } catch (Throwable t) {
            failed = true;
            t.printStackTrace();
        }
    }

    private static byte[] png(BufferedImage img) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bo);
        return bo.toByteArray();
    }

    private static Color mix(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static byte[] makeShine() throws Exception {
        BufferedImage img = new BufferedImage(512, 8, BufferedImage.TYPE_INT_ARGB);
        for (int xx = 0; xx < 512; xx++) {
            double a = Math.exp(-Math.pow((xx - 200) / 34.0, 2)) * 0.85 + Math.exp(-Math.pow((xx - 330) / 12.0, 2)) * 0.5;
            int al = (int) (Math.min(1, a) * 255);
            for (int yy = 0; yy < 8; yy++) img.setRGB(xx, yy, (al << 24) | 0xFFF8DC);
        }
        return png(img);
    }

    private static byte[] makeBase() throws Exception {
        final int W = 256, H = 384;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g.setPaint(new GradientPaint(0, 0, new Color(24, 18, 66), 0, H, new Color(150, 86, 160)));
        g.fillRect(0, 0, W, H);

        // crystal facets
        Random r = new Random(7);
        int cols = 6, rows = 9;
        double cw = W / (double) cols, ch = H / (double) rows;
        double[][][] p = new double[rows + 1][cols + 1][2];
        for (int j = 0; j <= rows; j++)
            for (int i = 0; i <= cols; i++) {
                p[j][i][0] = i * cw + ((i > 0 && i < cols) ? (r.nextDouble() - .5) * cw * .6 : 0);
                p[j][i][1] = j * ch + ((j > 0 && j < rows) ? (r.nextDouble() - .5) * ch * .6 : 0);
            }
        for (int j = 0; j < rows; j++)
            for (int i = 0; i < cols; i++) {
                double[][][] tri = {{p[j][i], p[j][i + 1], p[j + 1][i]}, {p[j][i + 1], p[j + 1][i + 1], p[j + 1][i]}};
                for (double[][] t : tri) {
                    float ty = (float) ((t[0][1] + t[1][1] + t[2][1]) / 3 / H);
                    Color base = mix(new Color(40, 30, 104), new Color(190, 112, 150), ty);
                    base = mix(base, new Color(255, 200, 110), Math.max(0, ty - 0.7f) * 1.2f);
                    float br = 0.78f + 0.5f * r.nextFloat();
                    g.setColor(new Color(Math.min(255, (int) (base.getRed() * br)), Math.min(255, (int) (base.getGreen() * br)), Math.min(255, (int) (base.getBlue() * br))));
                    Path2D.Double path = new Path2D.Double();
                    path.moveTo(t[0][0], t[0][1]); path.lineTo(t[1][0], t[1][1]); path.lineTo(t[2][0], t[2][1]); path.closePath();
                    g.fill(path);
                    g.setStroke(new BasicStroke(1.2f));
                    g.setColor(new Color(255, 255, 255, 48));
                    g.draw(path);
                }
            }
        for (int k = 0; k < 16; k++) {
            int i = r.nextInt(cols), j = r.nextInt(rows);
            Path2D.Double path = new Path2D.Double();
            path.moveTo(p[j][i][0], p[j][i][1]); path.lineTo(p[j][i + 1][0], p[j][i + 1][1]); path.lineTo(p[j + 1][i][0], p[j + 1][i][1]); path.closePath();
            g.setColor(new Color(255, 255, 255, 18 + r.nextInt(34)));
            g.fill(path);
        }

        // gold trim + collar
        g.setStroke(new BasicStroke(7f));
        g.setPaint(new GradientPaint(0, 0, new Color(255, 232, 150), 0, H, new Color(214, 140, 40)));
        g.drawRect(3, 3, W - 7, H - 7);
        g.setPaint(new GradientPaint(0, 0, new Color(255, 226, 140, 235), 0, 28, new Color(214, 140, 40, 120)));
        g.fillRect(0, 0, W, 28);

        // logo layer (halo + gem + text)
        BufferedImage logo = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D lg = logo.createGraphics();
        lg.setRenderingHints(g.getRenderingHints());
        lg.setStroke(new BasicStroke(9f));
        lg.setColor(new Color(255, 214, 110));
        lg.draw(new Ellipse2D.Double(58, 112, 140, 40));
        lg.setStroke(new BasicStroke(3f));
        lg.setColor(new Color(255, 250, 225));
        lg.draw(new Ellipse2D.Double(60, 110, 136, 38));
        Path2D.Double gem = new Path2D.Double();
        gem.moveTo(100, 178); gem.lineTo(156, 178); gem.lineTo(182, 202); gem.lineTo(128, 268); gem.lineTo(74, 202); gem.closePath();
        lg.setPaint(new GradientPaint(0, 178, new Color(255, 244, 200), 0, 268, new Color(255, 160, 50)));
        lg.fill(gem);
        lg.setStroke(new BasicStroke(2f));
        lg.setColor(new Color(255, 255, 255, 190));
        lg.draw(gem);
        lg.drawLine(74, 202, 182, 202); lg.drawLine(100, 178, 112, 202); lg.drawLine(156, 178, 144, 202);
        lg.drawLine(112, 202, 128, 268); lg.drawLine(144, 202, 128, 268);
        try {
            Font f = new Font(Font.SANS_SERIF, Font.BOLD, 44);
            lg.setFont(f);
            FontMetrics fm = lg.getFontMetrics();
            lg.setColor(new Color(255, 244, 205));
            lg.drawString("HALO", (W - fm.stringWidth("HALO")) / 2, 322);
            Font f2 = new Font(Font.SANS_SERIF, Font.BOLD, 19);
            lg.setFont(f2);
            FontMetrics fm2 = lg.getFontMetrics();
            String s = "V I S U A L S";
            lg.setColor(new Color(255, 206, 96));
            lg.drawString(s, (W - fm2.stringWidth(s)) / 2, 350);
        } catch (Throwable ignored) { /* no fonts available: the halo + gem emblem is still drawn */ }
        lg.dispose();

        float[] k = new float[81];
        java.util.Arrays.fill(k, 1f / 81f);
        ConvolveOp blur = new ConvolveOp(new Kernel(9, 9, k), ConvolveOp.EDGE_NO_OP, null);
        BufferedImage glow = blur.filter(blur.filter(logo, null), null);
        g.setComposite(AlphaComposite.SrcOver.derive(0.95f));
        g.drawImage(glow, 0, 0, null);
        g.drawImage(glow, 0, 0, null);
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(logo, 0, 0, null);

        // sparkles
        for (int i = 0; i < 20; i++) {
            double sx = 12 + r.nextDouble() * (W - 24), sy = 36 + r.nextDouble() * (H - 60), s = 2 + r.nextDouble() * 5;
            Path2D.Double star = new Path2D.Double();
            star.moveTo(sx, sy - s); star.lineTo(sx + s * .2, sy - s * .2); star.lineTo(sx + s, sy); star.lineTo(sx + s * .2, sy + s * .2);
            star.lineTo(sx, sy + s); star.lineTo(sx - s * .2, sy + s * .2); star.lineTo(sx - s, sy); star.lineTo(sx - s * .2, sy - s * .2); star.closePath();
            g.setColor(new Color(255, 255, 255, 150 + r.nextInt(100)));
            g.fill(star);
        }
        g.dispose();
        return png(img);
    }

    // ------------------------------------------------------------------ simulation
    /** Random point on the cape (for sparkles). */
    public static double[] randomPoint() {
        if (!init) return null;
        int k = rnd.nextInt(NX * NY);
        return new double[]{x[k], y[k], z[k]};
    }

    public static void update(double ox, double oy, double oz, double yaw, boolean sneak, float dt) {
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), lx = Math.cos(yaw), lz = Math.sin(yaw);
        bX = -fx; bZ = -fz;
        double sy = oy + (sneak ? 1.27 : 1.43), bk = 0.14 + (sneak ? 0.07 : 0);
        double[] ax = new double[NX], az = new double[NX];
        for (int i = 0; i < NX; i++) {
            double lat = (i / (double) (NX - 1) - 0.5) * WIDTH;
            ax[i] = ox + lx * lat + bX * bk; az[i] = oz + lz * lat + bZ * bk;
        }
        if (!init) {
            for (int j = 0; j < NY; j++)
                for (int i = 0; i < NX; i++) {
                    int k = j * NX + i;
                    x[k] = px[k] = ax[i] + bX * 0.02 * j; y[k] = py[k] = sy - j * LENGTH / (NY - 1); z[k] = pz[k] = az[i] + bZ * 0.02 * j;
                }
            init = true;
        }
        acc += Math.min(dt, 0.1f);
        int steps = 0;
        final double h = 1.0 / 60.0;
        while (acc >= h && steps++ < 4) {
            acc -= h;
            double damp = Math.exp(-2.6 * h);
            for (int k = NX; k < NX * NY; k++) {
                double vx = (x[k] - px[k]) * damp, vy = (y[k] - py[k]) * damp, vz = (z[k] - pz[k]) * damp;
                px[k] = x[k]; py[k] = y[k]; pz[k] = z[k];
                x[k] += vx; y[k] += vy - 11.0 * h * h; z[k] += vz;
            }
            for (int i = 0; i < NX; i++) { x[i] = px[i] = ax[i]; y[i] = py[i] = sy; z[i] = pz[i] = az[i]; }
            for (int it = 0; it < 5; it++) {
                for (int j = 0; j < NY; j++)
                    for (int i = 0; i < NX; i++) {
                        int k = j * NX + i;
                        if (i + 1 < NX) link(k, k + 1, WIDTH / (NX - 1));
                        if (j + 1 < NY) link(k, k + NX, LENGTH / (NY - 1));
                    }
                for (int i = 0; i < NX; i++) { x[i] = ax[i]; y[i] = sy; z[i] = az[i]; }
            }
            for (int k = NX; k < NX * NY; k++) {
                double rx = x[k] - ox, rz = z[k] - oz;
                double lat = rx * lx + rz * lz, back = rx * bX + rz * bZ;
                if (y[k] < sy + 0.02 && y[k] > oy + 0.02 && Math.abs(lat) < 0.36 && back < 0.17) {
                    double push = 0.17 - back;
                    x[k] += bX * push; z[k] += bZ * push;
                }
                if (y[k] < oy + 0.02) y[k] = oy + 0.02;
            }
        }
    }

    private static void link(int a, int b, double rest) {
        double dx = x[b] - x[a], dy = y[b] - y[a], dz = z[b] - z[a];
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d < 1e-9) return;
        double diff = (d - rest) / d;
        boolean pa = a < NX, pb = b < NX;
        double wa = pa ? 0 : (pb ? 1 : 0.5), wb = pb ? 0 : (pa ? 1 : 0.5);
        x[a] += dx * diff * wa; y[a] += dy * diff * wa; z[a] += dz * diff * wa;
        x[b] -= dx * diff * wb; y[b] -= dy * diff * wb; z[b] -= dz * diff * wb;
    }

    // ------------------------------------------------------------------ render
    private static void vtx(MatrixStack.Entry e, VertexConsumer vc, int k, float u, float v, int alpha, double off) {
        vc.vertex(e, (float) (x[k] + bX * off - Gfx.cam.x), (float) (y[k] - Gfx.cam.y), (float) (z[k] + bZ * off - Gfx.cam.z))
                .color(255, 255, 255, alpha).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(LIGHT).normal(e, 0f, 1f, 0f);
    }

    public static void render(MatrixStack.Entry e, VertexConsumerProvider.Immediate imm, double t, double shineSpeed, boolean shine) {
        ensureTextures();
        if (!ready || !init) return;
        RenderLayer base = RenderLayer.getEntityTranslucent(BASE);
        VertexConsumer vc = imm.getBuffer(base);
        for (int j = 0; j < NY - 1; j++)
            for (int i = 0; i < NX - 1; i++) {
                int a = j * NX + i, b = a + 1, c = a + NX + 1, d = a + NX;
                float u0 = i / (float) (NX - 1), u1 = (i + 1) / (float) (NX - 1), v0 = j / (float) (NY - 1), v1 = (j + 1) / (float) (NY - 1);
                vtx(e, vc, a, u0, v0, 255, 0); vtx(e, vc, b, u1, v0, 255, 0); vtx(e, vc, c, u1, v1, 255, 0); vtx(e, vc, d, u0, v1, 255, 0);
            }
        imm.draw(base);
        if (!shine) return;
        RenderLayer sl = RenderLayer.getEntityTranslucent(SHINE);
        VertexConsumer sv = imm.getBuffer(sl);
        float o = (float) (-0.5 + ((t * 0.4 * shineSpeed) % 1.0) * 1.7);
        for (int j = 0; j < NY - 1; j++)
            for (int i = 0; i < NX - 1; i++) {
                int a = j * NX + i, b = a + 1, c = a + NX + 1, d = a + NX;
                float f0 = i / (float) (NX - 1), f1 = (i + 1) / (float) (NX - 1), g0 = j / (float) (NY - 1), g1 = (j + 1) / (float) (NY - 1);
                float s0 = 0.45f, sg = 0.18f;
                vtx(e, sv, a, o + f0 * s0 + g0 * sg, 0.5f, 255, 0.006); vtx(e, sv, b, o + f1 * s0 + g0 * sg, 0.5f, 255, 0.006);
                vtx(e, sv, c, o + f1 * s0 + g1 * sg, 0.5f, 255, 0.006); vtx(e, sv, d, o + f0 * s0 + g1 * sg, 0.5f, 255, 0.006);
            }
        imm.draw(sl);
    }
}
