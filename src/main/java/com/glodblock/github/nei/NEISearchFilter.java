package com.glodblock.github.nei;

import java.util.regex.Pattern;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.glodblock.github.common.storage.IFluidCellInventory;
import com.glodblock.github.common.storage.IFluidCellInventoryHandler;
import com.glodblock.github.common.storage.IStorageFluidCell;

import appeng.api.AEApi;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import codechicken.nei.SearchField;
import codechicken.nei.SearchTokenParser;
import codechicken.nei.api.ItemFilter;

public class NEISearchFilter implements SearchTokenParser.ISearchParserProvider {

    @Override
    public ItemFilter getFilter(String searchText) {
        Pattern pattern = SearchField.getPattern(searchText);
        return pattern == null ? null : new Filter(pattern);
    }

    @Override
    public char getPrefix() {
        return 0;
    }

    @Override
    public EnumChatFormatting getHighlightedColor() {
        return null;
    }

    @Override
    public SearchTokenParser.SearchMode getSearchMode() {
        return SearchTokenParser.SearchMode.ALWAYS;
    }

    public static class Filter implements ItemFilter {

        Pattern pattern;

        public Filter(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matches(ItemStack itemStack) {
            if (itemStack.getItem() instanceof IStorageFluidCell) {
                final IMEInventoryHandler<?> inventory = AEApi.instance().registries().cell()
                    .getCellInventory(itemStack, null, StorageChannel.FLUIDS);
                if (inventory instanceof final IFluidCellInventoryHandler handler) {
                    final IFluidCellInventory cellInventory = handler.getCellInv();
                    if (cellInventory != null) {
                        for (IAEFluidStack fluid : cellInventory.getContents()) {
                            boolean result = pattern.matcher(fluid.getFluidStack().getLocalizedName().toLowerCase())
                                .find();
                            if (result) return true;
                        }

                    }
                }
            }
            return false;
        }

    }
}
