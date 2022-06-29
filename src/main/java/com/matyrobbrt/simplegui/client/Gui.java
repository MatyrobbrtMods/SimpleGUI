package com.matyrobbrt.simplegui.client;

import com.matyrobbrt.simplegui.client.element.window.Window;
import com.matyrobbrt.simplegui.inventory.SelectedWindowData;
import com.matyrobbrt.simplegui.client.element.GuiElement;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@MethodsReturnNonnullByDefault
public interface Gui {

    default void displayTooltips(PoseStack matrix, int mouseX, int mouseY, Component... components) {
        this.displayTooltips(matrix, mouseX, mouseY, List.of(components));
    }

    default void displayTooltips(PoseStack matrix, int mouseX, int mouseY, List<Component> components) {
        Screen screen;
        if (this instanceof Screen) {
            screen = (Screen) this;
        } else {
            //Otherwise, try falling back to the current screen
            screen = Minecraft.getInstance().screen;
            if (screen == null) {
                return;
            }
        }
        screen.renderComponentTooltip(matrix, components, mouseX, mouseY);
    }

    default int getLeft() {
        if (this instanceof AbstractContainerScreen screen) {
            return screen.getGuiLeft();
        }
        return 0;
    }

    default int getTop() {
        if (this instanceof AbstractContainerScreen screen) {
            return screen.getGuiTop();
        }
        return 0;
    }

    default int getWidth() {
        if (this instanceof AbstractContainerScreen screen) {
            return screen.getXSize();
        }
        return 0;
    }

    default int getHeight() {
        if (this instanceof AbstractContainerScreen screen) {
            return screen.getYSize();
        }
        return 0;
    }

    default void addWindow(Window window) {
    }

    default void removeWindow(Window window) {
    }

    default boolean currentlyQuickCrafting() {
        return false;
    }

    @Nullable
    default Window getWindowHovering(double mouseX, double mouseY) {
        return null;
    }

    @Nullable
    Font getFont();

    default void renderItem(PoseStack matrix, @Nonnull ItemStack stack, int xAxis, int yAxis) {
        renderItem(matrix, stack, xAxis, yAxis, 1);
    }

    default void renderItem(PoseStack matrix, @Nonnull ItemStack stack, int xAxis, int yAxis, float scale) {
        ClientUtil.renderItem(matrix, getItemRenderer(), stack, xAxis, yAxis, scale, getFont(), null, false);
    }

    ItemRenderer getItemRenderer();

    default void renderItemTooltip(PoseStack matrix, @Nonnull ItemStack stack, int xAxis, int yAxis) {
    }

    default void renderItemTooltipWithExtra(PoseStack matrix, @Nonnull ItemStack stack, int xAxis, int yAxis, List<Component> toAppend) {
        if (toAppend.isEmpty()) {
            renderItemTooltip(matrix, stack, xAxis, yAxis);
        }
    }

    default void renderItemWithOverlay(PoseStack matrix, @Nonnull ItemStack stack, int xAxis, int yAxis, float scale, @Nullable String text) {
        ClientUtil.renderItem(matrix, getItemRenderer(), stack, xAxis, yAxis, scale, getFont(), text, true);
    }

    default void setSelectedWindow(SelectedWindowData selectedWindow) {
    }

    default void addFocusListener(GuiElement element) {
    }

    default void removeFocusListener(GuiElement element) {
    }

    default void focusChange(GuiElement changed) {
    }

    default void incrementFocus(GuiElement current) {
    }

}
