package dev.achievementhelper.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class TrackedHud {
    record Bounds(int x, int y, int width, int height) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    }
    static List<ProgressEntry> entries(boolean preview) {
        var entries = ProgressStore.TRACKED.stream().map(ProgressStore.ENTRIES::get)
                .filter(e -> e != null && !e.done()).toList();
        if (preview && entries.isEmpty()) return ProgressStore.ENTRIES.values().stream().filter(e -> !e.done()).limit(2).toList();
        return entries;
    }
    static Bounds bounds(int sw, int sh, int count) {
        double scale = HudSettings.scale;
        int logicalWidth = Math.min(180, sw / 3);
        int visible = Math.min(count, Math.max(1, (int) ((sh - 12) / (35 * scale))));
        int w = (int) Math.ceil(logicalWidth * scale), h = (int) Math.ceil(visible * 35 * scale);
        return new Bounds(6 + (int) ((sw - w - 12) * HudSettings.x),
                6 + (int) ((sh - h - 12) * HudSettings.y), w, h);
    }
    static Bounds render(GuiGraphicsExtractor g, boolean preview) {
        var mc = Minecraft.getInstance();
        var entries = entries(preview);
        var bounds = bounds(g.guiWidth(), g.guiHeight(), Math.max(1, entries.size()));
        int width = Math.min(180, g.guiWidth() / 3);
        g.pose().pushMatrix();
        g.pose().translate(bounds.x(), bounds.y());
        g.pose().scale((float) HudSettings.scale);
        int cy = 0;
        for (var e : entries) {
            if ((cy + 35) * HudSettings.scale > bounds.height() + 1) break;
            g.fill(0, cy, width, cy + 32, 0xB8151515);
            g.fill(0, cy, 2, cy + 32, 0xFFB6A16A);
            g.item(e.icon(), 5, cy + 5);
            g.text(mc.font, mc.font.plainSubstrByWidth(e.title().getString(), width - 29), 25, cy + 3, 0xFFFFFFFF);
            g.text(mc.font, e.progressText(), 25, cy + 15, 0xFFE0D5AF);
            HelperScreen.bar(g, 5, cy + 26, width - 10, e.percent());
            cy += 35;
        }
        if (preview && entries.isEmpty()) {
            g.fill(0, 0, width, 32, 0xB8151515);
            g.text(mc.font, HelperScreen.tr("hud.preview"), 5, 5, 0xFFFFFFFF);
            HelperScreen.bar(g, 5, 24, width - 10, 0.4f);
        }
        g.pose().popMatrix();
        if (preview) g.outline(bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, bounds.height() + 2, 0xFFFFD56A);
        return bounds;
    }
}
