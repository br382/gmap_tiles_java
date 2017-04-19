import java.lang.Math;
import java.util.ArrayList;

public class GmapUtils {
	/**
	GmapUtils is a collection of measurement conversions
	for use between lat-lon-km, pyramid (absolute) pixels (px,py,zoom) and tile index (tx,ty,zoom)
	which are commonly used with web map viewers.
	
	Pyramid pixels and tile indexes are very similar, because
	their zoom values are the same, and (px,py)/TILE_SIZE == (tx,ty).
	*/
	public static double EARTH_RADIUS_KM  =  6371.03;
	public static int    TILE_SIZE        =  256;
	public static double MIN_LAT          = -Math.PI/2.0;
	public static double MAX_LAT          =  Math.PI/2.0;
	public static double MIN_LON          = -Math.PI;
	public static double MAX_LON          =  Math.PI;
        public static int    MAX_ZOOM         =  20; //int() may overflow greatly exceeded, and most map sources specify an upper limit is below this.
	
	public static int maxAbspx(int zoom) {
		/**
		maxAbspx(int zoom) { return pixel_dimension_max; }
		
		Notes:
			Used for determining the pyramid pixels range of a dimension.
			Valid (px,py) are specified as: int(), bound=[-1, max ).
		*/
		if ((zoom > MAX_ZOOM)||(zoom<0)) { throw new IllegalArgumentException("ERR -- GmapUtils.maxAbspx -- Invalid Zoom Level"); }
		return( TILE_SIZE * (int)(Math.pow(2,zoom)) );
	}
	
	public static double[] abspx2latlon(int zoom, int x, int y) {
		/**
		abspx2latlon(int zoom, int x, int y) { return new double[] {lat, lon}; }
		
		Notes:
			Given:        (px,py) = latlon2px(zoom,lat,lon)
			Error Margin: (lat,lon) - abspx2latlon(zoom,px,py) < abspx2latlonErrMargin(zoom,px,py)
			Some exceptions apply, see abspx2latlonErrMargin(...) for details.
		*/
		int max = maxAbspx(zoom);
		while (x<0)   { x += max; }
		while (y<0)   { y += max; }
		while (x>max) { x -= max; }
		while (y>max) { y -= max; }
		double lon  = ( ((double)x)*360.0 / ((double)max) ) - 180.0;
		while (lon > 180.0) { lon -= 360.0; }
		while (lon < 180.0) { lon += 360.0; }
		double expo = ( (double)(y-( ((double)TILE_SIZE) * Math.pow(2.0,(double)zoom) )) / ( ((double)max)/(-2.0*Math.PI)) );
		double lat  = ( ((2.0*Math.atan(Math.exp(expo))) -(Math.PI/2.0) ) / (Math.PI/180.0) );
		return(new double[] {lat,lon});
	}
	
	public static double[] tile2latlon(int zoom, int x, int y) {
		/**
		tile2latlon(int zom, int x, int y) { return new double[] {lat,lon}; }
		*/
		return( abspx2latlon(zoom, x*TILE_SIZE, y*TILE_SIZE) );
	}
	
	public static double[] abspx2latlonErrMargin(int zoom, int x, int y, int px_off) {
		/**
		abspx2latlonErrMargin(int zoom, int x, int y, int px_off=1) { return new double[] {lat,lon}; }
		
		Notes:
			Returns positive maximum lat-lon variance within a px_off difference.
			Ignores possible errors created by poles, and  360/180/90 deltas may silently be returned as 0.
			Assumes input, and input+px_off pixels are valid.
			Assumes input+px_off is max difference on an approximate sphere shape earth model.
		*/
		double[] val   = abspx2latlon(zoom, x,        y       );
		double[] val_x = abspx2latlon(zoom, x+px_off, y       );
		double[] val_y = abspx2latlon(zoom, x,        y+px_off);
		double   lat   = Math.max( Math.abs(val[0]-val_x[0]), Math.abs(val[0]-val_y[0]) );
		double   lon   = Math.max( Math.abs(val[1]-val_x[1]), Math.abs(val[1]-val_y[1]) );
		return( new double[] {lat,lon} );
	}
	
