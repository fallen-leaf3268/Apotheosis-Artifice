package com.apotheosis_artifice.mixin;

import com.apotheosis_artifice.CatOverride;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.shadowsoffire.apotheosis.adventure.commands.LootifyCommand;
import dev.shadowsoffire.apotheosis.adventure.commands.RarityCommand;
import dev.shadowsoffire.apotheosis.adventure.commands.SocketCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {LootifyCommand.class, RarityCommand.class, SocketCommand.class}, remap = false)
public abstract class CurioCommandMixin {
    @Inject(method = {
        "lambda$register$1(Lcom/mojang/brigadier/context/CommandContext;)I",
        "lambda$register$2(Lcom/mojang/brigadier/context/CommandContext;)I"
    }, at = @At("HEAD"), cancellable = true)
    private static void artifice$rejectCurioCommand(CommandContext<CommandSourceStack> context,
        CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        if (CatOverride.isApotheosisBlocked(context.getSource().getPlayerOrException().getMainHandItem())) {
            context.getSource().sendFailure(Component.translatable("commands.generic.failed"));
            cir.setReturnValue(0);
        }
    }
}
