
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;
import java.util.ArrayList;
import java.util.Locale;

public class GHrouting {
    private static GraphHopper hopper = null;
    private static String osmFile = null;
    private static String graphFolder = null;
    private static int nthreads = 1;
    
    public static void main(String[] args) {
//        String osmFile = "./maps/Mazowieckie.pbf";
//        String graphFolder = "./Storage.GHopper";  // where to store graphhopper files?
//        InitGHopper(osmFile, graphFolder, 1);
//        ExampleRouting(osmFile, graphFolder, 1);

    }
    
    public static void ConvertOSM() {
        
    }
    
    public static void InitGHopper(String osmFile, String graphFolder,
            int nthreads, boolean rmprevious, int verbose) {
        
        // check if map exists
        if (!AuxTools.FileExists(osmFile)) {
            System.out.printf("Map: %s not found!\n", osmFile);
            return;
        }
        
        // remove previous graphFolder
        if (rmprevious) {
            AuxTools.DeleteFolder(graphFolder);
        }
        
        GHrouting.osmFile = osmFile;
        GHrouting.graphFolder = graphFolder;
        GHrouting.nthreads = nthreads;
        
        if (verbose > 0) {
            if (AuxTools.FileExists(graphFolder)) {
                System.out.println("* Loading graph prepared for: " + osmFile);
                System.out.println("  * Graph stored in: " + graphFolder);
            } else {
                System.out.println("* Creating GraphHopper graph from: " + osmFile);
                System.out.println("  * Storing graph in: " + graphFolder);
            }
        }

        // create one GraphHopper instance
        hopper = new GraphHopperOSM().forServer(); //.forDesktop();
        hopper.setWorkerThreads(nthreads);
        hopper.setDataReaderFile(osmFile);
        hopper.setGraphHopperLocation(graphFolder);
        hopper.setEncodingManager(new EncodingManager("car"));  // <<<-------  check what we can set to optimize.. eg turn off instructions etc.. (on these settings depends how long it will take to load graph)
        hopper.importOrLoad();
    }
    
    public static double getDistance(Coord p1, Coord p2) {
        GHPoint startPoint = new GHPoint((Double) p1.x(), (Double) p1.y());
        GHPoint endPoint = new GHPoint((Double) p2.x(), (Double) p2.y());
        
        GHRequest req = new GHRequest(startPoint, endPoint)
                .setWeighting("fastest")
                .setVehicle("car")
                .setLocale(Locale.ROOT);
        GHResponse rsp = hopper.route(req);

        if (rsp.hasErrors()) {  // first check for errors
            System.out.println("Errors: " + rsp.getErrors());
            return -1.;
        }

        // results
        PathWrapper path = rsp.getBest();  // use the best path, see the GHResponse class for more possibilities.
        return path.getDistance();
    }
    
    public static ArrayList<Coord> getPath(Coord p1, Coord p2) {
        ArrayList<Coord> coordpath = new ArrayList<>();
        
        GHPoint startPoint = new GHPoint((Double) p1.x(), (Double) p1.y());
        GHPoint endPoint = new GHPoint((Double) p2.x(), (Double) p2.y());
        
        GHRequest req = new GHRequest(startPoint, endPoint)
                .setWeighting("fastest")
                .setVehicle("car")
                .setLocale(Locale.ROOT);
        GHResponse rsp = hopper.route(req);

        if (rsp.hasErrors()) {  // first check for errors
            System.out.println("!" + rsp.getErrors() + " : (" + startPoint.lat + ",  " + startPoint.lon + ") --> (" + endPoint.lat + ", " + endPoint.lon + ")");
            coordpath.add(p1);
            coordpath.add(p2);
            return coordpath;
        }

        // results
        PathWrapper path = rsp.getBest();  // use the best path, see the GHResponse class for more possibilities.        
        PointList pointList = path.getPoints();
        for (GHPoint3D p : pointList) {
            coordpath.add(new Coord(p.lat, p.lon, ""));
        }
        
        return coordpath;
//        return null; // DEV
    }


}