	public static double[] abspx2latlonErrMargin(int zoom, int x, int y) {
		return abspx2latlonErrMargin(zoom,x,y,1);
	}
	
	public static double[] tile2latlonErrMargin(int zoom, int tx, int ty) {
		/**
		tile2latlonErrMargin(int zoom, int tx, int ty) { return new double[] {lat,lon}; }
		
		Notes:
			See abspx2latlonErrMargin(...) for usage and assumptions.
		*/
		return abspx2latlonErrMargin(zoom, tx*TILE_SIZE, ty*TILE_SIZE, TILE_SIZE);
	}
	
	public static int[] latlon2abspx(int zoom, double lat, double lon) {
		/**
		latlon2abspx(int zoom, double lat, double lon) { return new int[] {px,py}; }
		
		Notes:
			Converts lat-long to nearest pyramid (absolute) pixel at a zoom level.
		*/
		double xd      = ((double)TILE_SIZE) * Math.pow(2,zoom) * (lon+180.0) / 360.0;
		double yd     = -(0.5*Math.log((1.0+Math.sin(Math.toRadians(lat))) / (1.0-Math.sin(Math.toRadians(lat)))) / Math.PI-1.0)*TILE_SIZE*Math.pow(2,zoom-1);
		double max_px = maxAbspx(zoom);
		int x = (int)xd;
		int y = (int)yd;
		if ((x<0)||(x>=max_px)||(y<0)||(y>=max_px)) { throw new ArithmeticException("ERR -- GmapUtils.latlon2abspx -- Equation error, bound exceeded."); }
		return( new int[] {x, y}  );
	}
	
	public static int[] latlon2tile(int zoom, double lat, double lon) {
		/**
		latlon2abspx(int zoom, double lat, double lon) { return new int[] {tx,ty}; }

		
		Notes:
			Uses latlon2abspx(...) scaled into tiles.
		*/
		int[] val = latlon2abspx(zoom,lat,lon);
		val[0] = val[0] / TILE_SIZE;
		val[1] = val[1] / TILE_SIZE;
		return( val );
	}
	
	public static double[] latlon2xyz(double lat, double lon) {
		/**
		latlon2xyz(double lat, double lon) { return new double[] {x,y,z}; }
		
		Notes:
			Converts lat-lon degrees into unit cartesian vectors.
			Usually not used directly, but as a helper function.
		*/
		double rad_lat = Math.toRadians(lat);
		double rad_lon = Math.toRadians(lon);
		double x       = Math.cos(rad_lat) * Math.cos(rad_lon);
		double y       = Math.cos(rad_lat) * Math.sin(rad_lon);
		double z       = Math.sin(rad_lat);
		return( new double[] {x,y,z} );
	}
	
	public static double[] xyz2latlon(double x, double y, double z) {
		/**
		xyz2latlon(double x, double y, double z) { return new double[] {lat,lon}; }
		
		Notes:
			Converts unit cartesian vectors to lat-lon degrees.
			Usually not used directly, but as a helper function.
		*/
		double rad_lon = Math.atan2(y,x);
		double hyp     = Math.sqrt(x*x + y*y);
		double rad_lat = Math.atan2(z, hyp);
		double lat = Math.toDegrees(rad_lat);
		double lon = Math.toDegrees(rad_lon);
		return( new double[] {lat,lon} );
	}
	
	public static double[] latlonCenter(ArrayList<double[]> points){
		/**
		latlonCenter(ArrayList<double[]> points) { return new double[] {lat,lon}; }
		
		Notes:
			Center calculated by assuming a sphere, averaging the cartesian values, and converting back to degrees.
			This is an estimation method, and does not use triangulation or arc paths of an ellipsoid.
		*/
		double[] avg = {0.0, 0.0, 0.0};
		int len      = points.size();
		for (int ind = 0; ind < len; ind+=1) {
			double[] val = points.get(ind);
			avg[0] += val[0];
			avg[1] += val[1];
			avg[2] += val[2];
		}
		avg[0] = avg[0] / ((double)len);
		avg[1] = avg[1] / ((double)len);
		avg[2] = avg[2] / ((double)len);
		return( xyz2latlon(avg[0],avg[1],avg[2]) );
	}
	
