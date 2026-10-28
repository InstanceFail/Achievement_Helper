package dev.achievementhelper.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class HelperScreen extends Screen {
    private static final String[] CATEGORIES = {"all", "story", "nether", "end", "adventure", "husbandry", "modded"};
    private final List<Button> rows = new ArrayList<>();
    private EditBox search;
    private Button statusButton, categoryButton, trackButton, checklistButton;
    private int x, y, w, h, split, top, bottom, offset, detailScroll, detailHeight;
    private int status, category, checklist;
    private String selected;
    private long revision = -1;
    private List<ProgressEntry> filtered = List.of();

    public HelperScreen() { super(tr("title")); }
    static Component tr(String key, Object... args) { return Component.translatable("ah." + key, args); }

    @Override protected void init() {
        String query = search == null ? "" : search.getValue();
        w = Math.min(850, width - 16); h = Math.min(520, height - 16);
        x = (width - w) / 2; y = (height - h) / 2;
        split = x + (int) (w * 0.43); top = y + 86; bottom = y + h - 32;
        addRenderableWidget(Button.builder(tr("hud.settings"), b -> minecraft.setScreen(new HudSettingsScreen(this)))
                .bounds(x + w - 112, y + 7, 102, 20).build());
        search = new EditBox(font, x + 10, y + 57, Math.max(65, (split - x) - 20), 20, tr("search"));
        search.setMaxLength(120); search.setHint(tr("search")); search.setValue(query);
        search.setResponder(value -> { offset = 0; refreshRows(); });
        addRenderableWidget(search);
        int filterWidth = (x + w - split - 25) / 2;
        statusButton = addRenderableWidget(Button.builder(tr("status." + status), b -> {
            status = (status + 1) % 4; offset = 0; b.setMessage(tr("status." + status)); refreshRows();
        }).bounds(split + 5, y + 57, filterWidth, 20).build());
        categoryButton = addRenderableWidget(Button.builder(tr("category." + CATEGORIES[category]), b -> {
            category = (category + 1) % CATEGORIES.length; offset = 0;
            b.setMessage(tr("category." + CATEGORIES[category])); refreshRows();
        }).bounds(split + filterWidth + 10, y + 57, filterWidth, 20).build());
        int footerWidth = Math.max(44, (split - x - 24) / 3);
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { offset = Math.max(0, offset - visibleRows()); refreshRows(); })
                .bounds(x + 10, bottom + 7, footerWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            offset = Math.min(Math.max(0, filtered.size() - visibleRows()), offset + visibleRows()); refreshRows();
        }).bounds(x + 14 + footerWidth, bottom + 7, footerWidth, 20).build());
        addRenderableWidget(Button.builder(tr("close"), b -> onClose())
                .bounds(x + 18 + footerWidth * 2, bottom + 7, footerWidth, 20).build());
        int rightWidth = x + w - split - 20;
        trackButton = addRenderableWidget(Button.builder(tr("track"), b -> {
            var e = selectedEntry(); if (e != null) ProgressStore.toggle(e); refreshRows();
        }).bounds(split + 10, bottom + 7, rightWidth / 2 - 3, 20).build());
        addRenderableWidget(Button.builder(tr("wiki"), b -> {
            var e = selectedEntry(); if (e != null) ConfirmLinkScreen.confirmLinkNow(this, GuideText.wiki(e));
        }).bounds(split + 14 + rightWidth / 2, bottom + 7, rightWidth / 2 - 3, 20).build());
        checklistButton = addRenderableWidget(Button.builder(tr("checklist." + checklist), b -> {
            checklist = (checklist + 1) % 3; detailScroll = 0; b.setMessage(tr("checklist." + checklist));
        }).bounds(split + 10, top + 25, rightWidth, 20).build());
        refreshRows();
    }

    private int visibleRows() { return Math.max(1, (bottom - top) / 32); }
    private ProgressEntry selectedEntry() { return ProgressStore.ENTRIES.get(selected); }

    private void refreshRows() {
        if (search == null) return;
        rows.forEach(this::removeWidget); rows.clear();
        String query = search.getValue().toLowerCase(Locale.ROOT).trim();
        filtered = ProgressStore.ENTRIES.values().stream()
                .filter(e -> query.isEmpty() || e.title().getString().toLowerCase(Locale.ROOT).contains(query) || e.id().contains(query))
                .filter(e -> status == 0 || status == 1 && !e.done() || status == 2 && e.done() || status == 3 && ProgressStore.TRACKED.contains(e.id()))
                .filter(e -> category == 0 || e.category().equals(CATEGORIES[category]))
                .sorted(Comparator.comparing((ProgressEntry e) -> e.category()).thenComparing(e -> e.title().getString())).toList();
        if (filtered.stream().noneMatch(e -> e.id().equals(selected))) {
            selected = filtered.isEmpty() ? null : filtered.getFirst().id(); detailScroll = 0;
        }
        offset = Math.clamp(offset, 0, Math.max(0, filtered.size() - visibleRows()));
        for (int i = 0; i < visibleRows() && offset + i < filtered.size(); i++) {
            ProgressEntry e = filtered.get(offset + i);
            Button row = new AdvancementRow(x + 10, top + i * 32, split - x - 20, e, e.id().equals(selected),
                    b -> { selected = e.id(); detailScroll = 0; refreshRows(); });
            row.setTooltip(net.minecraft.client.gui.components.Tooltip.create(e.title().copy().append("\n" + e.progressText())));
            rows.add(addRenderableWidget(row));
        }
        var e = selectedEntry();
        if (trackButton != null) {
            boolean tracked = e != null && ProgressStore.TRACKED.contains(e.id());
            trackButton.setMessage(tr(tracked ? "untrack" : ProgressStore.TRACKED.size() >= 5 ? "track_limit" : "track"));
            trackButton.active = e != null && !e.done() && (tracked || ProgressStore.TRACKED.size() < 5);
        }
        revision = ProgressStore.revision;
    }

    @Override public void tick() { if (revision != ProgressStore.revision) refreshRows(); }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // Bevel and inset panels echo the vanilla inventory and advancement windows.
        g.fill(x, y, x + w, y + h, 0xFF373737);
        g.fill(x + 1, y + 1, x + w - 2, y + h - 2, 0xFFC6C6C6);
        g.horizontalLine(x + 1, x + w - 2, y + 1, 0xFFFFFFFF);
        g.verticalLine(x + 1, y + 1, y + h - 2, 0xFFFFFFFF);
        g.fill(x + 7, top - 4, split - 4, bottom + 2, 0xFF252525);
        g.fill(split + 5, top - 4, x + w - 7, bottom + 2, 0xFF191919);
        g.text(font, title, x + 10, y + 9, 0xFF3F3F3F, false);
        long done = ProgressStore.ENTRIES.values().stream().filter(ProgressEntry::done).count();
        int total = ProgressStore.ENTRIES.size();
        float ratio = total == 0 ? 0 : (float) done / total;
        g.text(font, tr("overall", done, total, Math.round(ratio * 100)), x + 10, y + 24, 0xFF3F3F3F, false);
        bar(g, x + 10, y + 41, w - 20, ratio);
        if (!ProgressStore.ready) {
            g.textWithWordWrap(font, tr("no_sync"), x + 15, top + 15, split - x - 30, 0xFFFFFFFF);
        } else if (filtered.isEmpty()) {
            g.textWithWordWrap(font, tr("empty"), x + 15, top + 15, split - x - 30, 0xFFFFFFFF);
        }
        ProgressEntry e = selectedEntry();
        checklistButton.visible = e != null;
        if (e != null) renderDetail(g, e, mouseX, mouseY);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void renderDetail(GuiGraphicsExtractor g, ProgressEntry e, int mx, int my) {
        int left = split + 12, right = x + w - 15, cw = right - left;
        g.item(e.icon(), left, top);
        g.text(font, font.plainSubstrByWidth(e.title().getString(), cw - 24), left + 23, top + 1, 0xFFFFFFFF);
        g.text(font, e.progressText(), left + 23, top + 13, 0xFFB5D87F);
        if (mx >= left && mx <= right && my >= top && my < top + 22) g.setTooltipForNextFrame(e.title(), mx, my);
        int viewTop = top + 52, viewHeight = bottom - viewTop - 4;
        if (viewHeight <= 0) return;
        detailScroll = Math.clamp(detailScroll, 0, Math.max(0, detailHeight - viewHeight));
        g.enableScissor(left, viewTop, right, bottom - 3);
        int cy = viewTop - detailScroll;
        cy = paragraph(g, e.description(), left, cy, cw - 6, 0xFFCCCCCC) + 8;
        cy = paragraph(g, tr("how"), left, cy, cw - 6, 0xFFE5C56E) + 4;
        cy = paragraph(g, GuideText.guide(e), left, cy, cw - 6, 0xFFFFFFFF) + 10;
        if (!e.effects().isEmpty()) {
            cy = paragraph(g, tr("simultaneous_effects"), left, cy, cw - 6, 0xFFE5C56E) + 4;
            for (String effect : e.effects()) {
                cy = paragraph(g, Component.literal("• ").append(Component.translatable("effect." + effect.replace(':', '.'))),
                        left, cy, cw - 6, 0xFFCCCCCC) + 3;
            }
            cy += 5;
        }
        cy = paragraph(g, tr("requirements"), left, cy, cw - 6, 0xFFE5C56E) + 4;
        cy = paragraph(g, tr("requirements_help"), left, cy, cw - 6, 0xFFAAAAAA) + 7;
        int index = 1;
        for (var group : e.requirements()) {
            boolean groupDone = group.stream().anyMatch(name -> e.criteria().getOrDefault(name, false));
            if (group.size() > 1) {
                cy = paragraph(g, tr("any_of", index, groupDone ? tr("done") : tr("remaining")), left, cy, cw - 6, 0xFFD1BD86) + 3;
            }
            for (String criterion : group) {
                boolean complete = e.criteria().getOrDefault(criterion, false);
                if (checklist == 1 && complete || checklist == 2 && !complete) continue;
                Component name = e.criteria().size() == 1 && e.id().startsWith("minecraft:")
                        ? Component.translatable("criterion.achievement_helper.task") : GuideText.criterion(e.id(), criterion);
                var line = Component.literal(complete ? "✓ " : "□ ").append(name);
                cy = paragraph(g, line, left + (group.size() > 1 ? 6 : 0), cy, cw - 13, complete ? 0xFF90C96C : 0xFFE0E0E0) + 4;
            }
            index++;
        }
        detailHeight = cy - (viewTop - detailScroll) + 5;
        g.disableScissor();
        if (detailHeight > viewHeight) {
            int thumb = Math.max(12, viewHeight * viewHeight / detailHeight);
            int thumbY = viewTop + (viewHeight - thumb) * detailScroll / Math.max(1, detailHeight - viewHeight);
            g.fill(right - 2, viewTop, right, viewTop + viewHeight, 0xFF333333);
            g.fill(right - 2, thumbY, right, thumbY + thumb, 0xFFAAAAAA);
        }
    }

    private int paragraph(GuiGraphicsExtractor g, Component text, int left, int cy, int cw, int color) {
        for (var line : font.split(text, Math.max(20, cw))) { g.text(font, line, left, cy, color); cy += 11; }
        return cy;
    }

    static void bar(GuiGraphicsExtractor g, int x, int y, int width, float progress) {
        g.fill(x, y, x + width, y + 5, 0xFF101010);
        g.fill(x + 1, y + 1, x + 1 + Math.round((width - 2) * Math.clamp(progress, 0, 1)), y + 4, 0xFF80B644);
    }

    @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (my >= top && my < bottom) {
            if (mx >= split && mx < x + w) detailScroll = Math.max(0, detailScroll - (int) (dy * 24));
            else if (mx >= x && mx < split) { offset -= (int) dy; refreshRows(); }
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (!search.isFocused() && event.key() == GLFW.GLFW_KEY_PAGE_DOWN) { detailScroll += 80; return true; }
        if (!search.isFocused() && event.key() == GLFW.GLFW_KEY_PAGE_UP) { detailScroll = Math.max(0, detailScroll - 80); return true; }
        if (!search.isFocused() && HelperClient.OPEN.matches(event)) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override public boolean isPauseScreen() { return false; }
}
