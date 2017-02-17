
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.pcj.PCJ;

/**
 * Handle connections and paths
 */
public class CalcPaths {

    final private MapTheGap MtG;

    public CalcPaths(MapTheGap mtg) {
        this.MtG = mtg;
    }

    public void FindPaths_Brutal(int verbose) {
        // very brute (but technically ie formally correct!) solution..
        // formally: each BTS has max 2 connections, no path splitting

        int ConnectsPerBTS = 2;  // number of allowed connections per BTS

        ArrayList<Coord> bspots;
        ArrayList<Coord> path;
        ArrayList<ArrayList<Coord>> paths;
        ArrayList<Integer> bts_ind = new ArrayList<>();
        int[] workload;
        int end_indx;

        // #0: get workload: split blindspots and distribute workload
        if (PCJ.myId() == 0) {
            if (verbose > 0) {
                System.out.println("* Finding paths - method: brutal");
            }
            end_indx = MtG.BSpots.size();
            workload = AuxTools.SplitWorkload(end_indx, verbose);

            for (int i = PCJ.threadCount() - 1; i > 0; i--) {
                int start_indx = end_indx - workload[i];
                if (start_indx < 0) {
                    start_indx = 0;
                }
                PCJ.put(new ArrayList(MtG.BSpots.subList(start_indx, end_indx)), i, MapTheGap.Shared.BSpots);
                end_indx = start_indx;
            }
            bspots = new ArrayList(MtG.BSpots.subList(0, end_indx));
        } else {  // other threads
            PCJ.monitor(MapTheGap.Shared.BSpots);
            PCJ.waitFor(MapTheGap.Shared.BSpots);
            bspots = MtG.BSpots;
        }

        // #all: for each blindspot find closest BTS
        MtG.Paths = new ArrayList<>(); // [ [coord(inf: "BTS"), blindspot, blindspot, ..], .. ] 
        for (Coord bspot : bspots) {
            Coord indist = AuxTools.FindClosestCoord2Array(MtG.Bts, bspot);
            int min_indx = (int) indist.x();
            if (bts_ind.contains(min_indx)) {
                int indx = bts_ind.indexOf(min_indx);
                MtG.Paths.get(indx).add(bspot);
            } else {
                bts_ind.add(min_indx);
                ArrayList<Coord> tmp = new ArrayList<>();
                tmp.add(new Coord(null, null, min_indx));
                tmp.add(bspot);
                MtG.Paths.add(tmp);
            }
        }
        PCJ.barrier();

        // #0: gather blindspots assigned to closest BTS, split workload by number od blindspots per BTS
        if (PCJ.myId() == 0) {
            bts_ind = new ArrayList<>();
            for (ArrayList<Coord> ipath : MtG.Paths) {
                bts_ind.add((int) ipath.get(0).inf());
            }

            for (int i = 1; i < PCJ.threadCount(); i++) {
                paths = (ArrayList) PCJ.get(i, MapTheGap.Shared.Paths);
                for (ArrayList<Coord> ipath : paths) {
                    int indx = (int) ipath.get(0).inf();
                    if (bts_ind.contains(indx)) {
                        MtG.Paths.get(bts_ind.indexOf(indx)).addAll(ipath.subList(1, ipath.size()));
                    } else {
                        bts_ind.add(indx);
                        MtG.Paths.add(ipath);
                    }
                }
            }
            workload = AuxTools.SplitWorkloadArray(MtG.Paths, verbose);
            end_indx = MtG.Paths.size();
            for (int i = PCJ.threadCount() - 1; i > 0; i--) {
                int start_indx = end_indx - workload[i];
                if (start_indx < 0) {
                    start_indx = 0;
                }
                PCJ.put(new ArrayList(MtG.Paths.subList(start_indx, end_indx)), i, MapTheGap.Shared.Paths);
                end_indx = start_indx;
            }
            MtG.Paths.subList(end_indx, MtG.Paths.size()).clear();
        } else {
            PCJ.monitor(MapTheGap.Shared.Paths);
            PCJ.waitFor(MapTheGap.Shared.Paths);
        }

        // #all: prepare final paths, make sure each BTS has maximum of <ConnectsPerBTS> connections
        paths = new ArrayList<>();
        MtG.FibreLen = 0.;
        for (int i = 0; i < MtG.Paths.size(); i++) {
            path = new ArrayList(MtG.Paths.get(i));
            ArrayList<ArrayList<Coord>> pathset = new ArrayList<>();
            for (int j = 0; j < ConnectsPerBTS; j++) {
                pathset.add(new ArrayList<>());
            }
            for (int j = 0; j < path.size(); j++) {
                if (j == 0) {
                    for (int k = 0; k < ConnectsPerBTS; k++) {
                        pathset.get(k).add(MtG.Bts.get((int) path.get(0).inf()));
                    }
                } else {
                    ArrayList<Coord> last_itm = new ArrayList<>();
                    for (int k = 0; k < ConnectsPerBTS; k++) {
                        last_itm.add(pathset.get(k).get(pathset.get(k).size() - 1));
                    }
                    Coord indist = AuxTools.FindClosestCoord2Array(last_itm, path.get(j));
                    pathset.get((int) indist.x()).add(path.get(j));
                    MtG.FibreLen += (double) indist.y();
                }
            }
            for (int j = 0; j < ConnectsPerBTS; j++) {
                if (pathset.get(j).size() > 1) {
                    paths.add(pathset.get(j));
                }
            }
        }
        MtG.Paths = paths;
        PCJ.barrier();

        // #0: gather all paths and fibre lengths
        if (PCJ.myId() == 0) {
            for (int i = 1; i < PCJ.threadCount(); i++) {
//                MtG.Paths.addAll((ArrayList) PCJ.get(i, MapTheGap.Shared.Paths));  // suggested by Rafal
                MtG.FibreLen += (double) PCJ.get(i, MapTheGap.Shared.FibreLen);
            }
            if (verbose > 0) {
                System.out.printf("  ---> Total fibre length: %.3f km\n", MtG.FibreLen / 1000.);
            }
        }
    }

