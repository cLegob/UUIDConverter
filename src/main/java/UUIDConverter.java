import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.json.JSONObject;

public class UUIDConverter {
    private static String input = "2b8b603b-4530-4aa5-8e49-c753e0d08c04";

    // Convert int[4] to UUID
    public static UUID fromIntArray(int[] intArray) {
        if (intArray == null || intArray.length != 4) {
            throw new IllegalArgumentException("Input must be an int array of length 4.");
        }

        ByteBuffer buffer = ByteBuffer.allocate(16);
        for (int value : intArray) {
            buffer.putInt(value);
        }

        buffer.flip();
        long mostSigBits = buffer.getLong();
        long leastSigBits = buffer.getLong();

        return new UUID(mostSigBits, leastSigBits);
    }

    // Convert UUID to int[4]
    public static int[] toIntArray(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        buffer.flip();
        int[] intArray = new int[4];
        for (int i = 0; i < 4; i++) {
            intArray[i] = buffer.getInt();
        }

        return intArray;
    }

    // Helper to print int[] as [I; x, y, z, w]
    private static String formatIntArray(int[] intArray) {
        StringBuilder sb = new StringBuilder("[I; ");
        for (int i = 0; i < intArray.length; i++) {
            sb.append(intArray[i]);
            if (i < intArray.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    // Fetch Minecraft username from UUID
    private static String getMinecraftName(UUID uuid) {
        try {
            String uuidStr = uuid.toString().replace("-", "");
            URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + uuidStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != 200) {
                return "Failed to fetch name (HTTP " + status + ")";
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            return json.getString("username");

        } catch (Exception e) {
            return "Error fetching name: " + e.getMessage();
        }
    }

    // Fetch UUID from Minecraft username using Ashcon API
    private static UUID getUUIDFromUsername(String username) {
        try {
            URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + username);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != 200) {
                System.err.println("Failed to fetch UUID for username '" + username + "' (HTTP " + status + ")");
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            // Get UUID string directly, as per current API format
            String uuidStr = json.getString("uuid");
            return UUID.fromString(uuidStr);

        } catch (Exception e) {
            System.err.println("Error fetching UUID from username: " + e.getMessage());
            return null;
        }
    }


    public static void main(String[] args) {
        input = input.trim();

        try {
            UUID uuid = null;
            int[] intArray = null;

            if (input.startsWith("[I;")) {
                // input is int array string
                String inside = input.substring(3, input.length() - 1).trim();
                String[] parts = inside.split(",");
                if (parts.length != 4) throw new IllegalArgumentException();

                intArray = new int[4];
                for (int i = 0; i < 4; i++) {
                    intArray[i] = Integer.parseInt(parts[i].trim());
                }

                uuid = fromIntArray(intArray);

            } else if (input.matches("[0-9a-fA-F\\-]{36}")) {
                // input looks like a UUID string with dashes
                uuid = UUID.fromString(input);

            } else {
                // Assume input is a username, fetch UUID from API
                uuid = getUUIDFromUsername(input);
                if (uuid == null) {
                    System.err.println("Could not resolve UUID for username: " + input);
                    return;
                }
            }

            // If we have UUID now, convert and print everything
            if (uuid != null) {
                System.out.println("Minecraft Username: " + getMinecraftName(uuid));
                System.out.println("UUID: " + uuid);
                intArray = toIntArray(uuid);
                System.out.println("int[]: " + formatIntArray(intArray));
            }

        } catch (Exception e) {
            System.err.println("Error processing input: " + e.getMessage());
        }
    }
}
