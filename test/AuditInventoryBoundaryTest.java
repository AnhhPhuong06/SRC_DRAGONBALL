import models.item.Item;
import services.player.InventoryService;
import server.Manager;
import models.Template;

/** Actual inventory API boundary checks; no DB or game startup. */
public final class AuditInventoryBoundaryTest {
    static int checks;
    static void check(boolean ok, String label) { checks++; if (!ok) throw new AssertionError(label); }
    public static void main(String[] args) {
        for (int id = 0; id < 1450; id++) {
            var template = new Template.ItemTemplate(); template.id = (short)id;
            template.type = 27; template.name = "fixture"; template.gender = 3;
            Manager.ITEM_TEMPLATES.add(template);
        }
        for (int id = 0; id < 252; id++) Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"fixture",0));
        var service = InventoryService.gI();
        for (int quantity : new int[]{-1, Integer.MIN_VALUE, 0}) {
            var player = ToriNguSacShopTest.player(10);
            Item stack = player.inventory.itemsBag.get(0);
            service.subQuantityItemsBag(player, 674, quantity);
            check(stack.quantity == 10, "ID-based subtraction cannot mint items for quantity " + quantity);
            service.subQuantityItem(player.inventory.itemsBag, stack, quantity);
            check(stack.quantity == 10, "Object-based subtraction rejects nonpositive quantity");
            check(!service.addItemList(player.inventory.itemsBag,ToriNguSacShopTest.item(674,quantity)), "Reject invalid stack input");
            Item gem = ToriNguSacShopTest.item(1,quantity); gem.template.type = 10;
            int before = player.inventory.gem;
            check(!service.addItemBag(player,gem) && player.inventory.gem == before, "Reject nonpositive currency pickup");
        }
        var player = ToriNguSacShopTest.player(10);
        check(!service.addItemBag(player,null),"Null reward rejected");
        check(!service.addItemList(player.inventory.itemsBag,new Item()),"Missing template rejected");
        service.subQuantityItemsBag(player,674,3);
        check(ToriNguSacShopTest.count(player,674)==7,"Valid subtraction retained");
        check(service.addItemList(player.inventory.itemsBag,ToriNguSacShopTest.item(674,2)),"Valid addition retained");
        check(ToriNguSacShopTest.count(player,674)==9,"Valid addition exact");
        player.inventory.gem=Integer.MAX_VALUE;player.inventory.ruby=Integer.MAX_VALUE;
        check(player.inventory.getGemAndRuby()==Integer.MAX_VALUE,"Combined currency cannot wrap negative");
        System.out.println("PASS: " + checks + " inventory boundary regressions");
    }
}
