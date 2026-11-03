package com.fizzysmp.hiddenmessages;

import java.util.*;
import java.util.logging.Level;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

public final class HiddenDeathMessages extends JavaPlugin implements Listener {
    private Rules rules;
    @Override public void onEnable() {
        try { rules=new Rules(getDataFolder().toPath().resolve("world-rules.properties")); }
        catch(Exception ex) { throw new IllegalStateException("Cannot read saved message rules; refusing to overwrite them",ex); }
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this,this);
    }
    private boolean hidden(Player player) {
        return player!=null && (player.isInvisible() || player.hasPotionEffect(PotionEffectType.INVISIBILITY)
            || player.getMetadata("vanished").stream().anyMatch(value->value.asBoolean()));
    }
    private String plain(net.kyori.adventure.text.Component component) {
        StringBuilder text=new StringBuilder();
        if(component instanceof net.kyori.adventure.text.TextComponent literal) text.append(literal.content());
        for(var child:component.children()) text.append(plain(child));
        return text.toString();
    }    private net.kyori.adventure.text.Component obfuscate(net.kyori.adventure.text.Component message, Player player) {
        if (message==null) return null;

        var replacement=net.kyori.adventure.text.Component.text("Hidden")
            .decorate(net.kyori.adventure.text.format.TextDecoration.OBFUSCATED);
        String nickname=plain(player.displayName());
        String text=plain(message);
        if(text.equals(player.getName()) || (!nickname.isBlank() && text.equals(nickname))) return replacement;
        message=message.hoverEvent(null).clickEvent(null).insertion(null);
        message=message.children(message.children().stream().map(child->obfuscate(child,player)).toList());
        if(message instanceof net.kyori.adventure.text.TranslatableComponent translated)
            message=translated.args(translated.args().stream().map(arg->obfuscate(arg,player)).toList());
        message=message.replaceText(builder->builder.matchLiteral(player.getName()).replacement(replacement));
        if(!nickname.isBlank()) message=message.replaceText(builder->builder.matchLiteral(nickname).replacement(replacement));
        return message;
    }    @EventHandler(priority=EventPriority.HIGHEST)
    public void death(PlayerDeathEvent event) {
        if (!getConfig().getBoolean("enabled",true)) return;
        Player victim=event.getEntity();
        Entity cause=event.getDamageSource().getCausingEntity();
        Player killer=cause instanceof Player player ? player : victim.getKiller();
        UUID world=victim.getWorld().getUID();
        if(Rules.hide(hidden(victim),hidden(killer),rules.get(world,Rules.DEATH),rules.get(world,Rules.KILL))) {
            var message=event.deathMessage(); if(hidden(victim) && rules.get(world,Rules.DEATH)) message=obfuscate(message,victim); if(hidden(killer) && rules.get(world,Rules.KILL)) message=obfuscate(message,killer); event.deathMessage(message);
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void petDeath(io.papermc.paper.event.entity.TameableDeathMessageEvent event) {
        if (!getConfig().getBoolean("enabled",true)) return;
        var pet=event.getEntity();
        var damage=pet.getLastDamageCause();
        Entity cause=damage==null ? null : damage.getDamageSource().getCausingEntity();
        Player killer=cause instanceof Player player ? player : pet.getKiller();
        if (hidden(killer) && rules.get(pet.getWorld().getUID(),Rules.KILL)) event.deathMessage(obfuscate(event.deathMessage(),killer));
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if (!sender.hasPermission("hiddenmessages.admin")) { sender.sendMessage("You do not have permission."); return true; }
        if(args.length!=1 || !Set.of("on","off","status").contains(args[0].toLowerCase(Locale.ROOT))) {
            sender.sendMessage("/hiddenmessages <on|off|status>"); return true;
        }
        if (!args[0].equalsIgnoreCase("status")) {
            getConfig().set("enabled",args[0].equalsIgnoreCase("on")); saveConfig();
        }
        sender.sendMessage("Invisible-name death message protection: "+(getConfig().getBoolean("enabled",true)?"ON":"OFF"));
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String label,String[] args) {
        if(!sender.hasPermission("hiddenmessages.admin") || args.length!=1) return List.of();
        return List.of("on","off","status").stream().filter(s->s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
    }
}


