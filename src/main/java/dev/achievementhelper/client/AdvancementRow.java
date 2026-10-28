package dev.achievementhelper.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Status remains visible independently of hover, selection and color perception. */
final class AdvancementRow extends Button {
    private final ProgressEntry entry;
    private final boolean selected;
    AdvancementRow(int x, int y, int width, ProgressEntry entry, boolean selected, OnPress action) {
        super(x, y, width, 28, entry.title().copy().append(" — ")
                .append(HelperScreen.tr(entry.done() ? "done" : "remaining")).append(" " + entry.progressText()), action, DEFAULT_NARRATION);
        this.entry = entry; this.selected = selected;
    }
    @Override public void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        var font = Minecraft.getInstance().font;
        int x = getX(), y = getY(), w = getWidth();
        int accent = entry.done() ? 0xFF8DDA75 : 0xFFE4B96E;
        g.fill(x, y, x + w, y + 28, entry.done() ? 0xFF253D29 : 0xFF39332A);
        g.fill(x, y, x + 3, y + 28, accent);
        g.outline(x, y, w, 28, selected ? 0xFFFFDD78 : isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF666666);
        String title = (ProgressStore.TRACKED.contains(entry.id()) ? "★ " : "") + entry.title().getString();
        g.text(font, font.plainSubstrByWidth(title, w - 14), x + 7, y + 4, 0xFFFFFFFF);
        g.text(font, HelperScreen.tr(entry.done() ? "done" : "remaining"), x + 7, y + 16, accent);
        String progress = font.plainSubstrByWidth(entry.progressText(), w / 2 - 5);
        g.text(font, progress, x + w - 5 - font.width(progress), y + 16, 0xFFD0D0D0);
    }
}
