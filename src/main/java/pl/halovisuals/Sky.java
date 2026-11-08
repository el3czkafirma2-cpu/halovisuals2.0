package pl.halovisuals;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Custom Sky: procedural gradient dome + nebula clouds + twinkling stars (no textures needed), slowly rotating. */
public final class Sky {
    private static final int MAX_STARS = 3200, BLOBS = 120;
    private static final int[][] ZENITH = {{3, 2, 16}, {70, 58, 150}, {26, 16, 72}, {0, 0, 4}};
    private static final int[][] HORIZON = {{26, 12, 58}, {255, 214, 140}, {255, 128, 150}, {4, 4, 14}};
    private static final int[][][] NEB = {
            {{160, 64, 230}, {60, 110, 255}, {255, 90, 190}, {70, 230, 255}},
            {{255, 214, 120}, {255, 255, 255}, {255, 170, 70}, {255, 235, 170}},
            {{255, 120, 170}, {200, 90, 255}, {255, 180, 120}, {120, 120, 255}},
            {{80, 80, 160}, {120, 120, 200}, {60, 60, 120}, {160, 160, 220}}};

    private static double[][] sdir, bdir;
    private static float[] ssize, sphase, sbright, bsize, balpha;
    private static int[] scol, bcol;
    private static boolean built;

    private Sky() {}

    /** Horizon colour of the style (used by the sky-colour mixin so the fog matches the dome). */
    public static int horizonRgb(int style) {
        int[] h = HORIZON[Math.max(0, Math.min(3, style))];
        return (h[0] << 16) | (h[1] << 8) | h[2];
    }

    private static void build() {
        Random r = new Random(1337);
        sdir = new double[MAX_STARS][3]; ssize = new float[MAX_STARS]; sphase = new float[MAX_STARS]; sbright = new float[MAX_STARS]; scol = new int[MAX_STARS];
        for (int i = 0; i < MAX_STARS; i++) {
            double z = r.nextDouble() * 2 - 1, ph = r.nextDouble() * Math.PI * 2, rr = Math.sqrt(1 - z * z);
            sdir[i] = new double[]{rr * Math.cos(ph), z, rr * Math.sin(ph)};
            ssize[i] = (float) (0.0022 + Math.pow(r.nextDouble(), 7) * 0.0075);
            sphase[i] = r.nextFloat() * 20f;
            sbright[i] = 0.45f + 0.55f * r.nextFloat();
            float c = r.nextFloat();
            scol[i] = c < 0.7f ? 0xFFFFFF : c < 0.85f ? 0xBFD8FF : 0xFFE2B0;
        }
        bdir = new double[BLOBS][3]; bsize = new float[BLOBS]; balpha = new float[BLOBS]; bcol = new int[BLOBS];
        double[] n = {0.35, 0.8, 0.4};
        double nl = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        for (int k = 0; k < 3; k++) n[k] /= nl;
        double[][] bs = Gfx.basis(n);
        for (int i = 0; i < BLOBS; i++) {
            double[] d;
            if (i < 90) { // milky-way band
                double a = r.nextDouble() * Math.PI * 2, off = r.nextGaussian() * 0.22;
                d = new double[]{Math.cos(a) * bs[0][0] + Math.sin(a) * bs[1][0] + n[0] * off, Math.cos(a) * bs[0][1] + Math.sin(a) * bs[1][1] + n[1] * off,
                        Math.cos(a) * bs[0][2] + Math.sin(a) * bs[1][2] + n[2] * off};
            } else {
                double z = r.nextDouble() * 2 - 1, ph = r.nextDouble() * Math.PI * 2, rr = Math.sqrt(1 - z * z);
                d = new double[]{rr * Math.cos(ph), z, rr * Math.sin(ph)};
            }
            double l = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            bdir[i] = new double[]{d[0] / l, d[1] / l, d[2] / l};
            bsize[i] = 0.12f + r.nextFloat() * 0.26f;
            balpha[i] = 0.06f + r.nextFloat() * 0.15f;
            bcol[i] = r.nextInt(4);
        }
        built = true;
    }

    private static int rgba(int[] c, float a) { return ((int) (255 * Math.max(0, Math.min(1, a))) << 24) | (c[0] << 16) | (c[1] << 8) | c[2]; }

    private static int mix(int[] a, int[] b, float t, float alpha) {
        t = Math.max(0, Math.min(1, t));
        return rgba(new int[]{(int) (a[0] + (b[0] - a[0]) * t), (int) (a[1] + (b[1] - a[1]) * t), (int) (a[2] + (b[2] - a[2]) * t)}, alpha);
    }

