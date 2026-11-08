package pl.halovisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import pl.halovisuals.gui.Draw;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Kill effects: Lightning strike, Angel Abduction (ghost + beam + wings) and a stylised Ragdoll (physics limbs). */
public final class KillFx {
    private static final class Bolt { double x, y, z; long t, seed; }
    private static final class Ab { double x, y, z, w, h; long t; }
    private static final class Limb {
        double x, y, z, vx, vy, vz, yaw, pitch, roll, wy, wp, wr, hx, hy, hz;
        int color;
    }
    private static final class Rag { long t; final List<Limb> limbs = new ArrayList<>(); }

    private static final List<Bolt> bolts = new ArrayList<>();
    private static final List<Ab> abds = new ArrayList<>();
    private static final List<Rag> rags = new ArrayList<>();
    private static final Random rnd = new Random();

    private KillFx() {}

    public static void clear() { bolts.clear(); abds.clear(); rags.clear(); }
    public static boolean active() { return !bolts.isEmpty() || !abds.isEmpty() || !rags.isEmpty(); }

    public static void lightning(double x, double y, double z) {
        Bolt b = new Bolt(); b.x = x; b.y = y; b.z = z; b.t = System.currentTimeMillis(); b.seed = rnd.nextLong();
        bolts.add(b);
    }

    public static void abduction(Entity e) {
        Ab a = new Ab(); a.x = e.getX(); a.y = e.getY(); a.z = e.getZ(); a.w = Math.max(0.4, e.getWidth()); a.h = Math.max(0.6, e.getHeight()); a.t = System.currentTimeMillis();
        abds.add(a);
    }

    public static void ragdoll(LivingEntity e, Vec3d push) {
        Rag r = new Rag();
        r.t = System.currentTimeMillis();
        double h = Math.max(0.6, e.getHeight()), w = Math.max(0.4, e.getWidth());
        double[][] defs = { // dx, dy(frac), hx, hy(frac), hz
                {-0.2, 0.19, 0.17, 0.19, 0.17}, {0.2, 0.19, 0.17, 0.19, 0.17}, {0, 0.56, 0.36, 0.19, 0.2},
                {-0.58, 0.56, 0.12, 0.19, 0.14}, {0.58, 0.56, 0.12, 0.19, 0.14}, {0, 0.88, 0.28, 0.12, 0.28}};
        int[] cols = {0xFFFFE9A8, 0xFFFFE9A8, 0xFFFFD84D, 0xFFFFF4D0, 0xFFFFF4D0, 0xFFFFFFFF};
        for (int i = 0; i < defs.length; i++) {
            Limb l = new Limb();
            l.x = e.getX() + defs[i][0] * w; l.y = e.getY() + defs[i][1] * h; l.z = e.getZ();
            l.hx = defs[i][2] * w; l.hy = defs[i][3] * h; l.hz = defs[i][4] * w;
            double sp = 0.30 + rnd.nextDouble() * 0.15;
            l.vx = push.x * sp + (rnd.nextDouble() - .5) * 0.12; l.vz = push.z * sp + (rnd.nextDouble() - .5) * 0.12; l.vy = 0.22 + rnd.nextDouble() * 0.2;
            l.wy = (rnd.nextDouble() - .5) * 0.5; l.wp = (rnd.nextDouble() - .5) * 0.5; l.wr = (rnd.nextDouble() - .5) * 0.5;
            l.color = cols[i];
            r.limbs.add(l);
        }
        rags.add(r);
    }

