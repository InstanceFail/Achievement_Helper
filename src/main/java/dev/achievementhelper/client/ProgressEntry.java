package dev.achievementhelper.client;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record ProgressEntry(String id, Component title, Component description, ItemStack icon,
                            boolean done, float percent, Map<String, Boolean> criteria, List<List<String>> requirements,
                            List<String> effects) {
    static ProgressEntry parse(JsonObject json) {
        var ops = Minecraft.getInstance().level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        Map<String, Boolean> criteria = new LinkedHashMap<>();
        json.getAsJsonObject("criteria").entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> criteria.put(e.getKey(), e.getValue().getAsBoolean()));
        List<List<String>> requirements = new java.util.ArrayList<>();
        json.getAsJsonArray("requirements").forEach(group -> {
            List<String> names = new java.util.ArrayList<>();
            group.getAsJsonArray().forEach(value -> names.add(value.getAsString()));
            requirements.add(List.copyOf(names));
        });
        List<String> effects = new java.util.ArrayList<>();
        json.getAsJsonArray("effects").forEach(value -> effects.add(value.getAsString()));
        return new ProgressEntry(json.get("id").getAsString(),
                ComponentSerialization.CODEC.parse(ops, json.get("title")).getOrThrow(),
                ComponentSerialization.CODEC.parse(ops, json.get("description")).getOrThrow(),
                new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(json.get("icon").getAsString()))),
                json.get("done").getAsBoolean(), json.get("percent").getAsFloat(), criteria, requirements, effects);
    }

    public int completedGroups() {
        return (int) requirements.stream().filter(group -> group.stream().anyMatch(name -> criteria.getOrDefault(name, false))).count();
    }

    public String progressText() {
        return completedGroups() + "/" + requirements.size() + "  ·  " + Math.round(percent * 100) + "%";
    }

    public String category() {
        String path = id.substring(id.indexOf(':') + 1);
        return id.startsWith("minecraft:") ? path.split("/")[0] : "modded";
    }
}
