package ovh.pdx.qmine.compat;


import net.minecraft.resources.ResourceLocation;

public final class IdCompat {
    private IdCompat() {
    }

    
    
    public static ResourceLocation of(String path) {
        return new ResourceLocation("qmine", path);
    }

    public static ResourceLocation of(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
