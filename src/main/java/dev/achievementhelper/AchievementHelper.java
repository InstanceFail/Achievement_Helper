package dev.achievementhelper;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(AchievementHelper.ID)
public final class AchievementHelper {
    public static final String ID = "achievement_helper";

    public AchievementHelper(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional()
                .playToClient(ProgressPayload.TYPE, ProgressPayload.CODEC));
        NeoForge.EVENT_BUS.addListener(ServerProgress::sync);
        NeoForge.EVENT_BUS.addListener(ServerProgress::changed);
    }
}
