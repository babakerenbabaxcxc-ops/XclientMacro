package ridzzxmc.xclient.core.event.events;

import net.minecraft.network.packet.Packet;
import ridzzxmc.xclient.core.event.Event;

/**
 * Fired for every inbound/outbound packet. Purely observational in this
 * template (e.g. ping calculation, chat timestamping, server info) - no
 * modules ship that fabricate or suppress packets to gain gameplay advantage.
 */
public class PacketEvent extends Event {

    public enum Direction { SEND, RECEIVE }

    private final Packet<?> packet;
    private final Direction direction;

    public PacketEvent(Packet<?> packet, Direction direction) {
        this.packet = packet;
        this.direction = direction;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public Direction getDirection() {
        return direction;
    }
}
