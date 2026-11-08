package pl.halovisuals.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import pl.halovisuals.ConfigStore;
import pl.halovisuals.Features;
import pl.halovisuals.Module;
import pl.halovisuals.ModuleManager;
import pl.halovisuals.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClickGui extends Screen {
    private record Hit(int x, int y, int w, int h, Module m, Setting s) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
    private record Act(int x, int y, int w, int h, Runnable r) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Hit> hits = new ArrayList<>();
    private final List<Act> acts = new ArrayList<>();
    private Setting dragging, listening;
    private int dragX, dragW;
    private String filter = "";
    private boolean filterFocused;
    private float open = 0f, scale = 1f;
    private int scroll = 0;
    private long animStart = System.currentTimeMillis(), closeStart;
    private boolean closing, openPlayed;
    private Object hoverKey;

    // Configs tab
    private boolean configOpen, cfgFocused;
    private String cfgText = "", status = "", lastId = "", lastShare = "";
    private long statusAt;
    private List<String> saved = new ArrayList<>();
    private int[] cfgRect = {0, 0, 0, 0};

    public ClickGui() { super(Text.literal("Halo Visuals")); }

    @Override public boolean shouldPause() { return false; }

    private static int rowH(Setting s) { return s.type == Setting.Type.HIDDEN ? 0 : s.type == Setting.Type.SLIDER ? 24 : 18; }
    private static int settingsH(Module m) { int h = 4; for (Setting s : m.settings) h += rowH(s); return h; }
    private void say(String s) { status = s; statusAt = System.currentTimeMillis(); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta); // vanilla background + blur
        long nowMs = System.currentTimeMillis();
        float pr = closing ? 1f - (nowMs - closeStart) / 200f : (nowMs - animStart) / 280f;
        pr = Math.max(0f, Math.min(1f, pr));
        open = pr < 0.5f ? 4 * pr * pr * pr : 1f - (float) Math.pow(-2 * pr + 2, 3) / 2f; // ease-in-out cubic
        if (closing && pr <= 0f) { closing = false; Draw.alphaMul = 1f; super.close(); return; }
        if (!openPlayed) { openPlayed = true; Features.uiSound(1.3f, 0.5f); }
        Draw.alphaMul = open;

        ctx.fillGradient(0, 0, width, height / 2, ((int) (0x38 * open) << 24) | 0xFFD84D, 0x00FFD84D);
        ctx.fillGradient(0, height / 2, width, height, 0x00FF8A1F, ((int) (0x30 * open) << 24) | 0xFF8A1F);

        // Auto-fit: layout designed for ~1000x560 logical px, scaled to ANY GUI Scale
        scale = Math.max(0.35f, Math.min(1.6f, Math.min(width / 1000f, height / 560f)));
        float sc = scale;
        mx = (int) (mx / sc);
        my = (int) (my / sc);
        int lw = (int) (width / sc);

        Object hk = null;
        for (Act a : acts) if (a.in(mx, my)) { hk = a; break; }
        if (hk == null) for (Hit hh : hits) if (hh.in(mx, my)) { hk = hh.s() != null ? hh.s() : hh.m() != null ? hh.m() : "search"; break; }
        if (hk != null && !(hk instanceof Act) && hk != hoverKey && open > 0.9f) Features.uiSound(2.0f, 0.18f);
        hoverKey = hk;
        hits.clear();
        acts.clear();

        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, height / 2f, 0f);
        ctx.getMatrices().scale(0.94f + 0.06f * open, 0.94f + 0.06f * open, 1f);
        ctx.getMatrices().translate(-width / 2f, -height / 2f, 0f);
        ctx.getMatrices().scale(sc, sc, 1f);

        // title
        ctx.getMatrices().push();
        ctx.getMatrices().translate(24, 14, 0);
        ctx.getMatrices().scale(2f, 2f, 1f);
        Draw.text(ctx, "Halo", 0, 0, Draw.YELLOW);
        ctx.getMatrices().pop();
        Draw.text(ctx, "VISUALS", 24 + Draw.width("Halo") * 2 + 6, 14, Draw.ORANGE);
        Draw.text(ctx, "v2.0  -  all features free", 24 + Draw.width("Halo") * 2 + 6, 25, Draw.a(Draw.WHITE, 140));

        // top bar: Configs button + filter
        int fx = lw - 24 - 190, fy = 14;
        Draw.panel(ctx, fx, fy, 190, 22, 11);
        String ft = filter.isEmpty() && !filterFocused ? "Type to filter..." : filter + (filterFocused && (nowMs / 500) % 2 == 0 ? "_" : "");
        Draw.text(ctx, ft, fx + 10, fy + 7, filter.isEmpty() ? Draw.GRAY : Draw.WHITE);
        hits.add(new Hit(fx, fy, 190, 22, null, null));

        int bx = fx - 8 - 100;
        boolean bh = mx >= bx && mx < bx + 100 && my >= fy && my < fy + 22;
        Draw.panel(ctx, bx, fy, 100, 22, 11);
        if (configOpen || bh) Draw.rrect(ctx, bx + 2, fy + 2, 96, 18, 9, Draw.a(Draw.YELLOW, configOpen ? 60 : 30));
        Draw.ctext(ctx, configOpen ? "Configs  ^" : "Configs  v", bx + 50, fy + 7, configOpen ? Draw.YELLOW : Draw.WHITE);
        acts.add(new Act(bx, fy, 100, 22, () -> {
            configOpen = !configOpen; cfgFocused = false;
            if (configOpen) saved = ConfigStore.list();
            Features.uiSound(1.6f, 0.4f);
        }));

        String f = filter.toLowerCase(Locale.ROOT);
        int gap = 8, pw = 150;
        int x0 = (lw - (pw * 6 + gap * 5)) / 2;
        int y0 = 48 + scroll + (int) ((1f - open) * -18);

        for (int ci = 0; ci < ModuleManager.CATS.length; ci++) {
            final String cat = ModuleManager.CATS[ci];
            int px = x0 + ci * (pw + gap);
            List<Module> ms = ModuleManager.ALL.stream()
                    .filter(m -> m.category.equals(cat) && (f.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(f))).toList();

            int ph = 30 + 6;
            for (Module m : ms) {
                m.expAnim += ((m.expanded ? 1f : 0f) - m.expAnim) * 0.25f;
                ph += 22 + (int) (settingsH(m) * m.expAnim);
            }
            Draw.panel(ctx, px, y0, pw, ph, 10);

            Draw.rrect(ctx, px + 8, y0 + 9, 12, 12, 6, Draw.a(Draw.YELLOW, 40));
            Draw.rrect(ctx, px + 10, y0 + 11, 8, 8, 4, Draw.accent(0.5f));
            Draw.text(ctx, cat, px + 26, y0 + 11, Draw.WHITE);
            String cnt = String.valueOf(ms.size());
            Draw.text(ctx, cnt, px + pw - 10 - Draw.width(cnt), y0 + 11, Draw.GRAY);
            Draw.gradH(ctx, px + 10, y0 + 28, px + pw - 10, y0 + 29, Draw.a(Draw.YELLOW, (int) (130 * open)), Draw.a(Draw.ORANGE, (int) (130 * open)));

            int y = y0 + 32;
            for (Module m : ms) {
                m.anim += ((m.enabled ? 1f : 0f) - m.anim) * 0.25f;
                boolean hover = mx >= px && mx < px + pw && my >= y && my < y + 22 && !(configOpen && inCfg(mx, my));
                int rx = px + 4, rw = pw - 8;
                if (m.anim > 0.02f) Draw.rrect(ctx, rx, y, rw, 20, 7, Draw.a(Draw.YELLOW, (int) (45 * m.anim)));
                else if (hover) Draw.rrect(ctx, rx, y, rw, 20, 7, Draw.a(Draw.WHITE, 22));
                if (m.anim > 0.02f) Draw.rrect(ctx, px + 6, y + 4, 2, 12, 1, Draw.a(Draw.accent(0.4f), (int) (255 * m.anim)));

                Draw.text(ctx, m.name, px + 12, y + 6, Draw.lerp(Draw.GRAY, Draw.WHITE, m.anim));
                boolean vis = false;
                for (Setting s : m.settings) if (s.type != Setting.Type.HIDDEN) { vis = true; break; }
                if (vis) Draw.text(ctx, m.expanded ? "v" : ">", px + pw - 50, y + 6, Draw.GRAY);

                int tx = px + pw - 34, ty = y + 4;
                Draw.rrect(ctx, tx - 2, ty - 2, 30, 16, 8, Draw.a(Draw.accent(0.5f), (int) (50 * m.anim)));
                Draw.rrectG(ctx, tx, ty, 26, 12, 6, Draw.lerp(Draw.a(Draw.WHITE, 55), Draw.YELLOW, m.anim), Draw.lerp(Draw.a(Draw.WHITE, 55), Draw.ORANGE, m.anim));
                Draw.rrect(ctx, tx + 2 + (int) (14 * m.anim), ty + 2, 8, 8, 4, Draw.WHITE);

                hits.add(new Hit(px, y, pw, 22, m, null));
                y += 22;

                int eh = (int) (settingsH(m) * m.expAnim);
                if (eh > 1) {
                    ctx.enableScissor(px, y, px + pw, y + eh);
                    int sy = y + 2;
                    for (Setting s : m.settings) {
                        if (s.type == Setting.Type.HIDDEN) continue;
                        drawSetting(ctx, px, sy, pw, s);
                        if (m.expAnim > 0.95f) hits.add(new Hit(px, sy, pw, rowH(s), m, s));
                        sy += rowH(s);
                    }
                    ctx.disableScissor();
                }
                y += eh;
            }
        }

        if (configOpen) drawConfigs(ctx, lw, mx, my);

        ctx.getMatrices().pop();
        Draw.alphaMul = 1f;
    }

    private boolean inCfg(int mx, int my) { return mx >= cfgRect[0] && mx < cfgRect[0] + cfgRect[2] && my >= cfgRect[1] && my < cfgRect[1] + cfgRect[3]; }

    private void button(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my, Runnable r) {
        boolean hv = mx >= x && mx < x + w && my >= y && my < y + h;
        Draw.rrect(ctx, x, y, w, h, h / 2, Draw.a(Draw.WHITE, hv ? 50 : 26));
        Draw.rrect(ctx, x + 1, y + 1, w - 2, h - 2, h / 2 - 1, hv ? Draw.a(Draw.YELLOW, 70) : Draw.a(0xFF000000, 60));
        Draw.ctext(ctx, label, x + w / 2, y + (h - 8) / 2, hv ? Draw.YELLOW : Draw.WHITE);
        acts.add(0, new Act(x, y, w, h, r)); // later buttons are on top -> checked first
    }

    private void drawConfigs(DrawContext ctx, int lw, int mx, int my) {
        int pw = 340, px = lw - 24 - pw, py = 44;
        int rows = Math.min(6, saved.size());
        int ph = 176 + (saved.isEmpty() ? 0 : 20 + rows * 22);
        cfgRect = new int[]{px - 4, py - 4, pw + 8, ph + 8};
        Draw.panel(ctx, px, py, pw, ph, 12);
        int x = px + 14, y = py + 12, inner = pw - 28;
        Draw.text(ctx, "Configs", x, y, Draw.YELLOW);
        Draw.text(ctx, "share your settings with a code", x + 50, y, Draw.GRAY);
        y += 20;

        Draw.text(ctx, "ID", x, y + 4, Draw.GRAY);
        Draw.rrect(ctx, x + 40, y, inner - 40 - 56, 16, 8, Draw.a(0xFF000000, 80));
        Draw.text(ctx, lastId.isEmpty() ? "-" : lastId, x + 48, y + 4, Draw.YELLOW);
        button(ctx, x + inner - 50, y, 50, 16, "Copy", mx, my, () -> { if (!lastId.isEmpty()) { client.keyboard.setClipboard(lastId); say("ID copied."); } });
        y += 22;
        Draw.text(ctx, "Share", x, y + 4, Draw.GRAY);
        Draw.rrect(ctx, x + 40, y, inner - 40 - 56, 16, 8, Draw.a(0xFF000000, 80));
        String sh = lastShare.isEmpty() ? "-" : (lastShare.length() > 30 ? lastShare.substring(0, 30) + "..." : lastShare);
        Draw.text(ctx, sh, x + 48, y + 4, Draw.WHITE);
        button(ctx, x + inner - 50, y, 50, 16, "Copy", mx, my, () -> { if (!lastShare.isEmpty()) { client.keyboard.setClipboard(lastShare); say("Share code copied."); } });
        y += 24;
        button(ctx, x, y, inner, 18, "Generate code from current settings", mx, my, () -> {
            String[] c = ConfigStore.generate(); lastId = c[0]; lastShare = c[1]; say("Generated " + c[0]);
        });
        y += 28;

        Draw.text(ctx, "Load code / name  |  Save as", x, y, Draw.GRAY);
        y += 12;
        int bw = 46, iw = inner - 2 * bw - 8;
        Draw.rrect(ctx, x, y, iw, 18, 9, Draw.a(cfgFocused ? Draw.YELLOW : Draw.WHITE, cfgFocused ? 90 : 40));
        Draw.rrect(ctx, x + 1, y + 1, iw - 2, 16, 8, Draw.a(0xFF000000, 120));
        String shown = cfgText.isEmpty() && !cfgFocused ? "HALO-1234 / HALO-... / name" : cfgText;
        while (Draw.width(shown) > iw - 16 && shown.length() > 4) shown = shown.substring(1);
        Draw.text(ctx, shown + (cfgFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : ""), x + 8, y + 5, cfgText.isEmpty() ? Draw.GRAY : Draw.WHITE);
        acts.add(new Act(x, y, iw, 18, () -> { cfgFocused = true; filterFocused = false; }));
        button(ctx, x + iw + 4, y, bw, 18, "Load", mx, my, () -> doLoad());
        button(ctx, x + iw + 8 + bw, y, bw, 18, "Save", mx, my, () -> { say(ConfigStore.save(cfgText)); saved = ConfigStore.list(); });
        y += 26;
        button(ctx, x, y, inner, 16, "Reset all settings to default", mx, my, () -> { ModuleManager.resetAll(); ModuleManager.save(); say("All settings reset."); });
        y += 24;

        if (!saved.isEmpty()) {
            Draw.text(ctx, "Saved configs", x, y, Draw.GRAY);
            y += 14;
            for (int i = 0; i < rows; i++) {
                final String n = saved.get(i);
                Draw.rrect(ctx, x, y, inner, 18, 9, Draw.a(Draw.WHITE, 18));
                Draw.text(ctx, n.length() > 26 ? n.substring(0, 26) : n, x + 10, y + 5, Draw.WHITE);
                button(ctx, x + inner - 70, y + 1, 44, 16, "Load", mx, my, () -> say(ConfigStore.load(n)));
                button(ctx, x + inner - 22, y + 1, 20, 16, "x", mx, my, () -> { ConfigStore.delete(n); saved = ConfigStore.list(); say("Deleted " + n); });
                y += 22;
            }
        }
        long el = System.currentTimeMillis() - statusAt;
        if (!status.isEmpty() && el < 4000) Draw.ctext(ctx, status, px + pw / 2, py + ph - 14, Draw.a(Draw.YELLOW, (int) (255 * Math.min(1f, (4000 - el) / 600f))));
    }

    private void doLoad() {
        say(ConfigStore.load(cfgText));
        saved = ConfigStore.list();
    }

    private void drawSetting(DrawContext ctx, int px, int sy, int pw, Setting s) {
        int sx = px + 12, sw = pw - 24;
        switch (s.type) {
            case TOGGLE -> {
                Draw.text(ctx, s.name, sx, sy + 5, Draw.GRAY);
                boolean on = s.value > 0.5;
                Draw.rrect(ctx, px + pw - 22, sy + 4, 10, 10, 3, on ? Draw.accent(0.5f) : Draw.a(Draw.WHITE, 60));
                if (on) Draw.rrect(ctx, px + pw - 19, sy + 7, 4, 4, 1, Draw.WHITE);
            }
            case SLIDER -> {
                Draw.text(ctx, s.name, sx, sy + 2, Draw.GRAY);
                String v = fmt(s);
                Draw.text(ctx, v, sx + sw - Draw.width(v), sy + 2, Draw.YELLOW);
                double ratio = (s.value - s.min) / (s.max - s.min);
                int fw = (int) (sw * ratio);
                Draw.rrect(ctx, sx, sy + 15, sw, 4, 2, Draw.a(Draw.WHITE, 55));
                Draw.rrectG(ctx, sx, sy + 15, Math.max(4, fw), 4, 2, Draw.accent(0f), Draw.accent((float) ratio));
                Draw.rrect(ctx, sx + fw - 3, sy + 13, 6, 8, 3, Draw.WHITE);
            }
            case MODE -> {
                Draw.text(ctx, s.name, sx, sy + 5, Draw.GRAY);
                String v = s.modes[(int) s.value];
                Draw.text(ctx, v, sx + sw - Draw.width(v), sy + 5, Draw.YELLOW);
            }
            case KEY -> {
                Draw.text(ctx, s.name, sx, sy + 5, Draw.GRAY);
                String v = listening == s ? "..." : "[" + InputUtil.fromKeyCode((int) s.value, 0).getLocalizedText().getString() + "]";
                Draw.text(ctx, v, sx + sw - Draw.width(v), sy + 5, listening == s ? Draw.ORANGE : Draw.YELLOW);
            }
            default -> {}
        }
    }

    private static String fmt(Setting s) {
        double v = s.value;
        return Math.abs(v - Math.round(v)) < 0.001 ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.2f", v);
    }

    private void applySlider(double mx) {
        if (dragging == null) return;
        double r = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        double v = dragging.min + r * (dragging.max - dragging.min);
        v = (dragging.max - dragging.min >= 10) ? Math.round(v) : Math.round(v * 100) / 100.0;
        dragging.value = v;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        mx /= scale; my /= scale;
        boolean wasCfg = cfgFocused;
        filterFocused = false; cfgFocused = false;
        for (Act a : acts) if (a.in(mx, my)) { a.r().run(); if (a.r() != null && !wasCfg) { /* sound handled by actions */ } Features.uiSound(1.6f, 0.4f); return true; }
        if (configOpen && inCfg((int) mx, (int) my)) { cfgFocused = wasCfg; return true; }
        for (Hit h : hits) {
            if (!h.in(mx, my)) continue;
            if (h.m() == null) { filterFocused = true; return true; }
            if (h.s() == null) {
                boolean arrow = mx > h.x() + h.w() - 56 && mx < h.x() + h.w() - 40;
                boolean hasVisible = false;
                for (Setting s : h.m().settings) if (s.type != Setting.Type.HIDDEN) { hasVisible = true; break; }
                if (btn == 0 && !(arrow && hasVisible)) { h.m().toggle(); Features.uiSound(h.m().enabled ? 1.9f : 1.2f, 0.5f); }
                else if (hasVisible) { h.m().expanded = !h.m().expanded; Features.uiSound(1.5f, 0.4f); }
                ModuleManager.save();
                return true;
            }
            Setting s = h.s();
            Features.uiSound(1.6f, 0.4f);
            switch (s.type) {
                case TOGGLE -> s.value = s.value > 0.5 ? 0 : 1;
                case MODE -> s.value = (s.value + (btn == 1 ? s.modes.length - 1 : 1)) % s.modes.length;
                case KEY -> listening = s;
                case SLIDER -> { dragging = s; dragX = h.x() + 12; dragW = h.w() - 24; applySlider(mx); }
                default -> {}
            }
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging != null) { applySlider(mx / scale); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (dragging != null) { dragging = null; ModuleManager.save(); }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(-500, Math.min(0, scroll + (int) (v * 24)));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (listening != null) {
            if (key != GLFW.GLFW_KEY_ESCAPE) listening.value = key;
            listening = null;
            ModuleManager.save();
            return true;
        }
        if (cfgFocused) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { cfgFocused = false; return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !cfgText.isEmpty()) { cfgText = cfgText.substring(0, cfgText.length() - 1); return true; }
            if (key == GLFW.GLFW_KEY_ENTER) { doLoad(); return true; }
            if (key == GLFW.GLFW_KEY_V && hasControlDown()) {
                String cb = client.keyboard.getClipboard();
                if (cb != null) cfgText = (cfgText + cb.trim()).substring(0, Math.min(700, (cfgText + cb.trim()).length()));
                return true;
            }
            return true;
        }
        if (filterFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !filter.isEmpty()) { filter = filter.substring(0, filter.length() - 1); return true; }
            if (key == GLFW.GLFW_KEY_ENTER) { filterFocused = false; return true; }
        } else if (key == (int) ModuleManager.get("Click GUI").num("Key")) {
            close();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (cfgFocused && c >= 32 && cfgText.length() < 700) { cfgText += c; return true; }
        if (filterFocused && c >= 32) { filter += c; return true; }
        return super.charTyped(c, mods);
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        closeStart = System.currentTimeMillis();
        Features.uiSound(0.9f, 0.5f);
    }

    @Override
    public void removed() { ModuleManager.save(); }
}
