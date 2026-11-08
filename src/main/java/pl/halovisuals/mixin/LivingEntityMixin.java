package pl.halovisuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.halovisuals.Module;
import pl.halovisuals.ModuleManager;

/** Swing Animations: adjustable swing speed for the local player. */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void hv$swingSpeed(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        Module m = ModuleManager.get("Swing Animations");
        if (m == null || !m.enabled) return;
        double sp = m.num("Speed");
        if (Math.abs(sp - 1.0) < 0.01) return;
        cir.setReturnValue(Math.max(1, (int) Math.round(cir.getReturnValue() / sp)));
    }
}
