import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.minecraft.server.v1_7_R1.World;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import util.Utils;

import java.io.File;
import java.util.*;

/**
 * Created by Florian on 27.05.14.
 */
public class BuyGS extends JavaPlugin {

    private static final String PLUGIN_NAME = "Florilucraft Plot System";

    private Economy econ = null;

    private FileConfiguration config = null;

    private File configFile;
    private File plotNames;

    public void onEnable(){
        setupEconomy();
        this.getLogger().info(PLUGIN_NAME + " (FCPS) started!");

        this.configFile = new File(getDataFolder(), "config.yml");
        this.plotNames = new File(getDataFolder(), "plotNames.txt");

        if(!configFile.exists()){
            configFile.getParentFile().mkdirs();
            Utils.copy(getResource("config.yml"), configFile);
        }
        if(!plotNames.exists()){
            plotNames.getParentFile().mkdirs();
            Utils.copy(getResource("plotNames.txt"), plotNames);
        }

        config = new YamlConfiguration();
        loadYamls();
    }

    public void onDisable(){
        this.getLogger().info(PLUGIN_NAME + " (FCPS) stopped!");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
        ListManager listManager = new ListManager();
        Player player = (Bukkit.getServer().getPlayer(sender.getName()));
        if(cmd.getName().equalsIgnoreCase("listgs")){
            if(args.length > 1){

                for(int i = 0; i < this.config.getList("plots").size(); i++){
                    sender.sendMessage(this.config.getList("plots").get(i).toString());
                }

                sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f zu viele Argumente!");
                return true;
            }else{
                Map<String, ProtectedRegion> map = getWorldGuard().getRegionManager(player.getWorld()).getRegions();
                ArrayList<String> plotList = new ArrayList<>();
                String list = "";
                for(Map.Entry<String, ProtectedRegion> entry : map.entrySet()){
                    String key = entry.getKey();
                    ProtectedRegion region = entry.getValue();
                    listManager.addEntry(region);
                }

                listManager.runPlotIDIdentification();

                if(sender.hasPermission("fcgsbuy.member") && args[0].equalsIgnoreCase("member")){
                    sender.sendMessage("§6[§2" + PLUGIN_NAME +"§6]§f " + listManager.getMemberList());
                }else if(sender.hasPermission("fcgsbuy.member") && args[0].equalsIgnoreCase("hochhaus")){
                    sender.sendMessage("§6[§2" + PLUGIN_NAME +"§6]§f " + listManager.getSkyscraperList());
                }else if(sender.hasPermission("fcgsbuy.vip") && args[0].equalsIgnoreCase("vip")){
                    sender.sendMessage("§6[§2 " + PLUGIN_NAME + "§6]§f " + listManager.getVipList());
                }else if(sender.hasPermission("fcgsbuy.admin") && args[0].equalsIgnoreCase("all")){
                    sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f " + listManager.getBigList());
                }else{
                    sender.sendMessage("Du hast keine Rechte um auf diesen Command zuzugreifen!");
                }
                return true;
            }
        }
        if(cmd.getName().equalsIgnoreCase("buygs")){
            String plotID = null;
            DefaultDomain domain = new DefaultDomain();
            domain.addPlayer(player.getName());
            if(args.length < 1){
                ProtectedRegion plot = isStandingOnPlot(sender);
                if(plot != null){
                    plotID = plot.getId();
                }else{
                    sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du stehst auf keinem GS oder auf einem nicht Gültigem!");
                    return false;
                }
                //sender.sendMessage("§6[§2Florilucraft Plot System§6]§f zu wenige Argumente!");
            }
            if(args.length == 1){
                plotID = args[0];
            }
            if(args.length > 1){
                sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f zu viele Argumente!");
                return false;
            }else {
                if(!alreadyHasPlot(sender)){
                    if(!hasOwner(plotID, sender)){
                        if(checkPlotType(plotID).equalsIgnoreCase("member")){
                            if(sender.hasPermission("fcgsbuy.member")){
                                getWorldGuard().getRegionManager(player.getWorld()).getRegion(plotID).setOwners(domain);
                                sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Dir wurde erfolgreich das GS " + plotID + " zugewiesen!");
                            }else{
                                sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du hast keine Berechtigung ein Member-GS zu holen!");
                                return true;
                            }
                        }else if(checkPlotType(plotID).equalsIgnoreCase("vip")){
                            if(sender.hasPermission("fcgsbuy.vip")){
                                if(econ.getBalance(player.getName()) >= 1000){
                                    EconomyResponse r = econ.withdrawPlayer(player.getName(), 1000);
                                    if(r.transactionSuccess()){
                                        getWorldGuard().getRegionManager(player.getWorld()).getRegion(plotID).setOwners(domain);
                                        sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du hast dir erfolgreich das GS " + plotID + " gekauft!");
                                    }else{
                                        sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Leider ist beim Kauf etwas schief gelaufen! " + r.errorMessage);
                                        return true;
                                    }
                                }else{
                                    sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du hast nicht genügend Geld um dir das GS zu kaufen, das GS kostet 1000 Euro!");
                                    return true;
                                }
                            }else{
                                sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du hast keine Berechtigung ein VIP-GS zu kaufen!");
                                return true;
                            }
                        }else{
                            sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du hast versucht ein nicht zur Verfuegung stehendes GS zu kaufen!");
                            return true;
                        }
                    }else{
                        sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Das Grundstueck gehoert bereits jemanden!");
                        return true;
                    }
                }else{
                    if(checkPlotType(plotID).equalsIgnoreCase("hochhaus") && canHaveSkyscraperPlot(sender)){
                        if(sender.hasPermission("fcgsbuy.member")){
                            if(econ.getBalance(player.getName()) >= 2000){
                                EconomyResponse r = econ.withdrawPlayer(player.getName(), 2000);
                                if(r.transactionSuccess()){
                                    getWorldGuard().getRegionManager(player.getWorld()).getRegion(plotID).setOwners(domain);
                                    saveWorld(player);
                                    sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du hast dir erfolgreich das GS " + plotID + " gekauft!");
                                }else{
                                    sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Leider ist beim Kauf etwas schief gelaufen!" + r.errorMessage);
                                    return true;
                                }
                            }else{
                                sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du hast nicht genügend Geld um dir das GS zu kaufen, das GS kostet 2000 Euro!");
                                return true;
                            }
                        }else{
                            sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du hast keine Berechtigung ein Hochhaus-GS zu kaufen!");
                            return true;
                        }
                    }else{
                        sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du kannst dir kein weiteres GS holen da du bereits eins hast!");
                    }
                    return true;
                }
            }
            saveWorld(player);
        }
        if(cmd.getName().equalsIgnoreCase("sellgs")){
            if(player.hasPermission("fcgsbuy.member")){
                ProtectedRegion plot = lookForOwnedPlot(sender);
                if(plot != null){
                    if(checkPlotType(plot.getId()).equals("member")){
                        sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du hast erfolgreich dein Member GS abgegeben!");
                    }else if(checkPlotType(plot.getId()).equals("vip")){
                        EconomyResponse r = econ.depositPlayer(player.getName(), 1000);
                        if(r.transactionSuccess()){
                            sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du hast erfolgreich dein VIP GS für 1000 Euro verkauft!");
                        }else{
                            sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Etwas ist schief gelaufen! Dein GS wurde nicht verkauft!");
                            return true;
                        }
                    }else if(checkPlotType(plot.getId()).equals("hochhaus")){
                        EconomyResponse r = econ.depositPlayer(player.getName(), 2000);
                        if(r.transactionSuccess()){
                            sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du hast erfolgreich dein Hochhaus-GS für 2000 Euro verkauft!");
                        }else{
                            sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Etwas ist schief gelaufen! Dein GS wurde nicht verkauft!");
                            return true;
                        }
                    }else{
                        sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Etwas ist schief gelaufen!");
                        return true;
                    }
                    plot.getOwners().removaAll();
                    try{
                        getWorldGuard().getRegionManager(player.getWorld()).save();
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally{
                        return true;
                    }
                }else{
                    sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du besitzt kein Grundstück!");
                    return true;
                }
            }else{
                sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du hast keine Berechtigung, um auf diesen Command zuzugreifen!");
            }
        }
        if(cmd.getName().equalsIgnoreCase("removeowner")){
            if(player.hasPermission("fcgsbuy.admin")){
                if(args.length != 1){
                    return false;
                }else{
                    String plotID = args[0];
                    getWorldGuard().getRegionManager(player.getWorld()).getRegion(plotID).getOwners().removaAll();
                    try{
                        getWorldGuard().getRegionManager(player.getWorld()).save();
                        sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Alle Besitzer vom Grundstück " + args[0] + " wurden entfernt!");
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally{
                        return true;
                    }
                }
            }
        }
        if(cmd.getName().equalsIgnoreCase("adduser")){
            if(player.hasPermission("fcgsbuy.member")){
                if(args.length != 1){
                    sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Nicht genügend oder zu viele Argumente!");
                    return false;
                }else{
                    Player other = (Bukkit.getPlayer(args[0]));
                    if(other != null){
                        if(other.isOnline()){
                            ProtectedRegion plot = lookForOwnedPlot(sender);
                            DefaultDomain domain = plot.getOwners();
                            domain.addPlayer(args[0]);
                            getWorldGuard().getRegionManager(player.getWorld()).getRegion(plot.getId()).setOwners(domain);
                            try{
                                getWorldGuard().getRegionManager(player.getWorld()).save();
                            }catch (Exception e){
                                e.printStackTrace();
                            }finally {
                                sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Spieler " + args[0] + " wurde erfolgreich zu deinem GS " + plot.getId() + " hinzugefügt!");
                                return true;
                            }
                        }else{
                            sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Der Spieler ist nicht online!");
                            return true;
                        }
                    }else{
                        sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Spieler " + args[0] + " existiert nicht!");
                        return true;
                    }
                }
            }
        }
        if(cmd.getName().equalsIgnoreCase("removeuser")){
            if(player.hasPermission("fcgsbuy.member")){
                if(args.length != 1){
                    sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Nicht genügend oder zu viele Argumente!");
                    return false;
                }else{
                    Player other = Bukkit.getPlayer(args[0]);
                    if(other != null){
                        if(other.isOnline()){
                            ProtectedRegion plot = lookForOwnedPlot(sender);
                            DefaultDomain domain = plot.getOwners();
                            domain.removePlayer(args[0]);
                            getWorldGuard().getRegionManager(player.getWorld()).getRegion(plot.getId()).setOwners(domain);
                            try{
                                getWorldGuard().getRegionManager(player.getWorld()).save();
                            }catch (Exception e){
                                e.printStackTrace();
                            }finally{
                                sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Spieler " + args[0] + " kann nun nicht mehr auf deinem GS " + plot.getId() + " bauen!");
                                return true;
                            }
                        }else{
                            sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Der Spieler " + args[0] + " ist nicht online!");
                            return true;
                        }
                    }else{
                        sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Der Spieler " + args[0] + " existiert nicht!");
                        return true;
                    }
                }
            }else{
                sender.sendMessage("§6[§2" + PLUGIN_NAME + "§6]§f Du hast keine Berechtigung, um diesen Command auszuführen!");
            }
        }
        return false;
    }

    private WorldGuardPlugin getWorldGuard(){
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if(plugin == null || !(plugin instanceof WorldGuardPlugin)){
            return null;
        }
        return (WorldGuardPlugin) plugin;
    }

    private boolean alreadyHasPlot(CommandSender sender){
        Player player = (Bukkit.getServer().getPlayer(sender.getName()));
        Map<String, ProtectedRegion> map = getWorldGuard().getRegionManager(player.getWorld()).getRegions();
        ArrayList<ProtectedRegion> plotList = new ArrayList<>();
        String list = "";
        for(Map.Entry<String, ProtectedRegion> entry : map.entrySet()){
            String key = entry.getKey();
            ProtectedRegion region = entry.getValue();
            plotList.add(region);
        }

        boolean bool = false;

        ArrayList<ProtectedRegion> ownedPlots = new ArrayList<>();

        for(int i = 0; i < plotList.size(); i++){
            if(plotList.get(i).getOwners().contains(player.getName())){
                ownedPlots.add(plotList.get(i));
            }
        }

        if(ownedPlots.size() > 0){
            bool = true;
        }

        return bool;
    }

    private boolean hasOwner(String plotID, CommandSender sender){
        ArrayList<ProtectedRegion> plotList = getPlotList(sender);
        ProtectedRegion plot = null;
        for(int i = 0; i < plotList.size(); i++){
            if(plotList.get(i).getId().equalsIgnoreCase(plotID)){
                plot = plotList.get(i);
            }
        }
        if(plot.getOwners().size() > 0){
            return true;
        }else{
            return false;
        }
    }

    public ArrayList<ProtectedRegion> getPlotList(CommandSender sender) {
        Player player = (Bukkit.getServer().getPlayer(sender.getName()));
        Map<String, ProtectedRegion> map = getWorldGuard().getRegionManager(player.getWorld()).getRegions();
        ArrayList<ProtectedRegion> plotList = new ArrayList<>();
        String list = "";
        for(Map.Entry<String, ProtectedRegion> entry : map.entrySet()){
            String key = entry.getKey();
            ProtectedRegion region = entry.getValue();
            plotList.add(region);
        }
        return plotList;
    }

    private String checkPlotType(String plotID){
        if(plotID.charAt(0) == 'm' && !Character.isLetter(plotID.charAt(1))){
            return "member";
        }else if(plotID.charAt(0) == 'h' && !Character.isLetter(plotID.charAt(1))){
            return "hochhaus";
        }else if(plotID.charAt(0) == 'v' && plotID.charAt(1) == 'i' && plotID.charAt(2) == 'p' && !Character.isLetter(plotID.charAt(3))){
            return "vip";
        }else{
            return null;
        }
    }

    private boolean setupEconomy(){
        if(getServer().getPluginManager().getPlugin("Vault") == null){
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if(rsp == null){
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private ProtectedRegion lookForOwnedPlot(CommandSender sender){
        Player player = (Bukkit.getServer().getPlayer(sender.getName()));
        ArrayList<ProtectedRegion> plotList = getPlotList(sender);
        ProtectedRegion ownedPlot = null;
        for(int i = 0; i < plotList.size(); i++){
            if(plotList.get(i).getOwners().contains(player.getName())){
                ownedPlot = plotList.get(i);
                break;
            }
        }
        return ownedPlot;
    }

    private boolean canHaveSkyscraperPlot(CommandSender sender){
        Player player = Bukkit.getServer().getPlayer(sender.getName());
        ArrayList<ProtectedRegion> plotList = getPlotList(sender);
        ArrayList<ProtectedRegion> ownedPlots = new ArrayList<>();
        for(int i = 0; i < plotList.size(); i++){
            if(plotList.get(i).getOwners().contains(player.getName())){
                ownedPlots.add(plotList.get(i));
            }
        }
        int memberPlot = 0, skyscraperPlot = 0, vipPlot = 0;
        for(int i = 0; i < ownedPlots.size(); i++){
            if(checkPlotType(ownedPlots.get(i).getId()).equals("member")){
                memberPlot += 1;
            }else if(checkPlotType(ownedPlots.get(i).getId()).equals("hochhaus")){
                skyscraperPlot += 1;
            }else if(checkPlotType(ownedPlots.get(i).getId()).equals("vip")){
                vipPlot += 1;
            }else{
                ;
            }
        }
        if(!(memberPlot > 1 || vipPlot > 1) && !(skyscraperPlot > 0)){
            return true;
        }else{
            return false;
        }
    }

    private ProtectedRegion isStandingOnPlot(CommandSender sender){
        Player player = Bukkit.getServer().getPlayer(sender.getName());
        ArrayList<ProtectedRegion> plotList = getPlotList(sender);
        ArrayList<ProtectedRegion> isStandingInPlots = new ArrayList<>();
        ProtectedRegion temporaryPlot = null;
        for(int i = 0; i < plotList.size(); i++){
            if(plotList.get(i).contains((int)player.getLocation().getX(), (int)player.getLocation().getY(), (int)player.getLocation().getZ())){
                isStandingInPlots.add(plotList.get(i));
            }
        }
        for(ProtectedRegion region : plotList){
            sender.sendMessage(region.getId());
            if((checkPlotType(region.getId()).equals("member")) || (checkPlotType(region.getId()).equalsIgnoreCase("vip")) || (checkPlotType(region.getId()).equals("hochhaus"))){
                return region;
            }else{
                return null;
            }
        }
        return null;
    }

    private boolean saveWorld(Player player){
        try{
            getWorldGuard().getRegionManager(player.getWorld()).save();
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            return true;
        }
    }

    private void saveYamls(){
        try{
            config.save(configFile);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void loadYamls(){
        try{
            config.load(configFile);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
