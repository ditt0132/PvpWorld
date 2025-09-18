package me.ujun.pvpWorld.arena;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ArenaAllocator {
        private final int spacingX, spacingZ, baseY;
        private final int cols; // 가로 칸 수 (한 행 최대 슬롯 수)
        private final AtomicInteger next = new AtomicInteger(0);

        private final Queue<Integer> free = new ConcurrentLinkedQueue<>();
        private final Set<Integer> inUse = ConcurrentHashMap.newKeySet();

        public ArenaAllocator(int spacingX, int spacingZ, int baseY) {
            this(spacingX, spacingZ, baseY, 64);
        }
        public ArenaAllocator(int spacingX, int spacingZ, int baseY, int cols) {
            this.spacingX = spacingX;
            this.spacingZ = spacingZ;
            this.baseY = baseY;
            this.cols = Math.max(1, cols);
        }


        public Allocation acquire(World w) {
            Integer idx = free.poll();
            if (idx == null) idx = next.getAndIncrement();
            inUse.add(idx);
            return new Allocation(idx, indexToOrigin(w, idx));
        }


        public void release(int slotIndex) {
            if (inUse.remove(slotIndex)) {
                free.offer(slotIndex);
            }
        }


        // 청크 로드하는 건데 쓸모 있을 지는 몰?루
        public void warmup(World w, Allocation alloc, int sizeX, int sizeZ) {

            int minCX = alloc.origin.getBlockX() >> 4;
            int minCZ = alloc.origin.getBlockZ() >> 4;
            int maxCX = (alloc.origin.getBlockX() + Math.max(0, sizeX - 1)) >> 4;
            int maxCZ = (alloc.origin.getBlockZ() + Math.max(0, sizeZ - 1)) >> 4;
            for (int cx = minCX; cx <= maxCX; cx++) {
                for (int cz = minCZ; cz <= maxCZ; cz++) {
                    w.getChunkAt(cx, cz).load();
                }
            }
        }


        private Location indexToOrigin(World w, int index) {
            int gx = Math.floorMod(index, cols);
            int gz = Math.floorDiv(index, cols);
            int ox = gx * spacingX;
            int oz = gz * spacingZ;
            return new Location(w, ox, baseY, oz);
        }

        public static final class Allocation {
            public final int slotIndex;
            public final Location origin;
            public Allocation(int slotIndex, Location origin) {
                this.slotIndex = slotIndex;
                this.origin = origin;
            }
        }
    }
