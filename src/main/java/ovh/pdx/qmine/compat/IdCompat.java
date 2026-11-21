package ovh.pdx.qmine.compat;


import net.minecraft.resources.ResourceLocation;

public final class IdCompat {
    private IdCompat() {
    }

    
    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath("qmine", path);
    }

    public static ResourceLocation of(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
    
}
