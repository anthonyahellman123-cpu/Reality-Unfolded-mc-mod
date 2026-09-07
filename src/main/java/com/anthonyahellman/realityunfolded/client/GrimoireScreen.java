package com.anthonyahellman.realityunfolded.client;

import com.anthonyahellman.realityunfolded.grimoire.GrimoireData;
import com.anthonyahellman.realityunfolded.network.GrimoireStatePacket;
import com.anthonyahellman.realityunfolded.network.ModNetwork;
import com.anthonyahellman.realityunfolded.spell.*;
import com.anthonyahellman.realityunfolded.spell.programmer.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/** Stable freeform graph editor foundation. Word behavior remains registry/runtime-owned. */
public final class GrimoireScreen extends Screen {
    private static final int PALETTE_WIDTH = 178;
    private static final int INSPECTOR_WIDTH = 212;
    private static final int HEADER_HEIGHT = 22;
    private static final int FOOTER_HEIGHT = 82;
    private static final int NODE_WIDTH = 106;
    private static final int NODE_HEIGHT = 34;
    private static final int PORT_RADIUS = 5;
    private static final int PALETTE_ROW_HEIGHT = 20;
    private static final int CATEGORY_ROW_HEIGHT = 15;
    private static final double MIN_ZOOM = 0.45D;
    private static final double MAX_ZOOM = 2.0D;

    private final List<GrimoireData.SpellSlot> slots = new ArrayList<>();
    private final List<Button> slotButtons = new ArrayList<>();
    private final List<Button> categoryButtons = new ArrayList<>();
    private final List<SpellWordPresentation> presentations = WordRegistry.presentations();
    private SpellGraphDraft graph = new SpellGraphDraft();
    private EditBox searchBox;
    private EditBox nameBox;
    private Button setRootButton;
    private Button parameterMinusButton;
    private Button parameterPlusButton;
    private Button deleteButton;
    private int selectedSlot;
    private int editingSlot;
    private int selectedNode = -1;
    private int paletteScroll;
    private String selectedCategory = "ALL";
    private String status = "Drag a glyph into the canvas. Reality resolves instructions at cast time.";
    private int statusColor = 0xFFC8B9DB;
    private double panX = 36.0D;
    private double panY = 28.0D;
    private double zoom = 1.0D;
    private SpellWordPresentation paletteDrag;
    private int draggingNode = -1;
    private double dragNodeOffsetX;
    private double dragNodeOffsetY;
    private boolean panning;
    private int pendingWireNode = -1;
    private SpellPort pendingWirePort;

    public GrimoireScreen(GrimoireStatePacket state) {
        super(Component.translatable("screen.reality_unfolded.grimoire"));
        applySnapshot(state);
        editingSlot = selectedSlot;
        status = state.message();
        statusColor = state.valid() ? 0xFF86D9B0 : 0xFFE4B06A;
    }

    @Override
    protected void init() {
        clearWidgets();
        slotButtons.clear();
        categoryButtons.clear();
        searchBox = new EditBox(font, 6, 18, PALETTE_WIDTH - 12, 18, Component.literal("Search words"));
        searchBox.setHint(Component.literal("Search words..."));
        searchBox.setResponder(ignored -> paletteScroll = 0);
        addRenderableWidget(searchBox);

        List<String> categories = categories();
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            Button button = Button.builder(Component.literal(displayCategory(category)), ignored -> {
                selectedCategory = category;
                paletteScroll = 0;
                refreshCategoryButtons();
            }).bounds(6, categoryListTop() + i * CATEGORY_ROW_HEIGHT,
                PALETTE_WIDTH - 12, CATEGORY_ROW_HEIGHT - 1).build();
            categoryButtons.add(addRenderableWidget(button));
        }

        int footerTop = footerTop();
        nameBox = new EditBox(font, 8, footerTop + 18,
            Math.min(224, Math.max(120, width - 500)), 18, Component.literal("Spell name"));
        nameBox.setMaxLength(GrimoireData.MAX_NAME_LENGTH);
        addRenderableWidget(nameBox);

        int slotWidth = Math.max(42, (width - 13) / GrimoireData.SLOT_COUNT - 2);
        for (int i = 0; i < GrimoireData.SLOT_COUNT; i++) {
            int slot = i;
            Button button = Button.builder(Component.empty(), ignored -> loadSlot(slot))
                .bounds(6 + i * (slotWidth + 2), height - 20, slotWidth, 16).build();
            slotButtons.add(addRenderableWidget(button));
        }

