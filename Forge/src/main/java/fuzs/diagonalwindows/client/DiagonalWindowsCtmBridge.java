package fuzs.diagonalwindows.client;

import fuzs.diagonalblocks.api.v2.DiagonalBlockTypes;
import fuzs.diagonalwindows.DiagonalWindows;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Constructor;
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
        int missing = 0;

        try {
            // Dynamic model registry maps only support keyed access during this event.
            for (Block block : DiagonalBlockTypes.WINDOW.getBlockConversions().values()) {
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    ModelResourceLocation modelId = BlockModelShaper.stateToModelLocation(state);
                    BakedModel model = models.get(modelId);
                    if (model == null) {
                        missing++;
                        continue;
                    }

                    candidates++;
                    if (ctmModelClass.isInstance(model)) {
                        alreadyWrapped++;
                        continue;
                    }

                    BakedModel wrapped = (BakedModel) constructor.newInstance(model, state);
                    models.put(modelId, wrapped);
                    newlyWrapped++;
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to wrap Diagonal Windows baked models with Continuity", e);
        }

        DiagonalWindows.LOGGER.info("Continuity model bridge processed {} models (already wrapped: {}, newly wrapped: {}, missing: {})",
                candidates, alreadyWrapped, newlyWrapped, missing);
    }
}
