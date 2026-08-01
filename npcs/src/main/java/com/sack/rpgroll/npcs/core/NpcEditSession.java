package com.sack.rpgroll.npcs.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado mutable de un NPC en edición, en memoria. No persiste a disco
 * hasta que NpcWriter.save() se invoca explícitamente desde la GUI.
 */
public class NpcEditSession {

    private final String id;
    private final boolean isNew;

    private String displayName;
    private String skinValue = "";
    private String skinSignature = "";
    private String pose = "STANDING";
    private String world;
    private double x, y, z;
    private float yaw, pitch;
    private final List<NpcAction> actions = new ArrayList<>();

    public NpcEditSession(String id, boolean isNew) {
        this.id = id;
        this.isNew = isNew;
        this.displayName = id;
    }

    public static NpcEditSession fromDefinition(NpcDefinition definition) {

        NpcEditSession session = new NpcEditSession(definition.id(), false);
        session.displayName = definition.displayName();
        session.skinValue = definition.skinValue();
        session.skinSignature = definition.skinSignature();
        session.pose = definition.pose();
        session.world = definition.world();
        session.x = definition.x();
        session.y = definition.y();
        session.z = definition.z();
        session.yaw = definition.yaw();
        session.pitch = definition.pitch();
        session.actions.addAll(definition.actions());

        return session;
    }

    public NpcDefinition toDefinition() {
        return new NpcDefinition(id, displayName, skinValue, skinSignature, pose,
                world, x, y, z, yaw, pitch, List.copyOf(actions));
    }

    public String getId() {
        return id;
    }

    public boolean isNew() {
        return isNew;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getSkinValue() {
        return skinValue;
    }

    public String getSkinSignature() {
        return skinSignature;
    }

    public void setSkin(String value, String signature) {
        this.skinValue = value;
        this.skinSignature = signature;
    }

    public String getPose() {
        return pose;
    }

    public void setPose(String pose) {
        this.pose = pose;
    }

    public boolean hasLocation() {
        return world != null;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setLocation(org.bukkit.Location location) {
        this.world = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    public List<NpcAction> getActions() {
        return actions;
    }

    public void addAction(NpcAction action) {
        actions.add(action);
    }

    public void removeAction(int index) {
        if (index >= 0 && index < actions.size()) {
            actions.remove(index);
        }
    }

    public boolean isComplete() {
        return displayName != null && !displayName.isBlank() && hasLocation();
    }

}