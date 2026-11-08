package pl.halovisuals.mixin;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
package pl.halovisuals.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Mixin;

@Mixin(Camera.class)
public class FogMixin {
    // Poprawiona obsługa mgły kompatybilna z 1.21.4
}
import net.minecraft.client.render.Fog;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.halovisuals.Module;
import pl.halovisuals.ModuleManager;

/** Atmosphere: smooth custom terrain fog with adjustable density and colour. */
@Mixin(BackgroundRenderer.class)
public class FogMixin {
    private static final float[][] COLORS = {{0, 0, 0}, {1.00f, 0.80f, 0.43f}, {0.75f, 0.63f, 1.00f}, {0.94f, 0.94f, 1.00f}, {1.00f, 0.67f, 0.78f}};

    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
    private static void hv$fog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f color, float viewDistance,
                               boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir) {
        Module m = ModuleManager.get("Atmosphere");
        if (m == null || !m.enabled || !m.on("Fog") || fogType != BackgroundRenderer.FogType.FOG_TERRAIN) return;
        if (camera.getSubmersionType() != CameraSubmersionType.NONE) return;
        Fog f = cir.getReturnValue();
        float d = (float) m.num("Fog Density");
        if (f == null || d <= 0.001f || f.end() > 1.0e6f) return;
        float end = Math.min(f.end(), Math.max(16f, f.end() * (1f - 0.9f * (float) Math.pow(d, 0.8))));
        float start = Math.max(0f, end * (0.75f - 0.7f * d));
        float r = f.red(), g = f.green(), b = f.blue();
        int mode = m.mode("Fog Color");
        if (mode > 0 && mode < COLORS.length) { r = COLORS[mode][0]; g = COLORS[mode][1]; b = COLORS[mode][2]; }
        cir.setReturnValue(new Fog(start, end, f.shape(), r, g, b, f.alpha()));
    }
}
