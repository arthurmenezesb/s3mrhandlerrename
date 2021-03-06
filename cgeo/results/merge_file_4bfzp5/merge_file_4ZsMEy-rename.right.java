package cgeo.geocaching;

import cgeo.geocaching.concurrent.Task;
import cgeo.geocaching.concurrent.BlockingThreadPool;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class StaticMapsProvider {

    private static final String MARKERS_URL = "http://cgeo.carnero.cc/_markers/";

    /** In my tests the "no image available" image had 5470 bytes, while "street only" maps had at least 20000 bytes */
    private static final int MIN_MAP_IMAGE_BYTES = 6000;

    /** ThreadPool restricting this to 1 Thread. **/
    private static final BlockingThreadPool pool = new BlockingThreadPool(1, Thread.MIN_PRIORITY);

    public static File getMapFile(final String geocode, String prefix, final int level, final boolean createDirs) {
        return LocalStorage.getStorageFile(geocode, "map_" + prefix + level, false, createDirs);
    }

    private static void downloadMapsInThread(final cgCache cache, String markerUrl, String prefix, String latlonMap, int edge, String waypoints) {
        downloadMap(cache, 20, "satellite", markerUrl, prefix, 1, latlonMap, edge, waypoints);
        downloadMap(cache, 18, "satellite", markerUrl, prefix, 2, latlonMap, edge, waypoints);
        downloadMap(cache, 16, "roadmap", markerUrl, prefix, 3, latlonMap, edge, waypoints);
        downloadMap(cache, 14, "roadmap", markerUrl, prefix, 4, latlonMap, edge, waypoints);
        downloadMap(cache, 11, "roadmap", markerUrl, prefix, 5, latlonMap, edge, waypoints);
    }

    private static void downloadMap(cgCache cache, int zoom, String mapType, String markerUrl, String prefix, int level, String latlonMap, int edge, String waypoints) {
        final String mapUrl = "http://maps.google.com/maps/api/staticmap?center=" + latlonMap;
        final String url = mapUrl + "&zoom=" + zoom + "&size=" + edge + "x" + edge + "&maptype=" + mapType + "&markers=icon%3A" + markerUrl + "%7C" + latlonMap + waypoints + "&sensor=false";
        final File file = getMapFile(cache.getGeocode(), prefix, level, true);
        final HttpResponse httpResponse = cgBase.request(url, null, false);
        if (httpResponse != null) {
            if (LocalStorage.saveEntityToFile(httpResponse.getEntity(), file)) {
                // Delete image if it has no contents
                final long fileSize = file.length();
                if (fileSize < MIN_MAP_IMAGE_BYTES) {
                    file.delete();
                }
            }
        }
    }

    public static void downloadMaps(cgCache cache, cgeoapplication app) {
        final Display display = ((WindowManager) app.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        downloadMaps(cache, display);
    }

    public static void downloadMaps(cgCache cache, Activity activity) {
        final Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        downloadMaps(cache, display);
    }

    public static void downloadMaps(cgCache cache, Display display) {
        if ((!Settings.isStoreOfflineMaps() && !Settings.isStoreOfflineWpMaps()) || StringUtils.isBlank(cache.getGeocode())) {
            return;
        }
        int edge = guessMinDisplaySide(display);
        if (Settings.isStoreOfflineMaps() && cache.getCoords() != null) {
            storeCacheStaticMap(cache, edge);
        }
        // download static map for current waypoints
        if (Settings.isStoreOfflineWpMaps() && CollectionUtils.isNotEmpty(cache.getWaypoints())) {
            for (cgWaypoint waypoint : cache.getWaypoints()) {
                storeWaypointStaticMap(cache, edge, waypoint);
            }
        }
    }

    public static void storeWaypointStaticMap(cgCache cache, Activity activity, cgWaypoint waypoint) {
        final Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int edge = StaticMapsProvider.guessMinDisplaySide(display);
        storeWaypointStaticMap(cache, edge, waypoint);
    }

    private static void storeWaypointStaticMap(cgCache cache, int edge, cgWaypoint waypoint) {
        if (waypoint.getCoords() == null) {
            return;
        }
        String wpLatlonMap = waypoint.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
        String wpMarkerUrl = getWpMarkerUrl(waypoint);
        // download map images in separate background thread for higher performance
        downloadMaps(cache, wpMarkerUrl, "wp" + waypoint.getId() + "_", wpLatlonMap, edge, "");
    }

    public static void storeCacheStaticMap(cgCache cache, Activity activity) {
        final Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int edge = StaticMapsProvider.guessMinDisplaySide(display);
        storeCacheStaticMap(cache, edge);
    }

    private static void storeCacheStaticMap(cgCache cache, int edge) {
        final String latlonMap = cache.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
        final StringBuilder waypoints = new StringBuilder();
        if (cache.hasWaypoints()) {
            for (cgWaypoint waypoint : cache.getWaypoints()) {
                if (waypoint.getCoords() == null) {
                    continue;
                }
                String wpMarkerUrl = getWpMarkerUrl(waypoint);
                waypoints.append("&markers=icon%3A");
                waypoints.append(wpMarkerUrl);
                waypoints.append("%7C");
                waypoints.append(waypoint.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA));
            }
        }
        // download map images in separate background thread for higher performance
        final String cacheMarkerUrl = getCacheMarkerUrl(cache);
        downloadMaps(cache, cacheMarkerUrl, "", latlonMap, edge, waypoints.toString());
    }

    private static int guessMinDisplaySide(Display display) {
        final int maxWidth = display.getWidth() - 25;
        final int maxHeight = display.getHeight() - 25;
        int edge = 0;
        if (maxWidth > maxHeight) {
            edge = maxWidth;
        } else {
            edge = maxHeight;
        }
        return edge;
    }

    private static void downloadMaps(final cgCache cache, final String markerUrl, final String prefix, final String latlonMap, final int edge, final String waypoints) {
        Task currentTask = new Task("getting static map") {

            @Override
            public void run() {
                downloadMapsInThread(cache, markerUrl, prefix, latlonMap, edge, waypoints);
            }
        };
        try {
            pool.add(currentTask, 20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(Settings.tag, "StaticMapsProvider.downloadMaps error adding task: " + e.toString());
        }
    }

    private static String getCacheMarkerUrl(final cgCache cache) {
        String type = cache.getType().id;
        if (cache.isFound()) {
            type += "_found";
        } else if (cache.isDisabled()) {
            type += "_disabled";
        }
        return cgBase.urlencode_rfc3986(MARKERS_URL + "marker_cache_" + type + ".png");
    }

    private static String getWpMarkerUrl(final cgWaypoint waypoint) {
        String type = waypoint.getWaypointType() != null ? waypoint.getWaypointType().id : null;
        return cgBase.urlencode_rfc3986(MARKERS_URL + "marker_waypoint_" + type + ".png");
    }

    public static void removeWpStaticMaps(int wp_id, String geocode) {
        for (int level = 1; level <= 5; level++) {
            try {
                if (wp_id > 0) {
                    StaticMapsProvider.getMapFile(geocode, "wp" + wp_id + "_", level, false).delete();
                }
            } catch (Exception e) {
                Log.e(Settings.tag, "StaticMapsProvider.removeWpStaticMaps: " + e.toString());
            }
        }
    }

    /**
     * Check if at least one map file exists for the given geocode.
     *
     * @param geocode
     * @return <code>true</code> if at least one mapfile exists; <code>false</code> otherwise
     */
    public static boolean doesExistStaticMapForCache(String geocode) {
        for (int level = 1; level <= 5; level++) {
            File mapFile = StaticMapsProvider.getMapFile(geocode, "", level, false);
            if (mapFile != null && mapFile.exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if at least one map file exists for the given geocode and waypoint ID.
     *
     * @param geocode
     * @param waypointId
     * @return <code>true</code> if at least one mapfile exists; <code>false</code> otherwise
     */
    public static boolean doesExistStaticMapForWaypoint(String geocode, int waypointId) {
        for (int level = 1; level <= 5; level++) {
            File mapFile = StaticMapsProvider.getMapFile(geocode, "wp" + waypointId + "_", level, false);
            if (mapFile != null && mapFile.exists()) {
                return true;
            }
        }
        return false;
    }
}