    private static double ground(double x, double y, double z) {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (int dy = 0; dy < 24; dy++) {
            BlockPos p = BlockPos.ofFloored(x, y - dy - 0.01, z);
            if (!mc.world.getBlockState(p).isAir()) return p.getY() + 1;
        }
        return y - 24;
    }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        long now = System.currentTimeMillis();
        bolts.removeIf(b -> now - b.t > 750);
        abds.removeIf(a -> now - a.t > 2600);
        rags.removeIf(r -> now - r.t > 3200);
        for (Ab a : abds) {
            double p = (now - a.t) / 2600.0, lift = p * p * 16;
            Particles.spawn(Particles.ANGEL_DUST, a.x, a.y + lift + a.h * 0.5, a.z, a.w + 0.6, a.h, 0.05, 3);
            Particles.spawn(Particles.SPARKS, a.x, a.y + lift * 0.5 + 1, a.z, 0.5, 4, 0.02, 1);
        }
        for (Rag r : rags)
            for (Limb l : r.limbs) {
                l.vy -= 0.06; l.vx *= 0.985; l.vz *= 0.985;
                l.x += l.vx; l.y += l.vy; l.z += l.vz;
                l.yaw += l.wy; l.pitch += l.wp; l.roll += l.wr;
                double g = ground(l.x, l.y + l.hy, l.z);
                double lowest = Math.min(l.hx, Math.min(l.hy, l.hz));
                if (l.y - lowest < g) {
                    l.y = g + lowest;
                    if (Math.abs(l.vy) > 0.05) l.vy *= -0.35; else l.vy = 0;
                    l.vx *= 0.7; l.vz *= 0.7; l.wy *= 0.65; l.wp *= 0.65; l.wr *= 0.65;
                }
            }
    }

    private static void layers(MatrixStack.Entry e, VertexConsumer q, double[][] pts, double[] w, int[] cols, double rx, double ry, double rz) {
        Gfx.strip(e, q, pts, w, cols, rx, ry, rz, 1.0);
        int[] c2 = new int[cols.length], c3 = new int[cols.length];
        for (int i = 0; i < cols.length; i++) { c2[i] = Gfx.alpha(cols[i], 0.4f); c3[i] = Gfx.alpha(cols[i], 0.14f); }
        Gfx.strip(e, q, pts, w, c2, rx, ry, rz, 3.5);
        Gfx.strip(e, q, pts, w, c3, rx, ry, rz, 8.0);
    }

    public static void render(MatrixStack.Entry e, VertexConsumer q, float td, double t, long now, double rx, double ry, double rz) {
        // ---- lightning
        for (Bolt b : bolts) {
            long age = now - b.t;
            float f = age < 80 ? 1f : age < 140 ? 0.25f : age < 220 ? 1f : age < 320 ? 0.55f : Math.max(0f, 1f - (age - 320) / 400f);
            Random r = new Random(b.seed);
            int N = 22;
            double[][] pts = new double[N + 1][3];
            double ox = 0, oz = 0;
            double[][] raw = new double[N + 1][2];
            for (int i = 0; i <= N; i++) { raw[i][0] = ox; raw[i][1] = oz; ox += (r.nextDouble() - .5) * 2.0; oz += (r.nextDouble() - .5) * 2.0; }
            for (int i = 0; i <= N; i++) {
                double fr = i / (double) N;
                pts[i][0] = b.x + raw[i][0] - raw[N][0] * fr; pts[i][1] = b.y + 55 * (1 - fr); pts[i][2] = b.z + raw[i][1] - raw[N][1] * fr;
            }
            double[] w = new double[N + 1];
            int[] cols = new int[N + 1];
            for (int i = 0; i <= N; i++) { w[i] = 0.07; cols[i] = Draw.a(Draw.lerp(Draw.WHITE, Draw.YELLOW, i / (float) N * 0.35f), (int) (255 * f)); }
            layers(e, q, pts, w, cols, rx, ry, rz);
            for (int br = 0; br < 4; br++) {
                int s = 4 + r.nextInt(N - 8);
                double[][] bp = new double[8][3];
                double[] bw = new double[8];
                int[] bc = new int[8];
                double dx = (r.nextDouble() - .5) * 2.2, dz = (r.nextDouble() - .5) * 2.2, cx = pts[s][0], cy = pts[s][1], cz = pts[s][2];
                for (int i = 0; i < 8; i++) {
                    bp[i][0] = cx; bp[i][1] = cy; bp[i][2] = cz;
                    cx += dx + (r.nextDouble() - .5) * 1.2; cz += dz + (r.nextDouble() - .5) * 1.2; cy -= 1.6 + r.nextDouble();
                    bw[i] = 0.035; bc[i] = Draw.a(Draw.lerp(Draw.WHITE, Draw.YELLOW, 0.4f), (int) (210 * f * (1 - i / 8f)));
                }
                layers(e, q, bp, bw, bc, rx, ry, rz);
            }
            float pr = Math.min(1f, age / 700f);
            Gfx.annulus(e, q, b.x, b.y + 0.05, b.z, 0.4 + pr * 3.4, 0.12, 48, tt -> Draw.a(Draw.lerp(Draw.WHITE, Draw.ORANGE, pr), (int) (230 * (1 - pr))));
        }
        // ---- abduction
        for (Ab a : abds) {
            double p = (now - a.t) / 2600.0, lift = p * p * 16;
            double env = Math.pow(Math.sin(Math.min(1.0, p) * Math.PI), 0.6);
            int N = 10;
            double[][] pts = new double[N + 1][3];
            double[] w = new double[N + 1];
            int[] cols = new int[N + 1];
            for (int i = 0; i <= N; i++) {
                double fr = i / (double) N;
                pts[i][0] = a.x; pts[i][1] = a.y - 0.3 + fr * 36; pts[i][2] = a.z;
                w[i] = 0.5 + 0.2 * p;
                cols[i] = Draw.a(Draw.lerp(Draw.WHITE, Draw.YELLOW, (float) fr), (int) (150 * Math.pow(1 - fr, 1.3) * env));
            }
            layers(e, q, pts, w, cols, rx, ry, rz);
            float al = (float) (Math.pow(1 - p, 1.2) * 0.6 * Math.min(1, p * 8 + 0.4));
            double yy = a.y + lift, spin = t * 1.3, h = a.h, wd = a.w;
            int c1 = Draw.a(0xFFFFF0C0, (int) (255 * al)), c2 = Draw.a(0xFFFFFFFF, (int) (255 * al));
            Gfx.obox(e, q, a.x - 0.2 * wd * Math.cos(spin), yy + 0.19 * h, a.z - 0.2 * wd * Math.sin(spin), 0.17 * wd, 0.19 * h, 0.17 * wd, spin, 0, 0, c1);
            Gfx.obox(e, q, a.x + 0.2 * wd * Math.cos(spin), yy + 0.19 * h, a.z + 0.2 * wd * Math.sin(spin), 0.17 * wd, 0.19 * h, 0.17 * wd, spin, 0, 0, c1);
            Gfx.obox(e, q, a.x, yy + 0.56 * h, a.z, 0.36 * wd, 0.19 * h, 0.2 * wd, spin, 0, 0, c2);
            Gfx.obox(e, q, a.x - 0.58 * wd * Math.cos(spin), yy + 0.56 * h, a.z - 0.58 * wd * Math.sin(spin), 0.12 * wd, 0.19 * h, 0.14 * wd, spin, 0, 0, c1);
            Gfx.obox(e, q, a.x + 0.58 * wd * Math.cos(spin), yy + 0.56 * h, a.z + 0.58 * wd * Math.sin(spin), 0.12 * wd, 0.19 * h, 0.14 * wd, spin, 0, 0, c1);
            Gfx.obox(e, q, a.x, yy + 0.88 * h, a.z, 0.28 * wd, 0.12 * h, 0.28 * wd, spin, 0, 0, c2);
            Wings.render(e, q, a.x, yy + 0.7 * h, a.z, spin, 0.55 * h / 1.8, 1.6, t, 0, true, Math.min(1f, al * 1.6f), 0);
        }
        // ---- ragdoll
        for (Rag r : rags) {
            long age = now - r.t;
            float al = age < 2300 ? 0.88f : Math.max(0f, 0.88f * (1f - (age - 2300) / 900f));
            for (Limb l : r.limbs) {
                // smooth between ticks
                double x = l.x + l.vx * td, y = l.y + l.vy * td, z = l.z + l.vz * td;
                Gfx.obox(e, q, x, y, z, l.hx, l.hy, l.hz, l.yaw + l.wy * td, l.pitch + l.wp * td, l.roll + l.wr * td, Draw.a(l.color, (int) (255 * al)));
                Gfx.obox(e, q, x, y, z, l.hx * 1.35, l.hy * 1.35, l.hz * 1.35, l.yaw, l.pitch, l.roll, Draw.a(Draw.YELLOW, (int) (40 * al)));
            }
        }
    }
}
