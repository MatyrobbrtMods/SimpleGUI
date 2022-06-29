package com.matyrobbrt.simplegui.client;

import com.matyrobbrt.simplegui.SimpleGUIMod;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = SimpleGUIMod.MOD_ID)
public class SimpleGUIModClient {
    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("jei")) {
            MinecraftForge.EVENT_BUS.addListener(SimpleGUIModClient::jeiOpen);
        }
    }

    public static void jeiOpen(ScreenOpenEvent event) {
        if (Minecraft.getInstance().screen instanceof SimpleGui<?> screen) {
            if (event.getScreen() instanceof IRecipesGui) {
                screen.switchingToJEI = true;
            }
        }
    }
}
