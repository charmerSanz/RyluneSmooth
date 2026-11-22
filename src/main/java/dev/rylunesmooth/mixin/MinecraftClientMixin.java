package dev.rylunesmooth.mixin;

import dev.rylunesmooth.client.RyluneSmoothClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "render(Z)V", at = @At("HEAD"))
    private void rylunesmooth$sampleFrameStart(boolean tick, CallbackInfo ci) {
        RyluneSmoothClient.onFrameStart(System.nanoTime());
    }
}
