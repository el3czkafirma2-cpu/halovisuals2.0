package pl.halovisuals.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.halovisuals.Module;
import pl.halovisuals.ModuleManager;
import pl.halovisuals.Sky;

/** Custom Sky: makes the vanilla sky/fog colour match the horizon of the custom dome. Works for int or Vec3d return types. */
@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void hv$sky(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Object> cir) {
        Module m = ModuleManager.get("Custom Sky");
        if (m == null || !m.enabled) return;
        int rgb = Sky.horizonRgb(m.mode("Style"));
        Object r = cir.getReturnValue();
        if (r instanceof Integer i) cir.setReturnValue((i & 0xFF000000) | rgb);
        else if (r instanceof Vec3d) cir.setReturnValue(new Vec3d(((rgb >> 16) & 255) / 255.0, ((rgb >> 8) & 255) / 255.0, (rgb & 255) / 255.0));
    }
}
