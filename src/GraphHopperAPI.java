
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GraphHopperAPI {
    boolean offlineMode;
    boolean inputLatLong;
    
    double distanceInMeters;
    double timeInMs;
    
    double minLat;
    double minLon;
    double maxLat;
    double maxLon;
    
    GHResponse rsp;
    GraphHopper hopper;
    public GraphHopperAPI(File file) {
        
        // create singleton
        hopper = new GraphHopper().forServer();
        hopper.setOSMFile(file.getName());
        
        // where to store graphhopper files?
        hopper.setGraphHopperLocation(".");
        hopper.setEncodingManager(new EncodingManager("car"));

        // now this can take minutes if it imports or a few seconds for loading
        // of course this is dependent on the area you import
        hopper.importOrLoad();
        
        minLat = hopper.getGraphHopperStorage().getBounds().minLat;
        minLon = hopper.getGraphHopperStorage().getBounds().minLon;
        maxLat = hopper.getGraphHopperStorage().getBounds().maxLat;
        maxLon = hopper.getGraphHopperStorage().getBounds().maxLon;
    }
    
    public void setRequest(double initialLat, double initialLong, double finalLat, double finalLong, String vehicle) {
        // simple configuration of the request object, see the GraphHopperServlet classs for more possibilities.
        GHRequest req = new GHRequest(initialLat, initialLong, finalLat, finalLong).
            setWeighting("fastest").
            setVehicle(vehicle);
            //setLocale(Locale.US);
            
        rsp = hopper.route(req);
    }
    
    public void calculate() {
        // first check for errors
        if(rsp.hasErrors()) {
           // handle them!
           System.out.println(rsp.getErrors());
           return;
        }
        
        // points, distance in meters and time in millis of the full path
        PointList pointList = rsp.getPoints();
        double distance = rsp.getDistance();
        long time = rsp.getTime();
        
        distanceInMeters = distance;
        timeInMs = time;
        
        System.out.println(distanceInMeters);
        System.out.println(minLat);
        System.out.println(minLon);
        System.out.println(maxLat);
        System.out.println(maxLon);
        
        InstructionList il = rsp.getInstructions();
        // iterate over every turn instruction
        for(Instruction instruction : il) {
           instruction.getDistance();
        }

        // or get the json
        List<Map<String, Object>> iList = il.createJson();

        // or get the result as gpx entries:
        List<GPXEntry> list = il.createGPXList();
    }
}