    public static void render(MatrixStack.Entry e, VertexConsumer q, Vec3d cam, double t, Module m, double R) {
        if (!built) build();
        int style = Math.max(0, Math.min(3, m.mode("Style")));
        double starsK = m.num("Stars"), nebK = m.num("Nebula"), rot = t * 0.004 * m.num("Rotation");
        double cr = Math.cos(rot), sr = Math.sin(rot);
        int[] zen = ZENITH[style], hor = HORIZON[style], bot = {hor[0] / 3, hor[1] / 3, hor[2] / 3};
        int[][] neb = NEB[style];

        // gradient dome
        final int LAT = 14, LON = 28;
        for (int i = 0; i < LAT; i++) {
            double p0 = -Math.PI / 2 + Math.PI * i / LAT, p1 = -Math.PI / 2 + Math.PI * (i + 1) / LAT;
            double s0 = Math.sin(p0), s1 = Math.sin(p1), c0 = Math.cos(p0), c1 = Math.cos(p1);
            int k0 = s0 >= 0 ? mix(hor, zen, (float) Math.pow(s0, 0.55), 1f) : mix(hor, bot, (float) -s0, 1f);
            int k1 = s1 >= 0 ? mix(hor, zen, (float) Math.pow(s1, 0.55), 1f) : mix(hor, bot, (float) -s1, 1f);
            for (int j = 0; j < LON; j++) {
                double l0 = Math.PI * 2 * j / LON, l1 = Math.PI * 2 * (j + 1) / LON;
                Gfx.quad(e, q, cam.x + R * c0 * Math.cos(l0), cam.y + R * s0, cam.z + R * c0 * Math.sin(l0), k0,
                        cam.x + R * c0 * Math.cos(l1), cam.y + R * s0, cam.z + R * c0 * Math.sin(l1), k0,
                        cam.x + R * c1 * Math.cos(l1), cam.y + R * s1, cam.z + R * c1 * Math.sin(l1), k1,
                        cam.x + R * c1 * Math.cos(l0), cam.y + R * s1, cam.z + R * c1 * Math.sin(l0), k1);
            }
        }

        // nebula clouds (soft discs tangent to the sky sphere)
        if (nebK > 0.01) {
            for (int i = 0; i < BLOBS; i++) {
                double dx = bdir[i][0] * cr - bdir[i][2] * sr, dy = bdir[i][1], dz = bdir[i][0] * sr + bdir[i][2] * cr;
                double[][] b = Gfx.basis(new double[]{dx, dy, dz});
                double rad = bsize[i] * R * 0.98, cx = cam.x + dx * R * 0.99, cy = cam.y + dy * R * 0.99, cz = cam.z + dz * R * 0.99;
                int cc = rgba(neb[bcol[i]], (float) (balpha[i] * nebK)), ce = cc & 0xFFFFFF;
                for (int s = 0; s < 12; s++) {
                    double a0 = s * Math.PI / 6, a1 = (s + 1) * Math.PI / 6;
                    Gfx.tri(e, q, cx, cy, cz, cc,
                            cx + (b[0][0] * Math.cos(a0) + b[1][0] * Math.sin(a0)) * rad, cy + (b[0][1] * Math.cos(a0) + b[1][1] * Math.sin(a0)) * rad, cz + (b[0][2] * Math.cos(a0) + b[1][2] * Math.sin(a0)) * rad, ce,
                            cx + (b[0][0] * Math.cos(a1) + b[1][0] * Math.sin(a1)) * rad, cy + (b[0][1] * Math.cos(a1) + b[1][1] * Math.sin(a1)) * rad, cz + (b[0][2] * Math.cos(a1) + b[1][2] * Math.sin(a1)) * rad, ce);
                }
            }
        }

        // stars
        int n = (int) (MAX_STARS * starsK);
        for (int i = 0; i < n; i++) {
            double dx = sdir[i][0] * cr - sdir[i][2] * sr, dy = sdir[i][1], dz = sdir[i][0] * sr + sdir[i][2] * cr;
            double t1x, t1y, t1z;
            if (Math.abs(dy) > 0.999) { t1x = 1; t1y = 0; t1z = 0; }
            else { double l = Math.sqrt(dx * dx + dz * dz); t1x = dz / l; t1y = 0; t1z = -dx / l; }
            double t2x = dy * t1z - dz * t1y, t2y = dz * t1x - dx * t1z, t2z = dx * t1y - dy * t1x;
            double s = ssize[i] * R, cx = cam.x + dx * R * 0.985, cy = cam.y + dy * R * 0.985, cz = cam.z + dz * R * 0.985;
            float tw = 0.55f + 0.45f * (float) Math.sin(t * (1.4 + sphase[i] % 2.2) + sphase[i]);
            float al = sbright[i] * tw;
            int col = (((int) (255 * Math.min(1f, al))) << 24) | scol[i];
            Gfx.quad(e, q, cx + t1x * s, cy + t1y * s, cz + t1z * s, col, cx + t2x * s, cy + t2y * s, cz + t2z * s, col,
                    cx - t1x * s, cy - t1y * s, cz - t1z * s, col, cx - t2x * s, cy - t2y * s, cz - t2z * s, col);
            if (ssize[i] > 0.0058f) { // bright stars: glow + 4-point flare
                int g0 = (((int) (90 * al)) << 24) | scol[i], g1 = scol[i];
                double gs = s * 3.2;
                Gfx.quad(e, q, cx + t1x * gs, cy + t1y * gs, cz + t1z * gs, g1, cx, cy, cz, g0, cx + t2x * gs, cy + t2y * gs, cz + t2z * gs, g1, cx + (t1x + t2x) * gs, cy + (t1y + t2y) * gs, cz + (t1z + t2z) * gs, g1);
                Gfx.tri(e, q, cx + t1x * gs * 1.8, cy + t1y * gs * 1.8, cz + t1z * gs * 1.8, g1, cx, cy, cz, g0, cx - t1x * gs * 1.8, cy - t1y * gs * 1.8, cz - t1z * gs * 1.8, g1);
                Gfx.tri(e, q, cx + t2x * gs * 1.8, cy + t2y * gs * 1.8, cz + t2z * gs * 1.8, g1, cx, cy, cz, g0, cx - t2x * gs * 1.8, cy - t2y * gs * 1.8, cz - t2z * gs * 1.8, g1);
            }
        }
    }
}
