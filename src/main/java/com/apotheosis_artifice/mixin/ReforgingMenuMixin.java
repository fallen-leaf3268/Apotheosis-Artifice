package com.apotheosis_artifice.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apotheosis_artifice.AffixTypes;
import com.apotheosis_artifice.ApotheosisArtificeMod;
import com.apotheosis_artifice.ISlotSelectMenu;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingMenu;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;

@Mixin(value = ReforgingMenu.class, remap = false)
public abstract class ReforgingMenuMixin implements ISlotSelectMenu {

    @Shadow
    private InternalItemHandler itemInv;
    @Shadow
    private InternalItemHandler choicesInv;
    @Shadow
    private RandomSource random;
    @Shadow
    private int seed;
    @Shadow
    private int[] costs;
    @Shadow
    private net.minecraft.world.entity.player.Player player;

    @Unique
    private int curiosforge_selectedSlotIdx = 0;

    @Unique
    private List<String> curiosforge_availableSlots = List.of();

    // 改为「每个菜单实例」缓存，避免 static 一次性缓存把 A 世界的重铸成本泄漏到 B 世界/数据包重载后。
    @Unique
    private int[] curiosforge_maxCosts = null;
    @Unique
    private boolean curiosforge_costsInit = false;

    @Unique
    private int[] curiosforge_getMaxCosts(net.minecraft.world.entity.player.Player player) {
        if (curiosforge_costsInit) return curiosforge_maxCosts;
        int maxS = 0, maxM = 0, maxL = 0;
        var all = player.level().getRecipeManager().getAllRecipesFor(dev.shadowsoffire.apotheosis.Apoth.RecipeTypes.REFORGING);
        for (var r : all) {
            if (r.sigilCost() > maxS) maxS = r.sigilCost();
            if (r.matCost() > maxM) maxM = r.matCost();
            if (r.levelCost() > maxL) maxL = r.levelCost();
        }
        curiosforge_maxCosts = new int[] { maxS, maxM, maxL };
        curiosforge_costsInit = true;
        return curiosforge_maxCosts;
    }

    public List<String> curiosforge_getAvailableSlots() {
        return this.curiosforge_availableSlots;
    }

    @Unique
    private void curiosforge_detectSlots(ItemStack input) {
        List<String> cats = new ArrayList<>();

        LootCategory trueNative = LootCategory.NONE;
        for (LootCategory c : LootCategory.VALUES) {
            if (c.isNone() || c.getName().startsWith("curio")) continue;
            if (c.isValid(input)) { trueNative = c; break; }
        }
        if (!trueNative.isNone()) {
            cats.add(trueNative.getName());
        }

        LootRarity rarity = null;
        var matSlot = ((ReforgingMenu)(Object)this).getSlot(1).getItem();
        if (!matSlot.isEmpty()) {
            var dh = RarityRegistry.getMaterialRarity(matSlot.getItem());
            if (dh.isBound()) rarity = dh.get();
        }

        var curiosSlots = CuriosApi.getSlots().keySet().stream()
            .filter(sid -> input.is(ItemTags.create(new ResourceLocation("curios:" + sid))))
            .sorted()
            .collect(Collectors.toList());
        curiosSlots.remove("curio");
        if (curiosSlots.isEmpty() && input.getCapability(CuriosCapability.ITEM).isPresent()) {
            curiosSlots = List.of("curio");
        }
        for (String slot : curiosSlots) {
            LootCategory cat = getOrCreateCurioCategory(slot);
            boolean has = rarity == null ? hasAffixFor(input, cat) : curiosforge_hasAffixForRarity(input, cat, rarity);
            if (!cat.isNone() && has) {
                cats.add("curio:" + slot);
            }
        }

        this.curiosforge_availableSlots = List.copyOf(cats);
        if (this.curiosforge_selectedSlotIdx >= cats.size())
            this.curiosforge_selectedSlotIdx = 0;
        // 单槽位：立即更新输入物品的 curio_artifice，避免原版处理读到旧的标签
        if (cats.size() == 1) {
            input.getOrCreateTagElement("affix_data").putString("curio_artifice", cats.get(0));
        }
    }


    @Unique
    private static LootCategory getOrCreateCurioCategory(String slotOrCatName) {
        String slotId = slotOrCatName.startsWith("curio:") ? slotOrCatName.substring(6) : slotOrCatName;
        String catName = "curio:" + slotId;
        LootCategory cat = LootCategory.byId(catName);
        if (cat == null || cat.isNone()) {
            cat = LootCategory.register(ApotheosisArtificeMod.CURIO, catName,
                s -> !s.isEmpty() && s.is(ItemTags.create(new ResourceLocation("curios:" + slotId))),
                new EquipmentSlot[]{EquipmentSlot.CHEST});
        }
        return cat;
    }

    public void curiosforge_selectSlot(int idx) {
        // 来自网络包，必须校验下界（上界在 detect/regenerate 里再钳）。
        this.curiosforge_selectedSlotIdx = Math.max(0, idx);
    }

