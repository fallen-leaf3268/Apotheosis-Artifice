package com.apotheosis_artifice.enchant;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.ench.library.EnchLibraryTile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

@Mod.EventBusSubscriber(modid = ApotheosisArtificeMod.MODID)
public class GemBinderItem extends Item {

    // ---- NBT 键前缀 ----
    private static final String PREFIX_GC = "gc_"; // 宝石柜
    private static final String PREFIX_LB = "lb_"; // 图书馆
    private static final String TAG_MODE = "binder_mode";
    private static final String TAG_SALVAGE_TYPE = "salvage_type";

    // mode: 0=deposit, 1=salvage
    // salvage_type: 0=gems, 1=equipment, 2=all

    public GemBinderItem(Properties props) {
        super(props);
    }

    // ---- 模式 API ----

    private static int getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_MODE) : 0;
    }

    private static int getSalvageType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_SALVAGE_TYPE) : 0;
    }

    private static boolean isSalvageMode(ItemStack stack) {
        return getMode(stack) == 1;
    }

    private static String getModeTranslationKey(ItemStack stack) {
        if (getMode(stack) == 0) return "info.apotheosis_artifice.binder.mode.deposit";
        int st = getSalvageType(stack);
        return switch (st) {
            case 0 -> "info.apotheosis_artifice.binder.salvage_gem";
            case 1 -> "info.apotheosis_artifice.binder.salvage_equip";
            case 2 -> "info.apotheosis_artifice.binder.salvage_all";
            default -> "info.apotheosis_artifice.binder.mode.salvage";
        };
    }

    /**
     * 循环模式（由 ToggleBinderPacket 调用）
     * deposit → salvage_gems → salvage_equip → salvage_all → 循环
     */
    public static void cycleMode(ItemStack stack, Player player) {
        int mode = getMode(stack);
        if (mode == 0) {
            stack.getOrCreateTag().putInt(TAG_MODE, 1);
            stack.getOrCreateTag().putInt(TAG_SALVAGE_TYPE, 0);
        } else {
            int st = (getSalvageType(stack) + 1) % 3;
            stack.getOrCreateTag().putInt(TAG_SALVAGE_TYPE, st);
        }
        player.displayClientMessage(
            Component.translatable(getModeTranslationKey(stack)).withStyle(ChatFormatting.YELLOW), true);
    }

    // ---- 独立绑定存取 ----

    private static void saveBinding(CompoundTag tag, String prefix, BlockPos pos, ResourceLocation dim, ResourceLocation blockId) {
        tag.putBoolean(prefix + "bound", true);
        tag.putString(prefix + "dim", dim.toString());
        tag.putInt(prefix + "x", pos.getX());
        tag.putInt(prefix + "y", pos.getY());
        tag.putInt(prefix + "z", pos.getZ());
        if (blockId != null) tag.putString(prefix + "block", blockId.toString());
    }

    @Nullable
    private static BlockPos getBoundPos(ItemStack stack, String prefix) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(prefix + "bound")) return null;
        return new BlockPos(tag.getInt(prefix + "x"), tag.getInt(prefix + "y"), tag.getInt(prefix + "z"));
    }

    @Nullable
    private static ResourceLocation getBoundDim(ItemStack stack, String prefix) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(prefix + "bound")) return null;
        String dim = tag.getString(prefix + "dim");
        return dim.isEmpty() ? null : ResourceLocation.tryParse(dim);
    }

    // ---- 绑定 ----

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        String prefix = null;
        if (be instanceof com.apotheosis_artifice.gemcase.GemCaseTile) {
            prefix = PREFIX_GC;
        } else {
            ResourceLocation blockId = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock());
            if (blockId != null && "apotheosis".equals(blockId.getNamespace()) && ("library".equals(blockId.getPath()) || "ender_library".equals(blockId.getPath()))) {
                prefix = PREFIX_LB;
            }
        }

        ResourceLocation blockId = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock());
        if (prefix != null) {
            if (!level.isClientSide) {
                ItemStack stack = ctx.getItemInHand();
                saveBinding(stack.getOrCreateTag(), prefix, pos, level.dimension().location(), blockId);
                Component blockName = new ItemStack(level.getBlockState(pos).getBlock()).getHoverName();
                ApotheosisArtificeMod.LOGGER.info("[Binder] bound {} at {}", blockName.getString(), pos);
                ctx.getPlayer().displayClientMessage(Component.translatable("info.apotheosis_artifice.binder.bound", blockName, pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // ---- 模式切换 ----

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int mode = getMode(stack);
            stack.getOrCreateTag().putInt(TAG_MODE, mode == 0 ? 1 : 0);
            player.displayClientMessage(Component.translatable(getModeTranslationKey(stack)).withStyle(ChatFormatting.YELLOW), true);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ---- 拾取处理 ----

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPickup(EntityItemPickupEvent event) {
        if (event.isCanceled()) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (player.level().isClientSide) return;

        ItemStack pickedUp = event.getItem().getItem();

        LazyOptional<ICuriosItemHandler> curiosInv = player.getCapability(CuriosCapability.INVENTORY);
        curiosInv.ifPresent(handler -> {
            Map<String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler> curios = handler.getCurios();
            for (var entry : curios.entrySet()) {
                var stackHandler = entry.getValue().getStacks();
                for (int i = 0; i < stackHandler.getSlots(); i++) {
                    ItemStack binder = stackHandler.getStackInSlot(i);
                    if (binder.isEmpty() || !(binder.getItem() instanceof GemBinderItem)) continue;

                    // 图书馆独立绑定 → 自动存入附魔书
                    BlockPos libPos = getBoundPos(binder, PREFIX_LB);
                    BlockEntity libBe = libPos != null ? getBoundTile(player, binder, libPos, PREFIX_LB) : null;
                    if (libBe instanceof EnchLibraryTile lib && pickedUp.getItem() == Items.ENCHANTED_BOOK) {
                        int count = pickedUp.getCount();
                        for (int c = 0; c < count; c++) {
                            lib.depositBook(pickedUp.copy());
                        }
                        pickedUp.setCount(0);
                        event.getItem().discard();
                        event.setCanceled(true);
                        return;
                    }

                    // 分解模式：根据子类型处理宝石/装备/全部
                    if (isSalvageMode(binder)) {
                        int st = getSalvageType(binder);
                        boolean isGemItem = isGem(pickedUp);
                        boolean isEquip = AffixHelper.hasAffixes(pickedUp);

                        boolean shouldSalvage = switch (st) {
                            case 0 -> isGemItem;
                            case 1 -> isEquip && !isGemItem;
                            case 2 -> isGemItem || isEquip;
                            default -> false;
                        };
                        if (shouldSalvage) {
                            handleSalvage(player, pickedUp, event);
                        }
                        // salvage 不 return，允许继续处理其他绑定
                    }

                    // 存入模式：宝石 → 宝石柜（每份 count=1）
                    if (!isSalvageMode(binder)) {
                        BlockPos gcPos = getBoundPos(binder, PREFIX_GC);
                        BlockEntity gcBe = gcPos != null ? getBoundTile(player, binder, gcPos, PREFIX_GC) : null;
                        if (isGem(pickedUp) && gcBe instanceof com.apotheosis_artifice.gemcase.GemCaseTile tile) {
                            int count = pickedUp.getCount();
                            for (int c = 0; c < count; c++) {
                                ItemStack single = pickedUp.copy();
                                single.setCount(1);
                                tile.depositGem(single);
                            }
                            pickedUp.setCount(0);
                            event.getItem().discard();
                            event.setCanceled(true);
                            return;
                        }
                    }
                }
            }
        });
    }

    @Nullable
    private static BlockEntity getBoundTile(Player player, ItemStack binder, BlockPos boundPos, String prefix) {
        BlockEntity be = player.level().getBlockEntity(boundPos);
        ResourceLocation boundDim = getBoundDim(binder, prefix);
        if (be == null && boundDim != null) {
            var dimKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, boundDim);
            var boundLevel = player.getServer().getLevel(dimKey);
            if (boundLevel != null) {
                be = boundLevel.getBlockEntity(boundPos);
            }
        }
        return be;
    }

    private static boolean isGem(ItemStack stack) {
        return stack.getItem() == Adventure.Items.GEM.get() && GemItem.getGem(stack).isBound();
    }

    private static void handleSalvage(Player player, ItemStack pickedUp, EntityItemPickupEvent event) {
        var recipe = com.apotheosis_artifice.PortableSalvagingMenu.findMatch(player.level(), pickedUp);
        if (recipe == null) return;
        var outputs = recipe.getOutputs();
        int outCount = outputs.size();
        ItemStack[] baseStacks = new ItemStack[outCount];
        int[] outMin = new int[outCount];
        int[] outMax = new int[outCount];
        for (int idx = 0; idx < outCount; idx++) {
            var out = outputs.get(idx);
            baseStacks[idx] = out.getStack().copy();
            int[] counts = com.apotheosis_artifice.PortableSalvagingMenu.getSalvageCounts(out, pickedUp);
            outMin[idx] = counts[0];
            outMax[idx] = counts[1];
        }
        int[] totals = new int[outCount];
        int count = pickedUp.getCount();
        var rand = player.level().random;
        for (int c = 0; c < count; c++) {
            for (int o = 0; o < outCount; o++) {
                int qty = outMin[o] + (outMax[o] > outMin[o] ? rand.nextInt(outMax[o] - outMin[o] + 1) : 0);
                if (qty > 0) totals[o] += qty;
            }
        }
        for (int o = 0; o < outCount; o++) {
            if (totals[o] <= 0) continue;
            var outStack = baseStacks[o].copy();
            outStack.setCount(totals[o]);
            var itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                player.level(), player.getX(), player.getY() + 0.5, player.getZ(), outStack);
            itemEntity.setPickUpDelay(0);
            player.level().addFreshEntity(itemEntity);
        }
        pickedUp.setCount(0);
        event.getItem().discard();
        event.setCanceled(true);
    }

    // ---- Curios 能力 ----

    @Override
    @Nullable
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            @Override
            public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
                if (cap == CuriosCapability.ITEM) {
                    return LazyOptional.of(() -> new ICurio() {
                        @Override
                        public ItemStack getStack() { return stack; }
                    }).cast();
                }
                return LazyOptional.empty();
            }
        };
    }

    // ---- Tooltip ----

    private static Component getBlockDisplayName(ItemStack stack, String prefix) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(prefix + "bound")) return null;
        String blockId = tag.getString(prefix + "block");
        ApotheosisArtificeMod.LOGGER.info("[Binder] getBlockDisplayName: prefix={} blockId={}", prefix, blockId);
        if (!blockId.isEmpty()) {
            var id = ResourceLocation.tryParse(blockId);
            if (id != null) {
                var block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(id);
                ApotheosisArtificeMod.LOGGER.info("[Binder] lookup: id={} block={}", id, block);
                if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
                    return new ItemStack(block).getHoverName();
                }
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        BlockPos gcPos = getBoundPos(stack, PREFIX_GC);
        ResourceLocation gcDim = getBoundDim(stack, PREFIX_GC);
        BlockPos lbPos = getBoundPos(stack, PREFIX_LB);
        ResourceLocation lbDim = getBoundDim(stack, PREFIX_LB);
        Component gcName = getBlockDisplayName(stack, PREFIX_GC);
        Component lbName = getBlockDisplayName(stack, PREFIX_LB);

        // 模式
        list.add(Component.translatable(getModeTranslationKey(stack)).withStyle(ChatFormatting.GOLD));
        if (isSalvageMode(stack)) {
            int st = getSalvageType(stack);
            String descKey = switch (st) {
                case 0 -> "info.apotheosis_artifice.binder.desc_salvage_gem";
                case 1 -> "info.apotheosis_artifice.binder.desc_salvage_equip";
                case 2 -> "info.apotheosis_artifice.binder.desc_salvage_all";
                default -> "info.apotheosis_artifice.binder.desc_salvage";
            };
            list.add(Component.translatable(descKey).withStyle(ChatFormatting.GRAY));
        }

        // 宝石柜坐标（仅在不涉及宝石分解的模式下显示）
        boolean showGcInfo = !isSalvageMode(stack) || getSalvageType(stack) == 1;
        if (showGcInfo) {
            if (gcName != null && gcPos != null) {
                list.add(Component.translatable("info.apotheosis_artifice.binder.gemcase_bound", gcName, gcPos.getX(), gcPos.getY(), gcPos.getZ(), gcDim != null ? gcDim.toString() : "?").withStyle(ChatFormatting.GREEN));
            } else {
                list.add(Component.translatable("info.apotheosis_artifice.binder.gemcase_unbound").withStyle(ChatFormatting.RED));
            }
        }

        // 图书馆（独立）
        if (lbName != null && lbPos != null) {
            list.add(Component.translatable("info.apotheosis_artifice.binder.library_bound", lbName, lbPos.getX(), lbPos.getY(), lbPos.getZ(), lbDim != null ? lbDim.toString() : "?").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            list.add(Component.translatable("info.apotheosis_artifice.binder.library_unbound").withStyle(ChatFormatting.RED));
        }
        list.add(Component.translatable("info.apotheosis_artifice.binder.desc_library").withStyle(ChatFormatting.GRAY));
    }
}
