package models.skill;

 


import java.util.ArrayList;
import java.util.List;
import models.player.Player;
import models.mob.Mob;
import services.Service;
import network.io.Message;

public class PlayerSkill {

    private Player player;
    public List<Skill> skills;
    public Skill skillSelect;

    public PlayerSkill(Player player) {
        this.player = player;
        skills = new ArrayList<>();
    }

    public Skill getSkillbyId(int id) {
        for (Skill skill : skills) {
            if (skill.template.id == id) {
                return skill;
            }
        }
        for (Skill skill : skills) {
            if (skill.skillId == id) {
                return skill;
            }
        }
        return null;
    }

    public byte[] skillShortCut = new byte[10];

    public void sendSkillShortCut() {
        Message msg;
        try {
            msg = Service.gI().messageSubCommand((byte) 61);
            msg.writer().writeUTF("KSkill");
            msg.writer().writeInt(skillShortCut.length);
            msg.writer().write(skillShortCut);
            player.sendMessage(msg);
            msg.cleanup();
            msg = Service.gI().messageSubCommand((byte) 61);
            msg.writer().writeUTF("OSkill");
            msg.writer().writeInt(skillShortCut.length);
            msg.writer().write(skillShortCut);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public boolean prepareQCKK;
    public boolean prepareTuSat;
    public boolean prepareLaze;

    public long lastTimePrepareQCKK;
    public long lastTimePrepareTuSat;
    public long lastTimePrepareLaze;

    public Player targetPlayerLaze;
    public Mob targetMobLaze;

    public byte getIndexSkillSelect() {
        switch (skillSelect.template.id) {
            case Skill.DRAGON:
            case Skill.DEMON:
            case Skill.GALICK:
            case Skill.KAIOKEN:
            case Skill.LIEN_HOAN:
                return 1;
            case Skill.KAMEJOKO:
            case Skill.ANTOMIC:
            case Skill.MASENKO:
                return 2;
            case Skill.LIEN_HOAN_CHUONG:
            case Skill.SUPER_KAME:
            case Skill.MA_PHONG_BA:
                return 4;
            default:
                return 3;
        }
    }

    public byte getSizeSkill() {
        byte size = 0;
        for (Skill skill : skills) {
            if (skill.skillId != -1) {
                if (!Skill.ENABLE_SKILL_BIEN_HINH && skill.template != null && skill.template.id == Skill.BIEN_HINH_GOKU) {
                    continue;
                }
                size++;
            }
        }
        return size;
    }

    public void dispose() {
        if (this.skillSelect != null) {
            this.skillSelect.dispose();
        }
        if (this.skills != null) {
            for (Skill skill : this.skills) {
                skill.dispose();
            }
            this.skills.clear();
        }
        this.player = null;
        this.skillSelect = null;
        this.skills = null;
        this.targetPlayerLaze = null;
        this.targetMobLaze = null;
    }
}
