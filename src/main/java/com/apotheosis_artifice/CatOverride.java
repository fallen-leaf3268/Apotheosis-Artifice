package com.apotheosis_artifice;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;

/**
 * 重铸期间临时覆盖 {@code LootCategory.forItem} 的返回值。
 * 用 ThreadLocal 而非普通 static 字段：forItem 被全局 patch，会在多个线程
 * （集成服的客户端渲染线程等）被调用；普通 static 字段会造成跨线程可见性竞争
 * 与跨物品串味。set(null) 等价于清除。
 */
public class CatOverride {
    private static final ThreadLocal<LootCategory> OVERRIDE = new ThreadLocal<>();

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
}
