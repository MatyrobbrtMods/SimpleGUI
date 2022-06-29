package com.matyrobbrt.simplegui.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public interface Packet {

    void encode(FriendlyByteBuf buf);
    void handle(NetworkEvent.Context context);

    default String getRequiredVersion() {
        return SimpleGuiNetwork.VERSION;
    }
}
