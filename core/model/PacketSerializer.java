package core.model;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Efficient binary serialization/deserialization for Packet objects.
 * Uses custom compact binary format for minimal bandwidth usage.
 *
 * Binary Format (Version 1):
 * ┌──────────────────────────────────────┐
 * │ FORMAT VERSION (1 byte)              │ 0x01
 * ├──────────────────────────────────────┤
 * │ PACKET ID LENGTH (2 bytes, BE)       │
 * │ PACKET ID (UTF-8 bytes)              │
 * ├──────────────────────────────────────┤
 * │ SOURCE ID LENGTH (2 bytes, BE)       │
 * │ SOURCE ID (UTF-8 bytes)              │
 * ├──────────────────────────────────────┤
 * │ HAS DESTINATION (1 byte: 0/1)        │
 * │ [IF HAS DEST] DEST LENGTH (2 bytes)  │
 * │ [IF HAS DEST] DESTINATION ID (UTF-8) │
 * ├──────────────────────────────────────┤
 * │ TTL (1 byte)                         │
 * ├──────────────────────────────────────┤
 * │ TIMESTAMP (8 bytes, BE)              │
 * ├──────────────────────────────────────┤
 * │ PAYLOAD LENGTH (4 bytes, BE)         │
 * │ PAYLOAD (variable bytes)             │
 * └──────────────────────────────────────┘
 */
public class PacketSerializer {
    private static final byte FORMAT_VERSION = 0x01;
    private static final int MAX_STRING_LENGTH = 1024; // 1KB max for IDs
    private static final int MAX_PAYLOAD_SIZE = 1024 * 1024; // 1MB max payload

    /**
     * Serializes a Packet to a byte array.
     *
     * @param packet the packet to serialize
     * @return byte array representing the packet
     * @throws SerializationException if serialization fails
     */
    public static byte[] serialize(Packet packet) throws SerializationException {
        if (packet == null) {
            throw new SerializationException("Packet cannot be null");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            // Write format version
            dos.writeByte(FORMAT_VERSION);

            // Write packet ID
            writeString(dos, packet.getPacketId(), "Packet ID");

            // Write source ID
            writeString(dos, packet.getSourceId(), "Source ID");

            // Write destination ID (can be null for broadcast)
            if (packet.getDestinationId() != null) {
                dos.writeByte(0x01); // Has destination
                writeString(dos, packet.getDestinationId(), "Destination ID");
            } else {
                dos.writeByte(0x00); // No destination (broadcast)
            }

            // Write TTL
            dos.writeByte(packet.getTtl());

            // Write timestamp
            dos.writeLong(packet.getTimestamp());

            // Write payload
            byte[] payload = packet.getPayload();
            if (payload == null) {
                dos.writeInt(0); // Empty payload
            } else {
                if (payload.length > MAX_PAYLOAD_SIZE) {
                    throw new SerializationException(
                            "Payload too large: " + payload.length + " bytes (max: " + MAX_PAYLOAD_SIZE + ")");
                }
                dos.writeInt(payload.length);
                dos.write(payload);
            }

            dos.flush();
            return baos.toByteArray();

        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize packet: " + e.getMessage(), e);
        }
    }

    /**
     * Deserializes a byte array to a Packet.
     *
     * @param data the byte array containing serialized packet
     * @return deserialized Packet
     * @throws SerializationException if deserialization fails
     */
    public static Packet deserialize(byte[] data) throws SerializationException {
        if (data == null || data.length == 0) {
            throw new SerializationException("Cannot deserialize: data is null or empty");
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {

            // Read and validate format version
            byte version = dis.readByte();
            if (version != FORMAT_VERSION) {
                throw new SerializationException(
                        "Unsupported packet format version: " + version + " (expected: " + FORMAT_VERSION + ")");
            }

            // Read packet ID
            String packetId = readString(dis, "Packet ID");

            // Read source ID
            String sourceId = readString(dis, "Source ID");

            // Read destination ID (may be null for broadcast)
            byte hasDestination = dis.readByte();
            String destinationId = null;
            if (hasDestination == 0x01) {
                destinationId = readString(dis, "Destination ID");
            }

            // Read TTL
            int ttl = dis.readUnsignedByte();

            // Read timestamp
            long timestamp = dis.readLong();

            // Read payload
            int payloadLength = dis.readInt();
            if (payloadLength < 0) {
                throw new SerializationException("Invalid payload length: " + payloadLength);
            }

            byte[] payload = null;
            if (payloadLength > 0) {
                if (payloadLength > MAX_PAYLOAD_SIZE) {
                    throw new SerializationException(
                            "Payload too large: " + payloadLength + " bytes (max: " + MAX_PAYLOAD_SIZE + ")");
                }
                payload = new byte[payloadLength];
                dis.readFully(payload);
            }

            return new Packet(packetId, sourceId, destinationId, ttl, timestamp, payload);

        } catch (SerializationException e) {
            throw e;
        } catch (EOFException e) {
            throw new SerializationException("Incomplete packet data: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize packet: " + e.getMessage(), e);
        }
    }

    /**
     * Writes a string to the data stream with length prefix.
     *
     * @param dos the DataOutputStream
     * @param str the string to write
     * @param fieldName the name of the field (for error messages)
     * @throws SerializationException if string is invalid
     * @throws IOException if write fails
     */
    private static void writeString(DataOutputStream dos, String str, String fieldName)
            throws SerializationException, IOException {
        if (str == null || str.isEmpty()) {
            throw new SerializationException(fieldName + " cannot be null or empty");
        }

        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_LENGTH) {
            throw new SerializationException(
                    fieldName + " too long: " + bytes.length + " bytes (max: " + MAX_STRING_LENGTH + ")");
        }

        dos.writeShort(bytes.length); // 2-byte length prefix
        dos.write(bytes);
    }

    /**
     * Reads a string from the data stream with length prefix.
     *
     * @param dis the DataInputStream
     * @param fieldName the name of the field (for error messages)
     * @return the deserialized string
     * @throws SerializationException if string is invalid
     * @throws IOException if read fails
     */
    private static String readString(DataInputStream dis, String fieldName)
            throws SerializationException, IOException {
        short length = dis.readShort();

        if (length <= 0) {
            throw new SerializationException(fieldName + " has invalid length: " + length);
        }

        if (length > MAX_STRING_LENGTH) {
            throw new SerializationException(
                    fieldName + " too long: " + length + " bytes (max: " + MAX_STRING_LENGTH + ")");
        }

        byte[] bytes = new byte[length];
        dis.readFully(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Calculates the approximate size of a serialized packet without actually serializing.
     * Useful for checking before transmission.
     *
     * @param packet the packet to measure
     * @return approximate byte size
     */
    public static int estimateSize(Packet packet) {
        if (packet == null) return 0;

        int size = 1; // Format version

        // Packet ID: 2 bytes length + UTF-8 bytes
        size += 2 + packet.getPacketId().getBytes(StandardCharsets.UTF_8).length;

        // Source ID: 2 bytes length + UTF-8 bytes
        size += 2 + packet.getSourceId().getBytes(StandardCharsets.UTF_8).length;

        // Destination ID: 1 byte flag + conditional 2 bytes length + UTF-8 bytes
        size += 1;
        if (packet.getDestinationId() != null) {
            size += 2 + packet.getDestinationId().getBytes(StandardCharsets.UTF_8).length;
        }

        // TTL: 1 byte
        size += 1;

        // Timestamp: 8 bytes
        size += 8;

        // Payload: 4 bytes length + payload data
        size += 4;
        if (packet.getPayload() != null) {
            size += packet.getPayload().length;
        }

        return size;
    }
}