    public void FindPaths_ClosestAndTSP(int verbose) {
        // find closest BTS + TSP on each path..
        // formally: each BTS has max 2 connections, no path splitting

        ArrayList<Coord> bspots;
        ArrayList<Coord> path;
        ArrayList<ArrayList<Coord>> paths;
        ArrayList<Integer> bts_ind = new ArrayList<>();
        int[] workload;
        int end_indx;

        // #0: get workload: split blindspots and distribute workload
        if (PCJ.myId() == 0) {
            if (verbose > 0) {
                System.out.println("* Finding paths - method: closest+TSP");
            }
            end_indx = MtG.BSpots.size();
            workload = AuxTools.SplitWorkload(end_indx, verbose);

            for (int i = PCJ.threadCount() - 1; i > 0; i--) {
                int start_indx = end_indx - workload[i];
                if (start_indx < 0) {
                    start_indx = 0;
                }
                PCJ.put(new ArrayList(MtG.BSpots.subList(start_indx, end_indx)), i, MapTheGap.Shared.BSpots);
                end_indx = start_indx;
            }
            bspots = new ArrayList(MtG.BSpots.subList(0, end_indx));
        } else {  // other threads
            PCJ.monitor(MapTheGap.Shared.BSpots);
            PCJ.waitFor(MapTheGap.Shared.BSpots);
            bspots = MtG.BSpots;
        }

        // #all: for each blindspot find closest BTS
        MtG.Paths = new ArrayList<>(); // [ [coord(inf: "BTS"), blindspot, blindspot, ..], .. ] 
        for (Coord bspot : bspots) {
            Coord indist = AuxTools.FindClosestCoord2Array(MtG.Bts, bspot);
            int min_indx = (int) indist.x();
            double min_indx_dist = (double) indist.y();
            bspot.Set(null, null, min_indx_dist);
            if (bts_ind.contains(min_indx)) {
                int indx = bts_ind.indexOf(min_indx);
                MtG.Paths.get(indx).add(bspot);
            } else {
                bts_ind.add(min_indx);
                ArrayList<Coord> tmp = new ArrayList<>();
                Coord closest_bts = MtG.Bts.get(min_indx);
                closest_bts.Set(null, null, min_indx);
                tmp.add(closest_bts);
                tmp.add(bspot);
                MtG.Paths.add(tmp);
            }
        }
        PCJ.barrier();

        // #0: gather blindspots assigned to closest BTS, split workload by number od blindspots per BTS
        if (PCJ.myId() == 0) {
            bts_ind = new ArrayList<>();
            for (ArrayList<Coord> ipath : MtG.Paths) {
                bts_ind.add((int) ipath.get(0).inf());
            }

            for (int i = 1; i < PCJ.threadCount(); i++) {
                paths = (ArrayList) PCJ.get(i, MapTheGap.Shared.Paths);
                for (ArrayList<Coord> ipath : paths) {
                    int indx = (int) ipath.get(0).inf();
                    if (bts_ind.contains(indx)) {
                        MtG.Paths.get(bts_ind.indexOf(indx)).addAll(ipath.subList(1, ipath.size()));
                    } else {
                        bts_ind.add(indx);
                        MtG.Paths.add(ipath);
                    }
                }
            }
            workload = AuxTools.SplitWorkloadArray(MtG.Paths, verbose);
            end_indx = MtG.Paths.size();
            for (int i = PCJ.threadCount() - 1; i > 0; i--) {
                int start_indx = end_indx - workload[i];
                if (start_indx < 0) {
                    start_indx = 0;
                }
                PCJ.put(new ArrayList(MtG.Paths.subList(start_indx, end_indx)), i, MapTheGap.Shared.Paths);
                end_indx = start_indx;
            }
            MtG.Paths.subList(end_indx, MtG.Paths.size()).clear();
        } else {
            PCJ.monitor(MapTheGap.Shared.Paths);
            PCJ.waitFor(MapTheGap.Shared.Paths);
        }

        // #all: prepare final paths - run TSP solver
        paths = new ArrayList<>();
        MtG.FibreLen = 0.;
        for (int i = 0; i < MtG.Paths.size(); i++) {
            path = new ArrayList(MtG.Paths.get(i));

            // TSP
            if (path.size() > 2) {
                path = AuxTools.SolveTSPsimple(path, path.size() * path.size(), 10000);
            } else {
                path.get(0).Set(null, null, AuxTools.distanceCoord(path.get(0), path.get(1)));
            }

            if (verbose > 1) {
                double dist0 = 0.;
                for (int ii = 0; ii < MtG.Paths.get(i).size() - 1; ii++) {
                    dist0 += AuxTools.distanceCoord(MtG.Paths.get(i).get(ii), MtG.Paths.get(i).get(ii + 1));
                }
                double dist1 = 0.;
                for (int ii = 0; ii < path.size() - 1; ii++) {
                    dist1 += AuxTools.distanceCoord(path.get(ii), path.get(ii + 1));
                }
                System.out.printf(">> %d |  %d/%d  %.4f -> %.4f\n", PCJ.myId(), i, MtG.Paths.size(), dist0, dist1);
            }

            MtG.FibreLen += (double) path.get(0).inf();
            paths.add(path);
        }
        MtG.Paths = paths;
        PCJ.barrier();

        // #0: gather all paths and fibre lengths
        if (PCJ.myId() == 0) {
            for (int i = 1; i < PCJ.threadCount(); i++) {
                MtG.Paths.addAll((ArrayList) PCJ.get(i, MapTheGap.Shared.Paths));
                MtG.FibreLen += (double) PCJ.get(i, MapTheGap.Shared.FibreLen);
            }
            if (verbose > 0) {
                System.out.printf("  ---> Total fibre length: %.3f km\n", MtG.FibreLen / 1000.);
            }
        }
    }

