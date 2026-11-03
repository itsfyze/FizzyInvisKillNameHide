package com.fizzysmp.hiddenmessages;

import java.io.*;
import java.nio.file.*;
import java.util.*;

final class Rules {
    static final String DEATH="hideInvisibleDeathMessages", KILL="hideInvisibleKillMessages";
    private final Path file;
    private Properties data=new Properties();
    Rules(Path file) throws IOException {
        this.file=file;
        if(Files.exists(file)) try(Reader in=Files.newBufferedReader(file)) { data.load(in); }
        for(String key:data.stringPropertyNames())
            if(!Set.of("true","false").contains(data.getProperty(key))) throw new IOException("Invalid boolean in saved rules: "+key);
    }
    static String name(String input) {
        for(String name:List.of(DEATH,KILL)) if(name.equalsIgnoreCase(input)) return name;
        return null;
    }
    boolean get(UUID world,String name) { return Boolean.parseBoolean(data.getProperty(world+"."+name,"true")); }
    void set(UUID world,String name,boolean value) throws IOException {
        if(name(name)==null) throw new IllegalArgumentException("Unknown rule");
        Properties next=new Properties(); next.putAll(data); next.setProperty(world+"."+name,Boolean.toString(value));
        Files.createDirectories(file.getParent());
        Path temp=Files.createTempFile(file.getParent(),"rules-",".tmp");
        try {
            try(Writer out=Files.newBufferedWriter(temp)) { next.store(out,"HiddenDeathMessages world rules"); }
            try { Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException ex) { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING); }
            data=next;
        } finally { Files.deleteIfExists(temp); }
    }
    static boolean hide(boolean invisibleVictim,boolean invisibleKiller,boolean hideDeaths,boolean hideKills) {
        return (invisibleVictim && hideDeaths) || (invisibleKiller && hideKills);
    }
    static String route(String input) {
        String text=input.startsWith("/") ? input.substring(1) : input;
        String[] args=text.trim().split("\\s+",3);
        if(args.length<2 || (!args[0].equalsIgnoreCase("gamerule") && !args[0].equalsIgnoreCase("minecraft:gamerule")) || name(args[1])==null) return input;
        return (input.startsWith("/") ? "/" : "")+"hiddendeathmessages:hiddenmessages "+args[1]+(args.length>2 ? " "+args[2] : "");
    }
}
