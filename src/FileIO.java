
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import org.pcj.PCJ;

/**
 *
 * handle IO
 */
public class FileIO {

    public static ArrayList<Coord> ReadCSV(String fname, String fname_storage, int ncols, int skiphdr, String convtype,
            char separ, char quotechar, boolean chkuniq, boolean chkuniq_allowslow, int verbose) {

        if (!convtype.equals("bts") && !convtype.equals("bspot")) {
            return null;
        }
        convtype = convtype.toUpperCase();

        ArrayList<Coord> array = new ArrayList<>();

        if (PCJ.myId() == 0) {
            CSVReader reader = null;
            int end_indx;
            int[] workload;

            if (verbose > 0) {
                System.out.println("* Opening file: " + fname);
            }

            // read input file
            if (quotechar == '-') {
                quotechar = CSVWriter.NO_QUOTE_CHARACTER;
            }
            try {
                reader = new CSVReader(new FileReader(fname), separ, quotechar, skiphdr);
                String[] nextLine;

                while ((nextLine = reader.readNext()) != null) {
                    if (nextLine.length != ncols) {
                        if (verbose > 0) {
                            System.out.println("File: " + fname + " - error (length) while parsing line: " + Arrays.toString(nextLine));
                        }
                        continue;
                    }
                    if (convtype.equals("BTS")) {
                        array.add(new Coord(nextLine[5], nextLine[4], ""));
                    } else {
                        array.add(new Coord(nextLine[11], nextLine[12], ""));
                    }
                }
            } catch (IOException e) {
                System.out.println(" ! Error reading file: " + fname);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    System.out.println(" ! Error closing file: " + fname);
                }
            }

            end_indx = array.size();
            if (verbose > 0) {
                System.out.printf("  - %s (all): %d\n", convtype, end_indx);
            }

            if (chkuniq) {  // remove duplicated with awk
                String tempfile = String.format(Locale.ROOT, "%s_temp", fname_storage);
                String uniqfile = String.format(Locale.ROOT, "%s_unique", fname_storage);
                StoreArrayTxt(tempfile, array, 0);
                boolean result = AuxTools.UniqueFile_Linux(tempfile, uniqfile);
                if (result) {
                    array = LoadArrayTxt(uniqfile, 0, false);
                } else if (chkuniq_allowslow) {  // slow checking for duplicated
                    if (verbose > 0) {
                        System.out.println("  (-) using much slower method to find duplicates..");
                    }
                    ArrayList<Coord> uniqarray = new ArrayList<>();
                    array.forEach((ar) -> {
                        if (!uniqarray.contains(ar)) {
                            uniqarray.add(ar);
                        }
                    });
                    array = uniqarray;
                }

                end_indx = array.size();
                if (true) {  // delete temp files
                    AuxTools.DeleteFile(tempfile);
                    AuxTools.DeleteFile(uniqfile);
                }
            }

            workload = AuxTools.SplitWorkload(end_indx, verbose);
            for (int i = PCJ.threadCount() - 1; i > 0; i--) {
                int start_indx = end_indx - workload[i];
                if (start_indx < 0) {
                    start_indx = 0;
                }
                StoreArrayTxt(String.format(Locale.ROOT, "%s_%d", fname_storage, i), new ArrayList(array.subList(start_indx, end_indx)), 0);
                end_indx = start_indx;
            }
            array.subList(end_indx, array.size()).clear();
        }
        PCJ.barrier();

        if (PCJ.myId() != 0) {
            array = LoadArrayTxt(String.format(Locale.ROOT, "%s_%d", fname_storage, PCJ.myId()), 0, false);
        }

        // DEV - print
        if (!true) {  // print sizes of distributed arrays
            System.out.printf("%d | array.size: %d : ", PCJ.myId(), array.size());
            if (false) {
                for (Coord bt : array) {
                    System.out.printf("(%s , %s) ", bt.x(), bt.y());
                }
            }
            System.out.printf("\n");
        }

        // convert
        for (Coord ar : array) {
            if (convtype.equals("BTS")) {
                ar.Set(AuxTools.Convert_dms2deg((String) ar.x(), verbose), AuxTools.Convert_dms2deg((String) ar.y(), verbose), "");
            } else {
                double[] xy92 = AuxTools.Convert_PUWG92(Double.parseDouble((String) ar.x()), Double.parseDouble((String) ar.y()));
                ar.Set(xy92[0], xy92[1], "");
            }
        }

        // store results in files
        if (PCJ.myId() != 0) {
            String tempfname = String.format(Locale.ROOT, "%s_%d", fname_storage, PCJ.myId());
            StoreArrayTxt(tempfname, array, 0);
        }
        PCJ.barrier();

