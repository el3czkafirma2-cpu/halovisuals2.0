package pl.halovisuals;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.Map;

/** Client-side chat commands: /halovs config ..., /halovs keybind clickgui <key> */
public final class Commands {
    private Commands() {}

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("shift", "left.shift"), Map.entry("lshift", "left.shift"), Map.entry("rshift", "right.shift"),
            Map.entry("ctrl", "left.control"), Map.entry("control", "left.control"), Map.entry("lctrl", "left.control"), Map.entry("rctrl", "right.control"),
            Map.entry("alt", "left.alt"), Map.entry("lalt", "left.alt"), Map.entry("ralt", "right.alt"),
            Map.entry("pageup", "page.up"), Map.entry("pagedown", "page.down"), Map.entry("grave", "grave.accent"), Map.entry("`", "grave.accent"),
            Map.entry("capslock", "caps.lock"), Map.entry("backspace", "backspace"), Map.entry("return", "enter"),
            Map.entry("rightshift", "right.shift"), Map.entry("leftshift", "left.shift"), Map.entry("ins", "insert"), Map.entry("del", "delete"));

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("halovs")
                        .executes(ctx -> { help(ctx.getSource()); return 1; })
                        .then(ClientCommandManager.literal("config")
                                .then(ClientCommandManager.literal("load")
                                        .then(ClientCommandManager.argument("code", StringArgumentType.greedyString())
                                                .executes(ctx -> { msg(ctx.getSource(), ConfigStore.load(StringArgumentType.getString(ctx, "code"))); return 1; })))
                                .then(ClientCommandManager.literal("export")
                                        .executes(ctx -> {
                                            String[] c = ConfigStore.generate();
                                            MinecraftClient.getInstance().keyboard.setClipboard(c[1]);
                                            msg(ctx.getSource(), "Config exported. ID: " + c[0] + "  (long share code copied to clipboard)");
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("save")
                                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                                .executes(ctx -> { msg(ctx.getSource(), ConfigStore.save(StringArgumentType.getString(ctx, "name"))); return 1; })))
                                .then(ClientCommandManager.literal("list")
                                        .executes(ctx -> { msg(ctx.getSource(), "Saved configs: " + String.join(", ", ConfigStore.list())); return 1; })))
                        .then(ClientCommandManager.literal("keybind")
                                .then(ClientCommandManager.literal("clickgui")
                                        .then(ClientCommandManager.argument("key", StringArgumentType.greedyString())
                                                .suggests((c, b) -> {
                                                    for (String k : new String[]{"insert", "i", "shift", "rshift", "ctrl", "alt", "grave", "f6", "home", "delete", "right.shift"}) b.suggest(k);
                                                    return b.buildFuture();
                                                })
                                                .executes(ctx -> { setKey(ctx.getSource(), StringArgumentType.getString(ctx, "key")); return 1; }))))));
    }

    private static void help(FabricClientCommandSource s) {
        msg(s, "/halovs config load <code|name>  |  config export  |  config save <name>  |  config list");
        msg(s, "/halovs keybind clickgui <key>   (e.g. insert, i, shift, rshift, f6)");
    }

    private static void msg(FabricClientCommandSource s, String t) {
        s.sendFeedback(Text.literal("[HV] ").formatted(Formatting.GOLD).append(Text.literal(t).formatted(Formatting.WHITE)));
    }

    private static void setKey(FabricClientCommandSource s, String raw) {
        String k = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '.');
        k = ALIASES.getOrDefault(k, k);
        try {
            InputUtil.Key key = InputUtil.fromTranslationKey("key.keyboard." + k);
            if (key == InputUtil.UNKNOWN_KEY) throw new IllegalArgumentException();
            ModuleManager.get("Click GUI").s("Key").value = key.getCode();
            ModuleManager.save();
            msg(s, "ClickGUI key set to " + key.getLocalizedText().getString());
        } catch (Exception e) {
            msg(s, "Unknown key '" + raw + "'. Try: insert, i, shift, rshift, ctrl, alt, f6, home...");
        }
    }
}
