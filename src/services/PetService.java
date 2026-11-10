package services;

 
import consts.ConstPlayer;
import java.util.ArrayList;
import java.util.List;
import models.item.Item;
import models.player.NewPet;
import models.player.Pet;
import models.player.Player;
import services.map.ChangeMapService;
import services.player.InventoryService;
import utils.SkillUtil;
import utils.Util;

public class PetService {

    private static PetService instance;

    public static PetService gI() {
        if (instance == null) {
            instance = new PetService();
        }
        return instance;
    }

    public List<Item> getAndClearOldPetItems(Player player) {
        List<Item> oldItems = new ArrayList<>();
        if (player != null && player.pet != null && player.pet.inventory != null && player.pet.inventory.itemsBody != null) {
            for (Item it : player.pet.inventory.itemsBody) {
                if (it != null && it.isNotNullItem()) {
                    Item copy = ItemService.gI().copyItem(it);
                    copy.quantity = Math.max(1, it.quantity);
                    oldItems.add(copy);
                } else {
                    oldItems.add(ItemService.gI().createItemNull());
                }
            }
            player.pet.inventory.trainArmor = null;
            player.pet.inventory.itemsBody.clear();
        }
        return oldItems;
    }

    public void applyOldItemsToPet(Player player, List<Item> oldItems) {
        if (player == null || player.pet == null || player.pet.inventory == null || oldItems == null || oldItems.isEmpty()) {
            return;
        }
        try {
            int newPetBodySize = player.pet.inventory.itemsBody.size();

            // 1. Toàn bộ Giáp luyện tập của đệ tử cũ (type 32 / isTrainArmor) LUÔN chuyển về hành trang sư phụ (itemsBag)
            // hoặc rương (itemsBox) nếu hành trang đầy để người chơi giữ được giáp, thấy và dùng trực tiếp trong hành trang sư phụ.
            java.util.Set<Integer> trainArmorIndices = new java.util.HashSet<>();
            for (int i = 0; i < oldItems.size(); i++) {
                Item it = oldItems.get(i);
                if (it != null && it.isNotNullItem() && (it.template.type == 32 || ItemService.gI().isTrainArmor(it))) {
                    trainArmorIndices.add(i);
                    it.quantity = Math.max(1, it.quantity);
                    if (!InventoryService.gI().addItemBag(player, it)) {
                        if (!InventoryService.gI().addItemBox(player, it)) {
                            Service.gI().sendThongBao(player, "Hành trang và rương đồ đã đầy!");
                        }
                    }
                }
            }

            // 2. Xử lý các trang bị khác của đệ tử cũ
            for (int i = 0; i < oldItems.size(); i++) {
                if (trainArmorIndices.contains(i)) {
                    continue; // Đã chuyển Giáp luyện tập về hành trang sư phụ
                }
                Item it = oldItems.get(i);
                if (it == null || !it.isNotNullItem()) {
                    continue;
                }
                it.quantity = Math.max(1, it.quantity);

                boolean equipped = false;
                if (i == 4 || i == 5) {
                    // Radar (4) & Cải trang (5): mọi đệ tử đều mặc được
                    if (newPetBodySize > i) {
                        player.pet.inventory.itemsBody.set(i, it);
                        equipped = true;
                    }
                } else if (i >= 0 && i <= 3) {
                    // Quần áo (Áo 0, Quần 1, Găng 2, Giày 3): kiểm tra trùng hệ phái
                    boolean genderMatch = (it.template.gender == 3 || it.template.gender == player.pet.gender);
                    if (genderMatch && newPetBodySize > i) {
                        player.pet.inventory.itemsBody.set(i, it);
                        equipped = true;
                    }
                } else if (i == 7 || i == 8) {
                    // Pet phụ (7) & Đeo lưng (8): đệ đặc biệt (Mabư, Beerus, Black, Ubu, Jiren...) có 9 ô
                    if (newPetBodySize > i) {
                        player.pet.inventory.itemsBody.set(i, it);
                        equipped = true;
                    }
                }

                if (!equipped) {
                    // Không tương thích hệ phái hoặc đệ thường không có slot 7,8: trả về hành trang/rương
                    if (!InventoryService.gI().addItemBag(player, it)) {
                        if (!InventoryService.gI().addItemBox(player, it)) {
                            Service.gI().sendThongBao(player, "Hành trang và rương đồ đã đầy!");
                        }
                    }
                }
            }

            player.pet.nPoint.calPoint();
            Service.gI().point(player);
            Service.gI().Send_Caitrang(player.pet);
            Service.gI().sendFlagBag(player.pet);
            Service.gI().showInfoPet(player);
            InventoryService.gI().sendItemBody(player);
            InventoryService.gI().sendItemBags(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void afterPetCreated(Player player, String chatMessage) {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                if (player != null && player.pet != null && player.zone != null) {
                    player.pet.joinMapMaster();
                }
                if (player != null && player.pet != null) {
                    Service.gI().showInfoPet(player);
                    if (chatMessage != null && !chatMessage.isEmpty()) {
                        Service.gI().chatJustForMe(player, player.pet, chatMessage);
                    }
                }
            } catch (Exception e) {
            }
        }).start();
    }