        int actionsX = width - 204;
        addRenderableWidget(Button.builder(Component.literal("Save"), ignored -> save(false))
            .bounds(actionsX, footerTop + 16, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save + Active"), ignored -> save(true))
            .bounds(actionsX + 78, footerTop + 16, 120, 20).build());

        int inspectorX = inspectorLeft() + 8;
        int inspectorActionsY = footerTop - 48;
        setRootButton = addRenderableWidget(Button.builder(Component.literal("Set as Root"), ignored -> setRoot())
            .bounds(inspectorX, inspectorActionsY, 92, 18).build());
        deleteButton = addRenderableWidget(Button.builder(Component.literal("Delete Node"), ignored -> deleteSelected())
            .bounds(width - 104, inspectorActionsY, 96, 18).build());
        parameterMinusButton = addRenderableWidget(Button.builder(Component.literal("Parameter −"), ignored -> adjustParameter(-1))
            .bounds(inspectorX, inspectorActionsY + 22, 92, 18).build());
        parameterPlusButton = addRenderableWidget(Button.builder(Component.literal("Parameter +"), ignored -> adjustParameter(1))
            .bounds(width - 104, inspectorActionsY + 22, 96, 18).build());

        loadEditingSlot();
        refreshSlotButtons();
        refreshCategoryButtons();
        updateInspectorControls();
    }

    private List<String> categories() {
        Set<String> values = new LinkedHashSet<>();
        values.add("ALL");
        presentations.forEach(word -> values.add(word.category()));
        return List.copyOf(values);
    }

