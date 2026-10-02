package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.Fordix;
import com.omneria.fordix.client.gui.layout.HudSnap;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * JourneyMap's minimap, moved through JourneyMap's own settings: its "Custom" position stores the center of the map
 * as a fraction of the screen. JourneyMap draws it from its own render events (not as a Forge overlay), so it
 * can't be wrapped like the others. Everything goes through reflection, JourneyMap being an optional mod; any failure
 * simply means the element isn't offered.
 */
public final class JourneyMapHud {
    public static final String ID = "journeymap";
    private static final int MARGIN = 4;

    private JourneyMapHud() {
    }

    public static boolean available() {
        return ModList.get().isLoaded("journeymap") && properties() != null;
    }

    /** The settings the live minimap reads (its current preset), falling back to the active preset's. */
    private static Object properties() {
        try {
            Object map = minimap();
            Object current = map == null ? null : map.getClass().getMethod("getCurrentMinimapProperties").invoke(map);
            if (current != null) {
                return current;
            }
            Object client = Class.forName("journeymap.client.JourneymapClient").getMethod("getInstance").invoke(null);
            return client == null ? null : client.getClass().getMethod("getActiveMiniMapProperties").invoke(client);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return null;
        }
    }

    private static Object minimap() throws ReflectiveOperationException {
        Class<?> manager = Class.forName("journeymap.client.ui.UIManager");
        Object instance = manager.getField("INSTANCE").get(null);
        return manager.getMethod("getMiniMap").invoke(instance);
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        return owner.getClass().getField(name).get(owner);
    }

    private static Object value(Object config) throws ReflectiveOperationException {
        return config.getClass().getMethod("get").invoke(config);
    }

    private static void set(Object config, Object value) throws ReflectiveOperationException {
        for (Method method : config.getClass().getMethods()) {
            if (method.getName().equals("set") && method.getParameterCount() == 1
                    && method.getParameterTypes()[0] == Object.class) {
                method.invoke(config, value);
                return;
            }
        }
        throw new NoSuchMethodException("set");
    }

    /** The map's box in scaled GUI pixels, whatever its position setting. */
    public static HudSnap.Rect rect(int width, int height) {
        try {
            Object props = properties();
            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            int w = 100;
            int h = 100;
            Object map = minimap();
            Object displayVars = map.getClass().getMethod("getDisplayVars").invoke(map);
            if (displayVars != null) {
                w = displayVars.getClass().getField("minimapWidth").getInt(displayVars);
                h = displayVars.getClass().getField("minimapHeight").getInt(displayVars);
            }
            w = Math.max(8, (int) Math.round(w / scale));
            h = Math.max(8, (int) Math.round(h / scale));
            String position = ((Enum<?>) value(field(props, "position"))).name();
            float px = (Float) value(field(props, "positionX"));
            float py = (Float) value(field(props, "positionY"));
            return switch (position) {
                case "Custom" -> new HudSnap.Rect(Math.round(px * width - w / 2F), Math.round(py * height - h / 2F), w, h);
                case "TopRight" -> new HudSnap.Rect(width - w - MARGIN, MARGIN, w, h);
                case "BottomRight" -> new HudSnap.Rect(width - w - MARGIN, height - h - MARGIN, w, h);
                case "BottomLeft" -> new HudSnap.Rect(MARGIN, height - h - MARGIN, w, h);
                case "TopLeft" -> new HudSnap.Rect(MARGIN, MARGIN, w, h);
                case "TopCenter" -> new HudSnap.Rect((width - w) / 2, MARGIN, w, h);
                default -> new HudSnap.Rect((width - w) / 2, (height - h) / 2, w, h);
            };
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return new HudSnap.Rect(width - 104, MARGIN, 100, 100);
        }
    }

    /** Puts the map's top-left corner at the rectangle's, switching JourneyMap to its Custom position. */
    public static void moveTo(HudSnap.Rect rect, int width, int height) {
        try {
            Object props = properties();
            Object position = field(props, "position");
            Class<?> enumClass = (Class<?>) position.getClass().getMethod("getEnumClass").invoke(position);
            set(position, enumValue(enumClass, "Custom"));
            set(field(props, "positionX"), (rect.x() + rect.w() / 2F) / width);
            set(field(props, "positionY"), (rect.y() + rect.h() / 2F) / height);
            refresh();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Fordix.LOGGER.warn("Could not move the JourneyMap minimap: {}", e.toString());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> enumClass, String name) {
        return Enum.valueOf((Class<? extends Enum>) enumClass, name);
    }

    public static void reset() {
        try {
            Object position = field(properties(), "position");
            Class<?> enumClass = (Class<?>) position.getClass().getMethod("getEnumClass").invoke(position);
            set(position, enumValue(enumClass, "TopRight"));
            setScale(1.0F);
            refresh();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Fordix.LOGGER.warn("Could not reset the JourneyMap minimap: {}", e.toString());
        }
    }

    /** Size relative to JourneyMap's default minimap size (its "size" setting is a percentage of the screen). */
    public static float scale() {
        try {
            Object size = field(properties(), "sizePercent");
            int now = (Integer) value(size);
            int usual = (Integer) size.getClass().getMethod("getDefaultValue").invoke(size);
            return usual > 0 ? now / (float) usual : 1.0F;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return 1.0F;
        }
    }

    public static void setScale(float scale) {
        try {
            Object size = field(properties(), "sizePercent");
            int usual = (Integer) size.getClass().getMethod("getDefaultValue").invoke(size);
            int min = (Integer) size.getClass().getMethod("getMinValue").invoke(size);
            int max = (Integer) size.getClass().getMethod("getMaxValue").invoke(size);
            set(size, Math.max(min, Math.min(max, Math.round(usual * scale))));
            refresh();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Fordix.LOGGER.warn("Could not resize the JourneyMap minimap: {}", e.toString());
        }
    }

    public static boolean hidden() {
        try {
            return !(Boolean) value(field(properties(), "enabled"));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return false;
        }
    }

    public static void setHidden(boolean hidden) {
        try {
            set(field(properties(), "enabled"), !hidden);
            refresh();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Fordix.LOGGER.warn("Could not toggle the JourneyMap minimap: {}", e.toString());
        }
    }

    /** Applies the change to the live map; JourneyMap writes its own file on {@link #save()}. */
    private static void refresh() throws ReflectiveOperationException {
        Object map = minimap();
        try {
            map.getClass().getMethod("updateDisplayVars", boolean.class).invoke(map, true);
        } catch (NoSuchMethodException ignored) {
            // older layout: the next frame picks the property up anyway
        }
    }

    public static void save() {
        try {
            Object props = properties();
            props.getClass().getMethod("save").invoke(props);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Fordix.LOGGER.warn("Could not save JourneyMap's settings: {}", e.toString());
        }
    }
}
