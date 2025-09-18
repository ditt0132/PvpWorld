package me.ujun.pvpWorld.arena;

import me.ujun.pvpWorld.saving.ArenasFile;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.util.*;

public class ArenaManager {
    private final ArenasFile arenasFile;
    public ArenaManager(ArenasFile arenasFile) { this.arenasFile = arenasFile; }

    public static final Map<String, ArenaMeta> arenas = new HashMap<>();

    public boolean exists(String name) { return arenas.containsKey(name.toLowerCase(Locale.ROOT)); }
    public ArenaMeta get(String name)   { return arenas.get(name.toLowerCase(Locale.ROOT)); }
    public Collection<ArenaMeta> all()  { return Collections.unmodifiableCollection(arenas.values()); }
    public void put(ArenaMeta m)        { arenas.put(m.name().toLowerCase(Locale.ROOT), m); }
    public boolean remove(String name)  { return arenas.remove(name.toLowerCase(Locale.ROOT)) != null; }

    // 진행중 세션
    public static class Pending {
        public final String name;
        public String display;
        public ArenaMeta.Point spawn1, spawn2, center;
        public Pending(String name) { this.name = name; }
    }
    private final Map<UUID, Pending> pending = new HashMap<>();

    public void beginCreate(UUID uid, String name, String displayName) {
        Pending pend = new Pending(name.toLowerCase(Locale.ROOT));
        pend.display = displayName;

        pending.put(uid, pend);
    }
    public void cancel(UUID uid) { pending.remove(uid); }
    public Pending getPending(UUID uid) { return pending.get(uid); }

    // 현재 플레이어 위치를 WE 선택 최소점 기준 상대좌표로 마크
    public boolean mark(UUID uid, org.bukkit.entity.Player p, String which) {
        Pending pend = pending.get(uid);
        if (pend == null) return false;

        try {
            var sel = WeHelper.requireSelection(p);
            var min = sel.getMinimumPoint();
            int dx = p.getLocation().getBlockX() - min.x();
            int dy = p.getLocation().getBlockY() - min.y();
            int dz = p.getLocation().getBlockZ() - min.z();
            float yaw =  snapToCardinal(p.getLocation().getYaw());
            float pitch = 0;
            ArenaMeta.Point pt = new ArenaMeta.Point(dx, dy, dz, yaw, pitch);

            switch (which.toLowerCase(Locale.ROOT)) {
                case "spawn1" -> pend.spawn1 = pt;
                case "spawn2" -> pend.spawn2 = pt;
                case "center" -> pend.center = pt;
                default -> { return false; }
            }
            return true;
        } catch (Exception e) {
            p.sendMessage("§cWorldEdit 선택이 필요합니다. //wand 로 pos1, pos2 지정");
            return false;
        }
    }

    public int snapToCardinal(float yaw) {
        float a = yaw % 360f;
        if (a < 0) a += 360f;                    // 0~360으로 정규화
        return ((int)Math.floor((a + 45f) / 90f) % 4) * 90; // 0,90,180,270
    }

    // WE 선택 → .schem 저장 + arenas.yml 메타 등록
    public boolean register(UUID uid, org.bukkit.entity.Player p, String type) {
        Pending pend = pending.get(uid);
        if (pend == null) return false;

        List<String> missing = new ArrayList<>();
        if (pend.spawn1 == null) missing.add("spawn1");
        if (pend.spawn2 == null) missing.add("spawn2");
        if (pend.center == null) missing.add("center");
        if (!missing.isEmpty()) {
            p.sendMessage("§c등록 실패: 마크되지 않은 포인트가 있습니다 → §e" + String.join(", ", missing));
            p.sendMessage("§7/pvpworld arena mark <spawn1|spawn2|center> 로 모두 지정하세요.");
            return false;
        }

        try {
            // 1) 선택 영역/클립보드
            com.sk89q.worldedit.regions.Region sel = WeHelper.requireSelection(p);
            var cb = WeHelper.copySelectionToClipboard(p);

            // 2) 크기는 Region에서 얻기 (deprecated 아님)
            int sizeX = sel.getWidth();
            int sizeY = sel.getHeight();
            int sizeZ = sel.getLength();

            // 3) .schem 저장 + 메타 기록
            java.io.File out = arenasFile.schemFile(pend.name);
            Bukkit.getLogger().info("Saving schem to: " + out.getAbsolutePath());
            WeHelper.writeSchem(cb, out);

            String display = (pend.display == null ? pend.name : pend.display);
            ArenaMeta meta = new ArenaMeta(
                    pend.name, display, type,
                    sizeX, sizeY, sizeZ,
                    pend.spawn1, pend.spawn2, pend.center,
                    out.getName()
            );
            put(meta);
            arenasFile.save();
            pending.remove(uid);



            p.sendMessage("§6name: " + pend.name +"\ndisplay: " + pend.display + "\ntype: " + type);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            p.sendMessage("§c등록 실패: " + e.getMessage());
            return false;
        }
    }


    public boolean pasteHere(org.bukkit.entity.Player p, String name) {
        ArenaMeta meta = get(name);
        if (meta == null) return false;
        try {
            var cb = WeHelper.readSchem(arenasFile.schemFile(meta.schem()));
            WeHelper.paste(cb, p.getWorld(), p.getLocation().getBlock().getLocation(), true);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            p.sendMessage("§c붙여넣기 실패: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(String name) {
        ArenaMeta m = get(name);
        if (m == null) return false;
        boolean ok = remove(name);
        if (ok) {
            arenasFile.save();
            try { arenasFile.schemFile(m.schem()).delete(); } catch (Exception ignored) {}
        }
        return ok;
    }

}

