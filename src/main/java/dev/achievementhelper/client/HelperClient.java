package dev.achievementhelper.client;

import dev.achievementhelper.AchievementHelper;
import dev.achievementhelper.ProgressPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = AchievementHelper.ID, dist = Dist.CLIENT)
public final class HelperClient {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(AchievementHelper.ID, "main"));
    static final KeyMapping OPEN = new KeyMapping("key.achievement_helper.open", GLFW.GLFW_KEY_H, CATEGORY);

    public HelperClient(IEventBus bus) {
        HudSettings.load();
        bus.addListener((RegisterKeyMappingsEvent event) -> { event.registerCategory(CATEGORY); event.register(OPEN); });
        bus.addListener((RegisterClientPayloadHandlersEvent event) -> event.register(ProgressPayload.TYPE,
                (payload, context) -> ProgressStore.receive(payload.json())));
        bus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(
                Identifier.fromNamespaceAndPath(AchievementHelper.ID, "tracked"), (graphics, delta) -> hud(graphics)));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            var mc = Minecraft.getInstance();
            while (OPEN.consumeClick()) if (mc.player != null && mc.screen == null) mc.setScreen(new HelperScreen());
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ProgressStore.clear());
    }

    private static void hud(GuiGraphicsExtractor g) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null || !ProgressStore.ready) return;
        TrackedHud.render(g, false);
    }
}
