package me.ujun.pvpWorld.arena;

public record ArenaMeta(
        String name,
        String display,
        String type,
        int sizeX, int sizeY, int sizeZ,
        Point spawn1, Point spawn2, Point center,
        String schem
) {
    public record Point(int dx, int dy, int dz, float yaw, float pitch) {}
}
