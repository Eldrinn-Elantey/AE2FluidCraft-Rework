package com.glodblock.github.client.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.client.gui.container.ContainerRenamer;
import com.glodblock.github.network.CPacketRenamer;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.IDropToFillTextField;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;

public class GuiRenamer extends AEBaseGui implements IDropToFillTextField {

    private final MEGuiTextField textField;

    public GuiRenamer(InventoryPlayer ip, ITerminalHost monitorable) {
        super(new ContainerRenamer(ip, monitorable));
        this.xSize = 256;

        this.textField = new MEGuiTextField(230, 12);
        this.textField.setMaxStringLength(32);
    }

    @Override
    public void initGui() {
        super.initGui();
        FluidCraft.proxy.netHandler.sendToServer(new CPacketRenamer(CPacketRenamer.Action.GET_TEXT));
        this.textField.x = this.guiLeft + 12;
        this.textField.y = this.guiTop + 35;
        this.textField.setFocused(true);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRendererObj.drawString(GuiText.Renamer.getLocal(), 12, 8, GuiColors.RenamerTitle.getColor());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/renamer.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        this.textField.drawTextBox();
    }

    @Override
    protected void mouseClicked(final int xCoord, final int yCoord, final int btn) {
        this.textField.mouseClicked(xCoord, yCoord, btn);
        super.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            FluidCraft.proxy.netHandler.sendToServer(new CPacketRenamer(this.textField.getText()));
        } else if (!this.textField.textboxKeyTyped(character, key)) {
            super.keyTyped(character, key);
        }
    }

    public boolean isOverTextField(final int mousex, final int mousey) {
        return textField.isMouseIn(mousex, mousey);
    }

    public void setTextFieldValue(final String displayName, final int mousex, final int mousey, final ItemStack stack) {
        textField.setText(displayName);
    }

    public void postUpdate(String text) {
        this.textField.setText(text);
    }
}
