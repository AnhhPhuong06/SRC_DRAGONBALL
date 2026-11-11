import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import models.player.Player;
import models.skill.Skill;
import network.io.Message;
import services.Service;

/** Verifies the actual cooldown packets without a database, server or live account. */
public final class NewPlayerSkillCooldownTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        CapturePlayer player = new CapturePlayer();
        // Level-one Dragon, Demon and Galick as assigned to new characters.
        for (int id : new int[]{0, 14, 28}) {
            Skill skill = skill(id, 500, 0);
            verify(player, skill, 0, 0, "Unused starter punch is ready immediately");
        }

        // This expired timestamp wraps to a positive int in the old calculation.
        Skill expired = skill(28, 500, System.currentTimeMillis() - 3_000_000_000L);
        verify(player, expired, 0, 0, "Old expired cooldown cannot wrap into weeks of waiting");
        verify(player, skill(28, 500, System.currentTimeMillis() - 1000),
                0, 0, "Recently expired cooldown is ready");
        verify(player, skill(28, 60_000, System.currentTimeMillis()),
                55_000, 60_000, "Active cooldown remains in force");

        // Exercise the multi-skill login packet with both ready and cooling skills.
        player.playerSkill.skills.clear();
        player.playerSkill.skills.add(skill(0, 500, 0));
        player.playerSkill.skills.add(expired);
        player.playerSkill.skills.add(skill(7, 60_000, System.currentTimeMillis()));
        Service.gI().sendTimeSkill(player);
        DataInputStream packet = player.packet();
        entry(packet, 0, 0, 0, "Login: unused punch");
        entry(packet, 28, 0, 0, "Login: long-expired punch");
        entry(packet, 7, 55_000, 60_000, "Login: active skill");
        check(packet.available() == 0, "Login packet contains exactly three entries");
        System.out.println("PASS: " + checks + " new-player, expired and active cooldown packet checks");
    }

    private static void verify(CapturePlayer player, Skill skill, int min, int max, String label)
            throws Exception {
        player.playerSkill.skills.clear();
        player.playerSkill.skills.add(skill);
        long savedTime = skill.lastTimeUseThisSkill;
        player.nPoint.mp = 17;
        for (boolean single : new boolean[]{false, true}) {
            if (single) {
                Service.gI().sendTimeSkill(player, skill);
            } else {
                Service.gI().sendTimeSkill(player);
            }
            DataInputStream packet = player.packet();
            entry(packet, skill.skillId, min, max, label);
            check(packet.available() == 0, label + ": no extra packet bytes");
            check(skill.lastTimeUseThisSkill == savedTime, label + ": preserve usage timestamp");
            check(player.nPoint.mp == 17, label + ": preserve mana");
        }
    }

    private static Skill skill(int id, int cooldown, long usedAt) {
        Skill skill = new Skill();
        skill.skillId = (short) id;
        skill.point = 1;
        skill.coolDown = cooldown;
        skill.lastTimeUseThisSkill = usedAt;
        return skill;
    }

    private static void entry(DataInputStream packet, int id, int min, int max, String label)
            throws Exception {
        check(packet.readShort() == id, label + ": correct skill ID");
        int actual = packet.readInt();
        check(actual >= min && actual <= max, label + ": remaining milliseconds " + actual);
    }

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) {
            throw new AssertionError(label);
        }
    }

    private static final class CapturePlayer extends Player {
        private byte command;
        private byte[] payload;

        @Override
        public void sendMessage(Message message) {
            command = message.command;
            payload = message.getData().clone();
        }

        DataInputStream packet() {
            check(command == -94 && payload != null, "Cooldown packet was sent");
            return new DataInputStream(new ByteArrayInputStream(payload));
        }
    }
}
