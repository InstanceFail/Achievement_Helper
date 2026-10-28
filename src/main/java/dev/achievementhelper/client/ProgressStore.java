package dev.achievementhelper.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.FMLPaths;

final class ProgressStore {
    static final Map<String, ProgressEntry> ENTRIES = new LinkedHashMap<>();
    static final Set<String> TRACKED = new LinkedHashSet<>();
    static boolean ready;
    static long revision;
    private static String scope;
    private static JsonObject saved = new JsonObject();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("achievement_helper_tracking.json");

    static void receive(String text) {
        revision++;
        var json = JsonParser.parseString(text).getAsJsonObject();
        if (json.has("reset")) {
            clear();
            var server = net.minecraft.client.Minecraft.getInstance().getCurrentServer();
            scope = (server == null ? "local" : server.ip) + "/" + json.get("scope").getAsString();
            try {
                saved = Files.exists(FILE) ? JsonParser.parseString(Files.readString(FILE)).getAsJsonObject() : new JsonObject();
                if (saved.has(scope)) saved.getAsJsonArray(scope).forEach(value -> TRACKED.add(value.getAsString()));
            } catch (Exception e) { LogUtils.getLogger().warn("Cannot read advancement tracking", e); }
        } else if (json.has("ready")) {
            ready = true;
            if (TRACKED.removeIf(id -> !ENTRIES.containsKey(id) || ENTRIES.get(id).done())) save();
        } else {
            ProgressEntry entry = ProgressEntry.parse(json);
            ENTRIES.put(entry.id(), entry);
            if (entry.done() && TRACKED.remove(entry.id())) save();
        }
    }

    static void clear() {
        ENTRIES.clear(); TRACKED.clear(); ready = false; scope = null;
    }

    static void toggle(ProgressEntry entry) {
        if (!TRACKED.remove(entry.id()) && !entry.done() && TRACKED.size() < 5) TRACKED.add(entry.id());
        save();
    }

    private static void save() {
        if (scope == null) return;
        var array = new com.google.gson.JsonArray();
        TRACKED.forEach(array::add);
        saved.add(scope, array);
        try {
            Files.createDirectories(FILE.getParent());
            Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(saved));
            Files.move(temporary, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { LogUtils.getLogger().warn("Cannot save advancement tracking", e); }
    }
}
