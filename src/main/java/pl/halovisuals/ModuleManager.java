package pl.halovisuals;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.*;
import java.util.*;

public final class ModuleManager {
    public static final List<Module> ALL = new ArrayList<>();
    private static final Map<String, Module> MAP = new HashMap<>();
    public static final String[] CATS = {"Combat", "HUD", "Animations", "Sound", "World", "Client"};
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("halovisuals.json");

    /** Discord application id for Rich Presence (config key "_discordAppId"). */
    public static String discordAppId = "";

    public static final String[] HIT_SOUNDS = {"silent", "halo", "bell", "bonk", "crystal", "crystal chord", "glass shard", "pling", "xylophone", "orb"};
    public static final String[] KILL_SOUNDS = {"Level Up", "Lightning", "Loud Crystal", "Beacon", "Bell Choir", "Glass Shatter"};

    private ModuleManager() {}

    public static Module get(String n) { return MAP.get(n); }

    private static Module add(String cat, String name, boolean def, Setting... s) {
        Module m = new Module(cat, name, true, def);
        m.settings.addAll(Arrays.asList(s));
        ALL.add(m); MAP.put(name, m);
        return m;
    }

    private static Setting style(String n, int idx) {
        return Setting.mode(n, idx, "Angel Dust", "Star Sparks", "Golden Petals", "Crystals", "Snowflakes", "Crimson Sparks", "Mixed");
    }

    private static Setting[] hud(double x, double y, Setting... extra) {
        List<Setting> l = new ArrayList<>(Arrays.asList(extra));
        l.add(Setting.slider("Scale", 0.6, 2.0, 1.0));
        l.add(Setting.hidden("X", x));
        l.add(Setting.hidden("Y", y));
        return l.toArray(new Setting[0]);
    }

    static {
        // ---- Combat ----
        add("Combat", "Hit Marker", true, Setting.slider("Size", 3, 12, 6), Setting.slider("Duration", 100, 800, 350));
        add("Combat", "Damage Numbers", false, Setting.slider("Scale", 0.5, 2.0, 1.0));
        add("Combat", "Hit Particles", false, style("Style", 6), Setting.slider("Amount", 1, 80, 28));
        add("Combat", "Hit Bubbles", false, Setting.slider("Amount", 1, 30, 8));
        add("Combat", "Target ESP", false,
                Setting.mode("Style", 0, "Angel Rings", "Soul", "Square", "Crystal Orbit"),
                Setting.mode("Palette", 0, "Golden", "Soul Blue", "Rose"),
                Setting.slider("Rings", 1, 4, 3), Setting.slider("Speed", 0.5, 3.0, 1.5), Setting.toggle("Glow", true));
        add("Combat", "Kill Effect", false, Setting.mode("Style", 1, "Burst", "Lightning", "Angel Abduction", "Ragdoll"));
        add("Combat", "Death Effect", false);
        add("Combat", "Totem Pop", false);
        add("Combat", "Hit Color", false, Setting.mode("Color", 0, "Gold", "White", "Rose"));
        add("Combat", "Hitbox Customizer", false,
                Setting.slider("Range", 8, 64, 32), Setting.slider("Expand", 0, 0.5, 0.0), Setting.toggle("Fill", true));
        // ---- HUD ----
        add("HUD", "Target HUD", true, hud(0.58, 0.60, Setting.toggle("Equipment", true)));
        add("HUD", "Watermark", true, hud(0.006, 0.012));
        add("HUD", "Dynamic Island", true);
        add("HUD", "Potions", false, hud(0.84, 0.10));
        add("HUD", "Cooldowns", false);
        add("HUD", "Combo Counter", false);
        add("HUD", "Cooldown Ring", false);
        add("HUD", "Crosshair", false,
                Setting.mode("Style", 0, "Cross", "Dot", "Circle"), Setting.slider("Size", 2, 10, 5), Setting.slider("Gap", 0, 8, 3));
        // ---- Animations ----
        add("Animations", "Swing Animations", false,
                Setting.mode("Style", 0, "Smooth Slash", "Orbit", "Float", "Spin", "Punch", "Fan"),
                Setting.slider("Strength", 0.5, 2.0, 1.0), Setting.slider("Speed", 0.5, 2.0, 1.0));
        add("Animations", "No Hurt Cam", false);
        // ---- Sound ----
        add("Sound", "Hit Sounds", true, Setting.mode("Style", 4, HIT_SOUNDS), Setting.slider("Loudness", 0.1, 3.0, 2.0));
        add("Sound", "Kill Sound", false, Setting.mode("Sound", 2, KILL_SOUNDS), Setting.slider("Loudness", 0.1, 3.0, 2.0));
        // ---- World ----
        add("World", "Jump Circles", false);
        add("World", "Jump Particles", false, style("Style", 0), Setting.slider("Amount", 1, 80, 30));
        add("World", "Ambient Particles", false, style("Style", 6), Setting.slider("Amount", 1, 20, 5));
        add("World", "Trails", false, style("Style", 0));
        add("World", "Halo", true, Setting.slider("Radius", 0.2, 0.5, 0.32), Setting.toggle("Glow", true), Setting.toggle("Crystals", true));
        add("World", "China Hat", false, Setting.slider("Radius", 0.3, 0.9, 0.55));
        add("World", "Custom Sky", false,
                Setting.mode("Style", 0, "Space Nebula", "Heaven Gold", "Twilight Rose", "Deep Void"),
                Setting.slider("Stars", 0.2, 1.0, 0.7), Setting.slider("Nebula", 0.0, 1.0, 0.8), Setting.slider("Rotation", 0.0, 3.0, 1.0));
        add("World", "Atmosphere", false,
                Setting.toggle("Lock Time", false), Setting.slider("Time", 0, 24000, 18000), Setting.toggle("Clear Weather", true),
                Setting.toggle("Fog", true), Setting.slider("Fog Density", 0.0, 1.0, 0.35),
                Setting.mode("Fog Color", 0, "Sky", "Gold", "Lavender", "White", "Rose"));
        add("World", "Cinematic Text", false);
        add("World", "Motion Blur", false, Setting.slider("Strength", 0.1, 1.0, 0.5));
        add("World", "Color Saturation", false,
                Setting.slider("Brightness", 0.0, 1.0, 0.25), Setting.slider("Saturation", 0.0, 1.0, 0.35),
                Setting.mode("Tint", 0, "Golden Hour", "Rose Quartz", "Ice", "Neutral Pop"));
        add("World", "Aspect Ratio", false, Setting.slider("Ratio", 0.8, 3.0, 1.78));
        add("World", "Fullbright", false);
        // ---- Client ----
        add("Client", "Click GUI", true, Setting.key("Key", 73), Setting.toggle("UI Sounds", true), Setting.slider("UI Volume", 0.1, 1.0, 0.6));
        add("Client", "Performance", false, Setting.slider("Particle Limit", 200, 4000, 500), Setting.toggle("Glow Layers", false));
        add("Client", "Discord Presence", false, Setting.toggle("ShowServer", false));
        add("Client", "Halo Nametag", false, Setting.toggle("Others", false), Setting.slider("Scale", 0.6, 2.0, 1.0));
        add("Client", "Cosmetics", false,
                Setting.toggle("Wings", true), Setting.slider("Wing Size", 0.6, 1.6, 1.0), Setting.slider("Flap Speed", 0.3, 2.5, 1.0),
                Setting.mode("Wing Palette", 0, "Golden", "Pearl", "Rose Gold"), Setting.toggle("Wing Glow", true),
                Setting.toggle("Cloak", true), Setting.slider("Shine Speed", 0.2, 2.5, 1.0),
                Setting.toggle("Orbit Stars", true));
    }

