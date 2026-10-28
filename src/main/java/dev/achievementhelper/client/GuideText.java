package dev.achievementhelper.client;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

final class GuideText {
    static Component guide(ProgressEntry entry) {
        String key = "guide.achievement_helper." + entry.id().replace(':', '.').replace('/', '.');
        return Component.translatable(I18n.exists(key) ? key : "ah.guide.fallback");
    }

    static Component criterion(String advancement, String raw) {
        if (raw.startsWith("armor_trimmed_minecraft:") && raw.endsWith("_smithing_trim")) {
            String template = raw.substring("armor_trimmed_minecraft:".length(), raw.length() - "_smithing_trim".length());
            return Component.translatable("item.minecraft." + template);
        }
        Identifier id = Identifier.tryParse(raw);
        String path = id == null ? raw : id.getPath();
        String namespace = id == null ? "minecraft" : id.getNamespace();
        if (advancement.endsWith("whole_pack")) {
            String wolfKey = "criterion.achievement_helper.wolf." + path;
            if (I18n.exists(wolfKey)) return Component.translatable(wolfKey);
        }
        String[] kinds = advancement.endsWith("adventuring_time") || advancement.endsWith("explore_nether")
                ? new String[]{"biome"} : new String[]{"entity", "item", "block", "effect"};
        for (String kind : kinds) {
            String key = kind + "." + namespace + "." + path.replace('/', '.');
            if (I18n.exists(key)) return Component.translatable(key);
        }
        String key = "criterion.achievement_helper." + path.replace(':', '.').replace('/', '.');
        if (I18n.exists(key)) return Component.translatable(key);
        String display = path.replace('_', ' ').replace('/', ' ');
        return Component.literal(display.isEmpty() ? raw : Character.toUpperCase(display.charAt(0)) + display.substring(1));
    }

    static String wiki(ProgressEntry entry) {
        if (entry.category().equals("modded")) return "https://minecraft.wiki/w/Advancement";
        String tab = switch (entry.category()) {
            case "story" -> "Minecraft";
            case "nether" -> "Nether";
            case "end" -> "The_End";
            case "husbandry" -> "Husbandry";
            default -> "Adventure";
        };
        return "https://minecraft.wiki/w/Tutorial:Advancement_guide/" + tab + "_tab";
    }
}
