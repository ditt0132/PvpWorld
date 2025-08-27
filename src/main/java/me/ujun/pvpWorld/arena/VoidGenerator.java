package me.ujun.pvpWorld.arena;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class VoidGenerator extends ChunkGenerator {
    @Override
    @SuppressWarnings("deprecation") // 구시그니처 사용 허용
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        return createChunkData(world); // 완전 공허
    }

    @Override public boolean shouldGenerateNoise()        { return false; }
    @Override public boolean shouldGenerateSurface()      { return false; }
    @Override public boolean shouldGenerateCaves()        { return false; }
    @Override public boolean shouldGenerateDecorations()  { return false; }
    @Override public boolean shouldGenerateMobs()         { return false; }
    @Override public boolean shouldGenerateStructures()   { return false; }

}
