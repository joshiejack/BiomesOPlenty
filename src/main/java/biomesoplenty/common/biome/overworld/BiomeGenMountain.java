/*******************************************************************************
 * Copyright 2015-2016, the Biomes O' Plenty Team
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * 
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/

package biomesoplenty.common.biome.overworld;

import java.util.Random;

import com.google.common.base.CaseFormat;

import biomesoplenty.api.block.IBlockPosQuery;
import biomesoplenty.api.config.IBOPWorldSettings;
import biomesoplenty.api.config.IBOPWorldSettings.GeneratorType;
import biomesoplenty.api.config.IConfigObj;
import biomesoplenty.api.enums.BOPClimates;
import biomesoplenty.api.enums.BOPPlants;
import biomesoplenty.api.enums.BOPTrees;
import biomesoplenty.api.enums.BOPWoods;
import biomesoplenty.api.generation.GeneratorStage;
import biomesoplenty.common.block.BlockBOPDoublePlant;
import biomesoplenty.common.util.biome.GeneratorUtils.ScatterYMethod;
import biomesoplenty.common.util.block.BlockQuery;
import biomesoplenty.common.world.generator.GeneratorDoubleFlora;
import biomesoplenty.common.world.generator.GeneratorFlora;
import biomesoplenty.common.world.generator.GeneratorGrass;
import biomesoplenty.common.world.generator.GeneratorLakes;
import biomesoplenty.common.world.generator.GeneratorOreSingle;
import biomesoplenty.common.world.generator.GeneratorSplotches;
import biomesoplenty.common.world.generator.GeneratorWaterside;
import biomesoplenty.common.world.generator.GeneratorWeighted;
import biomesoplenty.common.world.generator.tree.GeneratorBasicTree;
import biomesoplenty.common.world.generator.tree.GeneratorPineTree;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

public class BiomeGenMountain extends BOPBiome
{
    
    public static enum MountainType {PEAKS, FOOTHILLS}
    
    public MountainType type;
    public IBlockState grassBlock;
    public IBlockState dirtBlock;
    public IBlockState coarseDirtBlock;
    public IBlockState stoneBlock;
        
    public BiomeGenMountain(MountainType type)
    {
        super("mountain_" + type.name().toLowerCase(), new PropsBuilder("Mountain " + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, type.toString())).withGuiColour(0x80A355).withTemperature(0.8F).withRainfall(0.1F));

        this.type = type;
        
        this.canSpawnInBiome = false;
        
        // terrain
        switch (type)
        {
            case PEAKS:
                this.terrainSettings.avgHeight(140).heightVariation(30, 60).octaves(1, 1, 2, 2, 3, 3).sidewaysNoise(0.1D); 
                break;
                
            case FOOTHILLS:
                this.terrainSettings.avgHeight(100).heightVariation(15, 30).octaves(0, 1, 1, 3, 1, 0).sidewaysNoise(0.1D); 
                break;
        }
        
        this.canGenerateRivers = false;
        this.canSpawnInBiome = false;
        this.canGenerateVillages = false;
        
        this.beachBiomeLocation = null;
        
        if (type == MountainType.PEAKS)
        {
            this.canGenerateVillages = false;
            
            // peaks are created in the biome gen layer, foothills don't have a weight - they only appear later around the peaks (in the biome edge layer)
            this.addWeight(BOPClimates.DRY_TEMPERATE, 3);
            
            // only sheep and wolves on the peaks
            this.spawnableCreatureList.clear();
            this.spawnableCreatureList.add(new Biome.SpawnListEntry(EntitySheep.class, 12, 4, 6));
            this.spawnableCreatureList.add(new Biome.SpawnListEntry(EntityWolf.class, 4, 4, 4));
        }        
        
        this.topBlock = Blocks.GRASS.getDefaultState();
        this.fillerBlock = Blocks.DIRT.getDefaultState();
        this.grassBlock = this.topBlock;
        this.dirtBlock = this.fillerBlock;
        this.coarseDirtBlock = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT);
        this.stoneBlock = Blocks.STONE.getDefaultState();
        
        // gravel
        this.addGenerator("gravel", GeneratorStage.SAND_PASS2, (new GeneratorWaterside.Builder()).amountPerChunk(6).maxRadius(7).with(Blocks.GRAVEL.getDefaultState()).create());
        this.addGenerator("gravel_patches", GeneratorStage.SAND_PASS2, (new GeneratorSplotches.Builder()).amountPerChunk(2).splotchSize(16).replace(this.topBlock).with(Blocks.GRAVEL.getDefaultState()).scatterYMethod(ScatterYMethod.AT_SURFACE).create());
      
        // lakes
        this.addGenerator("lakes", GeneratorStage.SAND, (new GeneratorLakes.Builder()).amountPerChunk(1.8F).waterLakeForBiome(this).create());        
        
        // trees & logs        
        IBlockPosQuery suitableTreePosition = BlockQuery.buildAnd().withAltitudeBetween(64, 140).materials(Material.GROUND, Material.GRASS).create();
        GeneratorWeighted treeGenerator = new GeneratorWeighted(3);
        this.addGenerator("trees", GeneratorStage.TREE, treeGenerator);
        treeGenerator.add("pine", 2, (new GeneratorPineTree.Builder()).minHeight(6).maxHeight(18).log(BOPWoods.PINE).leaves(BOPTrees.PINE).placeOn(suitableTreePosition).create());        
        treeGenerator.add("oak", 1, (new GeneratorBasicTree.Builder()).create());

        // grasses
        GeneratorWeighted grassGenerator = new GeneratorWeighted(5.0F);
        this.addGenerator("grass", GeneratorStage.GRASS, grassGenerator);
        grassGenerator.add("tallgrass", 2, (new GeneratorGrass.Builder()).with(BlockTallGrass.EnumType.GRASS).generationAttempts(128).create());
        grassGenerator.add("mediumgrass", 1, (new GeneratorGrass.Builder()).with(BOPPlants.MEDIUMGRASS).generationAttempts(128).create());
        grassGenerator.add("shortgrass", 1, (new GeneratorGrass.Builder()).with(BOPPlants.SHORTGRASS).generationAttempts(128).create());
        grassGenerator.add("wheatgrass", 1, (new GeneratorGrass.Builder()).with(BOPPlants.WHEATGRASS).generationAttempts(128).create());
        grassGenerator.add("dampgrass", 1, (new GeneratorGrass.Builder()).with(BOPPlants.DAMPGRASS).generationAttempts(128).create());

        // other plants
        this.addGenerator("shrubs", GeneratorStage.FLOWERS,(new GeneratorFlora.Builder()).amountPerChunk(1.0F).with(BOPPlants.SHRUB).generationAttempts(type == MountainType.FOOTHILLS ? 64 : 32).create());
        this.addGenerator("ferns", GeneratorStage.FLOWERS,(new GeneratorFlora.Builder()).amountPerChunk(2.0F).with(BlockTallGrass.EnumType.FERN).generationAttempts(type == MountainType.FOOTHILLS ? 64 : 32).create());
        this.addGenerator("leaf_piles", GeneratorStage.FLOWERS,(new GeneratorFlora.Builder()).amountPerChunk(0.8F).with(BOPPlants.LEAFPILE).create());
        this.addGenerator("dead_leaf_piles", GeneratorStage.FLOWERS,(new GeneratorFlora.Builder()).amountPerChunk(1.2F).with(BOPPlants.DEADLEAFPILE).create());
        
        if (type == MountainType.FOOTHILLS)
        {
            this.addGenerator("flax", GeneratorStage.FLOWERS, (new GeneratorDoubleFlora.Builder()).amountPerChunk(0.1F).with(BlockBOPDoublePlant.DoublePlantType.FLAX).create());
            this.addGenerator("berry_bushes", GeneratorStage.FLOWERS,(new GeneratorFlora.Builder()).amountPerChunk(0.3F).with(BOPPlants.BERRYBUSH).create());
            this.addGenerator("water_reeds", GeneratorStage.LILYPAD, (new GeneratorFlora.Builder()).amountPerChunk(0.4F).with(BOPPlants.REED).generationAttempts(32).create());
        }
        
        // TODO: some flowers?
                
        // gem
        this.addGenerator("emeralds", GeneratorStage.SAND, (new GeneratorOreSingle.Builder()).amountPerChunk(12).with(Blocks.EMERALD_ORE.getDefaultState()).create());
   
    }
    
    @Override
    public void configure(IConfigObj conf)
    {
        super.configure(conf);
        
        this.grassBlock = this.topBlock;
        this.dirtBlock = this.fillerBlock;        
        this.coarseDirtBlock = conf.getBlockState("coarseDirtBlock", this.coarseDirtBlock);
        this.stoneBlock = conf.getBlockState("stoneBlock", this.stoneBlock);
        //this.snowBlock = conf.getBlockState("snowBlock", this.snowBlock);
        //this.packedSnowBlock = conf.getBlockState("packedSnowBlock", this.packedSnowBlock);
    }
    
    @Override
    public void applySettings(IBOPWorldSettings settings)
    {
        if (!settings.isEnabled(GeneratorType.MUSHROOMS)) {this.removeGenerator("glowshrooms");}
        
        if (!settings.isEnabled(GeneratorType.FLOWERS)) {this.removeGenerator("miners_delight");}
        
        if (!settings.isEnabled(GeneratorType.ROCK_FORMATIONS)) {this.removeGenerator("stone_formations");}
        
        if (!settings.isEnabled(GeneratorType.FLAX)) {this.removeGenerator("flax");}
        if (!settings.isEnabled(GeneratorType.BERRY_BUSHES)) {this.removeGenerator("berry_bushes");}
        
        if (!settings.isEnabled(GeneratorType.FOLIAGE)) {this.removeGenerator("bushes"); this.removeGenerator("koru"); this.removeGenerator("shrubs"); this.removeGenerator("leaf_piles"); this.removeGenerator("dead_leaf_piles"); this.removeGenerator("clover_patches"); this.removeGenerator("sprouts");}
        
        if (!settings.isEnabled(GeneratorType.PLANTS)) {this.removeGenerator("cattail"); this.removeGenerator("double_cattail"); this.removeGenerator("river_cane"); this.removeGenerator("tiny_cacti"); this.removeGenerator("roots"); this.removeGenerator("rafflesia"); this.removeGenerator("desert_sprouts");}
        
        if (!settings.isEnabled(GeneratorType.WATER_PLANTS)) {this.removeGenerator("algae"); this.removeGenerator("water_reeds"); this.removeGenerator("medium_lily"); this.removeGenerator("small_lily"); this.removeGenerator("tiny_lily");}
        
        GeneratorWeighted treeGen = (GeneratorWeighted)this.getGenerator("trees");
        if (!settings.isEnabled(GeneratorType.TREES)) {this.removeGenerator("trees");
        
        IBlockPosQuery suitableTreePosition = BlockQuery.buildAnd().withAltitudeBetween(64, 140).materials(Material.GROUND, Material.GRASS).create();
        GeneratorWeighted treeGenerator = new GeneratorWeighted(3);
        this.addGenerator("trees", GeneratorStage.TREE, treeGenerator);
        treeGenerator.add("pine", 2, (new GeneratorPineTree.Builder()).minHeight(6).maxHeight(18).log(BlockPlanks.EnumType.SPRUCE).leaves(BlockPlanks.EnumType.SPRUCE).placeOn(suitableTreePosition).create());        
        treeGenerator.add("oak", 1, (new GeneratorBasicTree.Builder()).create());
        }
        
        GeneratorWeighted grassGen = (GeneratorWeighted)this.getGenerator("grass");
        if (!settings.isEnabled(GeneratorType.GRASSES)) {grassGen.removeGenerator("shortgrass"); grassGen.removeGenerator("mediumgrass"); grassGen.removeGenerator("wheatgrass"); grassGen.removeGenerator("dampgrass");}
    }
    
    
    
    @Override
    public void genTerrainBlocks(World world, Random rand, ChunkPrimer primer, int x, int z, double noise)
    {
        int localX = x & 15;
        int localZ = z & 15;
        int height = 255;
        while (height > 0 && primer.getBlockState(localX, height, localZ).getMaterial() == Material.AIR) {height--;}
        int peakLine = 140 + (int)(noise * 5);
        
        if (height > peakLine)
        {
            if (noise > 1.7D)
            {
                this.topBlock = this.stoneBlock;
                this.fillerBlock = this.stoneBlock;
            }
            else
            {
                this.topBlock = this.grassBlock;
                this.fillerBlock = this.dirtBlock;
            }
        }
        else
        {
            if (noise < -1.4D) 
            {
                this.topBlock = this.coarseDirtBlock;
                this.fillerBlock = this.coarseDirtBlock;
            }
            else if (this.type == MountainType.PEAKS && noise > 1.7D)
            {
                this.topBlock = this.stoneBlock;
                this.fillerBlock = this.stoneBlock;
            }
            else
            {
                this.topBlock = this.grassBlock;
                this.fillerBlock = this.dirtBlock;
            }
        }
        super.genTerrainBlocks(world, rand, primer, x, z, noise);
    }
    
}