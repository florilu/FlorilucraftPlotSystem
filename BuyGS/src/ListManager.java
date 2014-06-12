import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.domains.Domain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sun.java_cup.internal.runtime.virtual_parse_stack;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Florian on 27.05.14.
 */
public class ListManager {
    private ArrayList<ProtectedRegion> plots = new ArrayList<>();
    private ArrayList<String> plotIDs = new ArrayList<>();

    private String memberList = "";
    private String vipList = "";
    private String skyscraperList = "";
    private String bigList = "";

    public ListManager(){

    }

    public void addEntry(ProtectedRegion entry){
        plots.add(entry);
    }

    public void runPlotIDIdentification(){
        for(int i = 0; i < plots.size(); i++){
            DefaultDomain plotDomain = plots.get(i).getOwners();
            if(plotDomain.size() < 1){
                plotIDs.add("§a" + plots.get(i).getId() + "§f");
            }else{
                plotIDs.add("§4" + plots.get(i).getId() + "§f");
            }
        }
        Collections.sort(plotIDs);
    }

    public String getMemberList(){
        for(int i = 0; i < plots.size(); i++){
            if(plotIDs.get(i).charAt(2) == 'm' && !isLetter(plotIDs.get(i), 3)){
                memberList = memberList + plotIDs.get(i) + ", ";
            }
        }
        System.out.println(memberList);

        return memberList;
    }

    public String getSkyscraperList(){
        for(int i = 0; i < plotIDs.size(); i++){
            if(plotIDs.get(i).charAt(2) == 'h' && !isLetter(plotIDs.get(i), 3)){
                skyscraperList = skyscraperList + plotIDs.get(i) + ", ";
            }
        }
        return skyscraperList;
    }

    public String getVipList(){
        for(int i = 0; i < plotIDs.size(); i++){
            if(plotIDs.get(i).length() > 3){
                if(plotIDs.get(i).charAt(2) == 'v' && plotIDs.get(i).charAt(3) == 'i' && plotIDs.get(i).charAt(4) == 'p' && !isLetter(plotIDs.get(i), 5)){
                    vipList = vipList + plotIDs.get(i) + ", ";
                }
            }
        }
        return vipList;
    }

    public String getBigList(){
        for(int i = 0; i < plotIDs.size(); i++){
            bigList = bigList + plotIDs.get(i) + ", ";
        }
        return bigList;
    }

    private boolean isLetter(String ID, int lastChar){
        return Character.isLetter(ID.charAt(lastChar + 1));
    }
}
