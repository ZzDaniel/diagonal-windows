package fuzs.diagonalwindows.client;

import fuzs.diagonalwindows.DiagonalWindows;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Map;

@Mod.EventBusSubscriber(modid = DiagonalWindows.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DiagonalWindowsCtmBridge {
    private static final String CTM_MODEL_CLASS = "me.pepperbell.continuity.client.model.CtmBakedModel";

    private DiagonalWindowsCtmBridge() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Class<?> ctmModelClass;
        Constructor<?> constructor;
        try {
            ctmModelClass = Class.forName(CTM_MODEL_CLASS);
            constructor = ctmModelClass.getConstructor(BakedModel.class, BlockState.class);
        } catch (ReflectiveOperationException e) {
            return;
        }

        Map<ResourceLocation, BakedModel> models = event.getModels();
        int candidates = 0;
        int alreadyWrapped = 0;
        int newlyWrapped = 0;

        try {
            for (Map.Entry<ResourceLocation, BakedModel> entry : new ArrayList<>(models.entrySet())) {
                ResourceLocation modelId = entry.getKey();
                if (!DiagonalWindows.MOD_ID.equals(modelId.getNamespace()) || !modelId.getPath().contains("/")) {
                    continue;
                }

                ResourceLocation blockId = new ResourceLocation(modelId.getNamespace(), modelId.getPath());
                Block block = BuiltInRegistries.BLOCK.get(blockId);
                if (!blockId.equals(BuiltInRegistries.BLOCK.getKey(block))) {
                    continue;
                }

                candidates++;
                BakedModel model = entry.getValue();
                if (ctmModelClass.isInstance(model)) {
                    alreadyWrapped++;
                    continue;
                }

                BakedModel wrapped = (BakedModel) constructor.newInstance(model, block.defaultBlockState());
                models.put(modelId, wrapped);
                newlyWrapped++;
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to wrap Diagonal Windows baked models with Continuity", e);
        }

        DiagonalWindows.LOGGER.info("Continuity model bridge processed {} models (already wrapped: {}, newly wrapped: {})",
                candidates, alreadyWrapped, newlyWrapped);
    }
}
