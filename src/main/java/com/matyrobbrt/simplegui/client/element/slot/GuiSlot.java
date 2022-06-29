package com.matyrobbrt.simplegui.client.element.slot;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.matyrobbrt.simplegui.client.ClientUtil;
import com.matyrobbrt.simplegui.client.Gui;
import com.matyrobbrt.simplegui.client.element.TexturedElement;
import com.matyrobbrt.simplegui.util.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.function.Supplier;

public class GuiSlot extends TexturedElement {

    private static final int INVALID_SLOT_COLOR = Color.RED.getRGB();
    public static final int DEFAULT_HOVER_COLOR = 0x80FFFFFF;
    private Supplier<ItemStack> validityCheck;
    private Supplier<ItemStack> storedStackSupplier;
    @Nullable
    private Hoverable onHover;
    @Nullable
    private Clickable onClick;
    private boolean renderHover;
    private boolean renderAboveSlots;

    public GuiSlot(Gui gui, int x, int y) {
        this(Utils.getResource("gui", "slot"), gui, x, y);
    }

    public GuiSlot(ResourceLocation texture, Gui gui, int x, int y) {
        super(texture, gui, x, y, 18, 18);
        active = false;
    }

    public GuiSlot validity(Supplier<ItemStack> validityCheck) {
        this.validityCheck = validityCheck;
        return this;
    }

    public GuiSlot stored(Supplier<ItemStack> storedStackSupplier) {
        this.storedStackSupplier = storedStackSupplier;
        return this;
    }

    public GuiSlot hover(Hoverable onHover) {
        this.onHover = onHover;
        return this;
    }

    public GuiSlot click(Clickable onClick) {
        this.onClick = onClick;
        return this;
    }

    @CanIgnoreReturnValue
    public GuiSlot setRenderHover(boolean renderHover) {
        this.renderHover = renderHover;
        return this;
    }

    public GuiSlot setRenderAboveSlots() {
        this.renderAboveSlots = true;
        return this;
    }

    @Override
    public void renderButton(@Nonnull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        if (!renderAboveSlots) {
            draw(matrix);
        }
    }

    @Override
    public void drawBackground(@Nonnull PoseStack matrix, int mouseX, int mouseY, float partialTick) {
        if (renderAboveSlots) {
            draw(matrix);
        }
    }

    private void draw(@Nonnull PoseStack matrix) {
        RenderSystem.setShaderTexture(0, getTexture());
        blit(matrix, x, y, 0, 0, width, height, width, height);
        drawContents(matrix);
    }

    protected void drawContents(@Nonnull PoseStack matrix) {
        if (validityCheck != null) {
            final var invalid = validityCheck.get();
            if (!invalid.isEmpty()) {
                // There's a (at least) 1px border
                final int xPos = x + borderSize(), yPos = y + borderSize();
                fill(matrix, xPos, yPos, xPos + 16, yPos + 16, INVALID_SLOT_COLOR);
                ClientUtil.resetColour();
                gui().renderItem(matrix, invalid, xPos, yPos);
            }
        } else if (storedStackSupplier != null) {
            final var stored = storedStackSupplier.get();
            if (!stored.isEmpty()) {
                gui().renderItem(matrix, stored, x + 1, y + 1);
            }
        }
    }

    @Override
    public void renderForeground(PoseStack matrix, int mouseX, int mouseY) {
        if (renderHover && isHoveredOrFocused()) {
            int xPos = x + 1;
            int yPos = y + 1;
            fill(matrix, xPos, yPos, xPos + 16, yPos + 16, DEFAULT_HOVER_COLOR);
            ClientUtil.resetColour();
        }
        if (isHoveredOrFocused()) {
            renderToolTip(matrix, mouseX - getGuiLeft(), mouseY - getGuiTop());
        }
    }

    @Override
    public void renderToolTip(@Nonnull PoseStack matrix, int mouseX, int mouseY) {
        super.renderToolTip(matrix, mouseX, mouseY);
        if (onHover != null) {
            onHover.onHover(this, matrix, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (onClick != null && isValidClickButton(button)) {
            if (mouseX >= x + borderSize() && mouseY >= y + borderSize() && mouseX < x + width - borderSize() && mouseY < y + height - borderSize()) {
                onClick.onClick(this, (int) mouseX, (int) mouseY);
                playDownSound(MINECRAFT.getSoundManager());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public int borderSize() {
        return 1;
    }
}
