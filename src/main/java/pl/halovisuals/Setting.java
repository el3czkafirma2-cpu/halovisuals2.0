package pl.halovisuals;

public class Setting {
    public enum Type { TOGGLE, SLIDER, MODE, KEY, HIDDEN }

    public final String name;
    public final Type type;
    public double value, min, max, def;
    public String[] modes;

    private Setting(String name, Type type) { this.name = name; this.type = type; }

    public static Setting toggle(String n, boolean v) {
        Setting s = new Setting(n, Type.TOGGLE); s.value = s.def = v ? 1 : 0; return s;
    }
    public static Setting slider(String n, double min, double max, double v) {
        Setting s = new Setting(n, Type.SLIDER); s.min = min; s.max = max; s.value = s.def = v; return s;
    }
    public static Setting mode(String n, int idx, String... modes) {
        Setting s = new Setting(n, Type.MODE); s.value = s.def = idx; s.modes = modes; return s;
    }
    public static Setting hidden(String n, double v) {
        Setting s = new Setting(n, Type.HIDDEN); s.value = s.def = v; s.min = 0; s.max = 1; return s;
    }
    public static Setting key(String n, int code) {
        Setting s = new Setting(n, Type.KEY); s.value = s.def = code; return s;
    }
}
