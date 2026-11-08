package pl.halovisuals;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Wings 2.0: a bone-driven (shoulder -> elbow -> wrist -> tip) wing carrying four layers of real feathers
 * (scapulars, coverts, secondaries, primaries). Each feather has a quill and a leaf-shaped vane with a
 * white -> gold -> amber gradient, soft edges and an optional glow pass. Flap is phase-delayed along the span.
 */
public final class Wings {
    private static final int K = 6; // segments per feather

    private static final int[][][] PALETTES = {
            {{255, 250, 235}, {255, 214, 110}, {255, 150, 40}},   // Golden
            {{255, 255, 255}, {226, 236, 255}, {170, 196, 255}},  // Pearl
            {{255, 242, 236}, {255, 192, 172}, {230, 112, 122}},  // Rose gold
    };

    private Wings() {}

    /** Layer description: count, arm range, direction angle range (deg, negative = downward), length range, max width, back offset. */
    private record Layer(int n, double s0, double s1, double a0, double a1, double l0, double l1, double wid, double back, float shade) {}

    private static final Layer[] LAYERS = {
            new Layer(5, 0.00, 0.30, -100, -115, 0.72, 0.50, 0.130, 0.020, 0.92f),   // scapulars
            new Layer(9, 0.60, 1.00, -6, -66, 1.00, 0.95, 0.105, 0.000, 1.00f),      // primaries (outer, longest)
            new Layer(8, 0.22, 0.62, -84, -70, 0.98, 0.84, 0.115, 0.012, 0.97f),     // secondaries
            new Layer(11, 0.06, 0.92, -78, -58, 0.40, 0.30, 0.075, 0.028, 1.06f),    // coverts (top layer)
    };

    private static double[] bez(double s) {
        // cubic Bezier arm curve in (lateral, up)
        double[][] p = {{0.06, 0.0}, {0.40, 0.80}, {1.05, 0.98}, {1.62, 0.56}};
        double u = 1 - s;
        return new double[]{u * u * u * p[0][0] + 3 * u * u * s * p[1][0] + 3 * u * s * s * p[2][0] + s * s * s * p[3][0],
                u * u * u * p[0][1] + 3 * u * u * s * p[1][1] + 3 * u * s * s * p[2][1] + s * s * s * p[3][1]};
    }

    private static int col(int[][] pal, float t, int alpha, float shade) {
        int[] a = t < 0.5f ? pal[0] : pal[1], b = t < 0.5f ? pal[1] : pal[2];
        float k = t < 0.5f ? t * 2 : (t - 0.5f) * 2;
        int r = (int) Math.min(255, (a[0] + (b[0] - a[0]) * k) * shade);
        int g = (int) Math.min(255, (a[1] + (b[1] - a[1]) * k) * shade);
        int bl = (int) Math.min(255, (a[2] + (b[2] - a[2]) * k) * shade);
        return (alpha << 24) | (r << 16) | (g << 8) | bl;
    }

    /**
     * @param bx,by,bz shoulder-centre (world); yaw body yaw (radians); size scale; flap speed multiplier;
     *                 t time (s); palette 0..2; alpha 0..1; fold 0..1 (sneaking/gliding folds the wings back).
     */
    public static void render(MatrixStack.Entry e, VertexConsumer q, double bx, double by, double bz, double yaw,
                              double size, double flapSpeed, double t, int palette, boolean glow, float alpha, double fold) {
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), lx = Math.cos(yaw), lz = Math.sin(yaw);
        double[] B = {-fx, 0, -fz};
        int[][] pal = PALETTES[Math.max(0, Math.min(2, palette))];
        double omega = 2.6 * flapSpeed;

