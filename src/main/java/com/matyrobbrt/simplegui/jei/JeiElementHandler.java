package com.matyrobbrt.simplegui.jei;

import com.matyrobbrt.simplegui.client.SimpleGui;
import com.matyrobbrt.simplegui.client.element.GuiElement;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class JeiElementHandler implements IGuiContainerHandler<SimpleGui<?>> {

    private static boolean isOutside(int x, int y, int width, int height, int parentX, int parentY, int parentWidth, int parentHeight) {
        return x < parentX || y < parentY || x + width > parentX + parentWidth || y + height > parentY + parentHeight;
    }

    private static List<Rect2i> getAreasFor(int parentX, int parentY, int parentWidth, int parentHeight, Collection<? extends GuiEventListener> children) {
        final var areas = new ArrayList<Rect2i>();
        for (final var child : children) {
            if (child instanceof final AbstractWidget widget && widget.visible) {
                if (isOutside(widget.x, widget.y, widget.getWidth(), widget.getHeight(), parentX, parentY, parentWidth, parentHeight)) {
                    areas.add(new Rect2i(widget.x, widget.y, widget.getWidth(), widget.getHeight()));
                }
                if (widget instanceof final GuiElement element) {
                    for (final var grandChildArea : getAreasFor(widget.x, widget.y, widget.getWidth(), widget.getHeight(), element.children())) {
                        if (isOutside(grandChildArea.getX(), grandChildArea.getY(), grandChildArea.getWidth(), grandChildArea.getHeight(),
                                parentX, parentY, parentWidth, parentHeight)) {
                            areas.add(grandChildArea);
                        }
                    }
                }
            }
        }
        return areas;
    }

    @Override
    public @NotNull List<Rect2i> getGuiExtraAreas(SimpleGui<?> gui) {
        final var parentX = gui.getLeft();
        final var parentY = gui.getTop();
        final var parentWidth = gui.getWidth();
        final var parentHeight = gui.getHeight();
        // Add children that may be outside
        final var extraAreas = getAreasFor(parentX, parentY, parentWidth, parentHeight, gui.children());
        // Add windows
        extraAreas.addAll(getAreasFor(parentX, parentY, parentWidth, parentHeight, gui.getWindows()));
        return extraAreas;
    }

    @Nullable
    @Override
    public Object getIngredientUnderMouse(SimpleGui<?> gui, double mouseX, double mouseY) {
        final var guiWindow = gui.getWindowHovering(mouseX, mouseY);
        return getIngredientUnderMouse(guiWindow == null ? gui.children() : guiWindow.children(), mouseX, mouseY);
    }

    @Nullable
    private Object getIngredientUnderMouse(List<? extends GuiEventListener> children, double mouseX, double mouseY) {
        for (final var child : children) {
            if (child instanceof final AbstractWidget widget) {
                if (!widget.visible) {
                    continue;
                }
                if (widget instanceof final GuiElement element) {
                    // Check the element's children
                    final var underGrandChild = getIngredientUnderMouse(element.children(), mouseX, mouseY);
                    if (underGrandChild != null) {
                        return underGrandChild;
                    }
                }
            }
            if (child instanceof final JEITarget target && child.isMouseOver(mouseX, mouseY)) {
                return target.getIngredient(mouseX, mouseY);
            }
        }
        return null;
    }
}
