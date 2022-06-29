package com.matyrobbrt.simplegui.jei;

import com.matyrobbrt.simplegui.annotations.CheckCaller;
import com.matyrobbrt.simplegui.client.SimpleGui;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class SimpleGuiJEI implements IModPlugin {
    public static final ResourceLocation ID = new ResourceLocation("simplegui", "jei");

    @CheckCaller(
            named = "mezz.jei.forge.util.AnnotatedInstanceUtil", // Somewhat risky if a JEI update changes that
            exception = "Tried instantiating the SimpleGUI Jei plugin"
    )
    public SimpleGuiJEI() {
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(SimpleGui.class, new JeiElementHandler());
    }
}
