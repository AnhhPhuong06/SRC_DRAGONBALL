import utils.JsonInput;
public final class AuditJsonDepthTest {
    public static void main(String[] args){
        if(JsonInput.hasExcessiveDepth("[".repeat(64)+"0"+"]".repeat(64)))throw new AssertionError("Boundary rejected");
        if(!JsonInput.hasExcessiveDepth("[".repeat(65)+"0"+"]".repeat(65)))throw new AssertionError("Excess depth accepted");
        String literal="{\"text\":\""+"[".repeat(1000)+"\",\"quoted\":\"\\\"[\"}";
        if(JsonInput.hasExcessiveDepth(literal))throw new AssertionError("Quoted/escaped brackets counted as structure");
        System.out.println("PASS: 3 JSON depth, boundary and escaped-string regressions");
    }
}
