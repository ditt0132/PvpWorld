package me.ujun.pvpWorld.arena;

import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public final class WeHelper {
    private WeHelper() {}

    // 항상 Bukkit Player를 받아서 내부에서 어댑트
    public static Region requireSelection(org.bukkit.entity.Player bukkit)
            throws IncompleteRegionException {
        com.sk89q.worldedit.entity.Player we = BukkitAdapter.adapt(bukkit);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(we);

        var weWorld = BukkitAdapter.adapt(bukkit.getWorld());
        return session.getSelection(weWorld);
    }

    public static Clipboard copySelectionToClipboard(org.bukkit.entity.Player bukkit)
            throws IncompleteRegionException, WorldEditException {
        Region region = requireSelection(bukkit);
        BlockVector3 min = region.getMinimumPoint();

        Clipboard cb = new BlockArrayClipboard(region);
        try (EditSession edit = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(bukkit.getWorld()))) {
            ForwardExtentCopy copy = new ForwardExtentCopy(edit, region, cb, min);
            copy.setCopyingEntities(true);
            Operations.complete(copy);        // ✅ 컴파일 OK (throws 선언)
        }
        return cb;
    }

    public static void writeSchem(Clipboard cb, File out) throws IOException {
        out.getParentFile().mkdirs();

        // 1) 포맷 자동 감지 (확장자로)
        ClipboardFormat fmt = ClipboardFormats.findByFile(out);

        // 2) 별칭 보조 (환경에 따라 alias가 다를 수 있음)
        if (fmt == null) {
            for (String alias : new String[]{"schem", "sponge", "sponge_schematic"}) {
                fmt = ClipboardFormats.findByAlias(alias);
                if (fmt != null) break;
            }
        }

        // 3) 정상 포맷 찾으면 그 포맷으로 저장
        if (fmt != null) {
            try (FileOutputStream fos = new FileOutputStream(out);
                 ClipboardWriter w = fmt.getWriter(fos)) {
                w.write(cb);
                return;
            }
        }

        // 4) 하드 폴백: SpongeSchematicWriter(NBTOutputStream 필요)로 직접 저장
        try (FileOutputStream fos = new FileOutputStream(out);
             GZIPOutputStream gz = new GZIPOutputStream(fos);
             NBTOutputStream nbt = new NBTOutputStream(gz)) {

            // 리플렉션으로 Sponge writer 호출 (존재하지 않으면 CNFE)
            Class<?> clazz = Class.forName("com.sk89q.worldedit.extent.clipboard.io.SpongeSchematicWriter");
            Object writer = clazz.getConstructor(NBTOutputStream.class).newInstance(nbt);
            clazz.getMethod("write", Clipboard.class).invoke(writer, cb);
            return;
        } catch (ClassNotFoundException e) {
            // Sponge writer 자체가 없는 정말 구환경
            throw new IOException(
                    "Sponge schematic writer not available in this runtime. " +
                            "Use a .schem-capable WorldEdit/FAWE or save with .schem extension.", e);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Failed to invoke SpongeSchematicWriter via reflection", e);
        }
    }

    public static Clipboard readSchem(File in) throws IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(in);
        if (format == null) throw new IOException("Unknown schematic format: " + in.getName());
        try (ClipboardReader reader = format.getReader(new FileInputStream(in))) {
            return reader.read();
        }
    }

    public static void paste(Clipboard cb, org.bukkit.World bWorld, Location origin, boolean ignoreAir) throws Exception {
        try (EditSession edit = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(bWorld))) {
            Operation op = new ClipboardHolder(cb)
                    .createPaste(edit)
                    .to(BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ()))
                    .ignoreAirBlocks(ignoreAir)
                    .build();
            Operations.complete(op);
        }
    }
}