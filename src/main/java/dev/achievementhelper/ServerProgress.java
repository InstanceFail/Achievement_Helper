package dev.achievementhelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.network.PacketDistributor;

final class ServerProgress {
    static void sync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(player -> {
            if (!player.connection.hasChannel(ProgressPayload.TYPE)) return;
            var reset = new JsonObject();
            reset.addProperty("reset", true);
            String world = player.level().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().toString();
            reset.addProperty("scope", java.util.UUID.nameUUIDFromBytes((world + player.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
            send(player, reset.toString());
            for (var holder : player.level().getServer().getAdvancements().getAllAdvancements()) {
                if (holder.value().display().isPresent()) {
                    sendEntry(player, holder, player.getAdvancements().getOrStartProgress(holder));
                }
            }
            send(player, "{\"ready\":true}");
        });
    }

    static void changed(AdvancementEvent.AdvancementProgressEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.connection != null && player.connection.hasChannel(ProgressPayload.TYPE)) {
            sendEntry(player, event.getAdvancement(), event.getAdvancementProgress());
        }
    }

    private static void sendEntry(ServerPlayer player, AdvancementHolder holder, AdvancementProgress progress) {
        holder.value().display().ifPresent(display -> {
            var json = new JsonObject();
            json.addProperty("id", holder.id().toString());
            var ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            json.add("title", ComponentSerialization.CODEC.encodeStart(ops, display.getTitle()).getOrThrow());
            json.add("description", ComponentSerialization.CODEC.encodeStart(ops, display.getDescription()).getOrThrow());
            json.addProperty("icon", BuiltInRegistries.ITEM.getKey(display.getIcon().item().value()).toString());
            json.addProperty("done", progress.isDone());
            json.addProperty("percent", progress.getPercent());
            JsonObject criteria = new JsonObject();
            holder.value().criteria().keySet().forEach(name -> criteria.addProperty(name,
                    progress.getCriterion(name) != null && progress.getCriterion(name).isDone()));
            json.add("criteria", criteria);
            JsonArray requirements = new JsonArray();
            holder.value().requirements().requirements().forEach(group -> {
                JsonArray array = new JsonArray();
                group.forEach(array::add);
                requirements.add(array);
            });
            json.add("requirements", requirements);
            // Status-effect challenges require simultaneous effects, not permanent per-effect progress.
            JsonArray effects = new JsonArray();
            var effectIds = new java.util.TreeSet<String>();
            holder.value().criteria().values().forEach(criterion -> {
                var encoded = net.minecraft.advancements.Criterion.CODEC.encodeStart(ops, criterion).result();
                encoded.ifPresent(value -> {
                    var definition = value.getAsJsonObject();
                    if (definition.has("conditions")) {
                        var conditions = definition.getAsJsonObject("conditions");
                        if (conditions.has("effects") && conditions.get("effects").isJsonObject())
                            effectIds.addAll(conditions.getAsJsonObject("effects").keySet());
                    }
                });
            });
            effectIds.forEach(effects::add);
            json.add("effects", effects);
            send(player, json.toString());
        });
    }

    private static void send(ServerPlayer player, String json) {
        PacketDistributor.sendToPlayer(player, new ProgressPayload(json));
    }
}
