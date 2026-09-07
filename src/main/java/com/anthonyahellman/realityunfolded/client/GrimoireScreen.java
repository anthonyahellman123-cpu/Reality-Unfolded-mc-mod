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
    private static final int PALETTE_WIDTH = 190;
    private static final int HEADER_HEIGHT = 24;
    private static final int FOOTER_HEIGHT = 104;
    private static final int NODE_WIDTH = 112;
    private static final int NODE_HEIGHT = 36;
    private static final int PORT_RADIUS = 5;
    private static final int PALETTE_ROW_HEIGHT = 22;
    private static final double MIN_ZOOM = 0.45D;
    private static final double MAX_ZOOM = 2.0D;

    private final List<GrimoireData.SpellSlot> slots = new ArrayList<>();
    private final List<Button> slotButtons = new ArrayList<>();
    private final List<Button> categoryButtons = new ArrayList<>();
    private final List<SpellWordPresentation> presentations = WordRegistry.presentations();
    private SpellGraphDraft graph = new SpellGraphDraft();
    private EditBox searchBox;
    private EditBox nameBox;
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
        searchBox = new EditBox(font, 7, 29, PALETTE_WIDTH - 14, 18, Component.literal("Search words"));
        searchBox.setHint(Component.literal("Search words..."));
        searchBox.setResponder(ignored -> paletteScroll = 0);
        addRenderableWidget(searchBox);

        List<String> categories = categories();
        int categoryWidth = (PALETTE_WIDTH - 17) / 2;
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            Button button = Button.builder(Component.literal(shortCategory(category)), ignored -> {
                selectedCategory = category;
                paletteScroll = 0;
                refreshCategoryButtons();
            }).bounds(7 + i % 2 * (categoryWidth + 3), 51 + i / 2 * 18, categoryWidth, 16).build();
            categoryButtons.add(addRenderableWidget(button));
        }

        int footerTop = footerTop();
        nameBox = new EditBox(font, PALETTE_WIDTH + 10, footerTop + 18,
            Math.max(90, width - PALETTE_WIDTH - 370), 18, Component.literal("Spell name"));
        nameBox.setMaxLength(GrimoireData.MAX_NAME_LENGTH);
        addRenderableWidget(nameBox);

        int slotWidth = Math.max(42, (width - 14) / GrimoireData.SLOT_COUNT - 3);
        for (int i = 0; i < GrimoireData.SLOT_COUNT; i++) {
            int slot = i;
            Button button = Button.builder(Component.empty(), ignored -> loadSlot(slot))
                .bounds(7 + i * (slotWidth + 3), height - 22, slotWidth, 17).build();
            slotButtons.add(addRenderableWidget(button));
        }

        int actionsX = Math.max(PALETTE_WIDTH + 112, width - 348);
        addRenderableWidget(Button.builder(Component.literal("Save"), ignored -> save(false))
            .bounds(actionsX, footerTop + 16, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save + Active"), ignored -> save(true))
            .bounds(actionsX + 76, footerTop + 16, 104, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Select Saved"), ignored -> selectSaved())
            .bounds(actionsX + 184, footerTop + 16, 94, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Set Root"), ignored -> setRoot())
            .bounds(actionsX, footerTop + 40, 72, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Param −"), ignored -> adjustParameter(-1))
            .bounds(actionsX + 76, footerTop + 40, 68, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Param +"), ignored -> adjustParameter(1))
            .bounds(actionsX + 148, footerTop + 40, 68, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Delete"), ignored -> deleteSelected())
            .bounds(actionsX + 220, footerTop + 40, 58, 18).build());

        loadEditingSlot();
        refreshSlotButtons();
        refreshCategoryButtons();
    }

    private List<String> categories() {
        Set<String> values = new LinkedHashSet<>();
        values.add("ALL");
        presentations.forEach(word -> values.add(word.category()));
        return List.copyOf(values);
    }

    private String shortCategory(String value) {
        return switch (value) {
            case "MANIFESTATION" -> "FORMS";
            case "MODIFIER" -> "POWER";
            default -> value;
        };
    }

    private void refreshCategoryButtons() {
        List<String> categories = categories();
        for (int i = 0; i < categoryButtons.size(); i++) {
            categoryButtons.get(i).active = !categories.get(i).equals(selectedCategory);
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

    private int paletteListTop() { return 126; }
    private int footerTop() { return height - FOOTER_HEIGHT; }
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

    private void selectSaved() { ModNetwork.selectSavedSlot(editingSlot); }
    private void setRoot() { if (graph.node(selectedNode) != null) graph.setRootId(selectedNode); }
    private void adjustParameter(int direction) { graph.adjustParameter(selectedNode, direction); }
    private void deleteSelected() {
        if (selectedNode < 0) return;
        graph.remove(selectedNode);
        selectedNode = -1;
        pendingWireNode = -1;
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
        graphics.fill(0, 0, width, height, 0xF00C0815);
        graphics.fill(0, 0, PALETTE_WIDTH, footer, 0xFF1A1228);
        graphics.fill(PALETTE_WIDTH, HEADER_HEIGHT, width, footer, 0xFF100B19);
        drawGrid(graphics, footer);
        graphics.fill(0, footer, width, height, 0xFF21162F);
        graphics.fill(PALETTE_WIDTH - 1, 0, PALETTE_WIDTH + 1, footer, 0xFF6842A8);
        graphics.fill(0, footer - 1, width, footer + 1, 0xFF6842A8);
        graphics.drawCenteredString(font, "SPELLCRAFT — INSTRUCTIONS FOR REALITY", width / 2, 8, 0xFFE9DEFF);
        graphics.drawString(font, "SEARCH / CATEGORIES", 7, 17, 0xFFBBA7DC, false);
        graphics.drawString(font, "WORDS — DRAG TO CANVAS", 7, paletteListTop() - 11, 0xFFBBA7DC, false);
        drawPalette(graphics, mouseX, mouseY);
        drawConnections(graphics);
        drawPendingWire(graphics, mouseX, mouseY);
        drawNodes(graphics, mouseX, mouseY);
        drawFooter(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
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
        for (int x = startX; x < width; x += spacing) if (x >= PALETTE_WIDTH) graphics.vLine(x, HEADER_HEIGHT, footer, 0x192D2141);
        for (int y = startY; y < footer; y += spacing) if (y >= HEADER_HEIGHT) graphics.hLine(PALETTE_WIDTH, width, y, 0x192D2141);
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
            if (hovered) graphics.renderTooltip(font, Component.literal(word.category() + " / " + word.subcategory()
                + " — " + word.description()), mouseX, mouseY);
        }
    }

    private void drawConnections(GuiGraphics graphics) {
        for (SpellGraphDraft.Edge edge : graph.edges()) {
            SpellGraphDraft.Node from = graph.node(edge.from());
            SpellGraphDraft.Node to = graph.node(edge.to());
            if (from == null || to == null) continue;
            Point start = outputPoint(from, edge.port());
            Point end = inputPoint(to);
            int color = edge.port() == SpellPort.FALSE ? 0xFFB25B88
                : edge.port() == SpellPort.TRUE ? 0xFF72D9A4 : 0xFF9A6DE0;
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
                int color = port == SpellPort.FALSE ? 0xFFB25B88 : port == SpellPort.TRUE ? 0xFF72D9A4 : 0xFF9A6DE0;
                graphics.fill(output.x() - PORT_RADIUS, output.y() - PORT_RADIUS,
                    output.x() + PORT_RADIUS, output.y() + PORT_RADIUS, color);
            }
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                graphics.renderTooltip(font, Component.literal(presentation.description()), mouseX, mouseY);
            }
        }
    }

    private void drawFooter(GuiGraphics graphics) {
        int top = footerTop();
        SpellGraphAnalysis.Summary summary = SpellGraphAnalysis.summarize(graph);
        graphics.drawString(font, "SPELL: " + nameBox.getValue(), PALETTE_WIDTH + 10, top + 7, 0xFFE8DDF5, false);
        graphics.drawString(font, "Connected " + summary.connectedNodes() + "  •  Unconnected "
            + summary.disconnectedNodes(), PALETTE_WIDTH + 10, top + 43, 0xFFB8A8CC, false);
        graphics.drawString(font, "Wire: output then input. Right/middle drag pans; wheel zooms.",
            PALETTE_WIDTH + 10, top + 57, 0xFF9B8BAB, false);
        graphics.drawString(font, trim(status, Math.max(100, width - PALETTE_WIDTH - 22)),
            PALETTE_WIDTH + 10, top + 72, statusColor, false);
        int infoX = Math.max(PALETTE_WIDTH + 10, width - 250);
        graphics.fill(infoX - 5, top + 62, width - 7, top + 95, 0x662B1C3F);
        graphics.drawString(font, "FORCE OF SPELL: " + summary.force(), infoX, top + 67, 0xFFE5D5FA, false);
        graphics.drawString(font, "LIVE MANA COST: " + summary.informationalManaCost() + " (info)",
            infoX, top + 81, 0xFFBDA9D8, false);
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
        return x >= PALETTE_WIDTH && x < width && y >= HEADER_HEIGHT && y < footerTop();
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
