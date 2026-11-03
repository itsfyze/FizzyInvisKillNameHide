package com.fizzysmp.hiddenmessages;
import java.nio.file.*;
import java.util.*;
public final class RulesTest {
    public static void main(String[] args) throws Exception {
        int checks=0;
        for(int bits=0;bits<16;bits++) {
            boolean victim=(bits&1)!=0,killer=(bits&2)!=0,deaths=(bits&4)!=0,kills=(bits&8)!=0;
            check(Rules.hide(victim,killer,deaths,kills)==((victim&&deaths)||(killer&&kills)),"Independent toggles"); checks++;
        }
        Path directory=Files.createTempDirectory(Path.of("test-output"),"rules-");
        Path file=directory.resolve("rules.properties"); UUID world=UUID.randomUUID(),other=UUID.randomUUID();
        Rules rules=new Rules(file);
        check(rules.get(world,Rules.DEATH)&&rules.get(world,Rules.KILL),"Both defaults true"); checks++;
        rules.set(world,Rules.DEATH,false);
        rules=new Rules(file);
        check(!rules.get(world,Rules.DEATH)&&rules.get(world,Rules.KILL),"Persisted independent toggle"); checks++;
        check(rules.get(other,Rules.DEATH),"Worlds isolated"); checks++;
        check(Rules.route("/gamerule showDeathMessages false").equals("/gamerule showDeathMessages false"),"Vanilla rules untouched"); checks++;
        check(Rules.route("/gamerule hideInvisibleDeathMessages false").equals("/hiddendeathmessages:hiddenmessages hideInvisibleDeathMessages false"),"Player routing"); checks++;
        check(Rules.route("minecraft:gamerule hideInvisibleKillMessages true world_nether").equals("hiddendeathmessages:hiddenmessages hideInvisibleKillMessages true world_nether"),"Console namespaced routing and world"); checks++;
        check(Rules.route("execute run gamerule hideInvisibleKillMessages true").startsWith("execute"),"Unrelated command not rewritten"); checks++;
        check(Rules.route("/hiddenmessages hideInvisibleKillMessages true").startsWith("/hiddenmessages"),"No routing recursion"); checks++;
        Path corrupt=directory.resolve("bad.properties"); Files.writeString(corrupt,"bad=maybe\n");
        try { new Rules(corrupt); throw new AssertionError("Invalid saved boolean accepted"); } catch(java.io.IOException expected) { checks++; }
        Path parent=directory.resolve("blocked"); Files.writeString(parent,"keep"); Rules blocked=new Rules(parent.resolve("rules.properties"));
        try { blocked.set(world,Rules.DEATH,false); throw new AssertionError("Expected failed write"); } catch(java.io.IOException expected) { checks++; }
        check(blocked.get(world,Rules.DEATH),"Failed write preserves active value"); checks++;
        System.out.println(checks+" checks passed: visibility policy, per-world defaults, persistence, routing and failure handling.");
    }
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
