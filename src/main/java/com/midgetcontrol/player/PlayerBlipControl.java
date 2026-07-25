package com.midgetcontrol.player;

import com.midgetcontrol.MidgetControl;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class PlayerBlipControl {
    private static final AttributeModifier HIDDEN_MODIFIER = new AttributeModifier(
            Identifier.fromNamespaceAndPath(MidgetControl.MOD_ID, "locator_blip_hidden"),
            -1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    private PlayerBlipControl() {
    }

    public static boolean apply(ServerPlayer player, boolean visible) {
        AttributeInstance transmitRange = player.getAttribute(Attributes.WAYPOINT_TRANSMIT_RANGE);
        if (transmitRange == null) {
            MidgetControl.LOGGER.error("Could not update the locator blip for {}", player.getName().getString());
            return false;
        }

        setVisible(transmitRange, visible);
        if (!visible) {
            player.level().getWaypointManager().untrackWaypoint(player);
        }
        return true;
    }

    static void setVisible(AttributeInstance transmitRange, boolean visible) {
        if (visible) {
            transmitRange.removeModifier(HIDDEN_MODIFIER.id());
        } else {
            transmitRange.addOrUpdateTransientModifier(HIDDEN_MODIFIER);
        }
    }
}