    // 重铸输入槽用的是 ReforgingMenu$1（UpdatingSlot 的匿名子类，构造时传入 itemInv），
    // 而材料槽用的是 PlaceboContainerMenu$UpdatingSlot（传入 ReforgingTableTile.inv）。
    // 因此只需 hook 匿名类的构造器、改它的 Predicate（参数下标 5：外部 this0,handler,x,y,idx,predicate）。
    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE",
            target = "Ldev/shadowsoffire/apotheosis/adventure/affix/reforging/ReforgingMenu$1;<init>(Ldev/shadowsoffire/apotheosis/adventure/affix/reforging/ReforgingMenu;Ldev/shadowsoffire/placebo/cap/InternalItemHandler;IIILjava/util/function/Predicate;)V",
            remap = false),
        index = 5)
    private Predicate<ItemStack> enhanceSlotValidator(Predicate<ItemStack> original) {
        return s -> original.test(s) && hasAffixFor(s);
    }

    @Inject(method = "slotsChanged", at = @At("HEAD"), remap = true)
    private void curiosforge_updateSlots(net.minecraft.world.Container container, CallbackInfo ci) {
        ItemStack input = ((ReforgingMenu)(Object)this).getSlot(0).getItem();
        if (input.isEmpty()) { this.curiosforge_availableSlots = List.of(); return; }
        curiosforge_detectSlots(input);
    }

    @Inject(method = "slotsChanged", at = @At("TAIL"), remap = true)
    private void curiosforge_regenerate(net.minecraft.world.Container container, CallbackInfo ci) {
        ReforgingMenu menu = (ReforgingMenu)(Object)this;
        ItemStack input = menu.getSlot(0).getItem();
        if (input.isEmpty()) {
            return;
        }

        if (this.curiosforge_availableSlots.size() > 1) {
            ItemStack mat = menu.getSlot(1).getItem();
            if (mat.isEmpty()) return;

            var rarityHolder = RarityRegistry.getMaterialRarity(mat.getItem());
            if (!rarityHolder.isBound()) return; // unbound 时 .get() 会抛 NPE，不能用 ==null 判断
            LootRarity rarity = rarityHolder.get();

            int sel = this.curiosforge_selectedSlotIdx;
            if (sel < 0 || sel >= this.curiosforge_availableSlots.size()) sel = 0;
            String catName = this.curiosforge_availableSlots.get(sel);

            LootCategory cat;
            if (catName.startsWith("curio:")) {
                cat = getOrCreateCurioCategory(catName);
            } else {
                cat = LootCategory.byId(catName);
            }
            if (cat == null || cat.isNone()) return;

            com.apotheosis_artifice.CatOverride.set(cat);
            try {
                for (int s = 0; s < 3; s++) {
                    RandomSource r = this.random;
                    r.setSeed(this.seed ^ ForgeRegistries.ITEMS.getKey(input.getItem()).hashCode() + s);
                    ItemStack copy = input.copy();
                    copy.getOrCreateTagElement("affix_data").putString("curio_artifice", cat.getName());
                    ItemStack result = LootController.createLootItem(copy, cat, rarity, r);
                    String cc = cat.getName();
                    result.getOrCreateTagElement("affix_data").putString("curio_artifice", cc);
                    this.choicesInv.setStackInSlot(s, result);
                }
            } finally {
                com.apotheosis_artifice.CatOverride.set(null);
            }
        }
        if (this.costs[0] == 0 && this.costs[1] == 0 && this.costs[2] == 0 && this.player != null && this.player.level() != null) {
            int[] max = curiosforge_getMaxCosts(this.player);
            if (max[0] > 0) this.costs[0] = max[0];
            if (max[1] > 0) this.costs[1] = max[1];
            if (max[2] > 0) this.costs[2] = max[2];
        }
    }

    private static boolean hasAffixFor(ItemStack stack, LootCategory cat) {
        if (cat == null || cat.isNone()) return false;
        return AffixRegistry.INSTANCE.getValues().stream()
            .anyMatch(a -> {
                if (!(a instanceof AffixTypes af)) return false;
                Set<LootCategory> types = af.curiosforge_getTypes();
                return types.contains(cat) || AffixTypes.curiosforge_typeMatches(types, cat);
            });
    }

    @Unique
    private static boolean curiosforge_hasAffixForRarity(ItemStack stack, LootCategory cat, LootRarity rarity) {
        for (var a : AffixRegistry.INSTANCE.getValues()) {
            if (!(a instanceof AffixTypes af)) continue;
            Set<LootCategory> types = af.curiosforge_getTypes();
            if (!types.contains(cat) && !AffixTypes.curiosforge_typeMatches(types, cat)) continue;
            if (((dev.shadowsoffire.apotheosis.adventure.affix.Affix) a).canApplyTo(stack, cat, rarity)) return true;
        }
        return false;
    }

    private static boolean hasAffixFor(ItemStack stack) {
        return hasAffixFor(stack, LootCategory.forItem(stack));
    }
}
