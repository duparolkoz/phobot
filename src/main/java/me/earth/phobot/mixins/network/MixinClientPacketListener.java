package me.earth.phobot.mixins.network;

import me.earth.phobot.event.LocalPlayerDeathEvent;
import me.earth.phobot.event.SetEquipmentEvent;
import me.earth.phobot.event.TotemPopEvent;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
    @Shadow @Final private Minecraft minecraft;

    @Inject(
        method = "handleEntityEvent",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V"),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void handleEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci, Entity entity, int i) {
        PingBypassApi.getEventBus().post(new TotemPopEvent(entity));
    }

    @Inject(
        method = "handlePlayerCombatKill",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;shouldShowDeathScreen()Z", shift = At.Shift.BEFORE))
    private void handlePlayerCombatKillHook(ClientboundPlayerCombatKillPacket clientboundPlayerCombatKillPacket, CallbackInfo ci) {
        PingBypassApi.getEventBus().post(new LocalPlayerDeathEvent(minecraft.player));
    }

    @Inject(
        method = "handleSetEquipment",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
            remap = false,
            shift = At.Shift.AFTER),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void handleSetEquipmentHook(ClientboundSetEquipmentPacket packet, CallbackInfo ci, Entity entity) {
        PingBypassApi.getEventBus().post(new SetEquipmentEvent(packet, entity));
    }

}