    private String displayCategory(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return normalized.isEmpty() ? normalized
            : Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private void refreshCategoryButtons() {
        List<String> categories = categories();
        for (int i = 0; i < categoryButtons.size(); i++) {
            boolean selected = categories.get(i).equals(selectedCategory);
            categoryButtons.get(i).setMessage(Component.literal((selected ? "◆ " : "")
                + displayCategory(categories.get(i))));
        }
    }

    private List<SpellWordPresentation> filteredWords() {
        String query = searchBox == null ? "" : searchBox.getValue().strip().toLowerCase(Locale.ROOT);
        return presentations.stream()
            .filter(word -> selectedCategory.equals("ALL") || word.category().equals(selectedCategory))
            .filter(word -> query.isEmpty() || (word.displayName() + " " + word.category() + " "
                + word.subcategory() + " " + word.description()).toLowerCase(Locale.ROOT).contains(query))
            .sorted(Comparator.comparing(SpellWordPresentation::category)
                .thenComparing(SpellWordPresentation::subcategory)
                .thenComparing(SpellWordPresentation::displayName)).toList();
    }

    private int categoryListTop() { return 53; }
    private int paletteListTop() { return categoryListTop() + categories().size() * CATEGORY_ROW_HEIGHT + 18; }
    private int footerTop() { return height - FOOTER_HEIGHT; }
    private int inspectorLeft() { return Math.max(PALETTE_WIDTH + 240, width - INSPECTOR_WIDTH); }
    private int paletteVisibleRows() { return Math.max(1, (footerTop() - paletteListTop() - 4) / PALETTE_ROW_HEIGHT); }

    private void loadSlot(int slot) {
        editingSlot = slot;
        loadEditingSlot();
        status = slot == selectedSlot ? "Editing the active spell." : "Editing saved slot " + (slot + 1) + ".";
        statusColor = 0xFFC8B9DB;
    }

    private void loadEditingSlot() {
        if (slots.isEmpty() || nameBox == null) return;
        GrimoireData.SpellSlot slot = slots.get(editingSlot);
        nameBox.setValue(slot.name());
        try {
            graph = !slot.graph().isBlank() ? SpellGraphCodec.decode(slot.graph())
                : slot.source().isBlank() ? new SpellGraphDraft()
                : SpellGraphCodec.fromProgram(SpellParser.parse(slot.source()));
        } catch (SpellValidationException exception) {
            graph = new SpellGraphDraft();
            status = "Stored instructions could not be opened safely: " + exception.getMessage();
            statusColor = 0xFFFF7474;
        }
        selectedNode = -1;
        pendingWireNode = -1;
        panX = 36;
        panY = 28;
        zoom = 1.0D;
        updateInspectorControls();
    }

    private void save(boolean select) {
        try {
            ModNetwork.saveGraph(editingSlot, nameBox.getValue(), SpellGraphCodec.encode(graph), select);
            status = "Saving graph instructions on the server...";
            statusColor = 0xFFC8B9DB;
        } catch (IllegalStateException exception) {
            status = "Graph safety limit reached: " + exception.getMessage();
            statusColor = 0xFFFF7474;
        }
    }

    private void setRoot() {
        if (graph.node(selectedNode) != null) graph.setRootId(selectedNode);
        updateInspectorControls();
    }

    private void adjustParameter(int direction) {
        graph.adjustParameter(selectedNode, direction);
        updateInspectorControls();
    }

    private void deleteSelected() {
        if (selectedNode < 0) return;
        graph.remove(selectedNode);
        selectedNode = -1;
        pendingWireNode = -1;
        updateInspectorControls();
    }

    private void updateInspectorControls() {
        if (setRootButton == null) return;
        SpellGraphDraft.Node node = graph.node(selectedNode);
        boolean selected = node != null;
        boolean parameterized = selected && WordRegistry.presentation(node.word()).hasPlayerParameter();
        setRootButton.visible = selected;
        setRootButton.active = selected && graph.rootId() != selectedNode;
        setRootButton.setMessage(Component.literal(graph.rootId() == selectedNode ? "Root Node" : "Set as Root"));
        deleteButton.visible = selected;
        parameterMinusButton.visible = parameterized;
        parameterPlusButton.visible = parameterized;
    }

    public void acceptServerState(GrimoireStatePacket state) {
        applySnapshot(state);
        status = state.message();
        statusColor = state.valid() ? 0xFF86D9B0 : 0xFFFF7474;
        loadEditingSlot();
        refreshSlotButtons();
    }

    public void acceptServerFeedback(String message, boolean valid, int ignoredManifestations) {
        status = message;
        statusColor = valid ? 0xFF86D9B0 : 0xFFFF7474;
    }

    private void applySnapshot(GrimoireStatePacket state) {
        slots.clear();
        slots.addAll(state.slots());
        while (slots.size() < GrimoireData.SLOT_COUNT) {
            int index = slots.size();
            slots.add(new GrimoireData.SpellSlot("Spell " + (index + 1), "", ""));
        }
        selectedSlot = Math.max(0, Math.min(GrimoireData.SLOT_COUNT - 1, state.selectedSlot()));
    }

    private void refreshSlotButtons() {
        for (int i = 0; i < slotButtons.size(); i++) {
            String name = slots.get(i).name();
            if (name.length() > 10) name = name.substring(0, 9) + "…";
            slotButtons.get(i).setMessage(Component.literal((i == selectedSlot ? "◆ " : "") + (i + 1) + " " + name));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            SpellWordPresentation paletteWord = paletteWordAt(mouseX, mouseY);
            if (paletteWord != null) {
                paletteDrag = paletteWord;
                return true;
            }
            if (insideCanvas(mouseX, mouseY)) {
                PortHit port = outputPortAt(mouseX, mouseY);
                if (port != null) {
                    pendingWireNode = port.nodeId();
                    pendingWirePort = port.port();
                    return true;
                }
                int input = inputNodeAt(mouseX, mouseY);
                if (pendingWireNode >= 0 && input >= 0) {
                    graph.connect(pendingWireNode, pendingWirePort, input);
                    pendingWireNode = -1;
                    return true;
                }
                int node = nodeAt(mouseX, mouseY);
                selectedNode = node;
                updateInspectorControls();
                if (node >= 0) {
                    SpellGraphDraft.Node value = graph.node(node);
                    dragNodeOffsetX = screenToWorldX(mouseX) - value.x();
                    dragNodeOffsetY = screenToWorldY(mouseY) - value.y();
                    draggingNode = node;
                } else pendingWireNode = -1;
                return true;
            }
        }
        if ((button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
            && insideCanvas(mouseX, mouseY)) {
            panning = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingNode >= 0 && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            graph.move(draggingNode, (int) Math.round(screenToWorldX(mouseX) - dragNodeOffsetX),
                (int) Math.round(screenToWorldY(mouseY) - dragNodeOffsetY));
            return true;
        }
        if (panning) {
            panX += dragX;
            panY += dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (paletteDrag != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (insideCanvas(mouseX, mouseY)) {
                SpellGraphDraft.Node added = graph.add(paletteDrag.id(),
                    (int) Math.round(screenToWorldX(mouseX) - NODE_WIDTH / 2.0D),
                    (int) Math.round(screenToWorldY(mouseY) - NODE_HEIGHT / 2.0D));
                if (added != null) selectedNode = added.id();
                else {
                    status = "The graph reached its node safety limit.";
                    statusColor = 0xFFFF7474;
                }
            }
            updateInspectorControls();
            paletteDrag = null;
            return true;
        }
        draggingNode = -1;
        panning = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < PALETTE_WIDTH && mouseY >= paletteListTop() && mouseY < footerTop()) {
            int maximum = Math.max(0, filteredWords().size() - paletteVisibleRows());
            paletteScroll = Math.max(0, Math.min(maximum, paletteScroll - (int) Math.signum(delta)));
            return true;
        }
        if (insideCanvas(mouseX, mouseY)) {
            double oldWorldX = screenToWorldX(mouseX);
            double oldWorldY = screenToWorldY(mouseY);
            zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * (delta > 0 ? 1.12D : 0.89D)));
            panX = mouseX - PALETTE_WIDTH - oldWorldX * zoom;
            panY = mouseY - HEADER_HEIGHT - oldWorldY * zoom;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE)
            && !searchBox.isFocused() && !nameBox.isFocused()) {
            deleteSelected();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && pendingWireNode >= 0) {
            pendingWireNode = -1;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int footer = footerTop();
        int inspector = inspectorLeft();
        graphics.fill(0, 0, width, height, 0xF00C0815);
        graphics.fill(0, 0, PALETTE_WIDTH, footer, 0xFF1A1228);
        graphics.fill(PALETTE_WIDTH, 0, inspector, footer, 0xFF100B19);
        graphics.fill(inspector, 0, width, footer, 0xFF181022);
        drawGrid(graphics, footer);
        graphics.fill(0, footer, width, height, 0xFF21162F);
        graphics.fill(PALETTE_WIDTH - 1, 0, PALETTE_WIDTH + 1, footer, 0xFF6842A8);
        graphics.fill(inspector - 1, 0, inspector + 1, footer, 0xFF6842A8);
        graphics.fill(0, footer - 1, width, footer + 1, 0xFF6842A8);
        graphics.drawCenteredString(font, "SPELL CANVAS", (PALETTE_WIDTH + inspector) / 2, 7, 0xFFD8C8EF);
        graphics.drawString(font, "SEARCH", 6, 6, 0xFFBBA7DC, false);
        graphics.drawString(font, "CATEGORIES", 6, categoryListTop() - 11, 0xFFBBA7DC, false);
        graphics.drawString(font, "WORDS", 6, paletteListTop() - 11, 0xFFBBA7DC, false);
        for (int i = 0; i < categoryButtons.size(); i++) {
            if (categories().get(i).equals(selectedCategory)) {
                int y = categoryListTop() + i * CATEGORY_ROW_HEIGHT;
                graphics.fill(2, y, 5, y + CATEGORY_ROW_HEIGHT - 1, 0xFFB882FF);
            }
        }
        drawPalette(graphics, mouseX, mouseY);
        graphics.enableScissor(PALETTE_WIDTH + 1, HEADER_HEIGHT, inspector - 1, footer - 1);
        drawConnections(graphics);
        drawPendingWire(graphics, mouseX, mouseY);
        drawNodes(graphics, mouseX, mouseY);
        graphics.disableScissor();
        drawInspector(graphics);
        drawFooter(graphics);
        updateInspectorControls();
        super.render(graphics, mouseX, mouseY, partialTick);
        SpellWordPresentation hoveredWord = paletteWordAt(mouseX, mouseY);
        if (hoveredWord != null && paletteDrag == null) {
            graphics.renderTooltip(font, Component.literal(hoveredWord.category() + " / "
                + hoveredWord.subcategory() + " — " + hoveredWord.description()), mouseX, mouseY);
        }
        if (paletteDrag != null) {
            graphics.fill(mouseX - 48, mouseY - 10, mouseX + 48, mouseY + 10, 0xEE3C285B);
            graphics.drawCenteredString(font, paletteDrag.glyph() + " " + paletteDrag.displayName(), mouseX,
                mouseY - 4, 0xFFFFFFFF);
        }
    }

