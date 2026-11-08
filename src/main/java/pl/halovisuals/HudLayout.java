package pl.halovisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import pl.halovisuals.gui.Draw;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/** Shared scale + position + drag-with-mouse (while chat is open) logic for movable HUD elements. */
public final class HudLayout {
    private static Map<String, int[]> cur = new LinkedHashMap<>();
    private static String dragging;
    private static double ddx, ddy;
    private static boolean lmbWas;

    private HudLayout() {}

    public static boolean chat() { return MinecraftClient.getInstance().currentScreen instanceof ChatScreen; }

    /** Call once at the start of every HUD frame. */
    public static void frame(int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Map<String, int[]> old = cur;
        cur = new LinkedHashMap<>();
        if (!chat()) {
            if (dragging != null) ModuleManager.save();
            dragging = null; lmbWas = false;
            return;
        }
        double mx = mc.mouse.getX() * w / (double) mc.getWindow().getWidth();
        double my = mc.mouse.getY() * h / (double) mc.getWindow().getHeight();
        boolean lmb = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (lmb && !lmbWas) {
            List<Map.Entry<String, int[]>> es = new ArrayList<>(old.entrySet());
            for (int i = es.size() - 1; i >= 0; i--) {
                int[] r = es.get(i).getValue();
                if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) { dragging = es.get(i).getKey(); ddx = mx - r[0]; ddy = my - r[1]; break; }
            }
        }
        if (!lmb) { if (dragging != null) ModuleManager.save(); dragging = null; }
        if (dragging != null) {
            Module m = ModuleManager.get(dragging);
            if (m != null) {
                m.s("X").value = Math.max(0, Math.min(1, (mx - ddx) / w));
                m.s("Y").value = Math.max(0, Math.min(1, (my - ddy) / h));
            }
        }
        lmbWas = lmb;
    }

    /** @return {x, y, scaledWidth, scaledHeight} of the element (also registers it for dragging). */
    public static int[] place(Module m, int w, int h, int pw, int ph) {
        float sc = (float) m.num("Scale");
        int rw = (int) (pw * sc), rh = (int) (ph * sc);
        int x = Math.max(0, Math.min(Math.max(0, w - rw), (int) (m.num("X") * w)));
        int y = Math.max(0, Math.min(Math.max(0, h - rh), (int) (m.num("Y") * h)));
        int[] r = {x, y, rw, rh};
        cur.put(m.name, r);
        return r;
    }

    public static void hint(DrawContext g, String label, int[] r) {
        if (!chat()) return;
        Draw.text(g, "Drag to move: " + label, r[0], Math.max(2, r[1] - 11), Draw.a(Draw.WHITE, 170));
    }
}
