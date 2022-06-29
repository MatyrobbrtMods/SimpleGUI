package com.matyrobbrt.simplegui.client;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.matyrobbrt.simplegui.util.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public interface SpecialFontRenderer {
    int getXSize();

    Font getFont();

    default int titleTextColor() {
        return 0xFF404040;
    }

    @CanIgnoreReturnValue
    default int drawString(PoseStack matrix, Component component, int x, int y, int color) {
        return getFont().draw(matrix, component, x, y, color);
    }

    default int getStringWidth(final Component component) {
        return getFont().width(component);
    }

    default void drawCenteredText(final PoseStack matrix, final Component component, final float x, final float y, final int color) {
        drawCenteredText(matrix, component, x, 0, y, color);
    }

    default void drawCenteredText(final PoseStack matrix, final Component component, final float xStart, final float areaWidth, final float y, final int color) {
        final int textWidth = getStringWidth(component);
        final float centerX = xStart + (areaWidth / 2F) - (textWidth / 2F);
        drawTextExact(matrix, component, centerX, y, color);
    }

    default void drawTitleText(final PoseStack matrix, final Component text, final float y) {
        drawCenteredTextScaledBound(matrix, text, getXSize() - 8, y, titleTextColor());
    }

    default void drawScaledCenteredText(final PoseStack matrix, final Component text, final float left, float y, final int color, final float scale) {
        int textWidth = getStringWidth(text);
        float centerX = left - (textWidth / 2F) * scale;
        drawTextWithScale(matrix, text, centerX, y, color, scale);
    }

    default void drawCenteredTextScaledBound(final PoseStack matrix, final Component text, final float maxLength, final float y, final int color) {
        drawCenteredTextScaledBound(matrix, text, maxLength, 0, y, color);
    }

    default void drawCenteredTextScaledBound(final PoseStack matrix, final Component text, final float maxLength, final float x, float y, final int color) {
        float scale = Math.min(1, maxLength / getStringWidth(text));
        drawScaledCenteredText(matrix, text, x + getXSize() / 2F, y, color, scale);
    }

    default void drawTextExact(final PoseStack matrix, final Component text, final float x, final float y, final int color) {
        matrix.pushPose();
        matrix.translate(x, y, 0);
        drawString(matrix, text, 0, 0, color);
        matrix.popPose();
    }

    default void drawTextWithScale(final PoseStack matrix, final Component text, final float x, final float y, final int color, final float scale) {
        prepTextScale(matrix, m -> drawString(m, text, 0, 0, color), x, y, scale);
    }

    default void prepTextScale(final PoseStack matrix, final Consumer<PoseStack> runnable, final float x, final float y, final float scale) {
        final float yAdd = 4 - (scale * 8) / 2F;
        matrix.pushPose();
        matrix.translate(x, y + yAdd, 0);
        matrix.scale(scale, scale, scale);
        runnable.accept(matrix);
        matrix.popPose();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    default WrappedTextRenderer newRenderer(Component text) {
        return new WrappedTextRenderer(this, text);
    }

    default WrappedTextRenderer newRenderer(String text) {
        return new WrappedTextRenderer(this, text);
    }

    class WrappedTextRenderer {
        private final List<LineData> linesToDraw = new ArrayList<>();
        private final SpecialFontRenderer font;
        private final String text;
        @Nullable
        private Font lastFont;
        private float lastMaxLength = -1;
        private float lineLength = 0;

        public WrappedTextRenderer(SpecialFontRenderer font, Component text) {
            this(font, text.getString());
        }

        public WrappedTextRenderer(SpecialFontRenderer font, String text) {
            this.font = font;
            this.text = text;
        }

        public void renderCentered(final PoseStack matrix, final float x, final float y, final int color, final float maxLength) {
            calculateLines(maxLength);
            float startY = y;
            for (LineData line : linesToDraw) {
                font.drawTextExact(matrix, line.component(), x - line.length() / 2, startY, color);
                startY += 9;
            }
        }

        public int renderWithScale(final PoseStack matrix, final float x, final float y, final int color, final float maxLength, final float scale) {
            calculateLines(maxLength / scale);
            font.prepTextScale(matrix, m -> {
                int startY = 0;
                for (LineData line : linesToDraw) {
                    font.drawString(m, line.component(), 0, startY, color);
                    startY += 9;
                }
            }, x, y, scale);
            return linesToDraw.size();
        }

        void calculateLines(final float maxLength) {
            //If something changed since the last time we calculated it
            final var font = this.font.getFont();
            if (font != null && (lastFont != font || lastMaxLength != maxLength)) {
                lastFont = font;
                lastMaxLength = maxLength;
                linesToDraw.clear();
                StringBuilder lineBuilder = new StringBuilder();
                StringBuilder wordBuilder = new StringBuilder();
                int spaceLength = lastFont.width(" ");
                int wordLength = 0;
                for (char c : text.toCharArray()) {
                    if (c == ' ') {
                        lineBuilder = addWord(lineBuilder, wordBuilder, maxLength, spaceLength, wordLength);
                        wordBuilder = new StringBuilder();
                        wordLength = 0;
                        continue;
                    }
                    wordBuilder.append(c);
                    wordLength += lastFont.width(Character.toString(c));
                }
                if (!wordBuilder.isEmpty()) {
                    lineBuilder = addWord(lineBuilder, wordBuilder, maxLength, spaceLength, wordLength);
                }
                if (!lineBuilder.isEmpty()) {
                    linesToDraw.add(new LineData(Utils.getString(lineBuilder.toString()), lineLength));
                }
            }
        }

        StringBuilder addWord(StringBuilder lineBuilder, StringBuilder wordBuilder, float maxLength, int spaceLength, int wordLength) {
            final float spacingLength = lineBuilder.isEmpty() ? 0 : spaceLength;
            if (lineLength + spacingLength + wordLength > maxLength) {
                linesToDraw.add(new LineData(Utils.getString(lineBuilder.toString()), lineLength));
                lineBuilder = new StringBuilder(wordBuilder);
                lineLength = wordLength;
            } else {
                if (spacingLength > 0) {
                    lineBuilder.append(" ");
                }
                lineBuilder.append(wordBuilder);
                lineLength += spacingLength + wordLength;
            }
            return lineBuilder;
        }

        public static int calculateHeightRequired(final Font font, final Component text, final int width, final float maxLength) {
            return calculateHeightRequired(font, text.getString(), width, maxLength);
        }

        public static int calculateHeightRequired(final Font font, final String text, final int width, final float maxLength) {
            final WrappedTextRenderer wrappedTextRenderer = new WrappedTextRenderer(new SpecialFontRenderer() {
                @Override
                public int getXSize() {
                    return width;
                }

                @Override
                public Font getFont() {
                    return font;
                }
            }, text);
            wrappedTextRenderer.calculateLines(maxLength);
            return 9 * wrappedTextRenderer.linesToDraw.size();
        }

        private record LineData(Component component, float length) {}
    }
}
