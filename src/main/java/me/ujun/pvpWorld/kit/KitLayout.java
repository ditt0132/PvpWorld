package me.ujun.pvpWorld.kit;

import java.util.ArrayList;
import java.util.List;

public class KitLayout {
        private KitLayout() {}

        // UI 고정 슬롯들
        public static final int UI_BOOTS   = 0;
        public static final int UI_LEG     = 1;
        public static final int UI_CHEST   = 2;
        public static final int UI_HELMET  = 3;
        public static final int UI_OFFHAND = 4;

        public static final int[] UI_SEPARATOR = range(9, 17);     // 클릭 막기
        public static final int[] UI_UNUSED_TOP = range(5, 8);     // 클릭 막기(미사용)

        // UI 인벤/핫바 구역
        public static final int[] UI_INV_3ROWS = range(18, 44);    // 27칸 (위→아래, 좌→우)
        public static final int[] UI_HOTBAR    = range(45, 53);    // 9칸

        // 캐논컬 인덱스
        // 0..8 핫바, 9..35 인벤
        public static final int[] CANON_INV_3ROWS = range(9, 35);
        public static final int[] CANON_HOTBAR    = range(0, 8);

        private static int[] range(int from, int toInclusive) {
            List<Integer> list = new ArrayList<>();
            for (int i = from; i <= toInclusive; i++) list.add(i);
            return list.stream().mapToInt(Integer::intValue).toArray();
        }
}