	public static double distanceTo(double[] coord_a, double[] coord_b, double sphere_radius){
		/**
		distanceTo(double[] {lat_a,lon_a, double[] {lat_b,lon_b}, double sphere_radius) { return (double)dist_km; }
		
		Notes:
			Calculates the arc distance between two points by assuming the shell of sphere.
		*/
		double rad_a_lat = Math.toRadians(coord_a[0]);
		double rad_a_lon = Math.toRadians(coord_a[1]);
		double rad_b_lat = Math.toRadians(coord_b[0]);
		double rad_b_lon = Math.toRadians(coord_b[1]);
		double accum  = Math.sin(rad_a_lat) * Math.sin(rad_b_lat);
		       accum += Math.cos(rad_a_lat) * Math.cos(rad_b_lat) * Math.cos(rad_a_lon - rad_b_lon);
		       accum  = Math.acos(accum);
		return( accum * sphere_radius );
	}
	
	public static double latlonRadius(double[] center_coord, ArrayList<double[]> points, double sphere_radius) {
		/**
		latlonRadius(double[] center_coord, ArrayList<double[]> points, double sphere_radius) { return (double)dist_km; }
		
		Notes:
			Given a known center for a set of points, brute force the maximum distance from the center as the radius.
			See distanceTo(...) for distance measurement assumptions.
		*/
		double dist = 0.0;
		for(int ind = 0; ind<points.size(); ind +=1) {
			double val = distanceTo(center_coord, points.get(ind), sphere_radius);
			if (val > dist) { dist = val; }
		}
		return(dist);
	}
	
	public static double[] boundingCoordinates(double[] point, double dist, double sphere_radius) {
		/**
		boundingCoordinates(double[] {lat,lon}, double dist, double sphere_radius) { return new double[] {a_lat,a_lon,b_lat,b_lon}; }
		
		Notes:
			Given a center point (lat,lon) with point radius (dist), and assumed sphere radius (sphere_radius),
			find the bounding box as a lat-long pair (a_lat,a_lon,b_lat,b_lon).
			Assumes a shell of a sphere with given radius.
			Handles angular coordinates boundry conditions and overflows.
		Reference: http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates
		*/
		if ((dist<0.0) || (sphere_radius<0)) { throw new ArithmeticException("ERR -- GmapUtils.boundingCoorinates -- Invalid Ranged Input"); }
		double rad_lat  = Math.toRadians(point[0]);
		double rad_lon  = Math.toRadians(point[1]);
		double rad_dist = dist / sphere_radius;
		double a_lat    = rad_lat - rad_dist;
		double a_lon    = 0.0;
		double b_lat    = rad_lat + rad_dist;
		double b_lon    = 0.0;
		if ((a_lat > MIN_LAT) && (b_lat < MAX_LAT)) {
			double delta_lon = Math.asin( Math.sin(rad_dist) / Math.cos(rad_lat) );
			       a_lon     = rad_lon - delta_lon;
			if (a_lon < MIN_LON) { a_lon += 2.0 * Math.PI; }
			b_lon = rad_lon + delta_lon;
			if (b_lon > MAX_LON) { b_lon -= 2.0 * Math.PI; }
		} else {// A Pole is within the distance.
			a_lat = Math.max(a_lat, MIN_LAT);
			b_lat = Math.min(b_lat, MAX_LAT);
			a_lon = MIN_LON;
			b_lon = MAX_LON;
		}
		return( new double[] { Math.toDegrees(a_lat), Math.toDegrees(a_lon), Math.toDegrees(b_lat), Math.toDegrees(b_lon) });
	}
	
