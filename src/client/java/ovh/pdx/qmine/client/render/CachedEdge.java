package ovh.pdx.qmine.client.render;

public record CachedEdge(float x1, float y1, float z1, float x2, float y2, float z2) {
    public static CachedEdge fromDoubles(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new CachedEdge((float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2);
    }

    public float normalX() {
        return x2 - x1;
    }

    public float normalY() {
        return y2 - y1;
    }

    public float normalZ() {
        return z2 - z1;
    }
}