    public void FindPaths_ClosestAndArea(int verbose) {
        // find closest BTS + furthest Bspot to mark area

        ArrayList<Coord> bspots;
        ArrayList<Coord> path;
        ArrayList<ArrayList<Coord>> paths;
        ArrayList<Integer> bts_ind = new ArrayList<>();
        int[] workload;
        int end_indx;

        // #0: get workload: split blindspots and distribute workload
        if (PCJ.myId() == 0) {
            if (verbose > 0) {
                System.out.println("* Finding paths - method: closest+areas");
            }
            end_indx = MtG.BSpots.size();
            workload = AuxTools.SplitWorkload(end_indx, verbose);

            for (int i = PCJ.threadCount() - 1; i > 0; i--) {
                int start_indx = end_indx - workload[i];
                if (start_indx < 0) {
                    start_indx = 0;
                }
                PCJ.put(new ArrayList(MtG.BSpots.subList(start_indx, end_indx)), i, MapTheGap.Shared.BSpots);
                end_indx = start_indx;
            }
            bspots = new ArrayList(MtG.BSpots.subList(0, end_indx));
        } else {  // other threads
            PCJ.monitor(MapTheGap.Shared.BSpots);
            PCJ.waitFor(MapTheGap.Shared.BSpots);
            bspots = MtG.BSpots;
        }

        // #all: for each blindspot find closest BTS
        MtG.Paths = new ArrayList<>(); // [ [coord(inf: "BTS"), blindspot, blindspot, ..], .. ] 
        for (Coord bspot : bspots) {
            Coord indist = AuxTools.FindClosestCoord2Array(MtG.Bts, bspot);
            int min_indx = (int) indist.x();
            double min_indx_dist = (double) indist.y();
            bspot.Set(null, null, min_indx_dist);
            if (bts_ind.contains(min_indx)) {
                int indx = bts_ind.indexOf(min_indx);
                MtG.Paths.get(indx).add(bspot);
            } else {
                bts_ind.add(min_indx);
                ArrayList<Coord> tmp = new ArrayList<>();
                Coord closest_bts = MtG.Bts.get(min_indx);
                closest_bts.Set(null, null, min_indx);
                tmp.add(closest_bts);
                tmp.add(bspot);
                MtG.Paths.add(tmp);
            }
        }
        PCJ.barrier();

        // #0: gather blindspots assigned to closest BTS, split workload by number od blindspots per BTS
        if (PCJ.myId() == 0) {
            bts_ind = new ArrayList<>();
            for (ArrayList<Coord> ipath : MtG.Paths) {
                bts_ind.add((int) ipath.get(0).inf());
            }

            for (int i = 1; i < PCJ.threadCount(); i++) {
                paths = (ArrayList) PCJ.get(i, MapTheGap.Shared.Paths);
                for (ArrayList<Coord> ipath : paths) {
                    int indx = (int) ipath.get(0).inf();
                    if (bts_ind.contains(indx)) {
                        MtG.Paths.get(bts_ind.indexOf(indx)).addAll(ipath.subList(1, ipath.size()));
                    } else {
                        bts_ind.add(indx);
                        ipath.get(0).Set(null, null, 0.);
                        MtG.Paths.add(ipath);
                    }
                }
            }
            workload = AuxTools.SplitWorkloadArray(MtG.Paths, verbose);
            end_indx = MtG.Paths.size();
            for (int i = PCJ.threadCount() - 1; i > 0; i--) {
                int start_indx = end_indx - workload[i];
                if (start_indx < 0) {
                    start_indx = 0;
                }
                PCJ.put(new ArrayList(MtG.Paths.subList(start_indx, end_indx)), i, MapTheGap.Shared.Paths);
                end_indx = start_indx;
            }
            MtG.Paths.subList(end_indx, MtG.Paths.size()).clear();
        } else {
            PCJ.monitor(MapTheGap.Shared.Paths);
            PCJ.waitFor(MapTheGap.Shared.Paths);
        }

        // #all: find most distant bspot
        for (int i = 0; i < MtG.Paths.size(); i++) {
            double max_dist = Double.NEGATIVE_INFINITY;
            int npath = MtG.Paths.get(i).size();
            for (int j = 1; j < npath; j++) {
                double dist = (double) MtG.Paths.get(i).get(j).inf();
                if (dist > max_dist) {
                    max_dist = dist;
                }
            }
            MtG.Paths.get(i).get(0).Set(null, null, max_dist);
        }
        PCJ.barrier();

        // #0: gather all paths
        if (PCJ.myId() == 0) {
            for (int i = 1; i < PCJ.threadCount(); i++) {
                MtG.Paths.addAll((ArrayList) PCJ.get(i, MapTheGap.Shared.Paths));
            }
        }
    }