	public static int[] abspxBounds(int zoom, double[] coord_a, double[] coord_b) {
		/**
		abspxBounds(int zoom, double[] coord_a, double[] coord_b) { return new int[]{min_x,min_y,max_x,max_y,count_x,count_y}; }
		
		Notes:
			Given a zoom, and two bounding lat-lon pairs,
			return the bounding absolute pixels (min_x,min_y,max_x,max_y) pairs,
			and number of (inclusive) pixels along each side of the pixel bounding box.
		*/
		int[] abspx_a     = latlon2abspx(zoom,coord_a[0],coord_a[1]);
                int[] abspx_b     = latlon2abspx(zoom,coord_b[0],coord_b[1]);
                int[] abspx_min   = { Math.min(abspx_a[0],abspx_b[0]), Math.min(abspx_a[1],abspx_b[1]) };
                int[] abspx_max   = { Math.max(abspx_a[1],abspx_b[1]), Math.max(abspx_b[1],abspx_b[1]) };
                int[] abspx_count = { Math.abs(abspx_max[0]-abspx_min[0]+1), Math.abs(abspx_max[1]-abspx_min[1]+1) }; //inclusive range
                return( new int[] { abspx_min[0],abspx_min[1],abspx_max[0],abspx_max[1],abspx_count[0],abspx_count[1] } );
	}
	
	public static int[] tileBounds(int zoom, double[] coord_a, double[] coord_b) {
		/**
		tileBounds(int zoom, double[] coord_a, double[] coord_b) { return new int[] {min_x,min_y,max_x,max_y,count_x,count_y}; }
		
		Notes:
			Given a zoom, and two bounding lat-long pairs,
			return the bounding tiles (min_x,min_y,max_x,max_y) pairs,
			and number of (inclusive) pixels on the sides fo the tile bounding box.
			Uses abspxBounds(...) and resizes into tiles.
		*/
		int[] tile = abspxBounds(zoom,coord_a,coord_b);
		for (int i = 0; i<6; i+=1) {
			tile[i] = tile[i]/TILE_SIZE;
		}
		tile[4] = tile[2]-tile[0]+1;
		tile[5] = tile[3]-tile[1]+1;
		return( new int[] {tile[0],tile[1],tile[2],tile[3],tile[4],tile[5]} );
	}
	
	public static int zoomFromCoords(int[] res_box, double[] coord_a, double[] coord_b, double[] coord_center) {
		/**
		zoomForCoords(int[] res_box, double[] coord_a, double[] coord_b, double[] coord_center) { return (int)zoom_level; }
		
		Notes:
			Find a zom level for a:   res_box     ={pixel_width,pixel_height}
			with res_box centered at: coord_center={lat,lon}
			and image tiles spanning: coord_a, coord_b
			Returns: (int)zoom_level
			This fuction will not account for rotation, if wanted increase the res_box size to accomidate rotate radius.
		*/
		if ((res_box[0]<0)||(res_box[1]<0)) { throw new ArithmeticException("ERR -- GmapUtils.zoomFromCoords -- Bounding Box Has Negitive Size"); }
		int     zoom    = 0;
		int[]   size_px = {0,0};
		boolean fits    = false;
		while ((size_px[0]<res_box[0]) || (size_px[1]<res_box[1]) || (fits!=true)) {
			int[] pixel_bound = abspxBounds(zoom,coord_a,coord_b);
			int[] tile_bound  = tileBounds( zoom,coord_a,coord_b);
			size_px[0]        = tile_bound[4]*TILE_SIZE;
			size_px[1]        = tile_bound[5]*TILE_SIZE;      //if downloaded this would be the composite image size
			int[] center_px   = latlon2abspx(zoom,coord_center[0],coord_center[1]);
			center_px[0]      = center_px[0] - pixel_bound[0];
			center_px[1]      = center_px[1] - pixel_bound[1]; // change (0,0) from absolute px to composite image (0,0) domain
			int[] offset_px   = {center_px[0]-(res_box[0]/2), center_px[1]-(res_box[1]/2)}; //difference betweeen composite image (0,0) and res_box (0,0) domain
			int[] fit_px      = {center_px[0]+offset_px[0], center_px[1]+offset_px[1]}; //furthest (x,y) needed in composite image to fit res_box
			if ((size_px[0]>fit_px[0])&&(fit_px[1]>fit_px[1])) { fits = true; }
			zoom += 1;
			if (zoom>MAX_ZOOM) { break; }
		}
		return(zoom-1);
	}
}