    private void drawGrid(GuiGraphics graphics, int footer) {
        int spacing = Math.max(12, (int) Math.round(32 * zoom));
        int startX = (int) ((PALETTE_WIDTH + panX) % spacing);
        int startY = (int) ((HEADER_HEIGHT + panY) % spacing);
        for (int x = startX; x < inspectorLeft(); x += spacing) {
            if (x >= PALETTE_WIDTH) graphics.vLine(x, HEADER_HEIGHT, footer, 0x192D2141);
        }
        for (int y = startY; y < footer; y += spacing) {
            if (y >= HEADER_HEIGHT) graphics.hLine(PALETTE_WIDTH, inspectorLeft(), y, 0x192D2141);
        }
    }

    private void drawPalette(GuiGraphics graphics, int mouseX, int mouseY) {
        List<SpellWordPresentation> filtered = filteredWords();
        for (int visible = 0; visible < paletteVisibleRows(); visible++) {
            int index = paletteScroll + visible;
            if (index >= filtered.size()) break;
            SpellWordPresentation word = filtered.get(index);
            int y = paletteListTop() + visible * PALETTE_ROW_HEIGHT;
            boolean hovered = mouseX >= 5 && mouseX < PALETTE_WIDTH - 5 && mouseY >= y && mouseY < y + 19;
            graphics.fill(5, y, PALETTE_WIDTH - 5, y + 19, hovered ? 0xFF4C326C : 0xFF2B1D3E);
            graphics.drawString(font, word.glyph() + " " + word.displayName(), 10, y + 5, 0xFFF0E8FF, false);
            graphics.drawString(font, trim(word.subcategory(), 70), PALETTE_WIDTH - 75, y + 5, 0xFF9D89BC, false);
        }
    }

