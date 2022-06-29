package com.matyrobbrt.simplegui.network;

import com.matyrobbrt.simplegui.SimpleGUIMod;
import com.matyrobbrt.simplegui.annotations.CheckCaller;
import com.matyrobbrt.simplegui.client.ClientUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

public class SimpleGuiNetwork {
    public static final String VERSION = "1.0";
    public static final ResourceLocation NAME = new ResourceLocation(SimpleGUIMod.MOD_ID, "network");
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(NAME)
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(str -> true)
            .serverAcceptedVersions(str -> true)
            .simpleChannel();

    @CheckCaller(value = SimpleGUIMod.class, exception = "Another mod tried to register SimpleGUI's networking!")
    public static void register() {
        class Registrar {
            int id = 0;
            <P extends Packet> void register(Class<P> pkt, Function<FriendlyByteBuf, P> decoder) {
                CHANNEL.messageBuilder(pkt, id++)
                        .consumer((packet, contextSupplier) -> {
                            final var ctx = contextSupplier.get();
                            ctx.enqueueWork(() -> packet.handle(ctx));
                            return true;
                        })
                        .encoder(Packet::encode)
                        .decoder(decoder)
                        .add();
            }
        }

        final var registry = new Registrar();
        registry.register(SelectWindowPacket.class, SelectWindowPacket::decode);
    }

    public static void sendToServer(Object packet) {
        if (FMLEnvironment.dist == Dist.CLIENT)
            ClientUtil.sendPacketToServer(packet);
    }
}
