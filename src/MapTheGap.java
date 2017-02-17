
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.pcj.Group;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 * @authors (alphabetical):
 *         - Aleksandra Kardas : ak195906@icm.edu.pl
 *         - Piotrek Konorski (PioKon) : pk32266@icm.edu.pl
 *         - Rafal Kowalczyk : rk383409@icm.edu.pl
 *         - Piotr Witkiewicz : pw392193@icm.edu.pl
 */

@RegisterStorage(MapTheGap.Shared.class)
public class MapTheGap implements StartPoint {

    static String[] cmdargs;

    @Storage(MapTheGap.class)
    enum Shared {
        Bts, BSpots, Paths, Routes, FibreLen, Pqual, Plength, BSpots_density, PointsNumber
    };
    ArrayList<Coord> Bts;
    ArrayList<Coord> BSpots;
    ArrayList<ArrayList<Coord>> Paths;
    ArrayList<ArrayList<Coord>> Routes;
    double FibreLen;
    ArrayList<Double> Pqual; 
    ArrayList<Double> Plength;
    double FibreLimit;
    ArrayList<Double> BSpots_density;
    int PointsNumber = 0;


    public static void main(String[] args) throws IOException {
        NodesDescription nd;
        cmdargs = args;
        if (true) {  // from file
            nd = new NodesDescription("nodes.txt");
        } else {
            String[] nodes = {"localhost", "localhost", "localhost", "localhost"};
            nd = new NodesDescription(nodes);
        }

        // GraphHopper routing init
        if (!true) {
//            int gh_nthreads = 1;  // doesn't really work.. (even outside of PCJ!)
//            GHrouting.InitGHopper(gh_nthreads);
        }

        PCJ.deploy(MapTheGap.class, nd);
    }

