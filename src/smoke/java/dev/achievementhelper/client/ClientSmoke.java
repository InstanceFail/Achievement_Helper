package dev.achievementhelper.client;

import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/** Exercises the actual server/client connection and UI in a disposable world. */
@Mod(value = "achievement_helper", dist = Dist.CLIENT)
public final class ClientSmoke {
    private int ticks, stage, wait;
    private int expectedGroups;
    private String criterion;
    private long revision;
    private CompletableFuture<Void> reload;
    private static final String BIOMES = "minecraft:adventure/adventuring_time";
    private static final String MOBS = "minecraft:adventure/kill_all_mobs";

    public ClientSmoke() { NeoForge.EVENT_BUS.addListener(this::tick); }

    private void tick(ClientTickEvent.Post ignored) {
        Minecraft mc = Minecraft.getInstance();
        if (++ticks > 6000) { finish(mc, "FAIL: timeout, stage " + stage); return; }
        try {
            switch (stage) {
                case 0 -> {
                    if (mc.screen == null || mc.getOverlay() != null || ticks < 30) return;
                    if (!mc.getLanguageManager().getSelected().equals("en_us")) {
                        mc.getLanguageManager().setSelected("en_us"); mc.options.languageCode = "en_us";
                        mc.reloadResourcePacks(); return;
                    }
                    HudSettings.x = 0.02; HudSettings.y = 0.16; HudSettings.scale = 0.8;
                    stage = 1;
                    mc.options.guiScale().set(2);
                    mc.options.renderDistance().set(4);
                    mc.options.simulationDistance().set(5);
                    mc.options.onboardAccessibility = false;
                    var settings = new LevelSettings("Achievement Helper smoke", GameType.CREATIVE,
                            new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("ah-smoke-" + System.currentTimeMillis(), settings,
                            new WorldOptions(2612L, false, false), WorldPresets::createFlatWorldDimensions, new TitleScreen());
                }
                case 1 -> {
                    if (!ProgressStore.ready || mc.player == null) return;
                    check(ProgressStore.ENTRIES.size() == 125, "125 visible vanilla advancements including hidden ones; got " + ProgressStore.ENTRIES.size());
                    check(ProgressStore.ENTRIES.get("minecraft:nether/all_effects").effects().size() == 34, "34 simultaneous status effects");
                    check(ProgressStore.ENTRIES.get("minecraft:nether/all_potions").effects().size() == 17, "17 potion effects");
                    for (var e : ProgressStore.ENTRIES.values()) {
                        String key = "guide.achievement_helper." + e.id().replace(':', '.').replace('/', '.');
                        check(net.minecraft.client.resources.language.I18n.exists(key), "guide exists: " + e.id());
                    }
                    ProgressStore.toggle(ProgressStore.ENTRIES.get(BIOMES));
                    ProgressStore.toggle(ProgressStore.ENTRIES.get(MOBS));
                    open(mc, "adventuring_time"); stage = 2; wait = 30;
                }
                case 2 -> {
                    if (--wait > 0) return;
                    screenshot(mc, "01-panel-english.png");
                    var biome = ProgressStore.ENTRIES.get(BIOMES);
                    expectedGroups = biome.completedGroups() + 1;
                    criterion = biome.criteria().entrySet().stream().filter(e -> !e.getValue()).findFirst().orElseThrow().getKey();
                    mc.getSingleplayerServer().execute(() -> {
                        var server = mc.getSingleplayerServer();
                        var player = server.getPlayerList().getPlayers().getFirst();
                        player.getAdvancements().award(server.getAdvancements().get(Identifier.parse(BIOMES)), criterion);
                    });
                    stage = 3;
                }
                case 3 -> {
                    if (!ProgressStore.ENTRIES.get(BIOMES).criteria().get(criterion)) return;
                    check(ProgressStore.ENTRIES.get(BIOMES).completedGroups() == expectedGroups, "partial biome update");
                    check(ProgressStore.TRACKED.contains(BIOMES), "partial progress remains tracked");
                    mc.getLanguageManager().setSelected("zh_cn"); mc.options.languageCode = "zh_cn";
                    reload = mc.reloadResourcePacks(); stage = 4;
                }
                case 4 -> {
                    if (!reload.isDone() || mc.getOverlay() != null) return;
                    check(HelperScreen.tr("title").getString().equals("全进度助手"), "Chinese switches with game language");
                    open(mc, "adventuring_time"); wait = 30; stage = 5;
                }
                case 5 -> {
                    if (--wait > 0) return;
                    screenshot(mc, "02-panel-chinese.png");
                    revision = ProgressStore.revision;
                    mc.getSingleplayerServer().execute(() -> NeoForge.EVENT_BUS.post(
                            new OnDatapackSyncEvent(mc.getSingleplayerServer().getPlayerList(), null)));
                    stage = 6;
                }
                case 6 -> {
                    if (ProgressStore.revision <= revision || !ProgressStore.ready) return;
                    check(ProgressStore.TRACKED.contains(BIOMES) && ProgressStore.TRACKED.contains(MOBS), "tracking persists across catalogue reset");
                    mc.setScreen(null); wait = 30; stage = 7;
                }
                case 7 -> {
                    if (--wait > 0) return;
                    screenshot(mc, "03-tracked-hud.png");
                    mc.getSingleplayerServer().execute(() -> {
                        var server = mc.getSingleplayerServer();
                        var player = server.getPlayerList().getPlayers().getFirst();
                        var holder = server.getAdvancements().get(Identifier.parse(MOBS));
                        holder.value().criteria().keySet().forEach(name -> player.getAdvancements().award(holder, name));
                    });
                    stage = 8;
                }
                case 8 -> {
                    if (!ProgressStore.ENTRIES.get(MOBS).done()) return;
                    check(!ProgressStore.TRACKED.contains(MOBS), "completed advancement automatically untracked");
                    check(ProgressStore.ENTRIES.get(MOBS).percent() == 1f, "completed percentage 100%");
                    mc.getSingleplayerServer().execute(() -> {
                        var server = mc.getSingleplayerServer();
                        var player = server.getPlayerList().getPlayers().getFirst();
                        var holder = server.getAdvancements().get(Identifier.parse(MOBS));
                        holder.value().criteria().keySet().forEach(name -> player.getAdvancements().revoke(holder, name));
                    });
                    stage = 9;
                }
                case 9 -> {
                    if (ProgressStore.ENTRIES.get(MOBS).percent() != 0f) return;
                    check(!ProgressStore.TRACKED.contains(MOBS), "revoking does not silently retrack");
                    HudSettings.x = 1; HudSettings.y = 1;
                    var bounds = TrackedHud.bounds(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 5);
                    check(bounds.x() + bounds.width() <= mc.getWindow().getGuiScaledWidth() - 6, "right edge remains on screen");
                    check(bounds.y() + bounds.height() <= mc.getWindow().getGuiScaledHeight() - 6, "bottom edge remains on screen");
                    HudSettings.x = 0.65; HudSettings.y = 0.1;
                    var settings = new HudSettingsScreen(new HelperScreen()); mc.setScreen(settings);
                    bounds = TrackedHud.bounds(settings.width, settings.height, TrackedHud.entries(true).size());
                    var info = new net.minecraft.client.input.MouseButtonInfo(0, 0);
                    check(settings.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(bounds.x() + 5, bounds.y() + 5, info), false), "grab HUD preview");
                    settings.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(bounds.x() + 20, bounds.y() + 15, info), 15, 10);
                    settings.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(bounds.x() + 20, bounds.y() + 15, info));
                    check(HudSettings.x > 0.65 && HudSettings.y > 0.1, "drag changes position");
                    settings.children().stream().filter(net.minecraft.client.gui.components.Button.class::isInstance)
                            .map(net.minecraft.client.gui.components.Button.class::cast).filter(b -> b.getMessage().getString().equals("−"))
                            .findFirst().orElseThrow().onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
                    check(Math.abs(HudSettings.scale - 0.75) < 0.001, "size control changes scale");
                    wait = 30; stage = 10;
                }
                case 10 -> {
                    if (--wait > 0) return;
                    screenshot(mc, "04-hud-settings.png");
                    double savedX = HudSettings.x, savedY = HudSettings.y;
                    mc.screen.onClose();
                    HudSettings.x = 0; HudSettings.y = 0; HudSettings.scale = 1;
                    HudSettings.load();
                    check(HudSettings.x == savedX && HudSettings.y == savedY && Math.abs(HudSettings.scale - 0.75) < 0.001, "position and scale saved and restored");
                    mc.getSingleplayerServer().execute(() -> {
                        var server = mc.getSingleplayerServer(); var player = server.getPlayerList().getPlayers().getFirst();
                        var holder = server.getAdvancements().get(Identifier.parse("minecraft:adventure/kill_a_mob"));
                        player.getAdvancements().award(holder, holder.value().criteria().keySet().iterator().next());
                    });
                    open(mc, "adventure/kill"); wait = 30; stage = 11;
                }
                case 11 -> {
                    if (--wait > 0 || !ProgressStore.ENTRIES.get("minecraft:adventure/kill_a_mob").done()) return;
                    screenshot(mc, "05-list-status.png"); mc.setScreen(null); wait = 30; stage = 12;
                }
                case 12 -> {
                    if (--wait > 0) return;
                    screenshot(mc, "06-hud-repositioned.png");
                    finish(mc, "PASS: full catalogue, bilingual guides, live progress, tracking persistence, auto-untrack, revocation, HUD drag, scale controls, saved HUD settings, screen bounds, completed/incomplete rows.");
                }
            }
        } catch (Throwable error) {
            LogUtils.getLogger().error("Achievement Helper smoke failed", error);
            finish(mc, "FAIL stage " + stage + ": " + error);
        }
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void open(Minecraft mc, String query) {
        var screen = new HelperScreen(); mc.setScreen(screen);
        screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow().setValue(query);
    }
    private static void screenshot(Minecraft mc, String filename) {
        Screenshot.grab(mc.gameDirectory, filename, mc.getMainRenderTarget(), 1, message -> LogUtils.getLogger().info("{}", message.getString()));
    }
    private void finish(Minecraft mc, String result) {
        if (stage == 99) return;
        stage = 99;
        try { Files.writeString(Path.of(mc.gameDirectory.toString(), "smoke-result.txt"), result); }
        catch (Exception e) { LogUtils.getLogger().error("Cannot write smoke result", e); }
        LogUtils.getLogger().info("{}", result);
        mc.stop();
    }
}