    public void createNormalPet(Player player, int gender, byte... limitPower) {
        createNormalPet(player, gender, null, limitPower);
    }

    public void createNormalPet(Player player, int gender, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, false, false, false, false, (byte) gender);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "Xin hãy thu nhận làm đệ tử");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createNormalPet(Player player, byte... limitPower) {
        createNormalPet(player, (List<Item>) null, limitPower);
    }

    public void createNormalPet(Player player, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, false, false, false, false);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "Xin hãy thu nhận làm đệ tử");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createMabuPet(Player player, byte... limitPower) {
        createMabuPet(player, (List<Item>) null, limitPower);
    }

    public void createMabuPet(Player player, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, true, false, false, false);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "Oa oa oa...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createMabuPet(Player player, int gender, byte... limitPower) {
        createMabuPet(player, gender, null, limitPower);
    }

    public void createMabuPet(Player player, int gender, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, true, false, false, false, (byte) gender);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "Oa oa oa...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createBerusPet(Player player, byte... limitPower) {
        createBerusPet(player, (List<Item>) null, limitPower);
    }

    public void createBerusPet(Player player, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, false, true, false, false);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "Thần hủy diệt hiện thân tất cả quỳ xuống...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createBerusPet(Player player, int gender, byte... limitPower) {
        createBerusPet(player, gender, null, limitPower);
    }

    public void createBerusPet(Player player, int gender, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, false, true, false, false, (byte) gender);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "Thần hủy diệt hiện thân tất cả quỳ xuống...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createBlackPet(Player player, byte... limitPower) {
        createBlackPet(player, (List<Item>) null, limitPower);
    }

    public void createBlackPet(Player player, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, false, false, false, true);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "Ta là Black Goku, kẻ sẽ hủy diệt toàn bộ loài người!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createBlackPet(Player player, int gender, byte... limitPower) {
        createBlackPet(player, gender, null, limitPower);
    }

    public void createBlackPet(Player player, int gender, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, false, false, false, true, (byte) gender);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "Ta là Black Goku, kẻ sẽ hủy diệt toàn bộ loài người!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createubuPet(Player player, byte... limitPower) {
        createubuPet(player, (List<Item>) null, limitPower);
    }

    public void createubuPet(Player player, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, false, false, true, false);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "À húuuuuuuu bố mày là Ubu nè!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createubuPet(Player player, int gender, byte... limitPower) {
        createubuPet(player, gender, null, limitPower);
    }

    public void createubuPet(Player player, int gender, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, false, false, true, false, (byte) gender);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "À húuuuuuuu bố mày là Ubu nè!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeNormalPet(Player player, int gender) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createNormalPet(player, gender, oldItems, limitPower);
    }

    public void changeNormalPet(Player player) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createNormalPet(player, oldItems, limitPower);
    }

    public void changeMabuPet(Player player) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createMabuPet(player, oldItems, limitPower);
    }

    public void changeMabuPet(Player player, int gender) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createMabuPet(player, gender, oldItems, limitPower);
    }

    public void changeBerusPet(Player player) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createBerusPet(player, oldItems, limitPower);
    }

    public void changeBerusPet(Player player, int gender) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createBerusPet(player, gender, oldItems, limitPower);
    }

    public void changeBlackPet(Player player) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createBlackPet(player, oldItems, limitPower);
    }

    public void changeBlackPet(Player player, int gender) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createBlackPet(player, gender, oldItems, limitPower);
    }

    public void createJirenPet(Player player, byte... limitPower) {
        createJirenPet(player, (List<Item>) null, limitPower);
    }

    public void createJirenPet(Player player, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, false, false, false, false, true);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "Ta là Jiren, kẻ mạnh nhất Vũ trụ 11!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createJirenPet(Player player, int gender, byte... limitPower) {
        createJirenPet(player, gender, null, limitPower);
    }

    public void createJirenPet(Player player, int gender, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, false, false, false, false, true, (byte) gender);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "Ta là Jiren, kẻ mạnh nhất Vũ trụ 11!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeJirenPet(Player player) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createJirenPet(player, oldItems, limitPower);
    }

    public void changeJirenPet(Player player, int gender) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createJirenPet(player, gender, oldItems, limitPower);
    }

    public void createCellMaxPet(Player player, byte... limitPower) {
        createCellMaxPet(player, (List<Item>) null, limitPower);
    }

    public void createCellMaxPet(Player player, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, false, false, false, false, false, true);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "ROOOAAARRRR! Ta là Cell Max!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createCellMaxPet(Player player, int gender, byte... limitPower) {
        createCellMaxPet(player, gender, null, limitPower);
    }

    public void createCellMaxPet(Player player, int gender, List<Item> oldItems, byte... limitPower) {
        try {
            createNewPet(player, false, false, false, false, false, true, (byte) gender);
            if (limitPower != null && limitPower.length == 1) {
                player.pet.nPoint.limitPower = limitPower[0];
            }
            applyOldItemsToPet(player, oldItems);
            afterPetCreated(player, "ROOOAAARRRR! Ta là Cell Max!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeCellMaxPet(Player player) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createCellMaxPet(player, oldItems, limitPower);
    }

    public void changeCellMaxPet(Player player, int gender) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createCellMaxPet(player, gender, oldItems, limitPower);
    }

    public void changeubuPet(Player player) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createubuPet(player, oldItems, limitPower);
    }

    public void changeubuPet(Player player, int gender) {
        if (player == null || player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        List<Item> oldItems = getAndClearOldPetItems(player);
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
        createubuPet(player, gender, oldItems, limitPower);
    }

    public void changeNamePet(Player player, String name) {
        try {
            if (!InventoryService.gI().isExistItemBag(player, 400)) {
                Service.gI().sendThongBao(player, "Bạn cần thẻ đặt tên đệ tử, mua tại Santa");
                return;
            } else if (Util.haveSpecialCharacter(name)) {
                Service.gI().sendThongBao(player, "Tên không được chứa ký tự đặc biệt");
                return;
            } else if (name.length() > 10) {
                Service.gI().sendThongBao(player, "Tên quá dài");
                return;
            }
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.name = "$" + name.toLowerCase().trim();
            InventoryService.gI().subQuantityItemsBag(player, InventoryService.gI().findItemBag(player, 400), 1);
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Service.gI().chatJustForMe(player, player.pet, "Cảm ơn sư phụ đã đặt cho con tên " + name);
                } catch (Exception e) {
                }
            }).start();
        } catch (Exception ex) {

        }
    }

    private int[] getDataPetNormal() {
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 105) * 20; //hp
        petData[1] = Util.nextInt(40, 105) * 20; //mp
        petData[2] = Util.nextInt(20, 45); //dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetMabu() {
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 105) * 20; //hp
        petData[1] = Util.nextInt(40, 105) * 20; //mp
        petData[2] = Util.nextInt(50, 120); //dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetPic() {
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 115) * 20; //hp
        petData[1] = Util.nextInt(40, 115) * 20; //mp
        petData[2] = Util.nextInt(70, 140); //dame
        petData[3] = Util.nextInt(9, 50); //def
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetBlack() {
        int[] petData = new int[5];
        petData[0] = Util.nextInt(48, 126) * 20; //hp  (Beerus +20%)
        petData[1] = Util.nextInt(48, 126) * 20; //mp  (Beerus +20%)
        petData[2] = Util.nextInt(24, 54); //dame      (Beerus +20%)
        petData[3] = Util.nextInt(11, 60); //def       (Beerus +20%)
        petData[4] = Util.nextInt(0, 2); //crit
        return petData;
    }

    private int[] getDataPetJiren() {
        int[] petData = new int[5];
        petData[0] = Util.nextInt(60, 150) * 20; //hp (+50%)
        petData[1] = Util.nextInt(60, 150) * 20; //mp (+50%)
        petData[2] = Util.nextInt(30, 68);       //dame (+50%)
        petData[3] = Util.nextInt(15, 75);       //def (+50%)
        petData[4] = Util.nextInt(0, 2);         //crit
        return petData;
    }

    private int[] getDataPetCellMax() {
        int[] petData = new int[5];
        petData[0] = Util.nextInt(80, 200) * 20; //hp (+80%)
        petData[1] = Util.nextInt(80, 200) * 20; //mp (+80%)
        petData[2] = Util.nextInt(40, 90);       //dame
        petData[3] = Util.nextInt(20, 90);       //def
        petData[4] = Util.nextInt(0, 3);         //crit
        return petData;
    }

    private void createNewPet(Player player, boolean isMabu, boolean isBeerus, boolean isubu, boolean isBlack, byte... gender) {
        createNewPet(player, isMabu, isBeerus, isubu, isBlack, false, false, gender);
    }

    private void createNewPet(Player player, boolean isMabu, boolean isBeerus, boolean isubu, boolean isBlack, boolean isJiren, byte... gender) {
        createNewPet(player, isMabu, isBeerus, isubu, isBlack, isJiren, false, gender);
    }

    private void createNewPet(Player player, boolean isMabu, boolean isBeerus, boolean isubu, boolean isBlack, boolean isJiren, boolean isCellMax, byte... gender) {
        int[] data;
        if (isCellMax) {
            data = getDataPetCellMax();
        } else if (isJiren) {
            data = getDataPetJiren();
        } else if (isBlack) {
            data = getDataPetBlack();
        } else if (isBeerus) {
            data = getDataPetBlack();
        } else if (isubu) {
            data = getDataPetPic();
        } else if (isMabu) {
            data = getDataPetMabu();
        } else {
            data = getDataPetNormal();
        }
        Pet pet = new Pet(player);
        pet.name = "$" + (isCellMax ? "Cell Max" : isJiren ? "Jiren" : isBlack ? "Black" : isubu ? "Ubu" : isBeerus ? "Beerus" : isMabu ? "Mabư" : "Đệ tử");
        pet.gender = (gender != null && gender.length != 0) ? gender[0] : (byte) Util.nextInt(0, 2);
        pet.id = player.isPl() ? -player.id : -Math.abs(player.id) - 100000;
        pet.nPoint.power = isMabu || isBeerus || isubu || isBlack || isJiren || isCellMax ? 1500000 : 2000;
        pet.typePet = (byte) (isCellMax ? 6 : isJiren ? 5 : isBlack ? 4 : isubu ? 3 : isBeerus ? 2 : isMabu ? 1 : 0);
        pet.nPoint.stamina = 1000;
        pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = data[0];
        pet.nPoint.mpg = data[1];
        pet.nPoint.dameg = data[2];
        pet.nPoint.defg = data[3];
        pet.nPoint.critg = data[4];
        int bodySlots = (isMabu || isBeerus || isubu || isBlack || isJiren || isCellMax) ? 9 : 7;
        for (int i = 0; i < bodySlots; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
        pet.playerSkill.skills.add(SkillUtil.createSkill(Util.nextInt(0, 2) * 2, 1));
        for (int i = 0; i < 3; i++) {
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }
        pet.nPoint.setFullHpMp();
        player.pet = pet;
    }

    public static void Pet2(Player pl, int h, int b, int l) {
        if (pl.newPet != null) {
            pl.newPet.dispose();
        }
        pl.newPet = new NewPet(pl, (short) h, (short) b, (short) l);
        pl.newPet.name = "$";
        pl.newPet.gender = pl.gender;
        pl.newPet.nPoint.tiemNang = 1;
        pl.newPet.nPoint.power = 1;
        pl.newPet.nPoint.limitPower = 1;
        pl.newPet.nPoint.hpg = 500000000;
        pl.newPet.nPoint.mpg = 500000000;
        pl.newPet.nPoint.hp = 500000000;
        pl.newPet.nPoint.mp = 500000000;
        pl.newPet.nPoint.dameg = 1;
        pl.newPet.nPoint.defg = 1;
        pl.newPet.nPoint.critg = 1;
        pl.newPet.nPoint.stamina = 1;
        pl.newPet.nPoint.setBasePoint();
        pl.newPet.nPoint.setFullHpMp();
        if (pl.zone != null) {
            pl.newPet.joinMapMaster();
        }
    }
}
