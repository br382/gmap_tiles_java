import java.lang.Math;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.Graphics;
import javax.imageio.ImageIO;

/**
GmapRender() is an object to handle the gathering, merging, and formatting map tiles for an output image.<p>
<p>
Features:
	- Externally specified tile sources.
	- Auto-fits lat-long points to specified resolution.
	- Auto-finds zoom-level if more than one lat-long point given.
	- GUI friendly tile downloader management and status notifications.
	- (Optional) compass overlay for output image.
	- (Optional) multiple map sources may be overlayed if some sources are transparencies.
*/
public class GmapRender {
	//Externally Applied Settings ==========
	private int[]                _res          = {0,0};
	private ArrayList<double[]>  _coords_list;
	private double               _angle        = 0;               //Angle to rotate by
	private int[]                _compass_px   = {0,0,0,0};       //{min_x,min_y,size_wid,size_hei}
	private String               _compass_icon = new String("");  //if len(...)>0 { useCompassIcon(); }
	private String               _temp_path    = new String("."); //$(pwd) default image download folder
	//private _____               _map_sources  = {};
	//private _____               _map_order    = {};
	private int                 _threads_max   = 1;
	private int                 _retry_after   = 900; //seconds
	//Internal State Values ==========
	/**Tile zoom level currently used for layers.*/
	private int                 _zoom          = 0;
	/**Bounding coordinate pairs {a_lat,a_lon,b_lat,b_lon}*/
	private ArrayList<double[]> _coord_bounds  = new ArrayList<double[]>(){{ add(new double[]{0.0,0.0}); add(new double[]{0.0,0.0}); }};
	private double[]            _coord_center  = {0.0,0.0};
    private double              _radius_km         = 1.0;
    private double              _radius_km_default = 1.0;
    private BufferedImage       _image_temp;
	
	//Constructor ==========
	public GmapRender() {
	}
	
	//Setters ==========
	/**
	* this.setResolution(int x, int y) {return;}
	* <p>
	* Notes:<p>
	* 	Sets the required output image resolution.<p>
	*/
	public void setResolution(int x, int y) { this._res[0] = x; this._res[1] = y; }
	
	/**
	* this.setcoordList( ArrayList<double[]> coord_list) {return;}
	* <p>
	* Notes:<p>
	* 	Define region to render by Arraylist of double[]{lat,lon} pairs to include within the region.<p>
    *   Also sets the internal values: radius_km_default, coord_center, coord_bounds.<p>
	*/
	public void setCoordList(ArrayList<double[]> coord_list, double radius_km_default) {
        this._radius_km_default = radius_km_default;
		this._coords_list       = new ArrayList<>(coord_list); //object copy, not reference
        this._coord_center      = GmapUtils.latlonCenter( this._coords_list );
        if (this._coords_list.size()>1) {
            this._radius_km     = GmapUtils.latlonRadius( this._coord_center, this._coords_list, GmapUtils.EARTH_RADIUS_KM );
        } else {
            this._radius_km     = this._radius_km_default;
        }
        this._coord_bounds      = GmapUtils.boundingCoordinates( this._coord_center, this._radius_km, GmapUtils.EARTH_RADIUS_KM );
	}
	
	/**
	* this.setHeading(double angle) {return;}
	* <p>
	* Notes:<p>
	* 	Sets angle (degrees) to rotate north-facing (top=north) orientation for formatted output.<p>
	* 	Rotation direction is defined by the image rotation library, and is direction geometric (angle>0 is rotation ccw) definition.<p>
	*/
	public void setHeading(double angle) { this._angle = angle; }
	
	/**
	* this.setCompassOverlay(int[] size={wid,hei}, int[] position={x,y}, String icon_file='./compass.png') {return;}
	* <p>
	* 	@param size		Resize icon_file to this resolution.
	* 	@param position		Place the resized icon_file at this position in the output image, when pixel origin (0,0) is defined by the image library (top-left).
	* 	@param icon_file	The icon/image file location, or "" if the compass overlay is disabled.
	*/
	public void setCompassOverlay(int[] size, int[] position, String icon_file) {
		this._compass_icon  = new String(icon_file);
		this._compass_px[0] = size[0];
		this._compass_px[1] = size[1];
		this._compass_px[2] = position[0];
		this._compass_px[3] = position[1];
	}
	
