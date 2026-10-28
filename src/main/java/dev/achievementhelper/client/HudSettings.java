package dev.achievementhelper.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import net.neoforged.fml.loading.FMLPaths;

final class HudSettings {
    static double x = 0.02, y = 0.16, scale = 0.8;
    static void load() {
        var file = FMLPaths.CONFIGDIR.get().resolve("achievement_helper_hud.json");
        if (!Files.exists(file)) return;
        try {
            var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            x = bounded(json.get("x").getAsDouble(), 0, 1, 0.02);
            y = bounded(json.get("y").getAsDouble(), 0, 1, 0.16);
            scale = bounded(json.get("scale").getAsDouble(), 0.5, 1.25, 0.8);
        } catch (Exception e) { LogUtils.getLogger().warn("Cannot load HUD settings", e); }
    }
    static double bounded(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }
    static void save() {
        var file = FMLPaths.CONFIGDIR.get().resolve("achievement_helper_hud.json");
        var json = new com.google.gson.JsonObject();
        json.addProperty("x", x); json.addProperty("y", y); json.addProperty("scale", scale);
        try {
            Files.createDirectories(file.getParent());
            var temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, new GsonBuilder().setPrettyPrinting().create().toJson(json));
            Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { LogUtils.getLogger().warn("Cannot save HUD settings", e); }
    }
}
