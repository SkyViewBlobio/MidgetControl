package com.midgetcontrol.player;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerBlipControlTest {
    @Test
    void hidesOnlyThroughTheMidgetControlModifier() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        AttributeInstance transmitRange = new AttributeInstance(
                Holder.direct(new RangedAttribute("test.waypoint_range", 100.0, 0.0, 100.0)),
                ignored -> {
                }
        );
        transmitRange.addTransientModifier(new AttributeModifier(
                Identifier.fromNamespaceAndPath("test", "other_modifier"),
                -0.5,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));

        PlayerBlipControl.setVisible(transmitRange, false);
        assertEquals(0.0, transmitRange.getValue());

        PlayerBlipControl.setVisible(transmitRange, true);
        assertEquals(50.0, transmitRange.getValue());
    }
}