	public void setTempDir(String path) {
		if (path.length() <=0) { this._temp_path = new String(".");  }
		else                   { this._temp_path = new String(path); }
	}
	
	public void setImageSources() {
	}
	
	/**
	* this.setThreads(int max_threads) {return;}
	* <p>
	* Notes:<p>
	* 	Specifies the maximum number of threads that may be spawned at one time to handle tile downloading.<p>
	*/
	public void setThreads(int max_threads) { if (max_threads>0) { this._threads_max = max_threads; }}
	
	/**
	* this.setRetryPeriod(int retry_after) {return;}
	* <p>
	* Notes:<p>
	* 	If a download job fails, after a period of time (default=900 seconds), the job will be retried.<p>
	* 	If a job has passed retry_after, it still may have to wait longer due to other jobs in the queue.
	* @param retry_after Seconds integer that specifies the minimum period to wait before the job is allowed to be retried.
	*/
	public void setRetryPeriod(int retry_after) { if (retry_after>=0) { this._retry_after = retry_after; }}
	
	//Getters ==========
	public BufferedImage update() {
        return( this._image_temp );
	}
	
	public int checkWorkers() { return(0);
	}
	
	public int inQueue() { return(0);
	}
	
	public int inThreads() { return(0);
	}
	
	/**
	* this.px2latlon(int x, int y) { return new double[] {lat, lon}; }
	* <p>
	* Notes:<p>
	* 	On output image of set resolution, translate pixel index into a latitude-longitude pair. <p>
	* 	Given input int[]{x,y} within [0,resolution), return double[]{lat,lon}.<p>
	* 	Otherwise throw ArithmeticException.<p>
	*/
	public double[] px2latlon(int x, int y) {
		if ((x<0)||(x>=this._res[0])||(y<0)||(this._res[1]>=y)) {
			throw new ArithmeticException("ERR -- GmapRender.px2latlon -- Queried pixel outside valid range.");
		}
		int[]    c_imgpx = {this._res[0],this._res[1]};
		double[] c_coord = GmapUtils.latlonCenter( this._coord_bounds );
		int[]    c_abspx = GmapUtils.latlon2abspx( this._zoom, c_coord[0], c_coord[1] );
		int[]    d_abspx = {c_abspx[0]-c_imgpx[0], c_abspx[1]-c_imgpx[1]};
		int[]    t_abspx = {x + d_abspx[0], y + d_abspx[1]};
		return( GmapUtils.abspx2latlon( this._zoom, t_abspx[0], t_abspx[1]) ); //target lat-long
	}
	
	/**
	* this.latlon2px(double lat, double lon) { return( new int[]{x,y}); }
	* 
	* Notes:<p>
	* 	Given a (lat,lon) coorinate pair, return the pixel coordinates on the output image.<p>
	* 	This function will always return integers, even if they are outsize the image resolution and may be negitive.<p>
	* @param lat Latitude value.
	* @param lon Longitude value.
	* @return    Output image pixel coordinates, not guaranteed to be within the image resolution, as an integer array of size 2.
	*/
	public int[] latlon2px(double lat, double lon) {
		int[]    center_imgpx = {this._res[0]/2,this._res[1]/2};
		int[]    center_abspx = GmapUtils.latlon2abspx( this._zoom, this._coord_center[0], this._coord_center[1] );
		int[]    target_abspx = GmapUtils.latlon2abspx( this._zoom, lat, lon);
		int[]    offset_abspx = {center_abspx[0]-center_imgpx[0], center_abspx[1]-center_imgpx[1]};
		int[]    target_imgpx = {target_abspx[0]-offset_abspx[1], target_abspx[1]-offset_abspx[1]};
		return( new int[]{ target_imgpx[0], target_imgpx[1]} );
	}
	
