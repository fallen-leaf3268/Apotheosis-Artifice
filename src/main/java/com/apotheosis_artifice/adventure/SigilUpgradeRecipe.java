package com.apotheosis_artifice.adventure;

import java.util.function.Supplier;

import com.google.gson.JsonObject;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.AdventureModule.ApothSmithingRecipe;
import dev.shadowsoffire.apotheosis.adventure.socket.ReactiveSmithingRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class SigilUpgradeRecipe extends ApothSmithingRecipe implements ReactiveSmithingRecipe {

    private final Operation operation;
    private final Item sigil;
    private final Serializer serializer;

    private SigilUpgradeRecipe(ResourceLocation id, Operation operation, Item sigil, Serializer serializer) {
        super(id, Ingredient.EMPTY, Ingredient.of(sigil), ItemStack.EMPTY);
        this.operation = operation;
        this.sigil = sigil;
        this.serializer = serializer;
    }

    @Override
    public boolean matches(Container inventory, Level level) {
        if (!Apotheosis.enableAdventure || !inventory.getItem(TEMPLATE).isEmpty()
            || inventory.getItem(BASE).getCount() != 1 || !inventory.getItem(ADDITION).is(this.sigil)) {
            return false;
        }
        return this.operation == Operation.SUPREMACY ? SigilAffixHelper.canApplySupremacy(inventory.getItem(BASE))
            : SigilAffixHelper.canApplyMalice(inventory.getItem(BASE));
    }

    @Override
    public ItemStack assemble(Container inventory, RegistryAccess registries) {
        if (!this.matches(inventory, null)) {
            return ItemStack.EMPTY;
        }
        if (this.operation == Operation.MALICE) {
            return SigilAffixHelper.prepareMalice(inventory.getItem(BASE));
        }
        ItemStack output = inventory.getItem(BASE).copy();
        SigilAffixHelper.applySupremacy(output);
        return output;
    }

    @Override
    public void onCraft(Container inventory, Player player, ItemStack output) {
        if (player.level().isClientSide || !Apotheosis.enableAdventure) {
            return;
        }
        if (this.operation == Operation.MALICE) {
            SigilAffixHelper.completePending(player, output);
        } else {
            player.level().playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS,
                1.0F, player.getRandom().nextFloat() * 0.4F + 0.8F);
        }
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return this.serializer;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public Operation operation() {
        return this.operation;
    }

    public Item sigil() {
        return this.sigil;
    }

    public enum Operation {
        SUPREMACY, MALICE
    }

    public static final class Serializer implements RecipeSerializer<SigilUpgradeRecipe> {

        private final Operation operation;
        private final Supplier<Item> sigil;

        public Serializer(Operation operation, Supplier<Item> sigil) {
            this.operation = operation;
            this.sigil = sigil;
        }

        @Override
        public SigilUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new SigilUpgradeRecipe(id, this.operation, this.sigil.get(), this);
        }

        @Override
        public SigilUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new SigilUpgradeRecipe(id, this.operation, this.sigil.get(), this);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SigilUpgradeRecipe recipe) {}
    }
}
