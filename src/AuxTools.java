
/**
 * Auxiliary Tools
 * */
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.pcj.PCJ;

public class AuxTools {

    public static double pathLength(ArrayList<Coord> path) {
        
        double lat1, lat2, lon1, lon2; 
        double length=0;
        
        for (int i = 0; i < path.size() - 1; i++) {
           lat1 = (double) path.get(i).x();
           lat2 = (double) path.get(i + 1).x();
           lon1 = (double) path.get(i).y();
           lon2 = (double) path.get(i + 1).y();
           length=length+distance(lat1, lat2, lon1, lon2);
        }        
                
        return length;
    }

    public static ArrayList<Coord> SolveTSP(ArrayList<Coord> path, int maxiter, int niterlimit, int[][] adjmtrx) {
        int nnodes = path.size();
        ArrayList<Coord> fin_path = new ArrayList<>();
        Random rand = new Random();
        double cur_dist, try_dist, best_dist;
        int[] ind_path = new int[nnodes];
        int[] ind_bestpath = new int[nnodes];
        int[] ind_trypath = new int[nnodes];
        int niter = 0;
        int best_niter = 0;

        // build adjacency matrix
        double adj_mtrx[][] = new double[nnodes][nnodes];
        for (int i = 0; i < path.size(); i++) {
            ind_path[i] = i;
            for (int j = i + 1; j < path.size(); j++) {
                double dist = distanceCoord(path.get(i), path.get(j));
                adj_mtrx[i][j] = dist;
                adj_mtrx[j][i] = dist;
            }
        }

        // calculate current path distance
        cur_dist = 0.;
        for (int i = 0; i < nnodes - 1; i++) {
            cur_dist += adj_mtrx[ind_path[i]][ind_path[i + 1]];
        }
        best_dist = cur_dist;
        System.arraycopy(ind_path, 0, ind_bestpath, 0, nnodes);

        while (niter < maxiter) {
            niter += 1;
            best_niter += 1;

            if (best_niter > niterlimit) {
                break;
            }

            // swap random nodes
            System.arraycopy(ind_path, 0, ind_trypath, 0, nnodes);

            if (rand.nextDouble() > 0.3) {
                int rind1 = rand.nextInt(nnodes);
                int rind2 = rind1;
                while (rind1 == rind2) {
                    rind2 = rand.nextInt(nnodes);
                }
                ind_trypath[rind2] = ind_path[rind1];
                ind_trypath[rind1] = ind_path[rind2];
            } else {
                for (int i = nnodes - 1; i > 0; i--) {
                    int indx = rand.nextInt(i + 1);
                    int tmp = ind_trypath[indx];
                    ind_trypath[indx] = ind_trypath[i];
                    ind_trypath[i] = tmp;
                }
            }

            // calculate current path distance
            try_dist = 0.;
            for (int i = 0; i < nnodes - 1; i++) {
                try_dist += adj_mtrx[ind_trypath[i]][ind_trypath[i + 1]];
            }

            // check/store best path
            if (try_dist < best_dist) {
                best_dist = try_dist;
                System.arraycopy(ind_trypath, 0, ind_bestpath, 0, nnodes);
                best_niter = 0;
            }

            // check 
            if (try_dist < cur_dist) {// || rand.nextDouble() > 0.8) {
                System.arraycopy(ind_trypath, 0, ind_path, 0, nnodes);
                cur_dist = try_dist;
            }
        }

        // prepare final path
        for (int i = 0; i < nnodes; i++) {
            fin_path.add(path.get(ind_bestpath[i]));
        }
        fin_path.get(0).Set(null, null, best_dist);
        return fin_path;
    }
    
