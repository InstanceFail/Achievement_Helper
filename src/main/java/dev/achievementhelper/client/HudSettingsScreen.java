package dev.achievementhelper.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

final class HudSettingsScreen extends Screen {
    private final Screen parent;
    private boolean dragging;
    private double grabX, grabY;
    HudSettingsScreen(Screen parent) { super(HelperScreen.tr("hud.settings")); this.parent = parent; }
    @Override protected void init() {
        int left = (width - 300) / 2, top = height - 75;
        String[] labels = {"X −", "X +", "Y −", "Y +", "−", "+"};
        for (int i = 0; i < 6; i++) {
            final int action = i;
            addRenderableWidget(Button.builder(net.minecraft.network.chat.Component.literal(labels[i]), b -> {
                switch (action) {
                    case 0 -> HudSettings.x = Math.max(0, HudSettings.x - 0.05);
                    case 1 -> HudSettings.x = Math.min(1, HudSettings.x + 0.05);
                    case 2 -> HudSettings.y = Math.max(0, HudSettings.y - 0.05);
                    case 3 -> HudSettings.y = Math.min(1, HudSettings.y + 0.05);
                    case 4 -> HudSettings.scale = Math.max(0.5, HudSettings.scale - 0.05);
                    case 5 -> HudSettings.scale = Math.min(1.25, HudSettings.scale + 0.05);
                }
            }).bounds(left + i * 50, top, 46, 20).build());
        }
        addRenderableWidget(Button.builder(HelperScreen.tr("hud.reset"), b -> {
            HudSettings.x = 0.02; HudSettings.y = 0.16; HudSettings.scale = 0.8;
        }).bounds(left, top + 27, 146, 20).build());
        addRenderableWidget(Button.builder(HelperScreen.tr("close"), b -> onClose()).bounds(left + 150, top + 27, 146, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        g.centeredText(font, HelperScreen.tr("hud.drag"), width / 2, 25, 0xFFE0D5AF);
        TrackedHud.render(g, true);
        g.fill(0, height - 94, width, height, 0xDD202020);
        g.centeredText(font, HelperScreen.tr("hud.values", Math.round(HudSettings.x * 100), Math.round(HudSettings.y * 100), Math.round(HudSettings.scale * 100)),
                width / 2, height - 89, 0xFFFFFFFF);
        super.extractRenderState(g, mx, my, delta);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        var bounds = TrackedHud.bounds(width, height, Math.max(1, TrackedHud.entries(true).size()));
        if (event.button() == 0 && event.y() < height - 94 && bounds.contains(event.x(), event.y())) {
            dragging = true; grabX = event.x() - bounds.x(); grabY = event.y() - bounds.y(); return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging) return super.mouseDragged(event, dx, dy);
        var bounds = TrackedHud.bounds(width, height, Math.max(1, TrackedHud.entries(true).size()));
        HudSettings.x = Math.clamp((event.x() - grabX - 6) / Math.max(1, width - bounds.width() - 12), 0, 1);
        HudSettings.y = Math.clamp((event.y() - grabY - 6) / Math.max(1, height - bounds.height() - 12), 0, 1);
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) { dragging = false; return super.mouseReleased(event); }
    @Override public void removed() { HudSettings.save(); }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