    public void PrepareRoutes(int verbose) {  // for each find route (on roads) with GHopper
        int[] workload;
        int end_indx;

        // #0: gather blindspots assigned to closest BTS, split workload by number od blindspots per BTS
        if (PCJ.myId() == 0) {
            if (verbose > 0) {
                System.out.println("* Preparing routes via roads/streets..");
            }

            workload = AuxTools.SplitWorkloadArray(MtG.Paths, verbose);
            end_indx = MtG.Paths.size();
            for (int i = PCJ.threadCount() - 1; i > 0; i--) {
                int start_indx = end_indx - workload[i];
                if (start_indx < 0) {
                    start_indx = 0;
                }
                PCJ.put(new ArrayList(MtG.Paths.subList(start_indx, end_indx)), i, MapTheGap.Shared.Paths);
                end_indx = start_indx;
            }
            MtG.Paths.subList(end_indx, MtG.Paths.size()).clear();
        } else {
            PCJ.monitor(MapTheGap.Shared.Paths);
            PCJ.waitFor(MapTheGap.Shared.Paths);
        }

        // #all: get GHopper routs
        MtG.Routes = new ArrayList<>();
        for (ArrayList<Coord> path : MtG.Paths) {
            ArrayList<Coord> route = new ArrayList<>();
            route.add(path.get(0));
            for (int i = 0; i < path.size() - 1; i++) {
                ArrayList<Coord> r = GHrouting.getPath(path.get(i), path.get(i + 1));
                if (r != null) {  // DEV   (r == null   not necesarry!)
                    route.addAll(r.subList(1, r.size()));
                } else {
                    continue; // deal with that ????
                }
            }
            MtG.Routes.add(route);
        }
        PCJ.barrier();

        // #0: gather all routes
        if (PCJ.myId() == 0) {
            for (int i = 1; i < PCJ.threadCount(); i++) {
                MtG.Routes.addAll((ArrayList) PCJ.get(i, MapTheGap.Shared.Routes));
            }
        }
    }

