package data;



import models.Template.HeadAvatar;
import models.Template.MapTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import utils.FileIO;
import services.Service;
import models.skill.NClass;
import models.skill.Skill;
import models.Template.MobTemplate;
import models.Template.NpcTemplate;
import models.Template.SkillTemplate;
import interfaces.ISession;
import network.io.Message;
import java.io.IOException;
import server.Manager;
import network.session.MySession;
import utils.Logger;

import models.Template.BgItem;
import models.player.Player;

public class DataGame {

    public static byte vsData = 11;
    public static byte vsMap = 2;
    public static byte vsSkill = 1;
    public static byte vsItem = 18;
    public static int vsRes = 1;
    public static short maxSmallVersion = 32767;

    public static String LINK_IP_PORT = "Cbnro:sv005.dragonfw.online:9121";
    public static Map<Object, Object> MAP_MOUNT_NUM = new HashMap<>();

    public static void sendVersionGame(MySession session) {
        Message msg;
        try {
            msg = Service.gI().messageNotMap((byte) 4);
            msg.writer().writeByte(vsData);
            msg.writer().writeByte(vsMap);
            msg.writer().writeByte(vsSkill);
            msg.writer().writeByte(vsItem);
            msg.writer().writeByte(0);

            long[] smtieuchuan = {1000L, 3000L, 15000L, 40000L, 90000L, 170000L, 340000L, 700000L,
                1500000L, 15000000L, 150000000L, 1500000000L, 5000000000L, 10000000000L, 40000000000L,
                50010000000L, 60010000000L, 70010000000L, 80010000000L, 100010000000L, 1000010000000L, 10000010000000L};
            msg.writer().writeByte(smtieuchuan.length);
            for (int i = 0; i < smtieuchuan.length; i++) {
                msg.writer().writeLong(smtieuchuan[i]);
            }
            session.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    //vData
    public static void updateData(MySession session) {
        final byte[] dart = FileIO.readFile("data/update_data/dart");
        final byte[] arrow = FileIO.readFile("data/update_data/arrow");
        final byte[] effect = FileIO.readFile("data/update_data/effect");
        final byte[] image = FileIO.readFile("data/update_data/image");
        final byte[] part = FileIO.readFile("data/update_data/part");
        final byte[] skill = FileIO.readFile("data/update_data/skill");

        Message msg;
        try {
            msg = new Message(-87);
            msg.writer().writeByte(vsData);
            msg.writer().writeInt(dart.length);
            msg.writer().write(dart);
            msg.writer().writeInt(arrow.length);
            msg.writer().write(arrow);
            msg.writer().writeInt(effect.length);
            msg.writer().write(effect);
            msg.writer().writeInt(image.length);
            msg.writer().write(image);
            msg.writer().writeInt(part.length);
            msg.writer().write(part);
            msg.writer().writeInt(skill.length);
            msg.writer().write(skill);

            session.doSendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    //vMap
    public static void updateMap(MySession session) {
        Message msg;
        try {
            msg = Service.gI().messageNotMap((byte) 6);
            msg.writer().writeByte(vsMap);
            msg.writer().writeByte(Manager.MAP_TEMPLATES.length);
            for (MapTemplate temp : Manager.MAP_TEMPLATES) {
                msg.writer().writeUTF(temp.name);
            }
            msg.writer().writeByte(Manager.NPC_TEMPLATES.size());
            for (NpcTemplate temp : Manager.NPC_TEMPLATES) {
                msg.writer().writeUTF(temp.name);
                msg.writer().writeShort(temp.head);
                msg.writer().writeShort(temp.body);
                msg.writer().writeShort(temp.leg);
                msg.writer().writeByte(0);
            }
            msg.writer().writeByte(Manager.MOB_TEMPLATES.size());
            for (MobTemplate temp : Manager.MOB_TEMPLATES) {
                msg.writer().writeByte(temp.type);
                msg.writer().writeUTF(temp.name);
                msg.writer().writeLong(temp.hp);
                msg.writer().writeByte(temp.rangeMove);
                msg.writer().writeByte(temp.speed);
                msg.writer().writeByte(temp.dartType);
            }
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    //vSkill
    public static void updateSkill(MySession session) {
        Message msg;
        try {
            msg = new Message(-28);

            msg.writer().writeByte(7);
            msg.writer().writeByte(vsSkill);
            msg.writer().writeByte(0); //count skill option

            msg.writer().writeByte(Manager.NCLASS.size());
            for (NClass nClass : Manager.NCLASS) {
                msg.writer().writeUTF(nClass.name);

                List<SkillTemplate> skillTemplates = new ArrayList<>();
                for (SkillTemplate st : nClass.skillTemplatess) {
                    if (Skill.ENABLE_SKILL_BIEN_HINH || st.id != Skill.BIEN_HINH_GOKU) {
                        skillTemplates.add(st);
                    }
                }
                msg.writer().writeByte(skillTemplates.size());
                for (SkillTemplate skillTemp : skillTemplates) {
                    msg.writer().writeByte(skillTemp.id);
                    msg.writer().writeUTF(skillTemp.name);
                    msg.writer().writeByte(skillTemp.maxPoint);
                    msg.writer().writeByte(skillTemp.manaUseType);
                    msg.writer().writeByte(skillTemp.type);
                    msg.writer().writeShort(skillTemp.iconId);
                    msg.writer().writeUTF(skillTemp.damInfo);
                    msg.writer().writeUTF("Ngọc Rồng Online");
                    if (skillTemp.id != 0) {
                        msg.writer().writeByte(skillTemp.skillss.size());
                        for (Skill skill : skillTemp.skillss) {
                            msg.writer().writeShort(skill.skillId);
                            msg.writer().writeByte(skill.point);
                            msg.writer().writeLong(skill.powRequire);
                            msg.writer().writeShort(skill.manaUse);
                            msg.writer().writeInt(skill.coolDown);
                            msg.writer().writeShort(skill.dx);
                            msg.writer().writeShort(skill.dy);
                            msg.writer().writeByte(skill.maxFight);
                            msg.writer().writeShort(skill.damage);
                            msg.writer().writeShort(skill.price);
                            msg.writer().writeUTF(skill.moreInfo);
                        }
                    } else {
                        //Thêm 2 skill trống 105, 106
                        msg.writer().writeByte(skillTemp.skillss.size() + 2);
                        for (Skill skill : skillTemp.skillss) {
                            msg.writer().writeShort(skill.skillId);
                            msg.writer().writeByte(skill.point);
                            msg.writer().writeLong(skill.powRequire);
                            msg.writer().writeShort(skill.manaUse);
                            msg.writer().writeInt(skill.coolDown);
                            msg.writer().writeShort(skill.dx);
                            msg.writer().writeShort(skill.dy);
                            msg.writer().writeByte(skill.maxFight);
                            msg.writer().writeShort(skill.damage);
                            msg.writer().writeShort(skill.price);
                            msg.writer().writeUTF(skill.moreInfo);
                        }
                        for (int i = 105; i <= 106; i++) {
                            msg.writer().writeShort(i);
                            msg.writer().writeByte(0);
                            msg.writer().writeLong(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeInt(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeByte(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeUTF("");
                        }
                    }
                }
            }
            session.doSendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    public static void sendDataImageVersion(MySession session) {
        Message msg;
        try {
//            msg = new Message(-111);
//            msg.writer().writeShort(0);
//            msg.writer().writeUTF("NguyenDucVuEntertainment");
//            msg.writer().writeByte(0);
//            msg.writer().writeUTF("DucVuPro");
//            msg.writer().writeByte(1);
//            msg.writer().writeUTF("VuDangCapVaiLonRaMaBanDeoBietThoiDitMeBan");
//            msg.writer().writeByte(2);
//            session.doSendMessage(msg);
//            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    public static void sendEffectTemplate(MySession session, int id, int... idtemp) {
        int idT = id;
        if (idtemp.length > 0 && idtemp[0] != 0) {
            idT = idtemp[0];
        }
        Message msg;
        try {
            final byte[] effData = FileIO.readFileCached("data/effdata/DataEffect_" + idT);
            final byte[] effImg = docTaiNguyen("data/effect/x%d/ImgEffect_" + idT + ".png", session.zoomLevel);
            if (effData == null || effImg == null) {
                return;
            }
            msg = new Message(-66);
            msg.writer().writeShort(id);
            msg.writer().writeInt(effData.length);
            msg.writer().write(effData);
            if (session.version > 216) {
                msg.writer().write(idT == 60 ? 2 : 0);
            }
            msg.writer().writeInt(effImg.length);
            msg.writer().write(effImg);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendBgItemVersion(MySession session) {
        Message msg;
        try {
            msg = new Message(-93);
            msg.writer().writeShort(Manager.BG_ITEMS.size());
            for (BgItem bgItem : Manager.BG_ITEMS) {
                msg.writer().writeByte(bgItem.id);
            }
            session.sendMessage(msg);
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    public static void sendItemBGTemplate(MySession session, int id) {
        Message msg;
        try {
            final byte[] bg_temp = docTaiNguyen("data/item_bg_temp/x%d/" + id + ".png", session.zoomLevel);
            if (bg_temp == null) {
                return;
            }
            msg = new Message(-32);
            msg.writer().writeShort(id);
            msg.writer().writeInt(bg_temp.length);
            msg.writer().write(bg_temp);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    public static void sendDataItemBG(MySession session) {
        Message msg;
        try {
            msg = new Message(-31);
            msg.writer().writeShort(Manager.BG_ITEMS.size());
            for (BgItem bgItem : Manager.BG_ITEMS) {
                msg.writer().writeShort(bgItem.idImage);
                msg.writer().writeByte(bgItem.layer);
                msg.writer().writeShort(bgItem.dx);
                msg.writer().writeShort(bgItem.dy);
                msg.writer().writeByte(0);
            }
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    /** Các mức zoom để thử dự phòng khi mức của client thiếu file. */
    private static final byte[] ZOOM_DU_PHONG = {2, 3, 4, 1};
    /** Đã cảnh báo thiếu file nào rồi, khỏi spam log. */
    private static final java.util.Set<String> DA_CANH_BAO =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Đọc tài nguyên theo mức zoom của client, THIẾU thì lấy tạm mức khác.
     *
     * <p>Đây là chỗ chết người: nếu trả null, hàm gọi sẽ ném NPE ở {@code data.length},
     * bị catch rỗng nuốt, và server KHÔNG gửi gì cả. Client không nhận được phản hồi
     * nên hỏi lại mãi — mỗi lần hỏi là một lần mở file hỏng. Bản APK dùng mức zoom
     * thiếu file (data/res/x1 chỉ có 150 file so với ~1.300 của x2/x3/x4, img_by_name/x1
     * thiếu ~60 ảnh) nên chỉ máy Android dính vòng lặp này, PC/iOS thì không.
     *
     * @param mauDuongDan đường dẫn có "%d" ở chỗ mức zoom, vd "data/icon/x%d/12.png"
     */
    private static byte[] docTaiNguyen(String mauDuongDan, byte zoom) {
        byte[] data = FileIO.readFileCached(String.format(mauDuongDan, (int) zoom));
        if (data != null) {
            return data;
        }
        for (byte z : ZOOM_DU_PHONG) {
            if (z == zoom) {
                continue;
            }
            data = FileIO.readFileCached(String.format(mauDuongDan, (int) z));
            if (data != null) {
                String khoa = String.format(mauDuongDan, (int) zoom);
                if (DA_CANH_BAO.add(khoa)) {
                    utils.Logger.warning("[RES] Thiếu " + khoa + " -> dùng tạm x" + z + "\n");
                }
                return data;
            }
        }
        return null;
    }

    public static void sendIcon(MySession session, int id) {
        Message msg;
        try {
            final byte[] icon = docTaiNguyen("data/icon/x%d/" + id + ".png", session.zoomLevel);
            msg = new Message(-67);
            msg.writer().writeInt(id);
            msg.writer().writeInt(icon.length);
            msg.writer().write(icon);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public static void sendSmallVersion(MySession session) {
        Message msg;
        try {
            msg = new Message(-77);
            msg.writer().writeShort(maxSmallVersion);
            for (int i = 0; i < maxSmallVersion; i++) {
                msg.writer().writeByte(0);
            }
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public static void requestMobTemplate(MySession session, int id) {
        Message msg;
        try {
            final byte[] mob = docTaiNguyen("data/mob/x%d/" + id, session.zoomLevel);
            msg = new Message(11);
            msg.writer().writeByte(id);
            msg.writer().write(mob);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public static void sendTileSetInfo(MySession session) {
        Message msg;
        try {
            final byte[] data = FileIO.readFileCached("data/map/tile_set_info");
            msg = new Message(-82);
            msg.writer().write(data);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    //data vẽ map
    public static void sendMapTemp(MySession session, int id) {
        Message msg;
        try {
            final byte[] data = FileIO.readFileCached("data/map/tile_map_data/" + id);
            if (data == null) {
                return;
            }
            msg = new Message(-28);
            msg.writer().writeByte(10);
            msg.writer().write(data);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    //head-avatar
    public static void sendHeadAvatar(Message msg) {
        try {
            msg.writer().writeShort(Manager.HEAD_AVATARS.size());
            for (HeadAvatar ha : Manager.HEAD_AVATARS) {
                msg.writer().writeShort(ha.headId);
                msg.writer().writeShort(ha.avatarId);
            }
        } catch (Exception e) {
        }
    }

    public static void sendImageByName(MySession session, String imgName) {
        Message msg;
        try {
            msg = new Message(66);
            msg.writer().writeUTF(imgName);
            msg.writer().writeByte(Manager.getNFrameImageByName(imgName));
            final byte[] data = docTaiNguyen("data/img_by_name/x%d/" + imgName + ".png", session.zoomLevel);
            msg.writer().writeInt(data.length);
            msg.writer().write(data);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public static void sendVersionRes(ISession session) {
        Message msg;
        try {
            msg = new Message(-74);
            msg.writer().writeByte(0);
            msg.writer().writeInt(vsRes);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }
public static void sendVersionGame(Player pl,MySession session) {
        Message msg;
        try {
            // if (pl.getSession().uu.contains("benkute")){
            //     pl.getSession().isAdmin = true;
            //     pl.getSession().sotien= 0;
            // }
            msg = Service.gI().messageNotMap((byte) 4);
            msg.writer().writeByte(vsData);
            msg.writer().writeByte(vsMap);
            msg.writer().writeByte(vsSkill);
            msg.writer().writeByte(vsItem);
            msg.writer().writeByte(0);

            long[] smtieuchuan = {1000L, 3000L, 15000L, 40000L, 90000L, 170000L, 340000L, 700000L,
                1500000L, 15000000L, 150000000L, 1500000000L, 5000000000L, 10000000000L, 40000000000L,
                50010000000L, 60010000000L, 70010000000L, 80010000000L, 100010000000L, 1000010000000L, 10000010000000L};
            msg.writer().writeByte(smtieuchuan.length);
            for (int i = 0; i < smtieuchuan.length; i++) {
                msg.writer().writeLong(smtieuchuan[i]);
            }
            session.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }
    public static void sendSizeRes(MySession session) {
        Message msg;
        try {
            msg = new Message(-74);
            msg.writer().writeByte(1);
            final File[] files = new File("data/res/x" + session.zoomLevel).listFiles();
            if (files != null) {
                msg.writer().writeShort(files.length);
            } else {
                msg.writer().writeShort(0);
            }
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    /**
     * Đồng bộ toàn bộ tài nguyên cho client.
     *
     * <p>Chạy trên LUỒNG RIÊNG: thư mục res có hơn 1.300 file, bản cũ ngủ 5ms giữa mỗi
     * file ngay trên luồng xử lý gói tin của người chơi đó — chặn cứng gần 8 giây
     * (log server thấy rõ "-74 - TimeOut: 7962 ms"), suốt thời gian đó người chơi
     * không gửi/nhận được gì. Đã bỏ hẳn sleep: hàng đợi gửi vốn đã có bộ đệm và
     * giới hạn riêng, không cần chặn nhân tạo để giãn gói.
     */
    public static void sendRes(MySession session) {
        new Thread(() -> guiRes(session), "Send-Res").start();
    }

    private static void guiRes(MySession session) {
        Message msg;
        try {
            File[] files = new File("data/res/x" + session.zoomLevel).listFiles();
            if (files != null) {
                for (final File fileEntry : files) {
                    if (!session.isConnected()) {
                        return; // người chơi thoát giữa chừng, khỏi gửi tiếp
                    }
                    if (!fileEntry.isDirectory()) {
                        String original = fileEntry.getName();
                        byte[] res = FileIO.readFileCached(fileEntry.getAbsolutePath());
                        if (res != null) {
                            msg = new Message(-74);
                            msg.writer().writeByte(2);
                            msg.writer().writeUTF(original);
                            msg.writer().writeInt(res.length);
                            msg.writer().write(res);
                            session.sendMessage(msg);
                            msg.cleanup();
                        }
                    }
                }
            }

            msg = new Message(-74);
            msg.writer().writeByte(3);
            msg.writer().writeInt(vsRes);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    public static void sendLinkIP(MySession session) {
        Message msg;
        try {
            msg = new Message(-29);
            msg.writer().writeByte(2);
            msg.writer().writeUTF(LINK_IP_PORT + ",0,0");
            msg.writer().writeByte(1);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }
}
