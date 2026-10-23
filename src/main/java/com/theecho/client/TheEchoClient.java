package com.theecho.client;

import com.theecho.ModBlocks;
import com.theecho.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;

public class TheEchoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.ECHO, EchoEntityRenderer::new);
        // El espejo tiene transparencia: sin esto se ve con fondo negro
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ECHO_MIRROR, RenderLayer.getTranslucent());
    }
}