    public void addPath(ArrayList<Coord> path) {
        MtG.Paths.add(path);
    }

    public void addPathLatLong(ArrayList<Double> latitudes, ArrayList<Double> longitudes) {
        Iterator<Double> lat = latitudes.iterator();
        Iterator<Double> lng = longitudes.iterator();
        ArrayList<Coord> path = new ArrayList<>();

        while (lat.hasNext() && lng.hasNext()) {
            path.add(new Coord(lat.next(), lng.next(), ""));
        }
        addPath(path);
    }

    public void addPathLatLongList(List<List<Double>> latlong) {
        ArrayList<Coord> path = new ArrayList<>();

        latlong.forEach((itm) -> {
            path.add(new Coord(itm.get(0), itm.get(1), ""));
        });

        addPath(path);
    }

//function pathQuality evaluates path quality by dividing the number of nodes by the path lenght    
    public double pathQuality(ArrayList<Coord> path, double pathlength) {
        double pqual;
        if (pathlength != 0) {
            pqual = 1000 * path.size() / pathlength;
        } else {
            pqual = 0.0;
        }
        return pqual;
    }

    public void optimizePathSet() {

        int numberofpaths; //liczba sciezek

        ArrayList<ArrayList<Coord>> OptimizedPaths;
        OptimizedPaths = new ArrayList<>();
        double Fibre;

        ArrayList<Coord> Tail;
        Tail = new ArrayList<>();

        //policzyć pathQuality dla poszczególnych ścieżek (na osobnych watkach)
        numberofpaths = MtG.Paths.size();//Paths.size();

        //pathlengths = new double[numberofpaths];
        for (int i = 0; i < numberofpaths; i++) {

            MtG.Plength.add(AuxTools.pathLength(MtG.Paths.get(i)));
            MtG.Pqual.add(pathQuality(MtG.Paths.get(i), MtG.Plength.get(i)));//.set(i, pathQuality(MtG.Paths.get(i), pathlengths[i]));

        }

        PCJ.barrier();

        if (PCJ.myId() == 0) { // watek zerowy pobiera dane ze wszystkich watkow

            for (int p = 1; p < PCJ.threadCount(); p++) {
                MtG.Paths.addAll((ArrayList) PCJ.get(p, MapTheGap.Shared.Paths));
                MtG.Pqual.addAll((ArrayList) PCJ.get(p, MapTheGap.Shared.Pqual));
                MtG.Plength.addAll((ArrayList) PCJ.get(p, MapTheGap.Shared.Plength));
            }

            //posortowac wedlug pathquality
            companionSort(MtG.Pqual, MtG.Paths);

            //doklejac kolejne sciezki az do osiagniecia limitu
            OptimizedPaths.add(MtG.Paths.get(0));
            Fibre = MtG.Plength.get(0);
            int k = 1;
            Fibre = Fibre + MtG.Plength.get(k);

            while ((Fibre < MtG.FibreLimit) && (k < MtG.Paths.size() - 1)) {
                OptimizedPaths.add(MtG.Paths.get(k));
                k = k + 1;
                Fibre = Fibre + MtG.Plength.get(k);
                //System.out.println(Fibre);
            }

            if (k != MtG.Paths.size() - 1) {

                //doklejamy kawalek nastęenej zeby dopelnic
                Fibre = Fibre - MtG.Plength.get(k);
                //System.out.println(Fibre);
                Tail.add(MtG.Paths.get(k).get(0));
                Tail.add(MtG.Paths.get(k).get(1));
                int j = 2;

                while ((Fibre < MtG.FibreLimit) && (j < MtG.Paths.get(k).size() - 1)) {
                    Fibre = Fibre + AuxTools.pathLength(Tail);
                    //System.out.println(Fibre);
                    OptimizedPaths.add(Tail);
                    Tail.add(MtG.Paths.get(k).get(j));
                    j++;
                }
                ///....

                MtG.FibreLen = Fibre;
                MtG.Paths = OptimizedPaths;
            } else {
                MtG.FibreLen = Fibre;
                OptimizedPaths.add(MtG.Paths.get(k));
                MtG.Paths = OptimizedPaths;
            }

        }

    }

