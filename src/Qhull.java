
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.pcj.PCJ;

/**
 * QHull wrapper
 *
 */
public class Qhull {
    
    final static private String qdelaunay_run = "./Qhull/run_qhull.sh";
    final private MapTheGap MtG;
    public String fname_points;
    public String outfname_base;

    public Qhull(MapTheGap mtg, String fname_points, String outfname_base) {
        this.MtG = mtg;
        this.fname_points = fname_points;
        this.outfname_base = outfname_base;
    }

    private static final String SufDensity = "_dens";  // siffix: densities 
    private static final String SufTriangul = "_trig";  // siffix: triangulation 
    private static final String SufChull = "_chul";  // siffix: convex hull 

    public void RunTriangulation() {
//        Make sure Qhull is compiled! (cd Qhull; make)
//        Input files must have Qhull format! : dimension, #points, list of points
        
        if (PCJ.myId() == 0) {
            System.out.printf("* Building triangulation... (%s -> %s)\n", fname_points, outfname_base);
            try {
                ProcessBuilder pb = new ProcessBuilder("sh", qdelaunay_run, fname_points, outfname_base);
                pb.redirectErrorStream(true);
                Process p = pb.start();     // Start the process.
                p.waitFor();                // Wait for the process to finish.
            } catch (Exception e) {
                System.out.println("! (Qhull.Triangulation:) Error running external command: " + e.getMessage());
            }
        }
    }
    
    public void ReadNeighbours() {  // simple to parallel - lack of time
        if (PCJ.myId() == 0) {
            int[] trigs = GetTriangles();
            
            ArrayList<Integer> lookup = new ArrayList<>();
            ArrayList<ArrayList<Integer>> neighours = new ArrayList<>();

            for (int i=0;i<trigs.length/3; i++) {
                for (int j=0; j<3; j++) {  // simple permutations
                    int v1 = trigs[3*i + (j%3)%3];
                    int v2 = trigs[3*i + (j%3+1)%3];
                    int v3 = trigs[3*i + (j%3+2)%3];
//                    System.out.printf("%d  %d  %d\n", v1, v2, v3);
                    
                    if (lookup.contains(v1)) {
                        int indx = lookup.indexOf(v1);
                        if (!neighours.get(indx).contains(v2)) {
                            neighours.get(indx).add(v2);
                        }
                        if (!neighours.get(indx).contains(v3)) {
                            neighours.get(indx).add(v3);
                        }
                    } else {
                        lookup.add(v1);
                        ArrayList<Integer> tmp = new ArrayList<>();
                        tmp.add(v1);
                        tmp.add(v2);
                        tmp.add(v3);
                        neighours.add(tmp);
                    }
                }
//                System.out.printf("\n");
            }
            
            for (ArrayList<Integer> nlist : neighours) {
                int pntindx = nlist.get(0);
                int[] n = new int[nlist.size()-1];
                for (int i=0;i<n.length;i++){
                    n[i] = nlist.get(i+1);
                }
                MtG.BSpots.get(pntindx).Set(null, null, n);
            }
            
            if (false) {  // test plot
                MtG.Paths = new ArrayList<>();
                int indx = 1000;
                for (int i=0;i<((int[])MtG.BSpots.get(indx).inf()).length; i++) {
                    ArrayList<Coord> tmp = new ArrayList<>();
                    tmp.add(MtG.BSpots.get(indx));
                    int a = ((int[]) MtG.BSpots.get(indx).inf())[i];
                    System.out.printf("%d\n", a);
                    tmp.add(MtG.BSpots.get(a));
                    MtG.Paths.add(tmp);
                }
            }
        }
        PCJ.barrier();
    }    
    
    public void LocalDensities() {
        if (PCJ.myId() == 0) {
            MtG.BSpots_density = new ArrayList<>();
            double[] dens = GetDensities();
            for (int i=0; i<dens.length; i++){
                MtG.BSpots_density.add(dens[i]);
            }
        }
        PCJ.barrier();
    }
    
    private int[] GetConvexHull() {
        int[] chull = {};
        try {
            chull = Files.lines(Paths.get(String.format("%s%s", outfname_base, SufChull))).mapToInt(Integer::parseInt).toArray();
        } catch (Exception e) {
            // error
        }
        return chull;
    }

    private double[] GetDensities() {
        double[] dens = {};
        try {
            dens = Files.lines(Paths.get(String.format("%s%s", outfname_base, SufDensity))).mapToDouble(Double::parseDouble).toArray();
        } catch (Exception e) {
            // error
        }
        return dens;
    }

    private int[] GetTriangles() {
        int[] trigs = {};
        try {
            trigs = Files.lines(Paths.get(String.format("%s%s", outfname_base, SufTriangul))).mapToInt(Integer::parseInt).toArray();
        } catch (Exception e) {
            // error
        }
        return trigs;        
    }

}