    public static ArrayList<Coord> SolveTSPsimple(ArrayList<Coord> path, int maxiter, int niterlimit) {
        int nnodes = path.size();
        ArrayList<Coord> fin_path = new ArrayList<>();
        Random rand = new Random();
        double cur_dist, try_dist, best_dist;
        int[] ind_path = new int[nnodes];
        int[] ind_bestpath = new int[nnodes];
        int[] ind_trypath = new int[nnodes];
        int niter = 0;
        int best_niter = 0;

        // build adjacency matrix
        double adj_mtrx[][] = new double[nnodes][nnodes];
        for (int i = 0; i < path.size(); i++) {
            ind_path[i] = i;
            for (int j = i + 1; j < path.size(); j++) {
                double dist = distanceCoord(path.get(i), path.get(j));
                adj_mtrx[i][j] = dist;
                adj_mtrx[j][i] = dist;
            }
        }

        // calculate current path distance
        cur_dist = 0.;
        for (int i = 0; i < nnodes - 1; i++) {
            cur_dist += adj_mtrx[ind_path[i]][ind_path[i + 1]];
        }
        best_dist = cur_dist;
        System.arraycopy(ind_path, 0, ind_bestpath, 0, nnodes);

        while (niter < maxiter) {
            niter += 1;
            best_niter += 1;

            if (best_niter > niterlimit) {
                break;
            }

            // swap random nodes
            System.arraycopy(ind_path, 0, ind_trypath, 0, nnodes);

            if (rand.nextDouble() > 0.3) {
                int rind1 = rand.nextInt(nnodes);
                int rind2 = rind1;
                while (rind1 == rind2) {
                    rind2 = rand.nextInt(nnodes);
                }
                ind_trypath[rind2] = ind_path[rind1];
                ind_trypath[rind1] = ind_path[rind2];
            } else {
                for (int i = nnodes - 1; i > 0; i--) {
                    int indx = rand.nextInt(i + 1);
                    int tmp = ind_trypath[indx];
                    ind_trypath[indx] = ind_trypath[i];
                    ind_trypath[i] = tmp;
                }
            }

            // calculate current path distance
            try_dist = 0.;
            for (int i = 0; i < nnodes - 1; i++) {
                try_dist += adj_mtrx[ind_trypath[i]][ind_trypath[i + 1]];
            }

            // check/store best path
            if (try_dist < best_dist) {
                best_dist = try_dist;
                System.arraycopy(ind_trypath, 0, ind_bestpath, 0, nnodes);
                best_niter = 0;
            }

            // check 
            if (try_dist < cur_dist) {// || rand.nextDouble() > 0.8) {
                System.arraycopy(ind_trypath, 0, ind_path, 0, nnodes);
                cur_dist = try_dist;
            }
        }

        // prepare final path
        for (int i = 0; i < nnodes; i++) {
            fin_path.add(path.get(ind_bestpath[i]));
        }
        fin_path.get(0).Set(null, null, best_dist);
        return fin_path;
    }

    public static int[] SplitWorkloadArray(ArrayList<ArrayList<Coord>> arr, int verbose) {  // thread #0 privileged
        int nproc = PCJ.threadCount();
        int[] workload = new int[nproc];
        int ndiv, w_indx, indx, sum = 0;

        for (int i = 0; i < arr.size(); i++) {
            sum += arr.get(i).size();
        }

        ndiv = (int) Math.ceil((double) sum / nproc);

        sum = 0;
        indx = arr.size();
        w_indx = nproc - 1;
        for (int i = arr.size() - 1; i >= 0; i--) {
            sum += arr.get(i).size();
            if ((sum > ndiv) || (i == 0)) {
                workload[w_indx] = indx - i;
                if (workload[w_indx] < 0) {
                    workload[w_indx] = 0;
                }
                w_indx -= 1;
                sum = 0;
                indx = i;
            }
        }

        if (verbose > 2) {
            System.out.printf("  * Workload: ");
            for (int i = 0; i < workload.length; i++) {
                System.out.printf("%d ", workload[i]);
            }
            System.out.printf("\n");
        }

        return workload;
    }

