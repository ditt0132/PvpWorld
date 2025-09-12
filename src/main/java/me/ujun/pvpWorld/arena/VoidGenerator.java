package me.ujun.pvpWorld.arena;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class VoidGenerator extends ChunkGenerator {
    @Override
    @SuppressWarnings("deprecation") // 구시그니처 사용 허용
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        return createChunkData(world); // 완전 공허
    }

    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(final @NotNull WorldInfo worldInfo) {
        return VoidBiomeProvider.INSTANCE;
    }

    @Override
    public boolean shouldGenerateNoise() { return false; }

    @Override
    public boolean shouldGenerateNoise(final @NotNull WorldInfo worldInfo, final @NotNull Random random, final int chunkX, final int chunkZ) {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() { return false; }

    @Override
    public boolean shouldGenerateSurface(final @NotNull WorldInfo worldInfo, final @NotNull Random random, final int chunkX, final int chunkZ) {
        return false;
    }


    @Override
    public boolean shouldGenerateCaves() { return false; }

    @Override
    public boolean shouldGenerateCaves(final @NotNull WorldInfo worldInfo, final @NotNull Random random, final int chunkX, final int chunkZ) {
        return false;
    }


    @Override
    public boolean shouldGenerateDecorations()  { return false; }

    @Override
    public boolean shouldGenerateDecorations(final @NotNull WorldInfo worldInfo, final @NotNull Random random, final int chunkX, final int chunkZ) {
        return false;
    }


    @Override
    public boolean shouldGenerateMobs() { return false; }

    @Override
    public boolean shouldGenerateMobs(final @NotNull WorldInfo worldInfo, final @NotNull Random random, final int chunkX, final int chunkZ) {
        return false;
    }


    @Override
    public boolean shouldGenerateStructures() { return false; }

    @Override
    public boolean shouldGenerateStructures(final @NotNull WorldInfo worldInfo, final @NotNull Random random, final int chunkX, final int chunkZ) {
        return false;
    }
}