        // Collect BTS
        if (PCJ.myId() == 0) {
            for (int i = 1; i < PCJ.threadCount(); i++) {
                String tempfname = String.format(Locale.ROOT, "%s_%d", fname_storage, i);
                array.addAll(LoadArrayTxt(tempfname, 0));
                if (true) {  // delete temp files
                    AuxTools.DeleteFile(tempfname);
                }
            }
            if (verbose > 0) {
                System.out.printf("  - %s (useful): %d\n", convtype, array.size());
            }

            // broadcast
            StoreArrayTxt(fname_storage, array, 0);
        }
        PCJ.barrier();

        if (PCJ.myId() != 0) {
            array = LoadArrayTxt(fname_storage, 0);
        }

        // DEV - print
        if (verbose > 2) {
            System.out.printf("%d | array.size: %d\n", PCJ.myId(), array.size());
            if (verbose > 3) {
                array.forEach((ar) -> {
                    System.out.printf("(%s , %s) ", ar.x(), ar.y());
                });
                System.out.printf("\n");
            }
        }

        return array;
    }

    public static void StoreArrayTxt(String fname, ArrayList<Coord> CoordArray, int verbose) {
        if (verbose > 0) {
            if (PCJ.myId() == 0) {
                System.out.println(" * Storing entires to file: " + fname);
            }
        }

        // remove if exists
        AuxTools.DeleteFile(fname);

        // store txt file
        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(fname), ';', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER);

            for (Coord itm : CoordArray) {
                String xfmt = "%s";
                String yfmt = "%s";
                String inffmt = "%s";
                if (itm.x() instanceof Double) {
                    xfmt = "%.7f";
                }
                if (itm.y() instanceof Double) {
                    yfmt = "%.7f";
                }
                if (itm.inf() instanceof Double) {
                    inffmt = "%.7f";
                }
                String[] record = {String.format(Locale.ROOT, xfmt, itm.x()), String.format(Locale.ROOT, yfmt, itm.y()), String.format(Locale.ROOT, inffmt, itm.inf())};
                writer.writeNext(record);
            }

        } catch (IOException e) {
            System.out.println(" ! Error writing to file: " + fname);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                System.out.println(" ! Error closing file: " + fname);
            }
        }
    }
    
    public static ArrayList<Coord> LoadArrayTxt(String fname, int verbose) {
        return LoadArrayTxt(fname, verbose, true);
    }
    
    public static ArrayList<Coord> LoadArrayTxt(String fname, int verbose, boolean tryConvDouble) {
        CSVReader reader = null;
        ArrayList<Coord> readarr = new ArrayList<>();

        if (verbose > 0) {
            if (PCJ.myId() == 0) {
                System.out.println("* Loading array from file: " + fname);
            }
        }

        try {
            reader = new CSVReader(new FileReader(fname), ';', CSVWriter.NO_QUOTE_CHARACTER, 0);
            String[] nextLine;

            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length < 3) {
                    continue;
                }
                Object x = nextLine[0];
                if (tryConvDouble) {
                    try {
                        x = Double.parseDouble(nextLine[0]);
                    } catch (NumberFormatException e) {
                    }
                }
                Object y = nextLine[1];
                if (tryConvDouble) {
                    try {
                        y = Double.parseDouble(nextLine[1]);
                    } catch (NumberFormatException e) {
                    }
                }
                Object inf = nextLine[2];
                if (tryConvDouble) {
                    try {
                        inf = Double.parseDouble(nextLine[2]);
                    } catch (NumberFormatException e) {
                    }
                }
                readarr.add(new Coord(x, y, inf));
            }
        } catch (IOException e) {
            System.out.println(" ! Error reading file: " + fname);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                System.out.println(" ! Error closing file: " + fname);
            }
        }

        return readarr;
    }

    public static void StoreArraysTxt(String fname, ArrayList<ArrayList<Coord>> CoordArrays, int verbose) {
        if (verbose > 0) {
            if (PCJ.myId() == 0) {
                System.out.println(" * Storing entires to file: " + fname);
            }
        }

        // remove if exists
        AuxTools.DeleteFile(fname);

        // store txt file
        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(fname), ';', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER);

            for (ArrayList<Coord> path : CoordArrays) {
                for (Coord itm : path) {
                    String xfmt = "%s";
                    String yfmt = "%s";
                    String inffmt = "%s";
                    if (itm.x() instanceof Double) {
                        xfmt = "%.7f";
                    }
                    if (itm.y() instanceof Double) {
                        yfmt = "%.7f";
                    }
                    if (itm.inf() instanceof Double) {
                        inffmt = "%.7f";
                    }
                    String[] record = {String.format(Locale.ROOT, xfmt, itm.x()), String.format(Locale.ROOT, yfmt, itm.y()), String.format(Locale.ROOT, inffmt, itm.inf())};
                    writer.writeNext(record);
                }
                String[] record = {"", "", ""};
                writer.writeNext(record);
            }

        } catch (IOException e) {
            System.out.println(" ! Error writing to file: " + fname);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                System.out.println(" ! Error closing file: " + fname);
            }
        }
    }

    public static ArrayList<ArrayList<Coord>> LoadArraysTxt(String fname, int verbose) {
        CSVReader reader = null;
        ArrayList<ArrayList<Coord>> readarrs = new ArrayList<>();

        if (verbose > 0) {
            if (PCJ.myId() == 0) {
                System.out.println("* Loading array from file: " + fname);
            }
        }

        try {
            reader = new CSVReader(new FileReader(fname), ';', CSVWriter.NO_QUOTE_CHARACTER, 0);
            String[] nextLine;
            ArrayList<Coord> new_path = new ArrayList<>();
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length < 3) {
                    continue;
                }
                if (nextLine[0].equals("")) {
                    readarrs.add(new_path);
                    new_path = new ArrayList<>();
                } else {
                    Object x = nextLine[0];
                    try {
                        x = Double.parseDouble(nextLine[0]);
                    } catch (NumberFormatException e) {
                    }
                    Object y = nextLine[1];
                    try {
                        y = Double.parseDouble(nextLine[1]);
                    } catch (NumberFormatException e) {
                    }
                    Object inf = nextLine[2];
                    try {
                        inf = Double.parseDouble(nextLine[2]);
                    } catch (NumberFormatException e) {
                    }
                    new_path.add(new Coord(x, y, inf));
                }
            }
        } catch (IOException e) {
            System.out.println(" ! Error reading file: " + fname);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                System.out.println(" ! Error closing file: " + fname);
            }
        }

        return readarrs;
    }

    public static void StoreArrayBinary(String fname, ArrayList<Coord> CoordArray, int verbose) {
        if (verbose > 0) {
            if (PCJ.myId() == 0) {
                System.out.println(" * Storing entires to file: " + fname);
            }
        }

        // remove if exists
        AuxTools.DeleteFile(fname);

        // convert to simpler array
//        ArrayList<String> array = new ArrayList<>();
//        CoordArray.forEach((coo) -> {
//            Double x = (Double) coo.x();
//            Double y = (Double) coo.y();
//            array.add(x.toString());
//            array.add(y.toString());
//        });

        // store file
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(fname);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(CoordArray);
        } catch (IOException e) {
            System.out.println(" ! Error writing to file: " + fname);
        }
    }

    public static ArrayList<Coord> LoadArrayBinary(String fname, int verbose) {
        ArrayList<Coord> readarr;
        FileInputStream fis;

        if (verbose > 0) {
            if (PCJ.myId() == 0) {
                System.out.println("* Loading array from file: " + fname);
            }
        }

        try {
            fis = new FileInputStream(fname);
            ObjectInputStream ois = new ObjectInputStream(fis);
            readarr = (ArrayList<Coord>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(" ! Error loading from file: " + fname);
            return null;
        }
        return readarr;
    }

    public static void StoreArraysBinary(String fname, ArrayList<ArrayList<Coord>> CoordArray, int verbose) {
        if (verbose > 0) {
            if (PCJ.myId() == 0) {
                System.out.println(" * Storing entires to file: " + fname);
            }
        }

        // remove if exists
        AuxTools.DeleteFile(fname);

        // store file
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(fname);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(CoordArray);
        } catch (IOException e) {
            System.out.println(" ! Error writing to file: " + fname);
        }
    }

    public static ArrayList<ArrayList<Coord>> LoadArraysBinary(String fname, int verbose) {
        ArrayList<ArrayList<Coord>> readarr;
        FileInputStream fis;

        if (verbose > 0) {
            if (PCJ.myId() == 0) {
                System.out.println("* Loading arrays from file: " + fname);
            }
        }

        try {
            fis = new FileInputStream(fname);
            ObjectInputStream ois = new ObjectInputStream(fis);
            readarr = (ArrayList<ArrayList<Coord>>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(" ! Error loading from file: " + fname);
            return null;
        }
        
        return readarr;
    }

    public static void PrintCoords(ArrayList<Coord> CoordArray) {
        CoordArray.forEach((itm) -> {
            System.out.println(itm.x() + " " + itm.y() + " " + itm.inf());
        });
    }
}
