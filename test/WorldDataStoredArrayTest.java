package server.web.panel;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.simple.JSONValue;

/** Regression for old map 136/169 rows, modern arrays and legacy waypoint names. */
public final class WorldDataStoredArrayTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        same("[]", false, "[]");
        same("[]", true, "[]");
        same("[[36,8,36252,324,192],[71,0,40732000,949,576]]", false,
                "[[36,8,36252,324,192],[71,0,40732000,949,576]]");
        same("[\"[36,8,36252,324,192]\",\"[71,0,40732000,949,576]\"]", false,
                "[[36,8,36252,324,192],[71,0,40732000,949,576]]");
        // Map 169: several mob tuples wrapped in one string; outer JSON is valid.
        same("[\"[66,1,5000000,96,336],[67,1,5000000,340,336],[78,1,6000000,601,216],"
                + "[79,1,6000000,725,144],[78,1,6000000,902,216],[66,1,6000000,279,96]\"]", false,
                "[[66,1,5000000,96,336],[67,1,5000000,340,336],[78,1,6000000,601,216],"
                + "[79,1,6000000,725,144],[78,1,6000000,902,216],[66,1,6000000,279,96]]");
        // Map 136: separate strings followed by a string containing two tuples and a newline.
        same("[\"[36,8,36252,324,192]\",\"[37,8,36252,1308,216],\r\n[71,0,40732000,949,576]\"]", false,
                "[[36,8,36252,324,192],[37,8,36252,1308,216],[71,0,40732000,949,576]]");
        same("[\"[74,900,408,10477]\"]", false, "[[74,900,408,10477]]");

        String waypoint = "[\"Đảo Kamê\",1608,312,1632,336,0,0,5,60,288]";
        same("[" + waypoint + "]", true, "[" + waypoint + "]");
        same("[\"" + waypoint + "\"]", true, "[" + waypoint + "]");
        String nameWithQuotes = "[[\"Cổng \\\"bí mật\\\", phía Đông\",1,2,3,4,0,0,5,6,7]]";
        same(nameWithQuotes, true, nameWithQuotes);
        String nameWithDelimiter = "[[\"Cổng ],[ không tách tên\",1,2,3,4,0,0,5,6,7]]";
        same(nameWithDelimiter, true, nameWithDelimiter);
        same("[\"[0,1,2,3,4]\",[5,6,7,8,9]]", false, "[[0,1,2,3,4],[5,6,7,8,9]]");

        for (String invalid : new String[]{null, "", " ", "null", "{}", "[null]", "[1,2,3]",
                "[true]", "[\"not an array\"]", "[[1,2],{}]", "[[1,2],null]", "[[1,2],3]", "[[1,2]"}) {
            for (boolean waypoints : new boolean[]{false, true}) {
                try {
                    WorldData.storedArray(invalid, waypoints);
                    throw new AssertionError("Must reject malformed data without silently skipping rows: " + invalid);
                } catch (PanelSupport.PanelException error) {
                    check(error.status == 409, "Malformed stored data remains a conflict");
                }
            }
        }
        if (args.length > 0) verifyMapSnapshot(Path.of(args[0]));
        System.out.println("PASS: " + checks + " stored-array compatibility and map snapshot checks");
    }

    private static void verifyMapSnapshot(Path file) throws Exception {
        JsonArray maps = new JsonParser().parse(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonArray();
        int spawns = 0;
        for (JsonElement entry : maps) {
            JsonObject map = entry.getAsJsonObject();
            for (String field : new String[]{"mobs", "npcs", "waypoints"}) {
                String raw = map.get(field).getAsString();
                JsonArray actual = WorldData.storedArray(raw, field.equals("waypoints"));
                String normalized = field.equals("waypoints")
                        ? raw.replace("[\"[", "[[").replace("]\"]", "]]").replace("\",\"", ",")
                        : raw.replace("\"", "");
                Object loaded = JSONValue.parse(normalized);
                check(loaded instanceof org.json.simple.JSONArray, "Game loader accepts map " + map.get("id") + "/" + field);
                JsonElement expected = new JsonParser().parse(loaded.toString());
                check(actual.equals(expected), "Panel preserves all tuples used by game loader: " + map.get("id") + "/" + field);
                if (field.equals("mobs")) spawns += actual.size();
                if (field.equals("mobs") && map.get("id").getAsInt() == 136) check(actual.size() == 8, "Map 136 retains eight mobs");
                if (field.equals("mobs") && map.get("id").getAsInt() == 169) check(actual.size() == 6, "Map 169 retains six mobs");
            }
        }
        System.out.println("Validated " + maps.size() + " map definitions and " + spawns + " mob spawns against game loader.");
    }

    private static void same(String raw, boolean waypoints, String expected) {
        check(WorldData.storedArray(raw, waypoints).equals(new JsonParser().parse(expected)), "Decode all tuples and preserve values");
    }
    private static void check(boolean ok, String label) {
        checks++; if (!ok) throw new AssertionError(label);
    }
}
