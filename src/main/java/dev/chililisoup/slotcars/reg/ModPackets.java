package dev.chililisoup.slotcars.reg;

import dev.chililisoup.slotcars.network.ServerboundMoveSlotCarPacket;

public class ModPackets {
    public static void init() {
        ServerboundMoveSlotCarPacket.register();
    }
}