        for (int side = -1; side <= 1; side += 2) {
            for (Layer L : LAYERS) {
                for (int i = 0; i < L.n(); i++) {
                    double f = L.n() == 1 ? 0 : i / (double) (L.n() - 1);
                    double s = L.s0() + (L.s1() - L.s0()) * f;
                    double[] root = bez(s);
                    double ang = Math.toRadians(L.a0() + (L.a1() - L.a0()) * f);
                    double len = (L.l0() + (L.l1() - L.l0()) * f) * (0.9 + 0.1 * Math.sin(f * Math.PI)) * size;
                    double wid = L.wid() * size * (0.85 + 0.3 * Math.sin(f * Math.PI));

                    // phase-delayed flap about the forward axis (tip lags behind the shoulder)
                    double th = 0.18 - fold * 0.75 + (1 - 0.7 * fold) * 0.46 * Math.sin(omega * t - 0.9 * s);
                    double ct = Math.cos(th), st = Math.sin(th);
                    double rl = root[0] * size, ru = root[1] * size;
                    double dl = Math.cos(ang), du = Math.sin(ang);
                    double rLat = rl * ct - ru * st, rUp = rl * st + ru * ct;
                    double dLat = dl * ct - du * st, dUp = dl * st + du * ct;

                    double backOff = L.back() + 0.10 * s * size;
                    double sx = bx + lx * side * rLat + B[0] * backOff, sy = by + rUp, sz = bz + lz * side * rLat + B[2] * backOff;
                    // direction (lat,up) + small sweep to the back
                    double dx = lx * side * dLat + B[0] * 0.14, dy = dUp, dz = lz * side * dLat + B[2] * 0.14;
                    double dn = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= dn; dy /= dn; dz /= dn;
                    // width axis: perpendicular to the feather, in the plane facing the back
                    double wx = dy * B[2] - dz * B[1], wy = dz * B[0] - dx * B[2], wz = dx * B[1] - dy * B[0];
                    double wn = Math.sqrt(wx * wx + wy * wy + wz * wz);
                    if (wn < 1e-6) continue;
                    wx /= wn; wy /= wn; wz /= wn;

                    feather(e, q, sx, sy, sz, dx, dy, dz, wx, wy, wz, len, wid, pal, L.shade(), alpha, glow);
                }
            }
        }
    }

    private static double prof(double u) {
        // leaf-shaped vane: slim near the quill, widest at ~42%, rounded tip
        if (u < 0.42) return 0.22 + 0.78 * Math.pow(u / 0.42, 0.8);
        double k = (u - 0.42) / 0.58;
        return Math.sqrt(Math.max(0, 1 - k * k));
    }

    private static void feather(MatrixStack.Entry e, VertexConsumer q, double sx, double sy, double sz, double dx, double dy, double dz,
                                double wx, double wy, double wz, double len, double wid, int[][] pal, float shade, float alpha, boolean glow) {
        double[][] c = new double[K + 1][3];
        for (int k = 0; k <= K; k++) {
            double u = k / (double) K, droop = 0.10 * len * u * u;
            c[k][0] = sx + dx * len * u;
            c[k][1] = sy + dy * len * u - droop;
            c[k][2] = sz + dz * len * u;
        }
        for (int k = 0; k < K; k++) {
            double u0 = k / (double) K, u1 = (k + 1) / (double) K;
            double w0 = wid * prof(u0), w1 = wid * prof(u1);
            int cen0 = col(pal, (float) u0, (int) (235 * alpha), shade), cen1 = col(pal, (float) u1, (int) (225 * alpha), shade);
            int edg0 = col(pal, (float) u0, (int) (140 * alpha), shade * 0.82f), edg1 = col(pal, (float) u1, (int) (110 * alpha), shade * 0.82f);
            // left half
            Gfx.quad(e, q, c[k][0], c[k][1], c[k][2], cen0, c[k][0] + wx * w0, c[k][1] + wy * w0, c[k][2] + wz * w0, edg0,
                    c[k + 1][0] + wx * w1, c[k + 1][1] + wy * w1, c[k + 1][2] + wz * w1, edg1, c[k + 1][0], c[k + 1][1], c[k + 1][2], cen1);
            // right half
            Gfx.quad(e, q, c[k][0], c[k][1], c[k][2], cen0, c[k][0] - wx * w0, c[k][1] - wy * w0, c[k][2] - wz * w0, edg0,
                    c[k + 1][0] - wx * w1, c[k + 1][1] - wy * w1, c[k + 1][2] - wz * w1, edg1, c[k + 1][0], c[k + 1][1], c[k + 1][2], cen1);
            // quill (thin bright shaft)
            int qc = (int) (255 * alpha) << 24 | 0xFFFFF0;
            double qw = 0.006 * (1 - u0 * 0.6);
            Gfx.quad(e, q, c[k][0] - wx * qw, c[k][1] - wy * qw, c[k][2] - wz * qw, qc, c[k][0] + wx * qw, c[k][1] + wy * qw, c[k][2] + wz * qw, qc,
                    c[k + 1][0] + wx * qw, c[k + 1][1] + wy * qw, c[k + 1][2] + wz * qw, qc, c[k + 1][0] - wx * qw, c[k + 1][1] - wy * qw, c[k + 1][2] - wz * qw, qc);
            if (glow) {
                int g0 = col(pal, (float) u0, (int) (34 * alpha), 1f), g1 = col(pal, (float) u1, (int) (28 * alpha), 1f);
                int ge = g0 & 0xFFFFFF, ge1 = g1 & 0xFFFFFF;
                Gfx.quad(e, q, c[k][0], c[k][1], c[k][2], g0, c[k][0] + wx * w0 * 2.1, c[k][1] + wy * w0 * 2.1, c[k][2] + wz * w0 * 2.1, ge,
                        c[k + 1][0] + wx * w1 * 2.1, c[k + 1][1] + wy * w1 * 2.1, c[k + 1][2] + wz * w1 * 2.1, ge1, c[k + 1][0], c[k + 1][1], c[k + 1][2], g1);
                Gfx.quad(e, q, c[k][0], c[k][1], c[k][2], g0, c[k][0] - wx * w0 * 2.1, c[k][1] - wy * w0 * 2.1, c[k][2] - wz * w0 * 2.1, ge,
                        c[k + 1][0] - wx * w1 * 2.1, c[k + 1][1] - wy * w1 * 2.1, c[k + 1][2] - wz * w1 * 2.1, ge1, c[k + 1][0], c[k + 1][1], c[k + 1][2], g1);
            }
        }
    }
}
