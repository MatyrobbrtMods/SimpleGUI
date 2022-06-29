package com.matyrobbrt.simplegui.client;

import com.google.common.collect.Iterators;
import com.matyrobbrt.simplegui.client.element.GuiElement;
import com.matyrobbrt.simplegui.network.Packet;
import com.matyrobbrt.simplegui.network.SimpleGuiNetwork;
import com.matyrobbrt.simplegui.util.Color;
import com.matyrobbrt.simplegui.util.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class ClientUtil {
    public static void resetColour() {
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public static void setColour(Color colour) {
        RenderSystem.setShaderColor(colour.rf(), colour.gf(), colour.bf(), colour.af());
    }

    /**
     * A method that renders background large textures, scaling them down or tilling them in order to fit the size requirements.
     * @apiNote this method shouldn't be used for small textures as due to the tilling, FPS will be reduced significantly
     * @param matrix the matrix
     * @param resource the resources to renderer
     * @param textureSideWidth the side width of the texture
     * @param textureSideHeight the side height of the texture
     * @param left the top x coordinate to start rendering from
     * @param top the top y coordinate to start rendering from
     * @param width the target width
     * @param height the target height
     * @param textureWidth the texture width
     * @param textureHeight the texture height
     */
    public static void renderBackgroundTexture(PoseStack matrix, ResourceLocation resource, int textureSideWidth, int textureSideHeight, int left, int top, int width, int height, int textureWidth, int textureHeight) {
        final var sideWidth = Math.min(textureSideWidth, width / 2);
        final var sideHeight = Math.min(textureSideHeight, height / 2);

        // Adjustment for small odd-height and odd-width GUIs
        final var leftWidth = sideWidth < textureSideWidth ? sideWidth + (width % 2) : sideWidth;
        final var topHeight = sideHeight < textureSideHeight ? sideHeight + (height % 2) : sideHeight;

        // Calculate texture centre
        final int texCentreWidth = textureWidth - textureSideWidth * 2, texCenterHeight = textureHeight - textureSideHeight * 2;
        final int centreWidth = width - leftWidth - sideWidth, centerHeight = height - topHeight - sideHeight;

        // Calculate the corner positions
        final var leftEdgeEnd = left + leftWidth;
        final var rightEdgeStart = leftEdgeEnd + centreWidth;
        final var topEdgeEnd = top + topHeight;
        final var bottomEdgeStart = topEdgeEnd + centerHeight;
        RenderSystem.setShaderTexture(0, resource);

        // Top Left Corner
        Screen.blit(matrix, left, top, 0, 0, leftWidth, topHeight, textureWidth, textureHeight);
        // Bottom Left Corner
        Screen.blit(matrix, left, bottomEdgeStart, 0, textureHeight - sideHeight, leftWidth, sideHeight, textureWidth, textureHeight);

        // Middle
        if (centreWidth > 0) {
            // Top Middle
            blitTiled(matrix, leftEdgeEnd, top, centreWidth, topHeight, textureSideWidth, 0, texCentreWidth, textureSideHeight, textureWidth, textureHeight);
            if (centerHeight > 0) {
                // Centre
                blitTiled(matrix, leftEdgeEnd, topEdgeEnd, centreWidth, centerHeight, textureSideWidth, textureSideHeight, texCentreWidth, texCenterHeight, textureWidth, textureHeight);
            }
            // Bottom Middle
            blitTiled(matrix, leftEdgeEnd, bottomEdgeStart, centreWidth, sideHeight, textureSideWidth, textureHeight - sideHeight, texCentreWidth, textureSideHeight, textureWidth, textureHeight);
        }

        if (centerHeight > 0) {
            // Left Middle
            blitTiled(matrix, left, topEdgeEnd, leftWidth, centerHeight, 0, textureSideHeight, textureSideWidth, texCenterHeight, textureWidth, textureHeight);
            // Right Middle
            blitTiled(matrix, rightEdgeStart, topEdgeEnd, sideWidth, centerHeight, textureWidth - sideWidth, textureSideHeight, textureSideWidth, texCenterHeight, textureWidth, textureHeight);
        }

        // Top Right Corner
        Screen.blit(matrix, rightEdgeStart, top, textureWidth - sideWidth, 0, sideWidth, topHeight, textureWidth, textureHeight);
        // Bottom Right Corner
        GuiComponent.blit(matrix, rightEdgeStart, bottomEdgeStart, textureWidth - sideWidth, textureHeight - sideHeight, sideWidth, sideHeight, textureWidth, textureHeight);
    }

    /**
     * Renders a tiled texture.
     */
    public static void blitTiled(PoseStack matrix, int x, int y, int width, int height, int texX, int texY, int texDrawWidth, int texDrawHeight, int textureWidth, int textureHeight) {
        // Calculate the amount of tiles
        final int xTiles = (int) Math.ceil((float) width / texDrawWidth), yTiles = (int) Math.ceil((float) height / texDrawHeight);

        var drawWidth = width;
        var drawHeight = height;
        for (var tileX = 0; tileX < xTiles; tileX++) {
            for (var tileY = 0; tileY < yTiles; tileY++) {
                final var renderWidth = Math.min(drawWidth, texDrawWidth);
                final var renderHeight = Math.min(drawHeight, texDrawHeight);
                Screen.blit(matrix, x + texDrawWidth * tileX, y + texDrawHeight * tileY, texX, texY, renderWidth, renderHeight, textureWidth, textureHeight);
                // We rendered a tile
                drawHeight -= texDrawHeight;
            }
            drawWidth -= texDrawWidth;
            drawHeight = height;
        }
    }

    public static boolean checkChildren(List<? extends GuiEventListener> children, Predicate<GuiElement> checker) {
        for (final var child : Utils.reverseIterator(children)) {
            if (child instanceof GuiElement element && checker.test(element)) {
                return true;
            }
        }
        return false;
    }

    public static void renderItem(PoseStack matrix, ItemRenderer renderer, @Nonnull ItemStack stack, int xAxis, int yAxis, float scale, Font font,
                                  @Nullable String text, boolean overlay) {
        if (!stack.isEmpty()) {
            try {
                matrix.pushPose();
                RenderSystem.enableDepthTest();
                if (scale != 1) {
                    //Translate before scaling, and then set xAxis and yAxis to zero so that we don't translate a second time
                    matrix.translate(xAxis, yAxis, 0);
                    matrix.scale(scale, scale, scale);
                    xAxis = 0;
                    yAxis = 0;
                }
                //Apply our matrix stack to the render system and pass an unmodified one to the render methods
                // Vanilla still renders the items using render system transformations so this is required to
                // have things render in the correct order
                PoseStack modelViewStack = RenderSystem.getModelViewStack();
                modelViewStack.pushPose();
                modelViewStack.mulPoseMatrix(matrix.last().pose());
                RenderSystem.applyModelViewMatrix();
                renderer.renderAndDecorateItem(stack, xAxis, yAxis);
                if (overlay) {
                    //When we render items ourselves in virtual slots or scroll slots we want to compress the z scale
                    // for rendering the stored count so that it doesn't clip with later windows
                    float previousOffset = renderer.blitOffset;
                    renderer.blitOffset -= 25;
                    renderer.renderGuiItemDecorations(font, stack, xAxis, yAxis, text);
                    renderer.blitOffset = previousOffset;
                }
                modelViewStack.popPose();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.disableDepthTest();
                matrix.popPose();
            } catch (Exception e) {
                Utils.LOGGER.error("Failed to render stack into gui: {}", stack, e);
            }
        }
    }

    public static void renderColourOverlay(PoseStack matrix, int x, int y, int width, int height, Color colour) {
        final var r = colour.rf();
        final var g = colour.gf();
        final var b = colour.bf();
        final var a = colour.af();
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        final var tesselator = Tesselator.getInstance();
        final var builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        final var matrix4f = matrix.last().pose();
        builder.vertex(matrix4f, width, y, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix4f, x, y, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix4f, x, height, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix4f, width, height, 0).color(r, g, b, a).endVertex();
        tesselator.end();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
    }

    public static float getPartialTicks() {
        return Minecraft.getInstance().getFrameTime();
    }

    public static void sendPacketToServer(Object packet) {
        if (Minecraft.getInstance().getConnection() == null)
            return;
        final var conn = NetworkHooks.getConnectionData(Minecraft.getInstance().getConnection().getConnection());
        if (conn == null)
            return;
        final var requiredVersion = packet instanceof Packet pkt ? pkt.getRequiredVersion() : SimpleGuiNetwork.VERSION;
        final var chan = conn.getChannels().get(SimpleGuiNetwork.NAME);
        if (Objects.equals(chan, requiredVersion))
            SimpleGuiNetwork.CHANNEL.sendToServer(packet);
    }
}
