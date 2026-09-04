package com.anthonyahellman.realityunfolded.client;

import com.anthonyahellman.realityunfolded.grimoire.GrimoireData;
import com.anthonyahellman.realityunfolded.network.GrimoireStatePacket;
import com.anthonyahellman.realityunfolded.network.ModNetwork;
import com.anthonyahellman.realityunfolded.spell.SpellValidationException;
import com.anthonyahellman.realityunfolded.spell.SpellWordPresentation;
import com.anthonyahellman.realityunfolded.spell.WordRegistry;
import com.anthonyahellman.realityunfolded.spell.programmer.VisualSpellDraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry-driven visual programmer. Raw source is compiler transport, never player input. */
public final class GrimoireScreen extends Screen {
    private static final int PANEL_HEIGHT = 236;
    private static final int VISIBLE_PROGRAM_NODES = 6;
    private final List<GrimoireData.SpellSlot> slots = new ArrayList<>();
    private final List<Button> slotButtons = new ArrayList<>();
    private final List<Button> programButtons = new ArrayList<>();
    private final Map<Button, SpellWordPresentation> glyphButtons = new LinkedHashMap<>();
    private int selectedSlot;
    private int editingSlot;
    private int selectedNode = -1;
    private int programScroll;
    private String status;
    private int statusColor;
    private int manifestations = -1;
    private VisualSpellDraft draft = new VisualSpellDraft();
    private EditBox nameBox;

    public GrimoireScreen(GrimoireStatePacket state) {
        super(Component.translatable("screen.reality_unfolded.grimoire"));
        applySnapshot(state);
        editingSlot = selectedSlot;
        manifestations = state.manifestations();
        setStatus(state.message(), state.valid());
    }

