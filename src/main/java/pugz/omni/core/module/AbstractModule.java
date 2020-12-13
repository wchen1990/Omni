package pugz.omni.core.module;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.registries.ForgeRegistries;
import pugz.omni.core.util.IBaseBlock;

public abstract class AbstractModule {
    private final String name;

    public AbstractModule(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    protected abstract void onInitialize();

    protected abstract void onClientInitialize();

    protected abstract void onPostInitialize();

    protected abstract void sendInitMessage();

    public void initialize() {
        registerBlocks();
        registerItems();
        registerTileEntities();
        registerEntities();
        registerBiomes();
        registerSurfaceBuilders();
        registerFeatures();
        registerStructures();
        registerCarvers();
        registerDimensions();
        registerEnchantments();
        registerEffects();
        registerSounds();
        registerParticles();
        registerStats();

        onInitialize();
        sendInitMessage();
    }

    public void initializeClient() {
        onClientInitialize();

        ForgeRegistries.BLOCKS.getEntries().forEach((block) -> {
            if (block.getValue() instanceof IBaseBlock) {
                IBaseBlock baseBlock = (IBaseBlock) block.getValue();

                if (baseBlock.getRenderType() != RenderType.getSolid()) RenderTypeLookup.setRenderLayer((Block) baseBlock, baseBlock.getRenderType());
            }
        });
    }

    public void initializePost() {
        onPostInitialize();

        registerConfiguredFeatures();
        registerConfiguredSurfaceBuilders();
        //registerStructureFeatures();

        ForgeRegistries.BLOCKS.getEntries().forEach((block) -> {
            if (block.getValue() instanceof IBaseBlock) {
                IBaseBlock baseBlock = (IBaseBlock) block.getValue();
                final FireBlock fire = (FireBlock) Blocks.FIRE;

                if (baseBlock.getFireFlammability() != 0 && baseBlock.getFireEncouragement() != 0) fire.setFireInfo((Block) baseBlock, baseBlock.getFireEncouragement(), baseBlock.getFireFlammability());
                if (baseBlock.getCompostChance() != 0.0F) ComposterBlock.CHANCES.put(((Block) baseBlock).asItem(), baseBlock.getCompostChance());
            }
        });
    }

    protected void registerBlocks() {
    }

    protected void registerItems() {
    }

    protected void registerTileEntities() {
    }

    protected void registerEntities() {
    }

    protected void registerBiomes() {
    }

    protected void registerSurfaceBuilders() {
    }

    protected void registerConfiguredSurfaceBuilders() {
    }

    protected void registerFeatures() {
    }

    protected void registerConfiguredFeatures() {
    }

    protected void registerStructures() {
    }

    protected void registerStructureFeatures() {
    }

    protected void registerCarvers() {
    }

    protected void registerDimensions() {
    }

    protected void registerEnchantments() {
    }

    protected void registerEffects() {
    }

    protected void registerParticles() {
    }

    protected void registerSounds() {
    }

    protected void registerStats() {
    }
}