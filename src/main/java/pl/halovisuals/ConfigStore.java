package pl.halovisuals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Config codes.
 *  - Short ID  "HALO-7892"  : saved on THIS computer (config/halovisuals/codes) - handy for yourself.
 *  - Share code "HALO-<text>": the (compressed) settings themselves - works for any player without a server.
 * Both are accepted by /halovs config load and by the Configs tab.
 */
public final class ConfigStore {
    private ConfigStore() {}

    private static Path dir(String sub) {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("halovisuals").resolve(sub);
        try { Files.createDirectories(p); } catch (IOException ignored) {}
        return p;
    }

    private static String clean(String name) { return name.replaceAll("[^A-Za-z0-9_\\-]", "_"); }

    /** @return {shortId, shareCode} */
    public static String[] generate() {
        String json = ModuleManager.exportJson(true).toString();
        String share = "HALO-" + encode(json);
        Random r = new Random();
        String id;
        do { id = String.valueOf(1000 + r.nextInt(9000)); } while (Files.exists(dir("codes").resolve(id + ".json")));
        try { Files.writeString(dir("codes").resolve(id + ".json"), ModuleManager.exportJson(true).toString()); } catch (IOException ignored) {}
        return new String[]{"HALO-" + id, share};
    }

    public static String load(String input) {
        String in = input == null ? "" : input.trim();
        if (in.isEmpty()) return "Enter a code or a config name.";
        try {
            JsonObject root = null;
            String label = in;
            if (in.regionMatches(true, 0, "HALO-", 0, 5)) {
                String rest = in.substring(5).trim();
                if (rest.matches("\\d{3,6}")) {
                    Path f = dir("codes").resolve(rest + ".json");
                    if (!Files.exists(f)) return "Code HALO-" + rest + " not found on this computer. Use the long share code.";
                    root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                } else {
                    root = JsonParser.parseString(decode(rest)).getAsJsonObject();
                    label = "share code";
                }
            } else {
                Path f = dir("configs").resolve(clean(in) + ".json");
                if (!Files.exists(f)) return "Config '" + in + "' not found.";
                root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            }
            int n = ModuleManager.applyJson(root, false);
            ModuleManager.save();
            return "Loaded " + label + " (" + n + " modules).";
        } catch (Exception e) {
            return "Invalid code.";
        }
    }

    public static String save(String name) {
        String n = clean(name.trim());
        if (n.isEmpty()) return "Enter a name first.";
        try {
            Files.writeString(dir("configs").resolve(n + ".json"), ModuleManager.exportJson(false).toString());
            return "Saved config '" + n + "'.";
        } catch (IOException e) { return "Could not save."; }
    }

    public static boolean delete(String name) {
        try { return Files.deleteIfExists(dir("configs").resolve(clean(name) + ".json")); } catch (IOException e) { return false; }
    }

    public static List<String> list() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir("configs"))) {
            s.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                String f = p.getFileName().toString();
                out.add(f.substring(0, f.length() - 5));
            });
        } catch (IOException ignored) {}
        Collections.sort(out);
        return out;
    }

    private static String encode(String json) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            try (DeflaterOutputStream d = new DeflaterOutputStream(bo)) { d.write(json.getBytes(StandardCharsets.UTF_8)); }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bo.toByteArray());
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    private static String decode(String s) throws IOException {
        byte[] raw = Base64.getUrlDecoder().decode(s);
        try (InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(raw))) {
            return new String(in.readNBytes(256 * 1024), StandardCharsets.UTF_8);
        }
    }
}
