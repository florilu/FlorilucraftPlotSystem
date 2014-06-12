/**
 * Created by Florian on 03.06.14.
 */
public class Plot {
    private String plotType = null;
    private String plotCost = null;
    private String permission = null;

    public Plot(String plotType, String plotCost, String permission){
        this.plotType = plotType;
        this.plotCost = plotCost;
        this.permission = permission;
    }

    public String getPlotType(){
        return plotType;
    }

    public int getPlotCost(){
        return Integer.valueOf(plotCost);
    }

    public String getPermission(){
        return permission;
    }
}