    //   Funkcja sortujaca tablice od najwiekszych do najmniejszych wartosci i w tej samej kolejnoscu ustawiajaca elementy tablicy towarzyszacej
    public static void companionSort(ArrayList array, ArrayList companion) {

        double temp1;
        Object temp2;
        int j, n = array.size();

        // turn input array into a heap
        for (j = n / 2; j > 0; j--) {
            adjustForCompanionSort(array, companion, j, n);
        }

        // remove largest elements and put them at the end
        // of the unsorted region until you are finished
        for (j = n - 1; j > 0; j--) {

            temp1 = (double) array.get(0);
            array.set(0, array.get(j));
            array.set(j, temp1);

            temp2 = companion.get(0);
            companion.set(0, companion.get(j));
            companion.set(j, temp2);

            adjustForCompanionSort(array, companion, 1, j);

        }
    }

    private static void adjustForCompanionSort(ArrayList array, ArrayList companion, int lower, int upper) {//(double[] array, int lower, int upper) {

        int j, k;
        double temp1;
        Object temp2;

        j = lower;
        k = lower * 2;

        while (k <= upper) {

            if ((k < upper) && ((double) array.get(k - 1) > (double) array.get(k))) {
                k += 1;
            }

            //if ((double) array.get(j - 1) < (double) array.get(k - 1)) {
            if ((double) array.get(j - 1) > (double) array.get(k - 1)) {

                temp1 = (double) array.get(j - 1);
                array.set(j - 1, array.get(k - 1));
                array.set(k - 1, temp1);

                //analogicznie przestawiamy elementy tablicy towarzyszacej               
                temp2 = companion.get(j - 1);
                companion.set(j - 1, companion.get(k - 1));
                companion.set(k - 1, temp2);

            }
            j = k;
            k *= 2;
        }
    }

    public void Optimize_TSP(){
    
        ArrayList<Coord> Path_tmp =new ArrayList<>();
        ArrayList<ArrayList<Coord>> AllPath_tmp=new ArrayList<>();
        
        for (int i=0;i<MtG.Paths.size();i++){
            
            if(MtG.Paths.get(i).size()>4){
            TSP tsp = new TSP((ArrayList<Coord>)MtG.Paths.get(i));
            Path_tmp=tsp.TSPrun();
            
             AllPath_tmp.add(Path_tmp);
             
            }
            //MtG.Paths.get(i)=tsp.TSPrun();
            AllPath_tmp.add(Path_tmp);
           
            
        }
       
        MtG.Paths=AllPath_tmp;
        
    
    }
}
