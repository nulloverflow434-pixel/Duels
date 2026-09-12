package me.realized.duels.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

public final class EnumUtil {
    private EnumUtil() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E> E getByName(final String name, Class<E> clazz) {
        if (name == null || clazz == null) return null;
        if (clazz.isEnum()) {
            return clazz.cast(Arrays.stream(clazz.getEnumConstants())
                .filter(type -> ((Enum) type).name().equalsIgnoreCase(name))
                .findFirst().orElse(null));
        }
        try {
            Method valueOf = clazz.getMethod("valueOf", String.class);
            return clazz.cast(valueOf.invoke(null, name.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_')));
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {}
        try {
            Class<?> registryClass = Class.forName("org.bukkit.Registry");
            Object registry = null;
            if (clazz.getName().contains("Sound")) registry = registryClass.getField("SOUNDS").get(null);
            else if (clazz.getName().contains("Attribute")) registry = registryClass.getField("ATTRIBUTE").get(null);
            if (registry != null) {
                Class<?> namespacedKey = Class.forName("org.bukkit.NamespacedKey");
                String keyName = name.toLowerCase(Locale.ROOT);
                Method get = registry.getClass().getMethod("get", namespacedKey);
                Object key = namespacedKey.getMethod("minecraft", String.class).invoke(null, keyName);
                Object result = get.invoke(registry, key);
                if (result != null) return clazz.cast(result);
                if (keyName.startsWith("generic_")) {
                    key = namespacedKey.getMethod("minecraft", String.class).invoke(null, keyName.substring(8));
                    result = get.invoke(registry, key);
                    if (result != null) return clazz.cast(result);
                }
            }
        } catch (ReflectiveOperationException ignored) {}
        return null;
    }
}