    // ------------------------------------------------------------ persistence
    public static void save() {
        try {
            JsonObject root = exportJson(false);
            root.addProperty("_discordAppId", discordAppId);
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void load() {
        try {
            if (!Files.exists(FILE)) { save(); return; }
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            if (root.has("_discordAppId")) discordAppId = root.get("_discordAppId").getAsString();
            applyJson(root, true);
            save();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /** Serialises modules. diffOnly = only values that differ from defaults (used for short share codes). */
    public static JsonObject exportJson(boolean diffOnly) {
        JsonObject root = new JsonObject();
        for (Module m : ALL) {
            JsonObject o = new JsonObject();
            JsonObject st = new JsonObject();
            boolean changed = m.enabled != m.defEnabled;
            for (Setting s : m.settings) {
                if (diffOnly && s.type == Setting.Type.KEY) continue; // keybind is personal
                if (!diffOnly || Math.abs(s.value - s.def) > 1e-9) { st.addProperty(s.name, s.value); changed = true; }
            }
            if (diffOnly && !changed) continue;
            o.addProperty("enabled", m.enabled);
            o.add("settings", st);
            root.add(m.name, o);
        }
        return root;
    }

    /** Applies a config JSON; returns the number of modules touched. */
    public static int applyJson(JsonObject root, boolean includeKey) {
        int n = 0;
        for (Module m : ALL) {
            if (!root.has(m.name) || !root.get(m.name).isJsonObject()) continue;
            JsonObject o = root.getAsJsonObject(m.name);
            n++;
            if (o.has("enabled")) { m.enabled = o.get("enabled").getAsBoolean(); m.anim = m.enabled ? 1f : 0f; }
            if (o.has("settings") && o.get("settings").isJsonObject()) {
                JsonObject st = o.getAsJsonObject("settings");
                for (Setting s : m.settings) {
                    if (!st.has(s.name)) continue;
                    if (s.type == Setting.Type.KEY && !includeKey) continue;
                    double v = st.get(s.name).getAsDouble();
                    switch (s.type) {
                        case SLIDER, HIDDEN -> v = Math.max(s.min, Math.min(s.max, v));
                        case MODE -> v = Math.max(0, Math.min(s.modes.length - 1, Math.round(v)));
                        case TOGGLE -> v = v > 0.5 ? 1 : 0;
                        default -> {}
                    }
                    s.value = v;
                }
            }
        }
        return n;
    }

    public static void resetAll() {
        for (Module m : ALL) {
            m.enabled = m.defEnabled; m.anim = m.enabled ? 1f : 0f;
            for (Setting s : m.settings) if (s.type != Setting.Type.KEY) s.value = s.def;
        }
    }
}
