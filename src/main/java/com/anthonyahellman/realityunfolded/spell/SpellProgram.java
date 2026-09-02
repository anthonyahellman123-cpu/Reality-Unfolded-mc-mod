package com.anthonyahellman.realityunfolded.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpellProgram {
    public static final int TERMINAL = -1;

    private final int rootNode;
    private final Map<Integer, SpellNode> nodes;
    private final String source;

    public SpellProgram(int rootNode, List<SpellNode> nodes, String source) {
        this.rootNode = rootNode;
        this.nodes = new LinkedHashMap<>();
        for (SpellNode node : nodes) this.nodes.put(node.id(), node);
        this.source = source;
    }

    public int rootNode() {
        return rootNode;
    }

    public SpellNode node(int id) {
        return nodes.get(id);
    }

    public List<SpellNode> nodes() {
        return List.copyOf(nodes.values());
    }

    public String source() {
        return source;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("root", rootNode);
        tag.putString("source", source);
        ListTag serializedNodes = new ListTag();
        for (SpellNode node : nodes.values()) serializedNodes.add(node.save());
        tag.put("nodes", serializedNodes);
        return tag;
    }

    public static SpellProgram load(CompoundTag tag) {
        ListTag serializedNodes = tag.getList("nodes", Tag.TAG_COMPOUND);
        List<SpellNode> nodes = new java.util.ArrayList<>();
        for (int i = 0; i < serializedNodes.size(); i++) {
            nodes.add(SpellNode.load(serializedNodes.getCompound(i)));
        }
        return new SpellProgram(tag.getInt("root"), nodes, tag.getString("source"));
    }
}