    public static int[] SplitWorkload(int n, int verbose) {  // thread #0 privileged
        int nproc = PCJ.threadCount();
        int[] workload = new int[nproc];
        int ndiv;

        if (nproc > n) {
            for (int i = 0; i < nproc; i++) {
                if (i == 0) {
                    ndiv = 0;
                } else {
                    ndiv = (i <= n) ? 1 : 0;
                }
                workload[i] = ndiv;
            }
        } else {
            ndiv = (int) Math.ceil((double) n / nproc);
            for (int i = 0; i < nproc; i++) {
                if (i == 0) {
                    workload[i] = (n - (nproc - 1) * ndiv);
                    if (workload[i] < 0) {
                        workload[i] = 0;
                    }
                } else {
                    workload[i] = ndiv;
                }
            }
        }

        if (verbose > 2) {
            System.out.printf("  * Workload: ");
            for (int i = 0; i < workload.length; i++) {
                System.out.printf("%d ", workload[i]);
            }
            System.out.printf("\n");
        }

        return workload;
    }

    public static boolean UniqueFile_Linux(String fname, String outname) {
        try {
            File logFile = new File(outname);
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/awk", "!seen[$0]++", fname);
            pb.redirectErrorStream(true);
            pb.redirectOutput(logFile);
            Process p = pb.start();     // Start the process.
            p.waitFor();                // Wait for the process to finish.
        } catch (Exception e) {
            System.out.println("! (AuxTools.UniqueFile_Linux:) Error running external command: "+e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean FileExists(String fname) {
        File file = new File(fname);
        return file.exists();
    }
    
    public static void DeleteFile(String fname) {
        try {
            File file = new File(fname);
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
        }
    }
    
    public static void DeleteFolder(String fname) {
        try {
            File file = new File(fname);
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
        }
    }
    
    public static ArrayList<Coord> FilterArrayByLocations(ArrayList<Coord> array,
            List<Coord> locations, int verbose) {
        
        if (locations == null || locations.isEmpty()) {
            return array;
        }
        
        ArrayList<Coord> newarr = new ArrayList<>();
        
        for (Coord crd : array) {
            for (Coord loc : locations) {
                if (distanceCoord(crd, loc) <= (double) loc.inf()) {
                    newarr.add(crd);
                    continue;
                }
            }
        }

        return newarr;
    }
    
    public static Coord FindClosestCoord2Array(ArrayList<Coord> array, Coord coord) {
        // find closest Coord to array of Coords
        double mindist = Double.POSITIVE_INFINITY;
        int minindx = -1;
        int i = 0;
        for (Coord crd : array) {
            double dist = AuxTools.distanceCoord(crd, coord);
            if (dist < mindist) {
                mindist = dist;
                minindx = i;
            }
            i++;
        }

        return new Coord(minindx, mindist, "");
    }

    public static String fmt(double d) {
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else {
            return String.format("%s", d);
        }
    }

    public static double distance(double lat1, double lat2, double lon1, double lon2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2.) * Math.sin(latDistance / 2.)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2.) * Math.sin(lonDistance / 2.);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = R * c * 1000; // convert to meters

        return dist;
    }

    public static double distance(double lat1, double lat2, double lon1,
            double lon2, double el1, double el2) {

        double dist = distance(lat1, lat2, lon1, lon2);

        double height = el1 - el2;
        dist = Math.pow(dist, 2) + Math.pow(height, 2);

        return Math.sqrt(dist);
    }

    public static double distanceCoord(Coord p1, Coord p2) {
        double lat1 = (double) p1.x();
        double lat2 = (double) p2.x();
        double lon1 = (double) p1.y();
        double lon2 = (double) p2.y();

        return distance(lat1, lat2, lon1, lon2);
    }

    // Convert DMS -> Deg
    private final static Pattern DMS_PATTERN = Pattern.compile(
            "(-?)([0-9]{1,2})([NSEW])([0-5]?[0-9])'([0-5]?[0-9])\"");

    private static double dms2Double(Matcher mtch) {
        int sgn = "".equals(mtch.group(1)) ? 1 : -1;
        double degs = Double.parseDouble(mtch.group(2));
        double mins = Double.parseDouble(mtch.group(4));
        double secs = Double.parseDouble(mtch.group(5));
        int dir = "NE".contains(mtch.group(3)) ? 1 : -1;

        return sgn * dir * (degs + mins / 60. + secs / 3600.);
    }

    public static double Convert_dms2deg(String dms, int verbose) {
        double deg = 0.;
        Matcher mtch = DMS_PATTERN.matcher(dms.trim());
        if (mtch.matches()) {
            deg = dms2Double(mtch);
        } else {
            if (verbose > 2) {
                System.out.println("!! Can\'t parse coord: " + dms);
            }
        }
        return deg;
    }
    // end: DMS -> Deg 

    // Convert PUWG92 to Long, Lat
    private final static double D2R = Math.PI / 180.0;
    private final static double R2D = 180.0 / Math.PI;
    private final static double FE = 500000.0;

    private static double CalculateESquared(double a, double b) {
        return ((a * a) - (b * b)) / (a * a);
    }

    private static double CalculateE2Squared(double a, double b) {
        return ((a * a) - (b * b)) / (b * b);
    }

    private static double denom(double es, double sphi) {
        double sinSphi = Math.sin(sphi);
        return Math.sqrt((1.0 - es * (sinSphi * sinSphi)));
    }

    private static double sphsr(double a, double es, double sphi) {
        double dn = denom(es, sphi);
        return a * (1.0 - es) / (dn * dn * dn);
    }

    private static double sphsn(double a, double es, double sphi) {
        double sinSphi = Math.sin(sphi);
        return a / Math.sqrt(1.0 - es * (sinSphi * sinSphi));
    }

    private static double sphtmd(double ap, double bp, double cp, double dp, double ep, double sphi) {
        return (ap * sphi) - (bp * Math.sin(2.0 * sphi)) + (cp * Math.sin(4.0 * sphi)) - (dp * Math.sin(6.0 * sphi)) + (ep * Math.sin(8.0 * sphi));
    }

    public static double[] Convert_PUWG92(double easting, double northing) {
        // code from: http://ecdis.republika.pl/page6434192505427b12099241.html
        double a = 6378137.0;
        double f = 1 / 298.257223563;
        boolean proj = true;

        double ok = 0.999923;

        if (proj) {
            ok = 0.9993;
        }

        double recf = 1.0 / f;
        double b = a * (recf - 1) / recf;
        double eSquared = CalculateESquared(a, b);
        double e2Squared = CalculateE2Squared(a, b);
        double tn = (a - b) / (a + b);
        double ap = a * (1.0 - tn + 5.0 * ((tn * tn) - (tn * tn * tn)) / 4.0 + 81.0 * ((tn * tn * tn * tn) - (tn * tn * tn * tn * tn)) / 64.0);
        double bp = 3.0 * a * (tn - (tn * tn) + 7.0 * ((tn * tn * tn) - (tn * tn * tn * tn)) / 8.0 + 55.0 * (tn * tn * tn * tn * tn) / 64.0) / 2.0;
        double cp = 15.0 * a * ((tn * tn) - (tn * tn * tn) + 3.0 * ((tn * tn * tn * tn) - (tn * tn * tn * tn * tn)) / 4.0) / 16.0;
        double dp = 35.0 * a * ((tn * tn * tn) - (tn * tn * tn * tn) + 11.0 * (tn * tn * tn * tn * tn) / 16.0) / 48.0;
        double ep = 315.0 * a * ((tn * tn * tn * tn) - (tn * tn * tn * tn * tn)) / 512.0;
        double nfn = 0;
        double strf = 0.0;
        double olam = 19.0 * D2R;

        if (proj) {

            nfn = -5300000.0;
        } else {
            if (easting < 6000000.0 && easting > 5000000.0) {
                strf = 5000000.0;
                olam = 15.0 * D2R;
            }
            if (easting < 7000000.0 && easting > 6000000.0) {
                strf = 6000000.0;
                olam = 18.0 * D2R;
            }
            if (easting < 8000000.0 && easting > 7000000.0) {
                strf = 7000000.0;
                olam = 21.0 * D2R;
            }
            if (easting < 9000000.0 && easting > 8000000.0) {
                strf = 8000000.0;
                olam = 24.0 * D2R;
            }
        }

        double tmd = (northing - nfn) / ok;
        double sr = sphsr(a, eSquared, 0.0);
        double ftphi = tmd / sr;

        for (int i = 0; i < 5; i++) {
            double t10 = sphtmd(ap, bp, cp, dp, ep, ftphi);
            sr = sphsr(a, eSquared, ftphi);
            ftphi = ftphi + (tmd - t10) / sr;
        }

        sr = sphsr(a, eSquared, ftphi);
        double sn = sphsn(a, eSquared, ftphi);
        double s = Math.sin(ftphi);
        double c = Math.cos(ftphi);
        double t = s / c;
        double eta = e2Squared * (c * c);
        double de = easting - FE - strf;
        double t10 = t / (2.0 * sr * sn * (ok * ok));
        double t11 = t * (5.0 + 3.0 * (t * t) + eta - 4.0 * (eta * eta) - 9.0 * (t * t) * eta) / (24.0 * sr * (sn * sn * sn) * (ok * ok * ok * ok));
        double t12 = t * (61.0 + 90.0 * (t * t) + 46.0 * eta + 45.0 * (t * t * t * t) - 252.0 * (t * t) * eta - 3.0 * (eta * eta) + 100.0 * (eta * eta * eta) - 66.0 * (t * t) * (eta * eta) - 90.0 * (t * t * t * t) * eta + 88.0 * (eta * eta * eta * eta) + 225.0 * (t * t * t * t) * (eta * eta) + 84.0 * (t * t) * (eta * eta * eta) - 192.0 * (t * t) * (eta * eta * eta * eta)) / (720.0 * sr * (sn * sn * sn * sn * sn) * (ok * ok * ok * ok * ok * ok));
        double t13 = t * (1385.0 + 3633 * (t * t) + 4095.0 * (t * t * t * t) + 1575.0 * (t * t * t * t * t * t)) / (40320 * sr * (sn * sn * sn * sn * sn * sn * sn) * (ok * ok * ok * ok * ok * ok * ok * ok));
        double lat = ftphi - (de * de) * t10 + (de * de * de * de) * t11 - (de * de * de * de * de * de) * t12 + (de * de * de * de * de * de * de * de) * t13;
        double t14 = 1.0 / (sn * c * ok);
        double t15 = (1.0 + 2.0 * (t * t) + eta) / (6.0 * (sn * sn * sn) * c * (ok * ok * ok));
        double t16 = 1.0 * (5.0 + 6.0 * eta + 28.0 * (t * t) - 3.0 * (eta * eta) + 8.0 * (t * t) * eta + 24.0 * (t * t * t * t) - 4.0 * (eta * eta * eta) + 4.0 * (t * t) * (eta * eta) + 24.0 * (t * t) * (eta * eta * eta)) / (120.0 * (sn * sn * sn * sn * sn) * c * (ok * ok * ok * ok * ok));
        double t17 = 1.0 * (61.0 + 662.0 * (t * t) + 1320.0 * (t * t * t * t) + 720.0 * (t * t * t * t * t * t)) / (5040.0 * (sn * sn * sn * sn * sn * sn * sn) * c * (ok * ok * ok * ok * ok * ok * ok));
        double dlam = de * t14 - (de * de * de) * t15 + (de * de * de * de * de) * t16 - (de * de * de * de * de * de * de) * t17;
        double lon = olam + dlam;
        lon *= R2D;
        lat *= R2D;

        return new double[]{lat, lon};
    }
    // end: Convert PUWG92 to Long, Lat

}
