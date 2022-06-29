package com.matyrobbrt.simplegui.network;

import com.matyrobbrt.simplegui.annotations.CallOnlyOn;
import com.matyrobbrt.simplegui.inventory.SelectedWindowData;
import com.matyrobbrt.simplegui.inventory.SimpleMenu;
import com.matyrobbrt.simplegui.inventory.WindowType;
import com.matyrobbrt.simplegui.util.Utils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;

public record SelectWindowPacket(SelectedWindowData selectedWindow) implements Packet {
    public SelectWindowPacket(@Nullable SelectedWindowData selectedWindow) {
        this.selectedWindow = selectedWindow;
    }

    @Override
    @CallOnlyOn(value = CallOnlyOn.Side.SERVER, logical = true)
    public void handle(NetworkEvent.Context context) {
        final var player = context.getSender();
        if (player != null && player.containerMenu instanceof SimpleMenu container) {
            container.setSelectedWindow(player.getUUID(), selectedWindow);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        if (selectedWindow == null) {
            buffer.writeShort(-1);
        } else {
            buffer.writeShort(selectedWindow.extraData);
            buffer.writeVarInt(selectedWindow.type.getId());
        }
    }

    @Override
    public String getRequiredVersion() {
        return "1.0";
    }

    public static SelectWindowPacket decode(FriendlyByteBuf buffer) {
        final var extraData = buffer.readShort();
        if (extraData == -1) {
            return new SelectWindowPacket(null);
        }
        final var id = buffer.readVarInt();
        final var type = WindowType.byId(id);
        if (type == null) {
            Utils.LOGGER.warn("Received invalid SelectWindowPacket with unknown window ID {}", id);
            return new SelectWindowPacket(null);
        }
        return new SelectWindowPacket(type == WindowType.UNSPECIFIED ? SelectedWindowData.UNSPECIFIED : new SelectedWindowData(type, extraData));
    }
}
