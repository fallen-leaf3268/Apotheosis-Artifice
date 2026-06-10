package com.apotheosis_artifice.enchant;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.apotheosis_artifice.ApotheosisArtificeMod;

import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
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

    private static final String TAG_BOUND = "bound";
    private static final String TAG_DIM = "bound_dim";
    private static final String TAG_X = "bound_x";
    private static final String TAG_Y = "bound_y";
    private static final String TAG_Z = "bound_z";
    private static final String TAG_SALVAGE = "salvage_mode";

    public GemBinderItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof com.apotheosis_artifice.gemcase.GemCaseTile) {
            if (!level.isClientSide) {
                ItemStack stack = ctx.getItemInHand();
                CompoundTag tag = stack.getOrCreateTag();
                tag.putBoolean(TAG_BOUND, true);
                tag.putString(TAG_DIM, level.dimension().location().toString());
                tag.putInt(TAG_X, pos.getX());
                tag.putInt(TAG_Y, pos.getY());
                tag.putInt(TAG_Z, pos.getZ());
                ctx.getPlayer().displayClientMessage(Component.translatable("info.apotheosis_artifice.binder.bound", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean current = isSalvageMode(stack);
            stack.getOrCreateTag().putBoolean(TAG_SALVAGE, !current);
            player.displayClientMessage(Component.translatable("info.apotheosis_artifice.binder.mode." + (!current ? "salvage" : "deposit")).withStyle(ChatFormatting.YELLOW), true);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static boolean isSalvageMode(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(TAG_SALVAGE);
    }

    @Nullable
    public static BlockPos getBoundPos(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(TAG_BOUND)) return null;
        return new BlockPos(tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z));
    }

    @Nullable
    public static ResourceLocation getBoundDim(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(TAG_BOUND)) return null;
        String dim = tag.getString(TAG_DIM);
        return dim.isEmpty() ? null : ResourceLocation.tryParse(dim);
    }

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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPickup(EntityItemPickupEvent event) {
        if (event.isCanceled()) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (player.level().isClientSide) return;

        ItemStack pickedUp = event.getItem().getItem();
        if (!isGem(pickedUp)) return;

        LazyOptional<ICuriosItemHandler> curiosInv = player.getCapability(CuriosCapability.INVENTORY);
        curiosInv.ifPresent(handler -> {
            Map<String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler> curios = handler.getCurios();
            for (var entry : curios.entrySet()) {
                var stackHandler = entry.getValue().getStacks();
                for (int i = 0; i < stackHandler.getSlots(); i++) {
                    ItemStack binder = stackHandler.getStackInSlot(i);
                    if (binder.isEmpty() || !(binder.getItem() instanceof GemBinderItem)) continue;

                    BlockPos boundPos = getBoundPos(binder);
                    ResourceLocation boundDim = getBoundDim(binder);
                    if (boundPos == null) continue;

                    BlockEntity be = player.level().getBlockEntity(boundPos);
                    if (!(be instanceof com.apotheosis_artifice.gemcase.GemCaseTile) && boundDim != null) {
                        var dimKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, boundDim);
                        var boundLevel = player.getServer().getLevel(dimKey);
                        if (boundLevel != null) {
                            be = boundLevel.getBlockEntity(boundPos);
                        }
                    }
                    if (isSalvageMode(binder)) {
                        var recipe = com.apotheosis_artifice.PortableSalvagingMenu.findMatch(player.level(), pickedUp);
                        if (recipe != null) {
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
                            return;
                        }
                    } else if (be instanceof com.apotheosis_artifice.gemcase.GemCaseTile tile) {
                        int countBefore = pickedUp.getCount();
                        for (int c = 0; c < countBefore; c++) {
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
        });
    }

    private static boolean isGem(ItemStack stack) {
        return stack.getItem() == Adventure.Items.GEM.get() && GemItem.getGem(stack).isBound();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (isSalvageMode(stack)) {
            list.add(Component.translatable("info.apotheosis_artifice.binder.salvage_mode").withStyle(ChatFormatting.GOLD));
            list.add(Component.translatable("info.apotheosis_artifice.binder.desc_salvage").withStyle(ChatFormatting.GOLD));
            return;
        }
        BlockPos pos = getBoundPos(stack);
        if (pos != null) {
            ResourceLocation dim = getBoundDim(stack);
            list.add(Component.translatable("info.apotheosis_artifice.binder.bound_to", pos.getX(), pos.getY(), pos.getZ(), dim != null ? dim.toString() : "?").withStyle(ChatFormatting.GREEN));
        } else {
            list.add(Component.translatable("info.apotheosis_artifice.binder.unbound").withStyle(ChatFormatting.RED));
            list.add(Component.translatable("info.apotheosis_artifice.binder.bind_hint").withStyle(ChatFormatting.GRAY));
        }
        list.add(Component.translatable("info.apotheosis_artifice.binder.pickup_mode").withStyle(ChatFormatting.GOLD));
        list.add(Component.translatable("info.apotheosis_artifice.binder.desc").withStyle(ChatFormatting.GOLD));
    }
}