	//Helpers ==========
	/**
	* _countfiles(String path) { return (int)count_files_in_path; }
	* <p>
	* Notes:<p>
	* 	Returns the count of files in the specified directory, non-recursively.<p>
	* 	Does not handle file/io exceptions that may be thrown.<p>
	*/
	private static int _countFiles(String path) {
		return( new File(path).list().length );
	}
	
	private static BufferedImage _mergeLayers(ArrayList<BufferedImage> img_list) {
        int width  = 0;
        int height = 0;
        for (int i = 0; i<img_list.size(); i+= 1) {
            width  = Math.max( width,  img_list.get(i).getWidth()  );
            height = Math.max( height, img_list.get(i).getHeight() );
        }
        BufferedImage composite = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
        Graphics g = composite.getGraphics();
        for (int i = 0; i<img_list.size(); i+= 1) {
            g.drawImage( img_list.get(i), 0,0, null);
        }
        return(composite);
	}
	
	private static ArrayList<String> _worker(String url, String useragent, String filename, int id) {
		return( new ArrayList<String>(){{add("");}} );
	}
	
	/**
    * this._findTiles() { return(new int[5]{tile_x_min,tile_x_max,tile_y_min,tile_y_max,zoom}); }
    * 
    * Notes:
    *   Uses intermediate values set by this.setCoordList(...)
    *
    * @see setCoordList
    */
	private int[] _findTiles() {
        int    radius_px       = (int)( Math.ceil(Math.sqrt( this._res[0]*this._res[0] + this._res[1]*this._res[1] )) );
        int[]  square_imgpx    = new int[2];
               square_imgpx[0] = (int)(Math.ceil(Math.sqrt(8.0)*(double)radius_px)); //expand to allow for this._res to be rotated within square_imgpx bounds.
               square_imgpx[1] = square_imgpx[0];
               this._zoom      = GmapUtils.zoomFromCoords( square_imgpx, this._coord_bounds.get(0), this._coord_bounds.get(1), this._coord_center );
        int[]  tile_bounds     = GmapUtils.tileBounds( this._zoom, this._coord_bounds.get(0), this._coord_bounds.get(1) );
        return( new int[]{ tile_bounds[0],tile_bounds[1],tile_bounds[2],tile_bounds[3], this._zoom} );
	}
	
	private String _genUrl(int uid, int x, int y, int zoom) { return( new String("") );
	}
	
	private String _genFilename(int uid, int x, int y, int zoom) {
        String ext = new String("jpg");
        if (true) throw new UnsupportedOperationException("ERR -- GampRender._genFilename -- Extension fetching is not completed.");
        return String.format("%d_%d_%d_%d.%s", uid, x, y, zoom, ext);
	}
	
	private void _queueTiles(int uid, int x_min, int x_max, int y_min, int y_max, int zoom) {
	}
	
	private BufferedImage _mergeTiles(int uid, int x_min, int x_max, int y_min, int y_max, int zoom) {
        int width  = GmapUtils.TILE_SIZE * (x_max - x_min + 1);
        int height = GmapUtils.TILE_SIZE * (y_max - y_min + 1);
        BufferedImage image_layer = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
        Graphics      graph_layer = image_layer.getGraphics();
        for (int x = x_min; x<=x_max; x+=1) {
            for (int y = y_min; y<=y_max; y+=1) {
                String        fname = this._genFilename( uid, x,y,this._zoom);
                File          fd    = new File( fname );
                if ((fd.exists())&&(fd.isFile())) {
                    try {
                        BufferedImage img = ImageIO.read( fd );
                        graph_layer.drawImage( img, GmapUtils.TILE_SIZE*(x-x_min), GmapUtils.TILE_SIZE*(y-y_min), null);
                    } catch (IOException e) {
                        System.err.println("ERR -- GmapRender._mergeTiles -- Error reading input file " + fname + "  " + e.getMessage());
                    }
                }
            }
        }
        return( image_layer );
    }
	
