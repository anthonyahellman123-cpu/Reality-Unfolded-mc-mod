package com.anthonyahellman.realityunfolded.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public record SpellNode(int id, SpellWordId word, int integerArgument,
                        double powerMultiplier, List<Integer> next) {
    public SpellNode {
        next = List.copyOf(next);
    }

    public SpellNode withPowerMultiplier(double value) {
        return new SpellNode(id, word, integerArgument, value, next);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", id);
        tag.putString("word", word.name());
        tag.putInt("integer_argument", integerArgument);
        tag.putDouble("power_multiplier", powerMultiplier);
        ListTag children = new ListTag();
        for (int child : next) {
            CompoundTag childTag = new CompoundTag();
            childTag.putInt("id", child);
            children.add(childTag);
        }
        tag.put("next", children);
        return tag;
    }

    public static SpellNode load(CompoundTag tag) {
        List<Integer> next = new ArrayList<>();
        ListTag children = tag.getList("next", Tag.TAG_COMPOUND);
        for (int i = 0; i < children.size(); i++) {
            next.add(children.getCompound(i).getInt("id"));
        }
        return new SpellNode(tag.getInt("id"), SpellWordId.parse(tag.getString("word")),
            tag.getInt("integer_argument"), tag.getDouble("power_multiplier"), next);
    }
}
