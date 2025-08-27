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

        // 재사용 풀
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

        /** 한 슬롯을 대여해서 원점 반환 (아레나 배치 전에 호출) */
        public Allocation acquire(World w) {
            Integer idx = free.poll();
            if (idx == null) idx = next.getAndIncrement();
            inUse.add(idx);
            return new Allocation(idx, indexToOrigin(w, idx));
        }

        /** 매치 종료 후 슬롯 반납 */
        public void release(int slotIndex) {
            // 중복 반납 방지
            if (inUse.remove(slotIndex)) {
                free.offer(slotIndex);
            }
        }

        /** (선택) 슬롯을 미리 청크 로드해 웜업 */
        public void warmup(World w, Allocation alloc, int sizeX, int sizeZ) {
            // 아레나 실제 크기에 맞춰 포함되는 청크 전체 로드
            int minCX = alloc.origin.getBlockX() >> 4;
            int minCZ = alloc.origin.getBlockZ() >> 4;
            int maxCX = (alloc.origin.getBlockX() + Math.max(0, sizeX - 1)) >> 4;
            int maxCZ = (alloc.origin.getBlockZ() + Math.max(0, sizeZ - 1)) >> 4;
            for (int cx = minCX; cx <= maxCX; cx++) {
                for (int cz = minCZ; cz <= maxCZ; cz++) {
                    w.getChunkAt(cx, cz).load(); // Paper면 비동기 로드를 쓰는 것도 가능
                }
            }
        }

        /** 인덱스 → 좌표 매핑 (고정 격자) */
        private Location indexToOrigin(World w, int index) {
            int gx = Math.floorMod(index, cols);
            int gz = Math.floorDiv(index, cols);
            int ox = gx * spacingX;
            int oz = gz * spacingZ;
            return new Location(w, ox, baseY, oz);
        }

        /** 대여 결과 */
        public static final class Allocation {
            public final int slotIndex;
            public final Location origin;
            public Allocation(int slotIndex, Location origin) {
                this.slotIndex = slotIndex;
                this.origin = origin;
            }
        }
    }
