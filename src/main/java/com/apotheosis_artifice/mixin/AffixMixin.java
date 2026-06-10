package com.apotheosis_artifice.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.apotheosis_artifice.AffixTypes;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;

@Mixin(value = Affix.class, remap = false)
public abstract class AffixMixin implements AffixTypes {
    // 默认使用 AffixTypes 接口的默认实现返回空集
    // 有 types 字段的子类通过各自的 Accessor 覆盖此方法
}
