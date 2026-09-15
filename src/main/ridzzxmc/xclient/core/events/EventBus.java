package ridzzxmc.xclient.core.event;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight annotation-driven event bus.
 *
 * Any object can register itself with {@link #subscribe(Object)}, and any
 * method annotated with {@link SubscribeEvent} taking a single {@link Event}
 * subclass parameter will be invoked whenever that event type is posted.
 *
 * This decouples modules from Fabric API's callback interfaces - internally
 * {@link ridzzxmc.xclient.core.XClientBridge} translates Fabric API callbacks
 * (tick, render, packet, keyboard) into calls to {@link #post(Event)}.
 */
public final class EventBus {

    public static final EventBus INSTANCE = new EventBus();

    private final Map<Class<? extends Event>, List<Listener>> listeners = new HashMap<>();
    private final List<Object> subscribers = new CopyOnWriteArrayList<>();

    private EventBus() {}

    public void subscribe(Object subscriber) {
        if (subscribers.contains(subscriber)) return;
        subscribers.add(subscriber);

        Class<?> type = subscriber.getClass();
        while (type != null && type != Object.class) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(SubscribeEvent.class)) continue;
                if (method.getParameterCount() != 1) continue;

                Class<?> paramType = method.getParameterTypes()[0];
                if (!Event.class.isAssignableFrom(paramType)) continue;

                method.setAccessible(true);
                @SuppressWarnings("unchecked")
                Class<? extends Event> eventType = (Class<? extends Event>) paramType;

                listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                        .add(new Listener(subscriber, method));
            }
            type = type.getSuperclass();
        }

        // keep priority order stable (highest first)
        listeners.values().forEach(list -> list.sort(Comparator.comparingInt(l -> -l.priority())));
    }

    public void unsubscribe(Object subscriber) {
        subscribers.remove(subscriber);
        listeners.values().forEach(list -> list.removeIf(l -> l.instance == subscriber));
    }

    public <T extends Event> T post(T event) {
        List<Listener> list = listeners.get(event.getClass());
        if (list == null) return event;

        for (Listener listener : list) {
            try {
                listener.method.invoke(listener.instance, event);
            } catch (Exception e) {
                System.err.println("[X Client] EventBus dispatch failed for " + listener.instance.getClass().getSimpleName());
                e.printStackTrace();
            }
        }
        return event;
    }

    private record Listener(Object instance, Method method) {
        int priority() {
            return method.getAnnotation(SubscribeEvent.class).priority();
        }
    }
}
