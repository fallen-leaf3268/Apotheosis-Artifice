package com.apotheosis_artifice;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import net.minecraft.world.item.ItemStack;

/**
 * 重铸期间临时覆盖 {@code LootCategory.forItem} 的返回值。
 * 用 ThreadLocal 而非普通 static 字段：forItem 被全局 patch，会在多个线程
 * （集成服的客户端渲染线程等）被调用；普通 static 字段会造成跨线程可见性竞争
 * 与跨物品串味。set(null) 等价于清除。
 */
public class CatOverride {
    private static final ThreadLocal<LootCategory> OVERRIDE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> NATIVE_LOOKUP = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> REFORGING = new ThreadLocal<>();

    public static void set(LootCategory cat) {
        if (cat == null) OVERRIDE.remove();
        else OVERRIDE.set(cat);
    }

    public static LootCategory get() {
        return OVERRIDE.get();
    }

    public static void clear() {
        OVERRIDE.remove();
    }

    public static LootCategory forNativeItem(ItemStack stack) {
        if (stack.isEmpty()) return LootCategory.NONE;
        LootCategory previous = OVERRIDE.get();
        Boolean previousLookup = NATIVE_LOOKUP.get();
        OVERRIDE.remove();
        NATIVE_LOOKUP.set(true);
        try {
            return LootCategory.forItem(stack);
        } finally {
            set(previous);
            if (previousLookup == null) NATIVE_LOOKUP.remove();
            else NATIVE_LOOKUP.set(previousLookup);
        }
    }

    public static boolean isNativeLookup() {
        return Boolean.TRUE.equals(NATIVE_LOOKUP.get());
    }

    public static boolean isReforging() {
        return Boolean.TRUE.equals(REFORGING.get());
    }

    public static void setReforging(boolean reforging) {
        if (reforging) REFORGING.set(true);
        else REFORGING.remove();
    }
}
