
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Create Google maps
 */
public class GoogleMap {

    final static String TemplateJS = "./maps/GoogleMap_template.js";  // Init map on Poland: lat, long, zoom
    final static String PLinit = "52.05, 19.45, 5";  // Init map on Poland: lat, long, zoom
    final static String WAWinit = "52.2296756, 21.012228700000037, 11";  // Init map on Warsaw: lat, long, zoom
    final static String PathStyle = "\"#00cc00\", 0.8, 2";  // Default path style: color, opacity, width // for light map, use this color of path: #880000
    final static String ButPattern = "<input id=\"but%d\" type=\"button\" value=\"%s\" onclick=\"resetmap(%d);\" />\n";

    static List<List<Object>> Locations = Arrays.asList(
            Arrays.asList("Polska", 7, 52.05, 19.45),
            Arrays.asList("Warszawa", 11, 52.2296756, 21.012228700000037),
            Arrays.asList("Wawa-Centrum", 13, 52.2296756, 21.012228700000037),
            Arrays.asList("ICM-Kupiecka", 15, 52.3207567110177, 21.015987396240234),
            Arrays.asList("Wawa-Wlochy", 15, 52.208014299528635, 20.90196132659912),
            Arrays.asList("Wawa-Wawer", 15, 52.181826554698716, 21.18992328643799)
    );

    static List<String> MapTemplate = new ArrayList<>();
    static List<String> MapFields = Arrays.asList(
            WAWinit, // initial view params: longitude, latitude, zoom
            "", // BTS markes to put on map: [x, y, #icon]
            "", // Bspot markes to put on map: [x, y, #icon]
            PathStyle, // path's: color, opacity, lines' width
            "", // path coords: [[x1, y1], [x2, y2], ..]
            "", // interesting locations: [x, y, zoom]
            "", // circular areas: [x, y, r (in metets)]
            "", // color style
            "false",  // BTS heat map
            "false",  // Bspot heat map
            "" // interesting sites buttons
    );

    public static void PrepareMap(ArrayList<Coord> bts_marks, ArrayList<Coord> bspots_marks,
            ArrayList<ArrayList<Coord>> paths, ArrayList<Coord> circles,
            boolean bts_heatmap, boolean bspot_heatmap, String ColorStyle,
            int verbose) {

        if (verbose > 0) {
            System.out.println("* Preparing Google map");
        }
        // Read JS Template
        ReadTemplate();

        // Set Locations
        SetLocations();

        // Set Color style
        SetColorStyle(ColorStyle);
        
        // Set BTS heatmap
        if (bts_heatmap) {
            MapFields.set(7, "var bts_heatmap = true;");
        } else {
            MapFields.set(7, "var bts_heatmap = false;");
        }

        // Set BTS heatmap
        if (bspot_heatmap) {
            MapFields.set(8, "var bspot_heatmap = true;");
        } else {
            MapFields.set(8, "var bspot_heatmap = false;");
        }

        // Reset
        MapFields.set(1, "");  // points (BTS)
        MapFields.set(2, "");  // points (Bspots)
        MapFields.set(4, "");  // paths

        // BTS
        StringBuilder BTSmarks = new StringBuilder();
        if ((bts_marks != null) && (bts_marks.size() > 0)) {
            bts_marks.forEach((itm) -> {
                BTSmarks.append(String.format(Locale.ROOT, "[%f, %f], ", itm.x(), itm.y()));
            });
        }
        if (BTSmarks.length() > 0) {
            MapFields.set(1, BTSmarks.toString());
        }

        // Blind spots
        StringBuilder Bspotmarks = new StringBuilder();
        if ((bspots_marks != null) && (bspots_marks.size() > 0)) {
            bspots_marks.forEach((itm) -> {
                Bspotmarks.append(String.format(Locale.ROOT, "[%f, %f], ", itm.x(), itm.y()));
            });
        }
        if (Bspotmarks.length() > 0) {
            MapFields.set(2, Bspotmarks.toString());
        }

        // Paths
        StringBuilder pathstr = new StringBuilder();
        if ((paths != null) && (paths.size() > 0)) {
            paths.forEach((itm) -> {
                pathstr.append("[");
                itm.forEach((i_itm) -> {
                    pathstr.append(String.format(Locale.ROOT, "[%f, %f], ", i_itm.x(), i_itm.y()));
                });
                pathstr.append("],");
            });
        }
        if (pathstr.length() > 0) {
            MapFields.set(4, pathstr.toString());
        }

        // Circles
        StringBuilder circs = new StringBuilder();
        if ((circles != null) && (circles.size() > 0)) {
            circles.forEach((itm) -> {
                circs.append(String.format(Locale.ROOT, "[%f, %f, %f], ", itm.x(), itm.y(), (double) itm.inf()));
            });
        }
        if (circs.length() > 0) {
            MapFields.set(6, circs.toString());
        }

    }

    public static void ReadTemplate() {
        try (BufferedReader br = Files.newBufferedReader(Paths.get(TemplateJS))) {
            MapTemplate = br.lines().collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println(" ! Error reading file: " + TemplateJS);
        }
    }

    public static void SetColorStyle(String color_style) {
        if (color_style.equals("light")) {
            MapFields.set(9, "style_light");
        } else {
            MapFields.set(9, "style_dark");
        }
    }

    public static void SetLocations() {
        StringBuilder locats = new StringBuilder();
        Locations.forEach((itm) -> {
            locats.append(String.format(Locale.ROOT, "[%f, %f, %d], ", itm.get(2), itm.get(3), itm.get(1)));
        });
        if (locats.length() > 0) {
            MapFields.set(5, locats.toString());
        }

        StringBuilder buts = new StringBuilder();
        for (int i = 0; i < Locations.size(); i++) {
            buts.append(String.format(ButPattern, i, Locations.get(i).get(0), i));
        }
        if (buts.length() > 0) {
            MapFields.set(10, buts.toString());
        }
    }

    public static void StoreMap(String fname, int verbose) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(fname);
            int i = 0;
            for (String str : MapTemplate) {
                if (str.contains("%s")) {
                    writer.write(str.format(MapFields.get(i)) + "\n");
                    i += 1;
                } else {
                    writer.write(str + "\n");
                }
            }
            if (verbose > 0) {
                System.out.println("  - Map saved: " + fname);
            }
        } catch (IOException e) {
            System.out.println(" ! Error writing to file: " + fname);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                System.out.println(" ! Error closing file: " + fname);
            }
        }
    }

}
