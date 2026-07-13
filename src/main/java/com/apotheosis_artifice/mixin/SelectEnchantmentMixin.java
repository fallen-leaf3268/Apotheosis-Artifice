package com.apotheosis_artifice.mixin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.apotheosis_artifice.ApotheosisConfig;
import com.google.common.collect.Lists;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.Arcana;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import dev.shadowsoffire.apotheosis.ench.table.IEnchantableItem;
import dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper;
import dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper.ArcanaEnchantmentData;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

@Mixin(RealEnchantmentHelper.class)
public class SelectEnchantmentMixin {

    /**
     * @author apotheosis_artifice
     * @reason 让保底魔咒数量随 arcana 线性增长（每 25 点 +1），并解除威力上限到 MAX_ETERNA*4。
     *         忠实复刻原逻辑，仅替换 arcana 保底段与内联威力上限。
     */
    @Overwrite(remap = false)
    public static List<EnchantmentInstance> selectEnchantment(RandomSource rand, ItemStack stack, int level, float quanta, float arcana, float rectification, boolean treasure, Set<Enchantment> blacklist) {
        List<EnchantmentInstance> chosenEnchants = Lists.newArrayList();
        int enchantability = stack.getEnchantmentValue();
        int srcLevel = level;
        if (enchantability > 0) {
            float quantaFactor = 1 + RealEnchantmentHelper.getQuantaFactor(rand, quanta, rectification);
            int powerCap = (int) (Math.max(EnchantingStatRegistry.getAbsoluteMaxEterna(), ApotheosisConfig.MAX_ETERNA.get()) * 4);
            level = Mth.clamp(Math.round(level * quantaFactor), 1, powerCap);
            Arcana arcanaVals = Arcana.getForThreshold(arcana);
            List<EnchantmentInstance> allEnchants = RealEnchantmentHelper.getAvailableEnchantmentResults(level, stack, treasure, blacklist);
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
            allEnchants.removeIf(e -> enchants.containsKey(e.enchantment));
            List<ArcanaEnchantmentData> possibleEnchants = allEnchants.stream().map(d -> new ArcanaEnchantmentData(arcanaVals, d)).collect(Collectors.toList());
            if (!possibleEnchants.isEmpty()) {
                int maxEnch = ApotheosisConfig.MAX_ENCHANTMENTS.get();
                chosenEnchants.add(pickData(rand, possibleEnchants));
                RealEnchantmentHelper.removeIncompatible(possibleEnchants, Util.lastOf(chosenEnchants));

                // 保底数量随 arcana 线性增长：每 25 点 +1 个（原版为 >=25 出2、>=75 出3）
                int guaranteed = (int) (arcana / 25F);
                for (int i = 0; i < guaranteed && !possibleEnchants.isEmpty() && chosenEnchants.size() < maxEnch; i++) {
                    chosenEnchants.add(pickData(rand, possibleEnchants));
                    RealEnchantmentHelper.removeIncompatible(possibleEnchants, Util.lastOf(chosenEnchants));
                }

                int randomBound = 50;
                if (level > 45) {
                    level = (int) (srcLevel * 1.15F);
                }

                while (rand.nextInt(randomBound) <= level && chosenEnchants.size() < maxEnch) {
                    if (!chosenEnchants.isEmpty()) RealEnchantmentHelper.removeIncompatible(possibleEnchants, Util.lastOf(chosenEnchants));

                    if (possibleEnchants.isEmpty()) {
                        break;
                    }

                    chosenEnchants.add(pickData(rand, possibleEnchants));
                    level /= 2;
                }
            }
        }
        return ((IEnchantableItem) stack.getItem()).selectEnchantments(chosenEnchants, rand, stack, srcLevel, quanta, arcana, treasure);
    }

    private static EnchantmentInstance pickData(RandomSource rand, List<ArcanaEnchantmentData> pool) {
        ArcanaEnchantmentData picked = WeightedRandom.getRandomItem(rand, pool).get();
        return ((ArcanaEnchantmentDataAccessor) (Object) picked).getData();
    }
}
