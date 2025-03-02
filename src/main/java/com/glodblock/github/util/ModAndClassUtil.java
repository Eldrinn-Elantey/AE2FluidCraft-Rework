package com.glodblock.github.util;

import java.lang.reflect.Field;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.core.AEConfig;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import cpw.mods.fml.common.Loader;

public final class ModAndClassUtil {

    public static boolean GT5 = false;
    public static boolean GT6 = false;
    public static boolean EC2 = false;
    public static boolean EIO = false;
    public static boolean FTR = false;
    public static boolean OC = false;
    public static boolean ThE = false;
    public static boolean WCT = false;
    public static boolean IC2 = false;
    public static boolean NEI = false;
    public static boolean COFH = false;
    public static boolean GTPP = false;
    public static boolean WAILA = false;
    public static boolean AVARITIA = false;
    public static boolean HODGEPODGE = false;

    public static boolean isV2;
    public static boolean isTypeFilter;
    public static boolean isDoubleButton;
    public static boolean isSaveText;
    public static boolean isSearchStringTooltip;
    public static boolean isCraftStatus;
    public static boolean isSearchBar;
    public static boolean isShiftTooltip;
    public static boolean isBigInterface;

    public static boolean isBeSubstitutionsButton;

    @SuppressWarnings("all")
    public static void init() {

        try {
            Field d = Upgrades.class.getDeclaredField("PATTERN_CAPACITY");
            if (d == null) isBigInterface = false;
            isBigInterface = true;
        } catch (NoSuchFieldException e) {
            isBigInterface = false;
        }

        try {
            Field d = GuiText.class.getDeclaredField("HoldShiftForTooltip");
            if (d == null) isShiftTooltip = false;
            isShiftTooltip = true;
        } catch (NoSuchFieldException e) {
            isShiftTooltip = false;
        }

        try {
            Field d = AEConfig.instance.getClass().getDeclaredField("preserveSearchBar");
            if (d == null) isSearchBar = false;
            isSearchBar = true;
        } catch (NoSuchFieldException e) {
            isSearchBar = false;
        }

        try {
            Field d = ActionItems.class.getDeclaredField("DOUBLE");
            if (d == null) isDoubleButton = false;
            isDoubleButton = true;
        } catch (NoSuchFieldException e) {
            isDoubleButton = false;
        }
        try {
            Field d = ButtonToolTips.class.getDeclaredField("BeSubstitutionsDescEnabled");
            isBeSubstitutionsButton = true;
        } catch (NoSuchFieldException e) {
            isBeSubstitutionsButton = false;
        }

        try {
            Field d = Settings.class.getDeclaredField("SAVE_SEARCH");
            if (d == null) isSaveText = false;
            isSaveText = true;
        } catch (NoSuchFieldException e) {
            isSaveText = false;
        }

        try {
            Field d = ButtonToolTips.class.getDeclaredField("SearchStringTooltip");
            if (d == null) isSearchStringTooltip = false;
            isSearchStringTooltip = true;
        } catch (NoSuchFieldException e) {
            isSearchStringTooltip = false;
        }

        try {
            Field d = Settings.class.getDeclaredField("CRAFTING_STATUS");
            if (d == null) isCraftStatus = false;
            isCraftStatus = true;
        } catch (NoSuchFieldException e) {
            isCraftStatus = false;
        }

        try {
            Class<?> calculatorV2 = Class.forName("appeng.crafting.v2.CraftingCalculations");
            if (calculatorV2 == null) isV2 = false;
            isV2 = true;
        } catch (ClassNotFoundException e) {
            isV2 = false;
        }
        try {
            Class<?> filter = Class.forName("appeng.core.features.registries.ItemDisplayRegistry");
            isTypeFilter = true;
        } catch (ClassNotFoundException e) {
            isTypeFilter = false;
        }

        if (Loader.isModLoaded("gregtech") && !Loader.isModLoaded("gregapi")) GT5 = true;
        if (Loader.isModLoaded("gregapi") && Loader.isModLoaded("gregapi_post")) GT6 = true;
        if (Loader.isModLoaded("extracells")) EC2 = true;
        if (Loader.isModLoaded("EnderIO")) EIO = true;
        if (Loader.isModLoaded("Forestry")) FTR = true;
        if (Loader.isModLoaded("OpenComputers")) OC = true;
        if (Loader.isModLoaded("thaumicenergistics")) ThE = true;
        if (Loader.isModLoaded("ae2wct")) WCT = true;
        if (Loader.isModLoaded("IC2")) IC2 = true;
        if (Loader.isModLoaded("NotEnoughItems")) NEI = true;
        if (Loader.isModLoaded("CoFHCore")) COFH = true;
        if (Loader.isModLoaded("miscutils")) GTPP = true;
        if (Loader.isModLoaded("Waila")) WAILA = true;
        if (Loader.isModLoaded("Avaritia")) AVARITIA = true;
        if (Loader.isModLoaded("hodgepodge")) HODGEPODGE = true;
    }
}
