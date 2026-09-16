package ridzzxmc.xclient.util;

import net.minecraft.client.option.SimpleOption;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Helpers for writing to vanilla {@link SimpleOption} values (gamma, FOV,
 * ...) from a module, used by Fullbright and Zoom.
 * <p>
 * Two problems come up when doing this from outside vanilla's own options
 * screen:
 * <ol>
 *   <li>{@code SimpleOption<T>} is generic - FOV is {@code SimpleOption<Integer>},
 *   gamma is {@code SimpleOption<Double>}, and so on - but that {@code T} is
 *   erased at runtime, so a {@code SimpleOption<?>} reference can safely be
 *   treated as {@code SimpleOption<Object>} at the bytecode level. That lets
 *   {@link #setClamped} take a plain {@code double} without the caller having
 *   to know or guess which boxed type a specific option actually stores.</li>
 *   <li>{@code setValue()} itself runs the option's own validator, which for
 *   some options (gamma in particular) clamps to a narrow vanilla range (e.g.
 *   [0.0, 1.0]) - fine for the options screen, but it defeats an effect that
 *   needs to go further than the slider ever could (vanilla's own maximum
 *   "Bright" gamma of 1.0 still leaves caves visibly dim; a genuine fullbright
 *   effect needs a much larger value). {@link #setUnclamped} writes straight
 *   past that validator via reflection on the option's backing field.</li>
 * </ol>
 * The backing field for problem 2 is located once by comparing each declared
 * field's current value against {@link SimpleOption#getValue()} rather than
 * by name, so a mapping/field-name change can only make this silently fall
 * back to the normal clamped path instead of crashing the game.
 */
public final class OptionAccess {

    private static volatile Field valueField;
    private static volatile boolean reflectionUnavailable;

    private OptionAccess() {}

    /** Boxes {@code rawValue} to match whatever type the option currently holds, then calls its own (clamping) setValue. */
    @SuppressWarnings("unchecked")
    public static void setClamped(SimpleOption<?> option, double rawValue) {
        Object boxed = box(option, rawValue);
        ((SimpleOption<Object>) option).setValue(boxed);
    }

    /**
     * Always applies the normal (clamped) path first - so whatever internal
     * listener/side effect {@code SimpleOption#setValue} triggers on a real
     * change still fires - then additionally tries {@link #setUnclamped} to
     * push past that clamp. Doing the unclamped write ONLY (the previous
     * behavior) meant that if the reflective bypass "succeeded" (found and
     * set the field) but the value still had no visible effect for some
     * other version-specific reason, the guaranteed-safe clamped fallback
     * never ran at all, silently regressing to no effect whatsoever instead
     * of at least vanilla's own maximum.
     */
    public static void setBestEffort(SimpleOption<?> option, double rawValue) {
        setClamped(option, rawValue);
        setUnclamped(option, rawValue);
    }

    /** Writes straight past setValue()'s validator. Returns false (does nothing) if the backing field couldn't be found. */
    public static boolean setUnclamped(SimpleOption<?> option, double rawValue) {
        if (reflectionUnavailable) return false;
        try {
            Field field = valueField;
            if (field == null) {
                field = resolveValueField(option);
                if (field == null) {
                    reflectionUnavailable = true;
                    return false;
                }
                valueField = field;
            }
            field.set(option, box(option, rawValue));
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            reflectionUnavailable = true;
            System.err.println("[X Client] Could not bypass a vanilla option's value clamp - falling back to its normal (clamped) range:");
            e.printStackTrace();
            return false;
        }
    }

    private static Object box(SimpleOption<?> option, double rawValue) {
        Object current = option.getValue();
        return (current instanceof Integer) ? (Object) (int) Math.round(rawValue) : (Object) rawValue;
    }

    private static Field resolveValueField(SimpleOption<?> option) {
        Object expected = option.getValue();

        Field named = tryField(option.getClass(), "value");
        if (named != null && matches(named, option, expected)) return named;

        // Walk the full class hierarchy, not just the option's exact runtime
        // class - if SimpleOption's backing field is declared on a superclass
        // (or the instance is some subclass), getDeclaredFields() on the leaf
        // class alone would silently miss it and this whole bypass would look
        // "unavailable" forever even though a matching field really exists.
        for (Class<?> type = option.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field candidate : type.getDeclaredFields()) {
                candidate.setAccessible(true);
                if (matches(candidate, option, expected)) return candidate;
            }
        }
        return null;
    }

    private static boolean matches(Field field, Object instance, Object expected) {
        try {
            return Objects.equals(field.get(instance), expected);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static Field tryField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
