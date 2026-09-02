package com.anthonyahellman.realityunfolded.spell.runtime;

import com.anthonyahellman.realityunfolded.RealityUnfolded;
import com.anthonyahellman.realityunfolded.entity.SpellBoltEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class LinkRuntime {
    private static final Map<ServerLevel, Map<LinkKey, LinkedHashSet<UUID>>> LINKS = new WeakHashMap<>();

    private LinkRuntime() {}

    public static synchronized void register(ServerLevel level, UUID castId, int nodeId, SpellBoltEntity bolt) {
        LinkKey key = new LinkKey(castId, nodeId);
        LinkedHashSet<UUID> members = LINKS.computeIfAbsent(level, ignored -> new java.util.HashMap<>())
            .computeIfAbsent(key, ignored -> new LinkedHashSet<>());
        if (members.add(bolt.getUUID())) {
            RealityUnfolded.LOGGER.info("[RU SPELL] LINK_CREATION cast={} node={} member={} memberCount={}",
                castId, nodeId, bolt.manifestationId(), members.size());
        }
    }

    public static synchronized void unregister(ServerLevel level, SpellBoltEntity bolt) {
        Map<LinkKey, LinkedHashSet<UUID>> levelLinks = LINKS.get(level);
        if (levelLinks == null) return;
        Iterator<Map.Entry<LinkKey, LinkedHashSet<UUID>>> iterator = levelLinks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LinkKey, LinkedHashSet<UUID>> entry = iterator.next();
            if (entry.getValue().remove(bolt.getUUID())) {
                RealityUnfolded.LOGGER.info("[RU SPELL] LINK_REMOVAL cast={} node={} member={} remaining={}",
                    entry.getKey().castId(), entry.getKey().nodeId(), bolt.manifestationId(), entry.getValue().size());
            }
            if (entry.getValue().isEmpty()) iterator.remove();
        }
        if (levelLinks.isEmpty()) LINKS.remove(level);
    }

    public static synchronized int relationshipCount(ServerLevel level) {
        Map<LinkKey, LinkedHashSet<UUID>> levelLinks = LINKS.get(level);
        return levelLinks == null ? 0 : levelLinks.size();
    }

    public static synchronized int memberCount(ServerLevel level) {
        Map<LinkKey, LinkedHashSet<UUID>> levelLinks = LINKS.get(level);
        return levelLinks == null ? 0 : levelLinks.values().stream().mapToInt(LinkedHashSet::size).sum();
    }

    public static synchronized void tickVisuals(ServerLevel level, SpellBoltEntity caller) {
        if (caller.tickCount % 4 != 0) return;
        Map<LinkKey, LinkedHashSet<UUID>> levelLinks = LINKS.get(level);
        if (levelLinks == null) return;
        for (LinkedHashSet<UUID> memberIds : levelLinks.values()) {
            if (memberIds.isEmpty() || !memberIds.iterator().next().equals(caller.getUUID())) continue;
            List<Entity> members = new ArrayList<>();
            memberIds.removeIf(id -> level.getEntity(id) == null);
            for (UUID id : memberIds) {
                Entity entity = level.getEntity(id);
                if (entity != null) members.add(entity);
            }
            for (int i = 1; i < members.size(); i++) drawLine(level, members.get(i - 1).position(), members.get(i).position());
        }
    }

    private static void drawLine(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(1, (int) Math.ceil(delta.length() * 2.0D));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(delta.scale((double) i / steps));
            level.sendParticles(ParticleTypes.ENCHANT, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    private record LinkKey(UUID castId, int nodeId) {}
}