    @Override
    protected void init() {
        slotButtons.clear();
        programButtons.clear();
        glyphButtons.clear();
        Layout layout = layout();

        for (int i = 0; i < GrimoireData.SLOT_COUNT; i++) {
            final int slot = i;
            Button button = Button.builder(Component.empty(), ignored -> editSlot(slot))
                .bounds(layout.panelX + 7, layout.panelY + 29 + i * 22, layout.slotWidth - 9, 19).build();
            slotButtons.add(addRenderableWidget(button));
        }

        int glyphButtonWidth = (layout.glyphWidth - 5) / 2;
        List<SpellWordPresentation> presentations = WordRegistry.presentations();
        for (int i = 0; i < presentations.size(); i++) {
            SpellWordPresentation presentation = presentations.get(i);
            int column = i % 2;
            int row = i / 2;
            Button button = Button.builder(Component.literal(presentation.glyph() + " " + presentation.displayName()),
                ignored -> append(presentation))
                .bounds(layout.glyphX + column * (glyphButtonWidth + 5), layout.panelY + 42 + row * 24,
                    glyphButtonWidth, 20).build();
            glyphButtons.put(addRenderableWidget(button), presentation);
        }

        nameBox = new EditBox(font, layout.editorX, layout.panelY + 31,
            Math.max(80, layout.editorWidth), 20, Component.translatable("screen.reality_unfolded.spell_name"));
        nameBox.setMaxLength(GrimoireData.MAX_NAME_LENGTH);
        addRenderableWidget(nameBox);

        int programY = layout.panelY + 65;
        for (int i = 0; i < VISIBLE_PROGRAM_NODES; i++) {
            final int visibleIndex = i;
            Button node = Button.builder(Component.empty(), ignored -> selectVisibleNode(visibleIndex))
                .bounds(layout.editorX, programY + i * 20, Math.max(80, layout.editorWidth), 18).build();
            programButtons.add(addRenderableWidget(node));
        }

        int manipulationY = layout.panelY + 187;
        int third = Math.max(24, (layout.editorWidth - 8) / 3);
        addRenderableWidget(Button.builder(Component.literal("↑"), ignored -> moveSelected(-1))
            .bounds(layout.editorX, manipulationY, third, 18).build());
        addRenderableWidget(Button.builder(Component.literal("↓"), ignored -> moveSelected(1))
            .bounds(layout.editorX + third + 4, manipulationY, third, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Remove"), ignored -> removeSelected())
            .bounds(layout.editorX + (third + 4) * 2, manipulationY,
                layout.editorWidth - (third + 4) * 2, 18).build());

        int actionWidth = Math.max(20, (layout.editorWidth - 12) / 4);
        addRenderableWidget(Button.builder(Component.literal("Validate"), ignored -> validateOnServer())
            .bounds(layout.editorX, layout.panelY + 208, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), ignored -> save(false))
            .bounds(layout.editorX + actionWidth + 4, layout.panelY + 208, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Select"), ignored -> selectSaved())
            .bounds(layout.editorX + (actionWidth + 4) * 2, layout.panelY + 208, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save + Active"), ignored -> save(true))
            .bounds(layout.editorX + (actionWidth + 4) * 3, layout.panelY + 208,
                layout.editorWidth - (actionWidth + 4) * 3, 20).build());

        loadEditingSlot();
        refreshSlotLabels();
        refreshProgramButtons();
        setInitialFocus(nameBox);
    }

    private void append(SpellWordPresentation presentation) {
        if (draft.nodes().size() >= VisualSpellDraft.MAX_GLYPHS) {
            setStatus("INVALID: Maximum of " + VisualSpellDraft.MAX_GLYPHS + " glyphs reached.", false);
            return;
        }
        draft.append(presentation.id());
        selectedNode = draft.nodes().size() - 1;
        ensureSelectedVisible();
        draftChanged();
    }

    private void selectVisibleNode(int visibleIndex) {
        int index = programScroll + visibleIndex;
        if (index < draft.nodes().size()) selectedNode = index;
        refreshProgramButtons();
    }

    private void moveSelected(int offset) {
        selectedNode = draft.move(selectedNode, offset);
        ensureSelectedVisible();
        draftChanged();
    }

    private void removeSelected() {
        if (selectedNode < 0) return;
        draft.remove(selectedNode);
        selectedNode = draft.nodes().isEmpty() ? -1 : Math.min(selectedNode, draft.nodes().size() - 1);
        ensureSelectedVisible();
        draftChanged();
    }

    private void draftChanged() {
        manifestations = -1;
        refreshProgramButtons();
        validateOnServer();
    }

    private void editSlot(int slot) {
        editingSlot = slot;
        loadEditingSlot();
        setStatus(slot == selectedSlot ? "Currently selected for the Void Caster."
            : "Editing slot " + (slot + 1) + ".", true);
    }

    private void loadEditingSlot() {
        if (nameBox == null || slots.isEmpty()) return;
        GrimoireData.SpellSlot slot = slots.get(editingSlot);
        nameBox.setValue(slot.name());
        try {
            draft = VisualSpellDraft.fromSource(slot.source());
            selectedNode = draft.nodes().isEmpty() ? -1 : 0;
        } catch (SpellValidationException exception) {
            draft = new VisualSpellDraft();
            selectedNode = -1;
            setStatus("INVALID SAVED SPELL: " + exception.getMessage(), false);
        }
        programScroll = 0;
        refreshProgramButtons();
    }

    private void validateOnServer() {
        ModNetwork.validateDraft(draft.source());
        setStatus("Validating visual program with server...", true);
    }

    private void save(boolean select) {
        ModNetwork.saveSlot(editingSlot, nameBox.getValue(), draft.source(), select);
        setStatus("Validating and saving on server...", true);
    }

    private void selectSaved() {
        ModNetwork.selectSavedSlot(editingSlot);
        setStatus("Selecting saved spell on server...", true);
    }

    public void acceptServerState(GrimoireStatePacket state) {
        applySnapshot(state);
        if (editingSlot >= slots.size()) editingSlot = selectedSlot;
        manifestations = state.manifestations();
        loadEditingSlot();
        refreshSlotLabels();
        setStatus(state.message(), state.valid());
    }

    public void acceptServerFeedback(String message, boolean valid, int estimatedManifestations) {
        manifestations = estimatedManifestations;
        setStatus(message, valid);
    }

    private void applySnapshot(GrimoireStatePacket state) {
        slots.clear();
        slots.addAll(state.slots());
        while (slots.size() < GrimoireData.SLOT_COUNT) {
            int index = slots.size();
            slots.add(new GrimoireData.SpellSlot("Spell " + (index + 1), ""));
        }
        selectedSlot = Math.max(0, Math.min(GrimoireData.SLOT_COUNT - 1, state.selectedSlot()));
    }

    private void refreshSlotLabels() {
        for (int i = 0; i < slotButtons.size(); i++) {
            String prefix = i == selectedSlot ? "▶ " : "";
            String name = slots.get(i).name();
            if (name.length() > 13) name = name.substring(0, 12) + "…";
            slotButtons.get(i).setMessage(Component.literal(prefix + (i + 1) + ". " + name));
        }
    }

    private void refreshProgramButtons() {
        if (programButtons.isEmpty()) return;
        List<VisualSpellDraft.GlyphNode> nodes = draft.nodes();
        for (int i = 0; i < programButtons.size(); i++) {
            int index = programScroll + i;
            Button button = programButtons.get(i);
            button.visible = index < nodes.size();
            button.active = index < nodes.size();
            if (index < nodes.size()) {
                SpellWordPresentation presentation = WordRegistry.presentation(nodes.get(index).word());
                String marker = index == selectedNode ? "◆ " : "  ";
                button.setMessage(Component.literal(marker + (index + 1) + ". "
                    + presentation.glyph() + "  " + presentation.displayName()));
            }
        }
    }

    private void ensureSelectedVisible() {
        if (selectedNode < programScroll) programScroll = selectedNode;
        if (selectedNode >= programScroll + VISIBLE_PROGRAM_NODES) {
            programScroll = selectedNode - VISIBLE_PROGRAM_NODES + 1;
        }
        int maximum = Math.max(0, draft.nodes().size() - VISIBLE_PROGRAM_NODES);
        programScroll = Math.max(0, Math.min(maximum, programScroll));
    }

    private void setStatus(String message, boolean valid) {
        status = message;
        statusColor = valid ? 0xFF69E6A6 : 0xFFFF6B6B;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maximum = Math.max(0, draft.nodes().size() - VISIBLE_PROGRAM_NODES);
        if (maximum > 0) {
            programScroll = Math.max(0, Math.min(maximum, programScroll - (int) Math.signum(delta)));
            refreshProgramButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Layout layout = layout();
        graphics.fill(layout.panelX, layout.panelY, layout.panelX + layout.panelWidth,
            layout.panelY + PANEL_HEIGHT, 0xF0100B1C);
        graphics.fill(layout.panelX + 3, layout.panelY + 3, layout.panelX + layout.panelWidth - 3,
            layout.panelY + PANEL_HEIGHT - 3, 0xFF211832);
        graphics.fill(layout.panelX + layout.slotWidth + 5, layout.panelY + 22,
            layout.panelX + layout.slotWidth + 7, layout.panelY + PANEL_HEIGHT - 7, 0xFF7250C7);
        graphics.fill(layout.glyphX + layout.glyphWidth + 4, layout.panelY + 22,
            layout.glyphX + layout.glyphWidth + 6, layout.panelY + PANEL_HEIGHT - 7, 0xFF7250C7);
        graphics.drawCenteredString(font, title, width / 2, layout.panelY + 8, 0xFFE9DEFF);
        graphics.drawString(font, "SPELL LIBRARY", layout.panelX + 7, layout.panelY + 18, 0xFFBBA7DC, false);
        graphics.drawString(font, "GLYPHS", layout.glyphX, layout.panelY + 27, 0xFFBBA7DC, false);
        graphics.drawString(font, "CURRENT SPELL", layout.editorX, layout.panelY + 18, 0xFFBBA7DC, false);

        graphics.drawString(font, "Void Caster: " + slots.get(selectedSlot).name(), layout.glyphX,
            layout.panelY + 168, 0xFFD8CAE9, false);
        graphics.drawString(font, manifestations >= 0 ? "Draft manifestations: " + manifestations
            : "Draft manifestations: —", layout.glyphX, layout.panelY + 180, 0xFFAFA2C4, false);
        if (selectedNode >= 0 && selectedNode < draft.nodes().size()) {
            SpellWordPresentation selected = WordRegistry.presentation(draft.nodes().get(selectedNode).word());
            graphics.drawString(font, trim(selected.category() + ": " + selected.description(),
                layout.panelWidth - layout.slotWidth - 22), layout.glyphX, layout.panelY + 194,
                0xFFAFA2C4, false);
        }
        graphics.drawString(font, status == null ? "" : trim(status,
            layout.panelWidth - layout.slotWidth - 22), layout.glyphX, layout.panelY + 219,
            statusColor, false);

        super.render(graphics, mouseX, mouseY, partialTick);
        for (Map.Entry<Button, SpellWordPresentation> entry : glyphButtons.entrySet()) {
            if (entry.getKey().isMouseOver(mouseX, mouseY)) {
                SpellWordPresentation word = entry.getValue();
                graphics.renderTooltip(font, Component.literal(word.category() + " — " + word.description()),
                    mouseX, mouseY);
                break;
            }
        }
    }

    private Layout layout() {
        int panelWidth = Math.min(620, width - 12);
        int panelX = (width - panelWidth) / 2;
        int panelY = Math.max(2, (height - PANEL_HEIGHT) / 2);
        int slotWidth = Math.max(82, Math.min(116, panelWidth / 5));
        int glyphWidth = Math.max(94, Math.min(126, panelWidth / 4));
        int glyphX = panelX + slotWidth + 12;
        int editorX = glyphX + glyphWidth + 10;
        int editorWidth = panelX + panelWidth - editorX - 8;
        return new Layout(panelWidth, panelX, panelY, slotWidth, glyphWidth, glyphX, editorX, editorWidth);
    }

    private String trim(String text, int maximumWidth) {
        return font.width(text) <= maximumWidth ? text : font.plainSubstrByWidth(text, maximumWidth - 10) + "…";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Layout(int panelWidth, int panelX, int panelY, int slotWidth,
                          int glyphWidth, int glyphX, int editorX, int editorWidth) {}
}