    private void drawConnections(GuiGraphics graphics) {
        for (SpellGraphDraft.Edge edge : graph.edges()) {
            SpellGraphDraft.Node from = graph.node(edge.from());
            SpellGraphDraft.Node to = graph.node(edge.to());
            if (from == null || to == null) continue;
            Point start = outputPoint(from, edge.port());
            Point end = inputPoint(to);
            int color = portColor(edge.port());
            drawWire(graphics, start.x(), start.y(), end.x(), end.y(), color);
        }
    }

    private void drawPendingWire(GuiGraphics graphics, int mouseX, int mouseY) {
        SpellGraphDraft.Node source = graph.node(pendingWireNode);
        if (source == null || pendingWirePort == null) return;
        Point start = outputPoint(source, pendingWirePort);
        drawWire(graphics, start.x(), start.y(), mouseX, mouseY, 0xFFE8CFFF);
    }

    private void drawWire(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int middle = (x1 + x2) / 2;
        line(graphics, x1, y1, middle, y1, color);
        line(graphics, middle, y1, middle, y2, color);
        line(graphics, middle, y2, x2, y2, color);
    }

    private void line(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (x1 == x2) graphics.fill(x1 - 1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2) + 1, color);
        else graphics.fill(Math.min(x1, x2), y1 - 1, Math.max(x1, x2) + 1, y1 + 1, color);
    }

    private void drawNodes(GuiGraphics graphics, int mouseX, int mouseY) {
        for (SpellGraphDraft.Node node : graph.nodes()) {
            int x = worldToScreenX(node.x());
            int y = worldToScreenY(node.y());
            int w = scaled(NODE_WIDTH);
            int h = scaled(NODE_HEIGHT);
            graphics.fill(x, y, x + w, y + h, node.id() == selectedNode ? 0xFF5A367C : 0xFF302044);
            graphics.fill(x, y, x + w, y + 2, node.id() == graph.rootId() ? 0xFFFFCC67 : 0xFF8B62C7);
            SpellWordPresentation presentation = WordRegistry.presentation(node.word());
            graphics.drawString(font, presentation.glyph() + " " + trim(presentation.displayName(), w - 20),
                x + 7, y + 7, 0xFFF3ECFF, false);
            String detail = presentation.hasPlayerParameter() ? node.integerArgument() + " " + presentation.parameter().unit()
                : presentation.subcategory();
            graphics.drawString(font, trim(detail, w - 16), x + 7, y + 20, 0xFFB7A5CE, false);
            Point input = inputPoint(node);
            graphics.fill(input.x() - PORT_RADIUS, input.y() - PORT_RADIUS, input.x() + PORT_RADIUS,
                input.y() + PORT_RADIUS, 0xFF7B6B91);
            for (SpellPort port : presentation.outputs()) {
                Point output = outputPoint(node, port);
                int color = portColor(port);
                graphics.fill(output.x() - PORT_RADIUS, output.y() - PORT_RADIUS,
                    output.x() + PORT_RADIUS, output.y() + PORT_RADIUS, color);
                String label = port == SpellPort.TRUE ? "T" : port == SpellPort.FALSE ? "F" : "";
                if (!label.isEmpty()) {
                    graphics.drawString(font, label, output.x() - 12, output.y() - 4, color, false);
                }
            }
        }
    }

    private int portColor(SpellPort port) {
        return port == SpellPort.FALSE ? 0xFFFF6B6B
            : port == SpellPort.TRUE ? 0xFF55E6A5 : 0xFFB882FF;
    }

    private void drawInspector(GuiGraphics graphics) {
        int left = inspectorLeft();
        int textX = left + 10;
        graphics.drawString(font, "NODE DETAILS", textX, 8, 0xFFD8C8EF, false);
        SpellGraphDraft.Node node = graph.node(selectedNode);
        if (node == null) {
            graphics.drawString(font, "No node selected.", textX, 31, 0xFF8F819F, false);
            return;
        }

        SpellWordPresentation word = WordRegistry.presentation(node.word());
        graphics.fill(left + 8, 25, width - 8, 54, 0xFF2B1D3E);
        graphics.drawString(font, word.glyph(), textX, 34, 0xFFCAA4FF, false);
        graphics.drawString(font, trim(word.displayName(), INSPECTOR_WIDTH - 42), textX + 22, 31,
            0xFFF3ECFF, false);
        graphics.drawString(font, displayCategory(word.category()) + " / " + displayCategory(word.subcategory()),
            textX + 22, 43, 0xFFAA98BD, false);

        int y = 65;
        for (var line : font.split(Component.literal(word.description()), INSPECTOR_WIDTH - 20)) {
            graphics.drawString(font, line, textX, y, 0xFFCFC2DC, false);
            y += 10;
        }
        y += 7;
        graphics.drawString(font, node.id() == graph.rootId() ? "ROOT: Yes" : "ROOT: No",
            textX, y, node.id() == graph.rootId() ? 0xFFFFCC67 : 0xFF9D8BAB, false);
        y += 14;
        if (word.hasPlayerParameter()) {
            String parameter = node.integerArgument() + " " + word.parameter().unit();
            graphics.drawString(font, "PARAMETER: " + parameter, textX, y, 0xFFE4D5F7, false);
            y += 14;
        }

        long incoming = graph.edges().stream().filter(edge -> edge.to() == node.id()).count();
        graphics.drawString(font, "INPUT WIRES: " + incoming, textX, y, 0xFFAA98BD, false);
        y += 14;
        graphics.drawString(font, "OUTPUTS", textX, y, 0xFFBBA7DC, false);
        y += 12;
        for (SpellPort port : word.outputs()) {
            SpellGraphDraft.Edge edge = graph.edges().stream()
                .filter(candidate -> candidate.from() == node.id() && candidate.port() == port)
                .findFirst().orElse(null);
            SpellGraphDraft.Node targetNode = edge == null ? null : graph.node(edge.to());
            String target = targetNode == null ? "Not connected" : WordRegistry.presentation(targetNode.word()).displayName();
            graphics.drawString(font, port.name() + " → " + trim(target, INSPECTOR_WIDTH - 78),
                textX, y, portColor(port), false);
            y += 12;
        }
    }

    private void drawFooter(GuiGraphics graphics) {
        int top = footerTop();
        SpellGraphAnalysis.Summary summary = SpellGraphAnalysis.summarize(graph);
        graphics.drawString(font, "CURRENT SPELL", 8, top + 7, 0xFFBBA7DC, false);
        int infoX = nameBox.getX() + nameBox.getWidth() + 12;
        graphics.drawString(font, "NODES  " + summary.connectedNodes() + " connected / "
            + summary.disconnectedNodes() + " loose", infoX, top + 8, 0xFFB8A8CC, false);
        graphics.drawString(font, "FORCE  " + summary.force(), infoX, top + 22, 0xFFE5D5FA, false);
        int manaX = Math.min(width - 340, infoX + 150);
        if (manaX > infoX + 80) {
            graphics.drawString(font, "MANA  " + summary.informationalManaCost() + " (info)",
                manaX, top + 22, 0xFFBDA9D8, false);
        }
        graphics.drawString(font, trim(status, Math.max(100, width - 16)), 8, top + 43, statusColor, false);
    }

    private SpellWordPresentation paletteWordAt(double mouseX, double mouseY) {
        if (mouseX < 5 || mouseX >= PALETTE_WIDTH - 5 || mouseY < paletteListTop() || mouseY >= footerTop()) return null;
        int visible = (int) ((mouseY - paletteListTop()) / PALETTE_ROW_HEIGHT);
        List<SpellWordPresentation> filtered = filteredWords();
        int index = paletteScroll + visible;
        return index >= 0 && index < filtered.size() ? filtered.get(index) : null;
    }

    private int nodeAt(double mouseX, double mouseY) {
        List<SpellGraphDraft.Node> nodes = graph.nodes();
        for (int i = nodes.size() - 1; i >= 0; i--) {
            SpellGraphDraft.Node node = nodes.get(i);
            int x = worldToScreenX(node.x());
            int y = worldToScreenY(node.y());
            if (mouseX >= x && mouseX <= x + scaled(NODE_WIDTH)
                && mouseY >= y && mouseY <= y + scaled(NODE_HEIGHT)) return node.id();
        }
        return -1;
    }

    private int inputNodeAt(double mouseX, double mouseY) {
        for (SpellGraphDraft.Node node : graph.nodes()) {
            Point point = inputPoint(node);
            if (distanceSquared(mouseX, mouseY, point.x(), point.y()) <= 64) return node.id();
        }
        return -1;
    }

    private PortHit outputPortAt(double mouseX, double mouseY) {
        for (SpellGraphDraft.Node node : graph.nodes()) for (SpellPort port : WordRegistry.presentation(node.word()).outputs()) {
            Point point = outputPoint(node, port);
            if (distanceSquared(mouseX, mouseY, point.x(), point.y()) <= 64) return new PortHit(node.id(), port);
        }
        return null;
    }

    private Point inputPoint(SpellGraphDraft.Node node) {
        return new Point(worldToScreenX(node.x()), worldToScreenY(node.y()) + scaled(NODE_HEIGHT) / 2);
    }

    private Point outputPoint(SpellGraphDraft.Node node, SpellPort port) {
        List<SpellPort> ports = WordRegistry.presentation(node.word()).outputs();
        int index = Math.max(0, ports.indexOf(port));
        int y = ports.size() == 1 ? NODE_HEIGHT / 2 : index == 0 ? NODE_HEIGHT / 3 : NODE_HEIGHT * 2 / 3;
        return new Point(worldToScreenX(node.x()) + scaled(NODE_WIDTH), worldToScreenY(node.y()) + scaled(y));
    }

    private boolean insideCanvas(double x, double y) {
        return x >= PALETTE_WIDTH && x < inspectorLeft() && y >= HEADER_HEIGHT && y < footerTop();
    }

    private int worldToScreenX(int worldX) { return (int) Math.round(PALETTE_WIDTH + panX + worldX * zoom); }
    private int worldToScreenY(int worldY) { return (int) Math.round(HEADER_HEIGHT + panY + worldY * zoom); }
    private double screenToWorldX(double screenX) { return (screenX - PALETTE_WIDTH - panX) / zoom; }
    private double screenToWorldY(double screenY) { return (screenY - HEADER_HEIGHT - panY) / zoom; }
    private int scaled(int value) { return Math.max(1, (int) Math.round(value * zoom)); }
    private static double distanceSquared(double x1, double y1, double x2, double y2) {
        double x = x1 - x2;
        double y = y1 - y2;
        return x * x + y * y;
    }
    private String trim(String text, int maximumWidth) {
        return font.width(text) <= maximumWidth ? text : font.plainSubstrByWidth(text, Math.max(1, maximumWidth - 8)) + "…";
    }

    @Override public boolean isPauseScreen() { return false; }
    private record Point(int x, int y) {}
    private record PortHit(int nodeId, SpellPort port) {}
}
