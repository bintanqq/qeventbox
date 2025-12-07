package me.bintanq.qEventBox.utility;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.inventory.meta.SkullMeta;
import java.lang.reflect.Field;
import java.util.UUID;

public class SkullUtility {

    private static Field profileField;

    // Method utama untuk mengatur tekstur Base64
    public static void setSkullTexture(SkullMeta meta, String textureValue) throws Exception {
        if (textureValue == null || textureValue.isEmpty()) return;

        // PERBAIKAN: Memberikan nama dummy ("dummy") agar tidak terjadi error "Profile name must not be null"
        GameProfile profile = new GameProfile(UUID.randomUUID(), "QEventBoxGUI");

        // 2. Tambahkan property "textures" (Base64)
        profile.getProperties().put("textures", new Property("textures", textureValue));

        // 3. Set GameProfile ke SkullMeta menggunakan Reflection
        if (profileField == null) {
            // Dapatkan Field 'profile' secara reflektif (hanya sekali)
            profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
        }

        // Set profile ke meta
        profileField.set(meta, profile);
    }
}