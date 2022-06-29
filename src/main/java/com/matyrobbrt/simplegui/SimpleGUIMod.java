package com.matyrobbrt.simplegui;

import com.matyrobbrt.simplegui.network.SimpleGuiNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SimpleGUIMod.MOD_ID)
public class SimpleGUIMod {

    public static final String MOD_ID = "simplegui";

    public SimpleGUIMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener((final FMLCommonSetupEvent event) -> SimpleGuiNetwork.register());
    }
}
