package com.glodblock.github.client.gui.container;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.glodblock.github.client.gui.container.base.FCBaseContainer;
import com.glodblock.github.inventory.item.IWirelessTerminal;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.util.DimensionalCoord;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketCompressedNBT;
import appeng.helpers.IInterfaceTerminalSupport;
import appeng.helpers.InterfaceTerminalSupportedClassProvider;
import appeng.helpers.InventoryAction;
import appeng.items.misc.ItemEncodedPattern;
import appeng.parts.AEBasePart;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.AdaptorIInventory;
import appeng.util.inv.AdaptorPlayerHand;
import appeng.util.inv.ItemSlot;
import appeng.util.inv.WrapperInvSlot;

public class ContainerInterfaceWireless extends FCBaseContainer {

    /**
     * this stuff is all server side
     */
    private static long autoBase = Long.MIN_VALUE;

    private final Multimap<IInterfaceTerminalSupport, InvTracker> supportedInterfaces = HashMultimap.create();
    private final Map<Long, InvTracker> byId = new HashMap<>();
    private IGrid grid;
    private NBTTagCompound data = new NBTTagCompound();

    public ContainerInterfaceWireless(final InventoryPlayer ip, final IWirelessTerminal monitorable) {
        super(ip, monitorable);
        if (Platform.isServer()) {
            this.grid = monitorable.getActionableNode().getGrid();
        }

        this.bindPlayerInventory(ip, 14, 0);
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isClient()) {
            return;
        }
        super.detectAndSendChanges();

        if (this.grid == null) {
            return;
        }

        int total = 0;
        boolean missing = false;

        final IActionHost host = this.getActionHost();
        if (host != null) {
            final IGridNode agn = host.getActionableNode();
            if (agn != null && agn.isActive()) {
                for (var clz : InterfaceTerminalSupportedClassProvider.getSupportedClasses()) {
                    for (final IGridNode gn : this.grid.getMachines(clz)) {
                        final IInterfaceTerminalSupport interfaceTerminalSupport = (IInterfaceTerminalSupport) gn
                                .getMachine();
                        if (!gn.isActive() || !interfaceTerminalSupport.shouldDisplay()) continue;

                        final Collection<InvTracker> t = supportedInterfaces.get(interfaceTerminalSupport);
                        final String name = interfaceTerminalSupport.getName();
                        missing = t.isEmpty() || t.stream().anyMatch(it -> !it.unlocalizedName.equals(name));
                        total += interfaceTerminalSupport.getPatternsConfigurations().length;
                        if (missing) break;
                    }
                    // we can stop if any is missing. The value of `total` is not important if `missing == true`
                    if (missing) break;
                }
            }
        }

        if (total != this.supportedInterfaces.size() || missing) {
            this.regenList(this.data);
        } else {
            for (final ContainerInterfaceWireless.InvTracker inv : supportedInterfaces.values()) {
                for (int x = 0; x < inv.client.getSizeInventory(); x++) {
                    if (this.isDifferent(inv.server.getStackInSlot(inv.offset + x), inv.client.getStackInSlot(x))) {
                        this.addItems(this.data, inv, x, 1);
                    }
                }
            }
        }

