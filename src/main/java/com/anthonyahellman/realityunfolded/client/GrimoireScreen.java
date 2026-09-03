package com.anthonyahellman.realityunfolded.client;

import com.anthonyahellman.realityunfolded.grimoire.GrimoireData;
import com.anthonyahellman.realityunfolded.network.GrimoireStatePacket;
import com.anthonyahellman.realityunfolded.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class GrimoireScreen extends Screen {
    private static final int PANEL_HEIGHT = 234;
    private final List<GrimoireData.SpellSlot> slots = new ArrayList<>();
    private final List<Button> slotButtons = new ArrayList<>();
    private int selectedSlot;
    private int editingSlot;
    private String status;
    private int statusColor;
    private EditBox nameBox;
    private EditBox sourceBox;

    public GrimoireScreen(GrimoireStatePacket state) {
        super(Component.translatable("screen.reality_unfolded.grimoire"));
        applySnapshot(state);
        this.editingSlot = this.selectedSlot;
        setStatus(state.message(), state.valid());
    }

    @Override
    protected void init() {
        slotButtons.clear();
        int panelWidth = Math.min(520, width - 20);
        int panelX = (width - panelWidth) / 2;
        int panelY = Math.max(10, (height - PANEL_HEIGHT) / 2);
        int leftWidth = Math.min(150, Math.max(104, panelWidth / 3));
        int editorX = panelX + leftWidth + 18;
        int editorWidth = panelWidth - leftWidth - 28;

        for (int i = 0; i < GrimoireData.SLOT_COUNT; i++) {
            final int slot = i;
            Button button = Button.builder(Component.empty(), ignored -> editSlot(slot))
                .bounds(panelX + 10, panelY + 27 + i * 24, leftWidth - 10, 20).build();
            slotButtons.add(addRenderableWidget(button));
        }

        nameBox = new EditBox(font, editorX, panelY + 41, editorWidth, 20,
            Component.translatable("screen.reality_unfolded.spell_name"));
        nameBox.setMaxLength(GrimoireData.MAX_NAME_LENGTH);
        addRenderableWidget(nameBox);

        sourceBox = new EditBox(font, editorX, panelY + 78, editorWidth, 22,
            Component.translatable("screen.reality_unfolded.spell_source"));
        sourceBox.setMaxLength(GrimoireData.MAX_SOURCE_LENGTH);
        addRenderableWidget(sourceBox);

        int buttonY = panelY + 163;
        int half = Math.max(70, (editorWidth - 6) / 2);
        addRenderableWidget(Button.builder(Component.literal("Validate"), ignored -> validateOnServer())
            .bounds(editorX, buttonY, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), ignored -> save(false))
            .bounds(editorX + half + 6, buttonY, editorWidth - half - 6, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Select Saved"), ignored -> selectSaved())
            .bounds(editorX, buttonY + 23, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save + Select"), ignored -> save(true))
            .bounds(editorX + half + 6, buttonY + 23, editorWidth - half - 6, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cast Selected"), ignored -> castSelected())
            .bounds(editorX, buttonY + 46, editorWidth, 20).build());

        loadEditingSlot();
        refreshSlotLabels();
        setInitialFocus(sourceBox);
    }

    private void editSlot(int slot) {
        editingSlot = slot;
        loadEditingSlot();
        setStatus(slot == selectedSlot ? "Currently selected for casting." : "Editing slot " + (slot + 1) + ".", true);
    }

    private void loadEditingSlot() {
        if (nameBox == null || sourceBox == null || slots.isEmpty()) return;
        GrimoireData.SpellSlot slot = slots.get(editingSlot);
        nameBox.setValue(slot.name());
        sourceBox.setValue(slot.source());
    }

    private void validateOnServer() {
        ModNetwork.validateDraft(sourceBox.getValue());
        setStatus("Validating with server...", true);
    }

    private void save(boolean select) {
        ModNetwork.saveSlot(editingSlot, nameBox.getValue(), sourceBox.getValue(), select);
        setStatus("Validating with server...", true);
    }

    private void selectSaved() {
        ModNetwork.selectSavedSlot(editingSlot);
        setStatus("Selecting saved spell on server...", true);
    }

    private void castSelected() {
        ModNetwork.castSelected();
        setStatus("Casting selected spell on server...", true);
    }

    public void acceptServerState(GrimoireStatePacket state) {
        applySnapshot(state);
        if (editingSlot >= slots.size()) editingSlot = selectedSlot;
        loadEditingSlot();
        refreshSlotLabels();
        setStatus(state.message(), state.valid());
    }

    public void acceptServerFeedback(String message, boolean valid) {
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
            String prefix = i == selectedSlot ? "▶ " : "  ";
            String name = slots.get(i).name();
            if (name.length() > 16) name = name.substring(0, 15) + "…";
            slotButtons.get(i).setMessage(Component.literal(prefix + (i + 1) + ". " + name));
        }
    }

    private void setStatus(String message, boolean valid) {
        status = message;
        statusColor = valid ? 0xFF69E6A6 : 0xFFFF6B6B;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(520, width - 20);
        int panelX = (width - panelWidth) / 2;
        int panelY = Math.max(10, (height - PANEL_HEIGHT) / 2);
        int leftWidth = Math.min(150, Math.max(104, panelWidth / 3));
        int editorX = panelX + leftWidth + 18;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + PANEL_HEIGHT, 0xF0151024);
        graphics.fill(panelX + 4, panelY + 4, panelX + panelWidth - 4, panelY + PANEL_HEIGHT - 4, 0xFF211832);
        graphics.fill(panelX + leftWidth + 8, panelY + 22, panelX + leftWidth + 10,
            panelY + PANEL_HEIGHT - 12, 0xFF8D5CFF);
        graphics.drawCenteredString(font, title, width / 2, panelY + 10, 0xFFE9DEFF);
        graphics.drawString(font, "Player Spell Library", panelX + 10, panelY + 18, 0xFFBBA7DC, false);
        graphics.drawString(font, "Spell name", editorX, panelY + 29, 0xFFD8CAE9, false);
        graphics.drawString(font, "Ordered spell words", editorX, panelY + 66, 0xFFD8CAE9, false);
        graphics.drawString(font, "BOLT  BREAK  IGNITE  IMPACT", editorX, panelY + 103,
            0xFFBBA7DC, false);
        graphics.drawString(font, "EXPLOSION  AMPLIFY  SPLIT(n)", editorX, panelY + 114,
            0xFFBBA7DC, false);
        graphics.drawString(font, "LINK  HOME", editorX, panelY + 125, 0xFFBBA7DC, false);
        graphics.drawString(font, "Use spaces or -> between words.", editorX, panelY + 137,
            0xFFAFA2C4, false);
        graphics.drawString(font, status == null ? "" : status, editorX, panelY + 150, statusColor, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
