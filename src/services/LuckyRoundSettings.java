package services;

import com.google.gson.Gson;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import services.func.MocVongQuayService;

/** Validated snapshots of the existing Thượng Đế configuration; no player data writes. */
public final class LuckyRoundSettings {
    public record Field(String key, String property, String label, int min, int max) {}
    public record Group(String key, String property, String label) {}
    public record Option(int id, int param) {}
    public record Reward(int itemId, int quantity, List<Option> options) {
        public Reward { options=List.copyOf(options); }
    }
    public record Milestone(int id, int spins, List<Reward> rewards) {
        public Milestone { rewards=List.copyOf(rewards); }
    }
    public record State(Map<String,Integer> values, Map<String,List<Integer>> groups, List<Milestone> milestones) {
        public State {
            values=Collections.unmodifiableMap(new LinkedHashMap<>(values));
            var copy=new LinkedHashMap<String,List<Integer>>();groups.forEach((key,list)->copy.put(key,List.copyOf(list)));
            groups=Collections.unmodifiableMap(copy);milestones=List.copyOf(milestones);
        }
    }
    public static final List<Field> FIELDS=List.of(
        new Field("idVatPhamQuay","id_vat_pham_quay","Vật phẩm dùng để quay",0,32767),
        new Field("soLuongVatPhamQuay","so_luong_vat_pham_quay","Số vật phẩm mỗi lượt",1,1_000_000),
        new Field("mocMoQuaHiem","moc_mo_qua_hiem","Số lượt mở nhóm quà sau mốc",0,Integer.MAX_VALUE),
        new Field("tiLeMoiMonSieuHiem","ti_le_moi_mon_sieu_hiem","Tỷ lệ mỗi món siêu hiếm (%)",0,100),
        new Field("tiLeNhom5","ti_le_nhom_5","Nhóm hiếm: tỷ lệ khi trượt siêu hiếm (%)",0,100),
        new Field("tiLeNhom15","ti_le_nhom_15","Nhóm đặc biệt: tỷ lệ khi trượt siêu hiếm (%)",0,100),
        new Field("soLuongHongNgoc","so_luong_hong_ngoc","Số hồng ngọc khi trúng",1,1_000_000),
        new Field("vangMin","vang_min","Vàng thưởng ít nhất",1,2_000_000_000),
        new Field("vangMax","vang_max","Vàng thưởng nhiều nhất",1,2_000_000_000));
    public static final List<Group> GROUPS=List.of(
        new Group("idNhom15","id_nhom_15","Nhóm đặc biệt"),
        new Group("idNhom5","id_nhom_5","Nhóm hiếm trước mốc"),
        new Group("idNhom5SauMoc","id_nhom_5_sau_moc","Nhóm hiếm sau mốc"),
        new Group("idNhomSieuHiem","id_nhom_sieu_hiem","Nhóm siêu hiếm trước mốc"),
        new Group("idNhomSieuHiemSauMoc","id_nhom_sieu_hiem_sau_moc","Nhóm siêu hiếm sau mốc"),
        new Group("idVatPhamBoTro","id_vat_pham_bo_tro","Vật phẩm bổ trợ trong nhóm thường"));
    private static final Gson JSON=new Gson();
    private final Path path;
    public LuckyRoundSettings(){this(Path.of("data/config/vong_quay_thoi_vang.properties"));}
    public LuckyRoundSettings(Path path){this.path=path;}

