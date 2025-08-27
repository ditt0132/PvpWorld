package me.ujun.pvpWorld.duel;

import me.ujun.pvpWorld.arena.ArenaMeta;
import me.ujun.pvpWorld.kit.Kit;
import org.bukkit.Location;

import java.util.*;

public class Instance {
    public final Kit kit;
    public final Location origin;

    public final Set<UUID> teamA = new HashSet<>();
    public final Set<UUID> teamB = new HashSet<>();
    public final Set<UUID> eliminated = new HashSet<>();

    public int slotIndex = -1;      // allocator 슬롯 인덱스
    public boolean ended = false;
    public boolean party;
    public String type;
    public Map<UUID, Integer> scoreMap = new HashMap<>();
    public Map<String, Integer> partyScoreMap = new HashMap<>();
    public int roundSetting;
    public int round = 0;
    public ArenaMeta meta;

    public final int sx, sy, sz;
    public volatile boolean countdown = true; // ⬅ 카운트다운 중 여부
    public int countdownTaskId = -1;          // ⬅ 3초 타이머
    public int timeoutTaskId = -1;          // ⬅ 10분 타이머

    public Instance(Kit kit, Location origin, int sx, int sy, int sz, String type, int roundSetting, ArenaMeta meta, boolean party) {
        this.kit = kit;
        this.origin = origin;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.type = type;
        this.roundSetting = roundSetting;
        this.meta = meta;
        this.party = party;
    }
}