    @Override
    public void main() throws Throwable {
        
        if (PCJ.myId() == 0) {
            System.out.printf("-> MapTheGap - start.. (#threads: %d)\n", PCJ.threadCount());
        }
        
        long startTime = System.currentTimeMillis();

        int verbose = 1;  // verbose level: 0: off; 1, 2, 3: level..
        boolean BTS_chkuniq = true;
        boolean BSpots_chkuniq = true;
        boolean BTS_chkuniq_allowslow = true;
        boolean BSpots_chkuniq_allowslow = true;
        String GMaps_fname = null;
        String BTS_filename = null;
        String BTS_filename_storage = null;
        String BSpots_filename = null;
        String BSpots_filename_storage = null;
        String BSpots_filename_storage_bin = null;
        String BSpots_qhullbase = null;
        String osmFile = null;
        String graphFolder = null;

        List<Coord> BTS_locations = null;  // manually set area filter (lat, long, radius)
        List<Coord> BSpots_locations = null;  // manually set area filter (lat, long, radius)
        
        String ColorStyle = "dark";  // google map color style: "light" or "dark"

        // Run mode
        String[] runmodes = {"selected", "poland"};  // #0 is default
        String runmode = runmodes[0];
        if (cmdargs.length > 0) {
            if (Arrays.asList(runmodes).contains(cmdargs[0].toLowerCase())) {
                runmode = cmdargs[0];
            }
        }

        // Set variables according to runmode
        if (runmode.equals("selected")) {
            BTS_chkuniq = true;
            BTS_chkuniq_allowslow = true;
            BTS_filename = "./data/LTE1800_2016_12_27.csv";
            BTS_filename_storage = "Storage.BTS_selected.txt";
            BTS_locations = Arrays.asList(
                    new Coord(52.2296756, 21.012228700000037, 20000.)  // Warszawa
//                    new Coord(50.25422988230621, 19.00634765625, 100000.)  // Katowice
            );

            BSpots_chkuniq = true;
            BSpots_chkuniq_allowslow = true;
            BSpots_qhullbase = "Storage.BSPOTS_selected_Qhull";
            BSpots_filename = "./data/bp_konkurs_2_20160912_Warszawa.csv";
            BSpots_filename_storage = "Storage.BSPOTS_selected.txt";
            BSpots_filename_storage_bin = "Storage.BSPOTS_selected.bin";
            BSpots_locations = Arrays.asList(
                    new Coord(52.2296756, 21.012228700000037, 20000.)  // Warszawa
//                    new Coord(50.25422988230621, 19.00634765625, 100000.)  // Katowice
            );

            osmFile = "./maps/Mazowieckie.pbf";
            graphFolder = "./Storage.GHopper_Selected";

            GMaps_fname = "./maps/MapTheGap_Warszawa.html";

        } else if (runmode.equals("poland")) {
            BTS_chkuniq = true;
            BTS_chkuniq_allowslow = true;
            BTS_filename = "/icm/tmp/hackaton/LTE1800 - stan na 2017-01-25.csv";
            BTS_filename_storage = "Storage.BTS_Poland.txt";

            BSpots_chkuniq = true;
            BSpots_chkuniq_allowslow = false;
            BSpots_qhullbase = "Storage.BSPOTS_Poland_Qhull";
            BSpots_filename = "/icm/tmp/hackaton/bp_konkurs_2_20160912.csv";
            BSpots_filename_storage = "Storage.BSPOTS_Poland.txt";
            BSpots_filename_storage_bin = "Storage.BSPOTS_Poland.bin";
            
            osmFile = "/icm/tmp/hackaton/poland-latest.osm.pbf";
            graphFolder = "./Storage.GHopper_Poland";

            GMaps_fname = "./maps/MapTheGap.html";
        }

        // GraphHopper routing
        if (!true) {
            if (PCJ.myId() == 0) {
                int gh_nthreads = 1;  // doesn't really work.. (even outside of PCJ!)
                boolean clean_previous_graph = false;
                GHrouting.InitGHopper(osmFile, graphFolder, gh_nthreads, clean_previous_graph, verbose);
            }
            
            if (false) {  // test example
                PCJ.barrier();
                Coord p1 = new Coord(52.1633613, 21.0732169, "");
                Coord p2 = new Coord(52.2402853, 21.0320209, "");
                double distance = GHrouting.getDistance(p1, p2);
                System.out.printf("Thread: %d  - distance: %.4f\n", PCJ.myId(), distance);
            }

            PCJ.barrier();  // Unfortunately !!!!  is there PCJ.barrier(int[] array_of_threads) ?????
                           // DO IT with: Group g = PCJ.join("group_name");
        }
        
        // Blind Spots
        if (true) {
            if (!true){  // read original file
                BSpots = FileIO.ReadCSV(BSpots_filename, BSpots_filename_storage, 13, 1, "bspot", ';', '\"', BSpots_chkuniq, BSpots_chkuniq_allowslow, verbose);
            } else {  // load from storage
                BSpots = FileIO.LoadArrayTxt(BSpots_filename_storage, verbose);
                if (PCJ.myId() == 0) System.out.printf("  -> loaded: %d\n", BSpots.size());
            }
            BSpots = AuxTools.FilterArrayByLocations(BSpots, BSpots_locations, verbose);
            if (PCJ.myId() == 0) System.out.printf("  -> filtered: %d\n", BSpots.size());
            PCJ.barrier();
        }
        
        // Qhull - build triangulation for Bspots
        if (!true) {
            Qhull qhull = new Qhull(this, BSpots_filename_storage, BSpots_qhullbase);
            if (!true) {  // run triangulation
                qhull.RunTriangulation();
            }
            PCJ.barrier();
            qhull.LocalDensities();
            if (!true) {
                qhull.ReadNeighbours();
            }
        }
        
        // BSpots - load/store whole ArrayList
        if (!true) {
            if (!true) {  // store
                if (PCJ.myId() == 0) {
                    FileIO.StoreArrayBinary(BSpots_filename_storage_bin, BSpots, verbose);
                } 
            } else {  // load
                BSpots = FileIO.LoadArrayBinary(BSpots_filename_storage_bin, verbose);
            }
        }
        
        // BTS
        if (true) {
            if (!true) {  // read original file
                Bts = FileIO.ReadCSV(BTS_filename, BTS_filename_storage, 10, 1, "bts", ';', '-', BTS_chkuniq, BTS_chkuniq_allowslow, verbose);
            } else {  // load from storage
                Bts = FileIO.LoadArrayTxt(BTS_filename_storage, verbose);
                if (PCJ.myId() == 0) System.out.printf("  -> loaded: %d\n", Bts.size());
            }
            Bts = AuxTools.FilterArrayByLocations(Bts, BTS_locations, verbose);
            if (PCJ.myId() == 0) System.out.printf("  -> filtered: %d\n", Bts.size());
            PCJ.barrier();
        }
                
        // Paths
        CalcPaths calcpaths = new CalcPaths(this);
        if (true) {
            if (true) {  // Paths: calculate
                calcpaths.FindPaths_Brutal(verbose);
//                calcpaths.FindPaths_ClosestAndTSP(verbose);
//                calcpaths.FindPaths_ClosestAndArea(verbose);
                if (PCJ.myId() == 0) {
                    FileIO.StoreArraysTxt("Storage.PATH.txt", Paths, verbose);
                }
            } else {  // Paths: restore from file
                if (PCJ.myId() == 0) {
                    Paths = FileIO.LoadArraysTxt("Storage.PATH.txt", verbose);
                    System.out.printf("  -> loaded: %d\n", Paths.size());
                }
                PCJ.barrier();
            }
        }
        
        // TSP
        if (true) {
            calcpaths.Optimize_TSP();
        }
        PCJ.barrier();
        
        // Routes
        if (!true) {
            if (true) {  // Routes: calculate
                calcpaths.PrepareRoutes(verbose);
                if (PCJ.myId() == 0) {
                    FileIO.StoreArraysTxt("Storage.ROUTES.txt", Routes, verbose);
                }
            } else {  // Routes: restore from file
                if (PCJ.myId() == 0) {                    
                    Routes = FileIO.LoadArraysTxt("Storage.ROUTES.txt", verbose);
                    System.out.printf("  -> loaded: %d\n", Routes.size());
                }
                PCJ.barrier();
            }
        }
                
        // Areas
        ArrayList areas = null;
        if (!true) {
            PCJ.barrier();
            areas = new ArrayList<>();
            for (ArrayList<Coord> path : Paths) {
                areas.add(path.get(0));
            }
            Paths = null;
        }

        // Maps
        if (!true) {
            if (PCJ.myId() == 0) {
                if (runmode.equals("selected")) {
//                    GoogleMap.PrepareMap(Bts, BSpots, Paths, areas, true, true, ColorStyle, verbose);
//                    GoogleMap.PrepareMap(Bts, BSpots, Routes, areas, false, false, ColorStyle, verbose);
                    GoogleMap.PrepareMap(Bts, BSpots, Paths, areas, false, false, ColorStyle, verbose);
                } else {
                    GoogleMap.PrepareMap(null, null, Paths, areas, false, false, ColorStyle, verbose);
                }
                GoogleMap.StoreMap(GMaps_fname, verbose);
            }
        }


        
        Pqual=new ArrayList<>();
        Plength=new ArrayList<>();
        FibreLimit=500000;
        calcpaths.optimizePathSet();

//        if (PCJ.myId() == 0) {
//                System.out.println("Fibre limit: "+ this.FibreLimit+ ", used fibre: "+ this.FibreLen+ ", number of paths: "+this.Paths.size());
//            }


//czas który upłynął
        long stopTime = System.currentTimeMillis();
       long elapsedTime = stopTime - startTime;  
       
       
       
        PCJ.barrier();

        ///wpisywnie do pliku
 
     //liczba podłączonych punktów: 
    
      for(int i=0;i<PCJ.threadCount();i++){
          if(PCJ.myId()==i){
              for(int j=0;j<Paths.size();j++){
                  PointsNumber+=Paths.get(i).size();
                  
              }
          }    
      }
      PCJ.barrier();
      
      if(PCJ.myId()==0){
          for(int i=1;i<PCJ.threadCount();i++){
         PointsNumber+=(int)PCJ.get(i,MapTheGap.Shared.PointsNumber);
          }
              
      }
      
      
      if(PCJ.myId()==0){
        try{
        PrintWriter writer = new PrintWriter("out.txt", "UTF-8");
        writer.println("ICM kdmszk30");
        writer.println("http:// ...");
        writer.println(PointsNumber);
        writer.println((double)elapsedTime/1000);
        
        //liczba wezłów i watków
        int watki =PCJ.threadCount();
        double wezly=watki/48;
        Math.floor(wezly);
        wezly+=watki%48;
        writer.println(wezly+" "+watki);
        
        writer.close();
    } catch (IOException e) {
       System.out.println("Wyjątek!! wpisywanie do pliku");
    }
      }

        PCJ.barrier();
        if (PCJ.myId() == 0) {
            System.out.println("-> MapTheGap - finish.");
        }
    }
        
}