    /**
    * this._orientLayer(BufferedImage in_image) { return in_image.subsectionOfSize(wid,hei); }
    * 
    * Notes:
    *   This method does not return a new image object, but a subsection reference to the original.
    *   In other words, a change made to either the input or output image, is shared by the other.
    *   This is because BufferedImage.getSubimage(x,y,w,h) is used for the cropping the dimensions.
    * 
    *   @return BufferedImage in_image.subsectionOfSize(wid,hei)  In other words the input and output share the same mutable data, and color change in one will affect the other.
    */
	private BufferedImage _orientLayer(BufferedImage in_image) {
        int   diameter_px       = (int)(Math.ceil(Math.sqrt( this._res[0]*this._res[0] + this._res[1]*this._res[1] )));
        int[] center_abspx      = GmapUtils.latlon2abspx( this._zoom, this._coord_center[0], this._coord_center[1] );
        int[] min_abspx         = GmapUtils.latlon2abspx( this._zoom, this._coord_bounds.get(0)[0], this._coord_bounds.get(0)[1] );
        int[] min_off_abspx     = {min_abspx[0] % GmapUtils.TILE_SIZE, min_abspx[1] % GmapUtils.TILE_SIZE};
        return( in_image.getSubimage(min_off_abspx[0],min_off_abspx[1], diameter_px,diameter_px) );
	}
	
    /**
    * this._orientOutput( BufferedImage in_image) { return new BufferedImage(); }
    * 
    * Notes:
    *   Given an assumed square input image, rotate and crop to this._res output resolution.
    * @return new BufferedImage() of size {this._res[0], this._res[1]} == {width, height}
    */
	private BufferedImage _orientOutput(BufferedImage in_image) {
        int                radius    = (int)Math.ceil(Math.sqrt( this._res[0]*this._res[0] + this._res[1]*this._res[1] )/2.0);
        int[]              center    = new int[]{ in_image.getWidth()/2, in_image.getHeight()};
        double             angle     = Math.toRadians( this._angle );
        AffineTransform    rot       = new AffineTransform();
                           rot.rotate( angle, center[0], center[1] );
        AffineTransformOp  rot_oper  = new AffineTransformOp(rot, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage      out_image = new BufferedImage(in_image.getWidth(), in_image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                           out_image = rot_oper.filter(  in_image, null );
        int[]              box       = new int[] {center[0]-radius, center[1]-radius, this._res[0], this._res[1] }; //min_x,min_y,width,height
                           out_image.getSubimage(box[0], box[1], box[2], box[3]);
        return( out_image );
	}
	
    /**
    * this._compassGen() { return new BufferedImage(); }
    * 
    * Notes:
    *   
    */
	private BufferedImage _compassGen() {
        BufferedImage      layer       = new BufferedImage( this._res[0], this._res[1], BufferedImage.TYPE_INT_ARGB );
        BufferedImage      in_image    = new BufferedImage( 0,0, BufferedImage.TYPE_INT_ARGB );
        File               fd          = new File( this._compass_icon );
        if ((fd.exists())&&(fd.isFile())) {
            try {
                in_image = ImageIO.read( fd );
            } catch (IOException e) {
                System.err.println("ERR -- GmapRender._compassGen -- Error reading input file " + this._compass_icon + "  " + e.getMessage());
            }
        }
        double             angle       = Math.toRadians( this._angle );
        AffineTransform    rot         = new AffineTransform();
                           rot.rotate( angle, in_image.getWidth()/2, in_image.getHeight()/2 );
        AffineTransformOp  rot_oper    = new AffineTransformOp(rot, AffineTransformOp.TYPE_BILINEAR);
                           in_image    = rot_oper.filter( in_image, null );
        Graphics           g           = layer.getGraphics();
                           g.drawImage( in_image, this._compass_px[0], this._compass_px[1], this._compass_px[2], this._compass_px[3], null); //min_x, min_y, width, height
        return( layer );
	}
}
