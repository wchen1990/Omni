package com.pugz.omni.core.module;

import com.google.common.collect.ImmutableSet;
import com.pugz.omni.common.block.colormatic.*;
import com.pugz.omni.common.entity.colormatic.FallingConcretePowderEntity;
import com.pugz.omni.common.world.OmniBiomeMaker;
import com.pugz.omni.core.registry.OmniBiomes;
import com.pugz.omni.core.registry.OmniBlocks;
import com.pugz.omni.core.registry.OmniEntities;
import com.pugz.omni.core.util.BiomeFeatures;
import com.pugz.omni.core.util.RegistryUtil;
import com.pugz.omni.core.util.TradeUtils;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Features;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ColormaticModule extends AbstractModule {
    public static final ColormaticModule instance = new ColormaticModule();
    public static List<Supplier<Block>> stackables = new ArrayList<Supplier<Block>>();

    public ColormaticModule() {
        super("Colormatic");
    }

    public void onInitialize() {
        MinecraftForge.EVENT_BUS.addListener(this::onWandererTrades);
        MinecraftForge.EVENT_BUS.addListener(this::onBiomeLoading);
        MinecraftForge.EVENT_BUS.addListener(this::onRightClickBlock);
    }

    @Override
    protected void registerBlocks() {
        for (DyeColor color : DyeColor.values()) {
            RegistryObject<Block> OVERRIDE_CONCRETE = RegistryUtil.createOverrideBlock(color.name().toLowerCase() + "_concrete", () -> new Block(AbstractBlock.Properties.from(Blocks.BLACK_CONCRETE)), null);
            RegistryObject<Block> OVERRIDE_CONCRETE_POWDER = RegistryUtil.createOverrideBlock(color.name().toLowerCase() + "_concrete_powder", () -> new ConcretePowderBlock(OVERRIDE_CONCRETE.get(), AbstractBlock.Properties.from(Blocks.BLACK_CONCRETE_POWDER)), null);

            RegistryObject<Block> QUILTED_CARPET = RegistryUtil.createBlock(color.name().toLowerCase() + "_quilted_carpet", QuiltedCarpetBlock::new, ItemGroup.DECORATIONS);
            RegistryObject<Block> QUILTED_WOOL = RegistryUtil.createBlock(color.name().toLowerCase() + "_quilted_wool", () -> new Block(AbstractBlock.Properties.create(Material.WOOL, color).hardnessAndResistance(0.8F).sound(SoundType.CLOTH)), ItemGroup.BUILDING_BLOCKS);
            RegistryObject<Block> GLAZED_TERRACOTTA_PILLAR = RegistryUtil.createBlock(color.name().toLowerCase() + "_glazed_terracotta_pillar", GlazedTerracottaPillarBlock::new, ItemGroup.DECORATIONS);
            RegistryObject<Block> CONCRETE = RegistryUtil.createBlock(color.name().toLowerCase() + "_concrete", () -> new LayerConcreteBlock(color), ItemGroup.BUILDING_BLOCKS);
            RegistryObject<Block> CONCRETE_POWDER = RegistryUtil.createBlock(color.name().toLowerCase() + "_concrete_powder", () -> new LayerConcretePowderBlock(CONCRETE.get(), color), ItemGroup.BUILDING_BLOCKS);
            //RegistryObject<Block> DYE_SACK = RegistryUtil.createBlock(color.name().toLowerCase() + "_dye_sack", () -> new Block(AbstractBlock.Properties.from(Blocks.BLACK_CONCRETE)), ItemGroup.BUILDING_BLOCKS);
        }

        for (Block block : ForgeRegistries.BLOCKS.getValues()) {
            if (block instanceof FlowerBlock || block instanceof MushroomBlock || block instanceof FungusBlock) {
                String name;

                if (block instanceof FungusBlock) {
                    name = StringUtils.replace(block.getRegistryName().getPath(), "us", "i");
                }
                else name = block.getRegistryName().getPath() + "s";

                final RegistryObject<Block> FLOWERS = RegistryUtil.createBlock(name, () -> new FlowersBlock(AbstractBlock.Properties.from(block), block), null);
                stackables.add(FLOWERS);
            }
        }

        OmniBlocks.TRADERS_QUILTED_CARPET = RegistryUtil.createBlock("traders_quilted_carpet", QuiltedCarpetBlock::new, ItemGroup.DECORATIONS);
        OmniBlocks.TRADERS_QUILTED_WOOL = RegistryUtil.createBlock("traders_quilted_wool", () -> new Block(AbstractBlock.Properties.create(Material.WOOL, DyeColor.BLUE).hardnessAndResistance(0.8F).sound(SoundType.CLOTH)), ItemGroup.BUILDING_BLOCKS);

        //OmniBlocks.EUCALYPTUS_LOG = RegistryUtil.createBlock(null, null, null);
        //OmniBlocks.EUCALYPTUS_PLANKS = RegistryUtil.createBlock(null, null, null);

        //OmniBlocks.FLOWER_STEM = RegistryUtil.createBlock(null, null, null);
        //OmniBlocks.DANDELION_PETAL_BLOCK = RegistryUtil.createBlock(null, null, null);
        //OmniBlocks.DANDELION_FLUFF_BLOCK = RegistryUtil.createBlock(null, null, null);
        //OmniBlocks.TULIP_PETAL_BLOCK = RegistryUtil.createBlock(null, null, null);
        //OmniBlocks.ROSE_PETAL_BLOCK = RegistryUtil.createBlock(null, null, null);
        //OmniBlocks.FLOWER_PLANKS = RegistryUtil.createBlock(null, null, null);
    }

    @Override
    protected void registerItems() {
        //RegistryObject<Item> DYES; ?

        //RegistryObject<Item> DANDELION_FLUFF;
    }

    @Override
    protected void registerEntities() {
        OmniEntities.FALLING_CONCRETE_POWDER = RegistryUtil.createEntity("falling_concrete_powder", () -> OmniEntities.createFallingBlockEntity(FallingConcretePowderEntity::new));

        //RegistryObject<EntityType<?>> AEROMA;
        //RegistryObject<EntityType<?>> KOALA;
    }

    @Override
    protected void registerBiomes() {
        OmniBiomes.FLOWER_FIELD = RegistryUtil.createBiome("flower_field", OmniBiomeMaker.makeFlowerFieldBiome(), BiomeManager.BiomeType.WARM, 1, BiomeDictionary.Type.PLAINS, BiomeDictionary.Type.RARE, BiomeDictionary.Type.OVERWORLD, BiomeDictionary.Type.LUSH);
        //RegistryObject<Biome> BLOOMING_FLOWER_FIELD;
        //RegistryObject<Biome> BLOOMING_FLOWER_FOREST;
        //RegistryObject<Biome> EUCALYPTUS_FOREST;
    }

    @Override
    protected void registerFeatures() {
        //RegistryObject<Feature<?>> GIANT_DANDELION;
        //RegistryObject<Feature<?>> GIANT_TULIP;
        //RegistryObject<Feature<?>> GIANT_ROSE;
        //RegistryObject<Feature<?>> EUCALYPTUS_TREE;
        //RegistryObject<Feature<?>> RAINBOW_EUCALYPTUS_TREE;
    }

    @Override
    protected void registerEffects() {
        //RegistryObject<Effect> ATTRACTION;
    }

    @Override
    protected void registerParticles() {
        //RegistryObject<ParticleType<?>> FLOWER_PARTICLE;
        //RegistryObject<ParticleType<?>> DYE_PARTICLE;
    }

    @Override
    protected void registerSounds() {
    }

    @Override
    protected void registerStats() {
    }

    public static BlockState getFlower(String name) {
        for (Supplier<Block> block : stackables) {
            if (block.get().getRegistryName().getPath().equals(name)) {
                return block.get().getDefaultState();
            }
        }

        return null;
    }

    public void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().addAll(ImmutableSet.of(
                new TradeUtils.ItemsForEmeraldsTrade(new ItemStack(OmniBlocks.RED_LOTUS_FLOWER.get()), 1, 1, 12, 1),
                new TradeUtils.ItemsForEmeraldsTrade(new ItemStack(OmniBlocks.BLUE_LOTUS_FLOWER.get()), 1, 1, 12, 1),
                new TradeUtils.ItemsForEmeraldsTrade(new ItemStack(OmniBlocks.PINK_LOTUS_FLOWER.get()), 1, 1, 12, 1),
                new TradeUtils.ItemsForEmeraldsTrade(new ItemStack(OmniBlocks.BLACK_LOTUS_FLOWER.get()), 1, 1, 12, 1),
                new TradeUtils.ItemsForEmeraldsTrade(new ItemStack(OmniBlocks.WHITE_LOTUS_FLOWER.get()), 1, 1, 12, 1),

                new TradeUtils.ItemsForEmeraldsTrade(new ItemStack(OmniBlocks.TRADERS_QUILTED_WOOL.get()), 1, 8, 8, 2)
        ));
    }

    public void onBiomeLoading(BiomeLoadingEvent event) {
        BiomeGenerationSettingsBuilder gen = event.getGeneration();
        ResourceLocation name = event.getName();
        Random random = new Random();

        if (BiomeFeatures.hasFeature(gen, name, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.FLOWER_DEFAULT)) {
            System.out.println(gen.toString() + " has flower default");

            BiomeFeatures.removeFeature(gen, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.FLOWER_DEFAULT);
            BiomeFeatures.addScatteredBlock(gen, getFlower("poppys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 12);
            BiomeFeatures.addScatteredBlock(gen, getFlower("dandelions").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 12);
        }

        /*
        if (BiomeFeatures.hasFeature(gen, name, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.FLOWER_FOREST)) {
            System.out.println(gen.toString() + " has flower forest");

            BiomeFeatures.removeFeature(gen, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.FLOWER_FOREST);
            BiomeFeatures.addScatteredBlock(gen, getFlower("poppys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 1), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("dandelions").with(FlowersBlock.FLOWERS, random.nextInt(3) + 1), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("alliums").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("cornflowers").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("red_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("orange_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("white_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("pink_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("lily_of_the_valleys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("oxeye_daisys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
        }

        if (BiomeFeatures.hasFeature(gen, name, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.FLOWER_PLAIN)) {
            System.out.println(gen.toString() + " has flower plain");

            BiomeFeatures.removeFeature(gen, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.FLOWER_PLAIN);
            BiomeFeatures.addScatteredBlock(gen, getFlower("poppys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 1), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("lily_of_the_valleys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("oxeye_daisys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("cornflowers").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("red_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("orange_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("white_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("pink_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
        }

        if (BiomeFeatures.hasFeature(gen, name, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.FLOWER_PLAIN_DECORATED)) {
            System.out.println(gen.toString() + " has flower plain decorated");

            BiomeFeatures.removeFeature(gen, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.FLOWER_PLAIN_DECORATED);
            BiomeFeatures.addScatteredBlock(gen, getFlower("poppys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 1), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("lily_of_the_valleys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("oxeye_daisys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("cornflowers").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("red_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("orange_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("white_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("pink_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
        }

        if (BiomeFeatures.hasFeature(gen, name, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.FLOWER_SWAMP)) {
            System.out.println(gen.toString() + " has flower swamp");

            BiomeFeatures.removeFeature(gen, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.FLOWER_SWAMP);
            BiomeFeatures.addScatteredBlock(gen, getFlower("blue_orchids").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 12);
        }

        if (BiomeFeatures.hasFeature(gen, name, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.FLOWER_WARM)) {
            System.out.println(gen.toString() + " has flower warm");

            BiomeFeatures.removeFeature(gen, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.FLOWER_WARM);
            BiomeFeatures.addScatteredBlock(gen, getFlower("poppys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("dandelions").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
        }

        if (BiomeFeatures.hasFeature(gen, name, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.PATCH_BROWN_MUSHROOM)) {
            System.out.println(gen.toString() + " has brown mushrooms");

            BiomeFeatures.removeFeature(gen, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.PATCH_BROWN_MUSHROOM);
            BiomeFeatures.addScatteredBlock(gen, getFlower("brown_mushrooms").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.STONE, Blocks.GRANITE, Blocks.ANDESITE, Blocks.DIORITE), 1, 16);
        }

        if (BiomeFeatures.hasFeature(gen, name, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.PATCH_RED_MUSHROOM)) {
            System.out.println(gen.toString() + " has red mushrooms");

            BiomeFeatures.removeFeature(gen, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.PATCH_RED_MUSHROOM);
            BiomeFeatures.addScatteredBlock(gen, getFlower("red_mushrooms").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.STONE, Blocks.GRANITE, Blocks.ANDESITE, Blocks.DIORITE), 1, 16);
        }

        if (BiomeFeatures.hasFeature(gen, name, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.CRIMSON_FUNGI)) {
            System.out.println(gen.toString() + " has crimson fungi");

            BiomeFeatures.removeFeature(gen, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.CRIMSON_FUNGI);
            BiomeFeatures.addScatteredBlock(gen, getFlower("crimson_fungi").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.CRIMSON_NYLIUM), 1, 16);
        }

        if (BiomeFeatures.hasFeature(gen, name, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.WARPED_FUNGI)) {
            System.out.println(gen.toString() + " has warped fungi");

            BiomeFeatures.removeFeature(gen, GenerationStage.Decoration.VEGETAL_DECORATION, () -> Features.WARPED_FUNGI);
            BiomeFeatures.addScatteredBlock(gen, getFlower("warped_fungi").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.WARPED_NYLIUM), 1, 16);
        }
        */

        if (name.equals(new ResourceLocation("omni", "flower_field"))) {
            BiomeFeatures.addScatteredBlock(gen, getFlower("poppys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 1), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("dandelions").with(FlowersBlock.FLOWERS, random.nextInt(3) + 1), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("alliums").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("cornflowers").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("red_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("orange_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("white_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("pink_tulips").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("lily_of_the_valleys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
            BiomeFeatures.addScatteredBlock(gen, getFlower("oxeye_daisys").with(FlowersBlock.FLOWERS, random.nextInt(3) + 2), ImmutableSet.of(Blocks.GRASS_BLOCK), 1, 16);
        }
    }

    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        BlockPos pos = event.getPos();
        World world = event.getWorld();
        Block block = world.getBlockState(pos).getBlock();
        PlayerEntity player = event.getPlayer();

        if ((block instanceof FlowerBlock || block instanceof MushroomBlock || block instanceof FungusBlock) && stack.getItem() == block.asItem() && !player.isSneaking()) {
            for (Supplier<Block> b : stackables) {
                if (((FlowersBlock)b.get()).getBase() == block) {
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    player.sendBreakAnimation(event.getHand());

                    world.setBlockState(pos, b.get().getDefaultState(), 3);
                }
            }
            event.setCanceled(true);
        }
    }
}