        if (!this.data.hasNoTags()) {
            try {
                NetworkHandler.instance
                        .sendTo(new PacketCompressedNBT(this.data), (EntityPlayerMP) this.getPlayerInv().player);
            } catch (final IOException ignored) {}

            this.data = new NBTTagCompound();
        }
    }

    @Override
    public void doAction(final EntityPlayerMP player, final InventoryAction action, final int slot, final long id) {
        final ContainerInterfaceWireless.InvTracker inv = this.byId.get(id);
        if (inv != null) {
            final ItemStack is = inv.server.getStackInSlot(slot + inv.offset);
            final boolean hasItemInHand = player.inventory.getItemStack() != null;

            final InventoryAdaptor playerHand = new AdaptorPlayerHand(player);

            final WrapperInvSlot slotInv = new ContainerInterfaceWireless.PatternInvSlot(inv.server);

            final IInventory theSlot = slotInv.getWrapper(slot + inv.offset);
            final InventoryAdaptor interfaceSlot = new AdaptorIInventory(theSlot);

            IInventory interfaceHandler = inv.server;
            boolean canInsert = true;

            switch (action) {
                case PICKUP_OR_SET_DOWN -> {
                    if (hasItemInHand) {
                        for (int s = 0; s < interfaceHandler.getSizeInventory(); s++) {
                            if (Platform.isSameItemPrecise(
                                    interfaceHandler.getStackInSlot(s),
                                    player.inventory.getItemStack())) {
                                canInsert = false;
                                break;
                            }
                        }
                        if (canInsert) {
                            ItemStack inSlot = theSlot.getStackInSlot(0);
                            if (inSlot == null) {
                                player.inventory.setItemStack(interfaceSlot.addItems(player.inventory.getItemStack()));
                            } else {
                                inSlot = inSlot.copy();
                                final ItemStack inHand = player.inventory.getItemStack().copy();

                                theSlot.setInventorySlotContents(0, null);
                                player.inventory.setItemStack(null);

                                player.inventory.setItemStack(interfaceSlot.addItems(inHand.copy()));

                                if (player.inventory.getItemStack() == null) {
                                    player.inventory.setItemStack(inSlot);
                                } else {
                                    player.inventory.setItemStack(inHand);
                                    theSlot.setInventorySlotContents(0, inSlot);
                                }
                            }
                        }
                    } else {
                        final IInventory mySlot = slotInv.getWrapper(slot + inv.offset);
                        mySlot.setInventorySlotContents(0, playerHand.addItems(mySlot.getStackInSlot(0)));
                    }
                }
                case SPLIT_OR_PLACE_SINGLE -> {
                    if (hasItemInHand) {
                        for (int s = 0; s < interfaceHandler.getSizeInventory(); s++) {
                            if (Platform.isSameItemPrecise(
                                    interfaceHandler.getStackInSlot(s),
                                    player.inventory.getItemStack())) {
                                canInsert = false;
                                break;
                            }
                        }
                        if (canInsert) {
                            ItemStack extra = playerHand.removeItems(1, null, null);
                            if (extra != null && !interfaceSlot.containsItems()) {
                                extra = interfaceSlot.addItems(extra);
                            }
                            if (extra != null) {
                                playerHand.addItems(extra);
                            }
                        }
                    } else if (is != null) {
                        ItemStack extra = interfaceSlot.removeItems((is.stackSize + 1) / 2, null, null);
                        if (extra != null) {
                            extra = playerHand.addItems(extra);
                        }
                        if (extra != null) {
                            interfaceSlot.addItems(extra);
                        }
                    }
                }
                case SHIFT_CLICK -> {
                    final IInventory mySlot = slotInv.getWrapper(slot + inv.offset);
                    final InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
                    mySlot.setInventorySlotContents(0, mergeToPlayerInventory(playerInv, mySlot.getStackInSlot(0)));
                }
                case MOVE_REGION -> {
                    final InventoryAdaptor playerInvAd = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
                    for (int x = 0; x < inv.client.getSizeInventory(); x++) {
                        inv.server.setInventorySlotContents(
                                x + inv.offset,
                                mergeToPlayerInventory(playerInvAd, inv.server.getStackInSlot(x + inv.offset)));
                    }
                }
                case CREATIVE_DUPLICATE -> {
                    if (player.capabilities.isCreativeMode && !hasItemInHand) {
                        player.inventory.setItemStack(is == null ? null : is.copy());
                    }
                }
                default -> {
                    return;
                }
            }

            this.updateHeld(player);
        }
    }

    private ItemStack mergeToPlayerInventory(InventoryAdaptor playerInv, ItemStack stack) {
        if (stack == null) return null;
        for (ItemSlot slot : playerInv) {
            if (Platform.isSameItemPrecise(slot.getItemStack(), stack)) {
                if (slot.getItemStack().stackSize < slot.getItemStack().getMaxStackSize()) {
                    ++slot.getItemStack().stackSize;
                    return null;
                }
            }
        }
        return playerInv.addItems(stack);
    }

    private void regenList(final NBTTagCompound data) {
        this.byId.clear();
        this.supportedInterfaces.clear();

        final IActionHost host = this.getActionHost();
        if (host != null) {
            final IGridNode agn = host.getActionableNode();
            if (agn != null && agn.isActive()) {
                for (var clz : InterfaceTerminalSupportedClassProvider.getSupportedClasses()) {
                    for (final IGridNode gn : this.grid.getMachines(clz)) {
                        final IInterfaceTerminalSupport terminalSupport = (IInterfaceTerminalSupport) gn.getMachine();
                        if (!gn.isActive() || !terminalSupport.shouldDisplay()) continue;

                        final var configurations = terminalSupport.getPatternsConfigurations();

                        for (int i = 0; i < configurations.length; ++i) {
                            this.supportedInterfaces.put(
                                    terminalSupport,
                                    new ContainerInterfaceWireless.InvTracker(terminalSupport, configurations[i], i));
                        }
                    }
                }
            }
        }

        data.setBoolean("clear", true);

        for (final ContainerInterfaceWireless.InvTracker inv : this.supportedInterfaces.values()) {
            this.byId.put(inv.which, inv);
            this.addItems(data, inv, 0, inv.client.getSizeInventory());
        }
    }

    @Override
    protected boolean isWirelessTerminal() {
        return true;
    }

    private boolean isDifferent(final ItemStack a, final ItemStack b) {
        if (a == null && b == null) {
            return false;
        }

        if (a == null || b == null) {
            return true;
        }

        return !ItemStack.areItemStacksEqual(a, b);
    }

    private void addItems(final NBTTagCompound data, final ContainerInterfaceWireless.InvTracker inv, final int offset,
            final int length) {
        final String name = '=' + Long.toString(inv.which, Character.MAX_RADIX);
        final NBTTagCompound tag = data.getCompoundTag(name);

        if (tag.hasNoTags()) {
            tag.setLong("sortBy", inv.sortBy);
            tag.setString("un", inv.unlocalizedName);
            tag.setInteger("x", inv.X);
            tag.setInteger("y", inv.Y);
            tag.setInteger("z", inv.Z);
            tag.setInteger("dim", inv.dim);
            tag.setInteger("side", inv.side.ordinal());
            tag.setInteger("size", inv.client.getSizeInventory());
        }

        for (int x = 0; x < length; x++) {
            final NBTTagCompound itemNBT = new NBTTagCompound();

            final ItemStack is = inv.server.getStackInSlot(x + offset + inv.offset);

            // "update" client side.
            inv.client.setInventorySlotContents(offset + x, is == null ? null : is.copy());

            if (is != null) {
                is.writeToNBT(itemNBT);
            }

            tag.setTag(Integer.toString(x + offset), itemNBT);
        }

        data.setTag(name, tag);
    }

    private static class InvTracker {

        private final long sortBy;
        private final long which = autoBase++;
        private final String unlocalizedName;
        private final IInventory client;
        private final IInventory server;
        private final int offset;
        private final int X;
        private final int Y;
        private final int Z;
        private final int dim;
        private final ForgeDirection side;

        public InvTracker(final DimensionalCoord coord, long sortValue, final IInventory patterns,
                final String unlocalizedName, int offset, int size, ForgeDirection side) {
            this(
                    coord.x,
                    coord.y,
                    coord.z,
                    coord.getDimension(),
                    sortValue,
                    patterns,
                    unlocalizedName,
                    offset,
                    size,
                    side);
        }

        public InvTracker(int x, int y, int z, int dim, long sortValue, final IInventory patterns,
                final String unlocalizedName, int offset, int size, ForgeDirection side) {
            this.server = patterns;
            this.client = new AppEngInternalInventory(null, size);
            this.unlocalizedName = unlocalizedName;
            this.sortBy = sortValue + offset << 16;
            this.offset = offset;
            this.X = x;
            this.Y = y;
            this.Z = z;
            this.dim = dim;
            this.side = side;
        }

        public InvTracker(IInterfaceTerminalSupport terminalSupport,
                IInterfaceTerminalSupport.PatternsConfiguration configuration, int index) {
            this(
                    terminalSupport.getLocation(),
                    terminalSupport.getSortValue(),
                    terminalSupport.getPatterns(index),
                    terminalSupport.getName(),
                    configuration.offset,
                    configuration.size,
                    terminalSupport instanceof AEBasePart ? ((AEBasePart) terminalSupport).getSide()
                            : ForgeDirection.UNKNOWN);
        }
    }

    private static class PatternInvSlot extends WrapperInvSlot {

        public PatternInvSlot(final IInventory inv) {
            super(inv);
        }

        @Override
        public boolean isItemValid(final ItemStack itemstack) {
            return itemstack != null && itemstack.getItem() instanceof ItemEncodedPattern;
        }
    }
}
