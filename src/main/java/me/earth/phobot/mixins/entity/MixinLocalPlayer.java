package me.earth.phobot.mixins.entity;

import com.mojang.authlib.GameProfile;
import me.earth.phobot.event.MoveEvent;
import me.earth.phobot.event.PathfinderUpdateEvent;
import me.earth.phobot.event.PostMotionPlayerUpdateEvent;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.modules.movement.NoSlowDown;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends AbstractClientPlayer {
    public MixinLocalPlayer(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
        throw new IllegalStateException("MixinLocalPlayer constructor called!");
    }

    // TODO: do not use redirect, we already need the less favorable redirect at isPassenger to avoid a conflict with future
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z", ordinal = 0))
    private boolean isPassengerHook(LocalPlayer player) {
        NoSlowDown.UseItemEvent event = new NoSlowDown.UseItemEvent();
        PingBypassApi.getEventBus().post(event);
        if (event.isCancelled()) {
            return true; // acting like we are a passenger will also skip the slowdown
        }

        return player.isPassenger();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V", shift = At.Shift.AFTER))
    private void postSuperTickHook(CallbackInfo ci) {
        PingBypassApi.getEventBus().post(new PathfinderUpdateEvent());
        PingBypassApi.getEventBus().post(new PreMotionPlayerUpdateEvent());
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;ambientSoundHandlers:Ljava/util/List;", shift = At.Shift.BEFORE))
    private void postMotionPlayerUpdateHook(CallbackInfo ci) {
        PingBypassApi.getEventBus().post(new PostMotionPlayerUpdateEvent());
    }

    @ModifyArg(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"),
        index = 1)
    private Vec3 travelHook(Vec3 moveVec) {
        MoveEvent event = new MoveEvent(moveVec);
        PingBypassApi.getEventBus().post(event);
        return event.getVec();
    }

}
