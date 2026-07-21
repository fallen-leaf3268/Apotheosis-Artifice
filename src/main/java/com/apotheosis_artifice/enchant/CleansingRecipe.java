package com.apotheosis_artifice.enchant;

import com.google.gson.JsonObject;

import dev.shadowsoffire.apotheosis.adventure.AdventureModule.ApothSmithingRecipe;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.socket.ReactiveSmithingRecipe;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class CleansingRecipe extends ApothSmithingRecipe implements ReactiveSmithingRecipe {

    private static final ResourceLocation ID = new ResourceLocation("apotheosis_artifice", "cleansing");

    public CleansingRecipe() {
        super(ID, Ingredient.EMPTY, Ingredient.EMPTY, ItemStack.EMPTY);
    }

    public CleansingRecipe(ResourceLocation id, Ingredient base, Ingredient addition, ItemStack result) {
        super(id, base, addition, result);
    }

    @Override
    public boolean matches(Container pInv, Level pLevel) {
        ItemStack base = pInv.getItem(1);
        if (base.isEmpty()) return false;
        ItemStack addition = pInv.getItem(2);
        return addition.getItem() == com.apotheosis_artifice.ApotheosisArtificeMod.SIGIL_OF_CLEANSING.get();
    }

    @Override
    public ItemStack assemble(Container pInv, RegistryAccess regs) {
        ItemStack out = pInv.getItem(1).copy();
        CompoundTag afxData = out.getTagElement(AffixHelper.AFFIX_DATA);
        if (afxData == null) return out;

        afxData.remove(AffixHelper.AFFIXES);
        afxData.remove(AffixHelper.RARITY);
        afxData.remove(AffixHelper.NAME);
        afxData.remove("gems");
        afxData.remove("sockets");
        afxData.remove("tiered_socket_tiers");
        afxData.remove("uuids");

        return out;
    }

    @Override
    public void onCraft(Container inv, Player player, ItemStack output) {
        ItemStack base = inv.getItem(1);
        var gems = SocketHelper.getGems(base);
        for (int i = 0; i < gems.size(); i++) {
            ItemStack stack = gems.get(i).gemStack();
            if (!stack.isEmpty()) {
                stack.removeTagKey(GemItem.UUID_ARRAY);
                if (!player.addItem(stack)) {
                    Block.popResource(player.level(), player.blockPosition(), stack);
                }
            }
        }
        SocketHelper.setGems(base, dev.shadowsoffire.apotheosis.adventure.socket.SocketedGems.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.SMITHING;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<CleansingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public CleansingRecipe fromJson(ResourceLocation pRecipeId, JsonObject pJson) {
            return new CleansingRecipe(pRecipeId, Ingredient.EMPTY, Ingredient.of(com.apotheosis_artifice.ApotheosisArtificeMod.SIGIL_OF_CLEANSING.get()), ItemStack.EMPTY);
        }

        @Override
        public CleansingRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
            return new CleansingRecipe(pRecipeId, Ingredient.EMPTY, Ingredient.of(com.apotheosis_artifice.ApotheosisArtificeMod.SIGIL_OF_CLEANSING.get()), ItemStack.EMPTY);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, CleansingRecipe pRecipe) {
        }
    }
}