    public State state() {
        synchronized(VongQuayThoiVangConfig.class) {
            try {
                var values=new LinkedHashMap<String,Integer>();
                for(var f:FIELDS)values.put(f.key(),VongQuayThoiVangConfig.class.getField(f.key()).getInt(null));
                var groups=new LinkedHashMap<String,List<Integer>>();
                for(var g:GROUPS){int[] ids=(int[])VongQuayThoiVangConfig.class.getField(g.key()).get(null);groups.put(g.key(),Arrays.stream(ids).boxed().toList());}
                List<Milestone> milestones=new ArrayList<>();
                for(int i=1;i<=4;i++) {
                    int spins=VongQuayThoiVangConfig.class.getField("moc"+i+"Spins").getInt(null);
                    String text=(String)VongQuayThoiVangConfig.class.getField("moc"+i+"RewardsStr").get(null);
                    var rewards=new ArrayList<Reward>();
                    for(var r:VongQuayThoiVangConfig.parseRewards(text)) {
                        var options=new ArrayList<Option>();if(r.options!=null)for(int[] o:r.options)options.add(new Option(o[0],o[1]));
                        rewards.add(new Reward(r.tempId,r.quantity,options));
                    }
                    milestones.add(new Milestone(i-1,spins,rewards));
                }
                return new State(values,groups,milestones);
            } catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
    }
    public static String revision(State state) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.toJson(state).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public void save(State next,String expected) throws IOException {
        validate(next);
        synchronized(VongQuayThoiVangConfig.class) {
            if(!revision(state()).equals(expected))throw new ConcurrentModificationException("Cấu hình đã thay đổi. Tải lại trước khi lưu.");
            var props=new Properties();
            if(Files.exists(path))try(var in=Files.newInputStream(path)){props.load(in);}
            for(var f:FIELDS)props.setProperty(f.property(),next.values().get(f.key()).toString());
            for(var g:GROUPS)props.setProperty(g.property(),String.join(", ",next.groups().get(g.key()).stream().map(Object::toString).toList()));
            for(var m:next.milestones()) {
                props.setProperty("moc"+(m.id()+1)+"_spins",Integer.toString(m.spins()));
                props.setProperty("moc"+(m.id()+1)+"_rewards",rewardString(m.rewards()));
            }
            Path destination=path.toAbsolutePath(),parent=destination.getParent();Files.createDirectories(parent);
            Path temp=Files.createTempFile(parent,"lucky-round-",".tmp");
            try {
                try(var out=Files.newOutputStream(temp)){props.store(out,"Cau hinh Vong Quay Thuong De");}
                try{Files.move(temp,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException e){Files.move(temp,destination,StandardCopyOption.REPLACE_EXISTING);}
            }finally{Files.deleteIfExists(temp);}
            try {
                for(var f:FIELDS)VongQuayThoiVangConfig.class.getField(f.key()).setInt(null,next.values().get(f.key()));
                for(var g:GROUPS)VongQuayThoiVangConfig.class.getField(g.key()).set(null,next.groups().get(g.key()).stream().mapToInt(Integer::intValue).toArray());
                for(var m:next.milestones()) {
                    VongQuayThoiVangConfig.class.getField("moc"+(m.id()+1)+"Spins").setInt(null,m.spins());
                    VongQuayThoiVangConfig.class.getField("moc"+(m.id()+1)+"RewardsStr").set(null,rewardString(m.rewards()));
                }
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            VongQuayThoiVangConfig.syncToModules();
        }
    }
    private static String rewardString(List<Reward> rewards) {
        var items=new ArrayList<MocVongQuayService.RewardItem>();
        for(var r:rewards)items.add(new MocVongQuayService.RewardItem(r.itemId(),r.quantity(),r.options().stream().map(o->new int[]{o.id(),o.param()}).toArray(int[][]::new)));
        return VongQuayThoiVangConfig.rewardsToString(items);
    }
    public static void validate(State state) {
        if(!state.values().keySet().equals(new HashSet<>(FIELDS.stream().map(Field::key).toList())))throw new IllegalArgumentException("Thiếu hoặc thừa trường cấu hình.");
        for(var f:FIELDS){Integer value=state.values().get(f.key());if(value==null||value<f.min()||value>f.max())throw new IllegalArgumentException(f.label()+" phải trong khoảng "+f.min()+".."+f.max());}
        if(state.values().get("vangMin")>state.values().get("vangMax"))throw new IllegalArgumentException("Vàng ít nhất không được lớn hơn vàng nhiều nhất.");
        if(state.values().get("tiLeNhom5")+state.values().get("tiLeNhom15")>100)throw new IllegalArgumentException("Tổng tỷ lệ nhóm hiếm và đặc biệt không quá 100%.");
        if(!state.groups().keySet().equals(new HashSet<>(GROUPS.stream().map(Group::key).toList())))throw new IllegalArgumentException("Cần đủ 6 nhóm quà.");
        for(var g:GROUPS){var ids=state.groups().get(g.key());if(ids.size()>200||ids.stream().anyMatch(id->id<0||id>32767)||new HashSet<>(ids).size()!=ids.size())throw new IllegalArgumentException("Danh sách ID không hợp lệ hoặc bị trùng: "+g.label());}
        for(String key:List.of("idNhomSieuHiem","idNhomSieuHiemSauMoc"))if((long)state.groups().get(key).size()*state.values().get("tiLeMoiMonSieuHiem")>100)throw new IllegalArgumentException("Tổng tỷ lệ nhóm siêu hiếm vượt 100%.");
        for(String key:List.of("idNhom5","idNhom5SauMoc"))if(state.values().get("tiLeNhom5")>0&&state.groups().get(key).isEmpty())throw new IllegalArgumentException("Nhóm hiếm đang bật cần có vật phẩm.");
        if(state.values().get("tiLeNhom15")>0&&state.groups().get("idNhom15").isEmpty())throw new IllegalArgumentException("Nhóm đặc biệt đang bật cần có vật phẩm.");
        if(state.groups().get("idVatPhamBoTro").isEmpty())throw new IllegalArgumentException("Cần ít nhất 1 vật phẩm bổ trợ.");
        if(state.milestones().size()!=4)throw new IllegalArgumentException("Cần đủ 4 mốc thưởng.");
        int previous=0;
        for(int i=0;i<4;i++) {
            var m=state.milestones().get(i);if(m.id()!=i||m.spins()<=previous||m.rewards().isEmpty()||m.rewards().size()>100)throw new IllegalArgumentException("4 mốc phải tăng dần, có quà và giữ đúng thứ tự.");previous=m.spins();
            for(var r:m.rewards()) {
                if(r.itemId()<0||r.itemId()>32767||r.quantity()<1||r.options().size()>255)throw new IllegalArgumentException("Vật phẩm hoặc số lượng quà mốc không hợp lệ.");
                Set<Integer> seen=new HashSet<>();for(var o:r.options())if(o.id()<0||o.id()>255||o.param()<0||o.param()>65535||!seen.add(o.id()))throw new IllegalArgumentException("Option quà mốc không hợp lệ hoặc bị trùng.");
            }
        }
    }
}
