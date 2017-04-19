import java.lang.Math;

public class UnitTestGmapUtils {
	public static void main(String[] args) {
		testConversions();
	}
	
	public static void testConversions() {
		double[]   COORD_A    = {41.85,-87.649999};
		double[][] coord_list = {COORD_A,COORD_A,COORD_A,COORD_A,COORD_A,COORD_A, COORD_A,COORD_A,COORD_A,COORD_A,COORD_A,COORD_A};
		int[][]    abspx_list = {{65,95},{131,190},{262,380},{525,761},{1050,1522},{2101,3045},{4202,6091},{8405,12182},{16811,24364},{33623,48729},{67247,97459},{134494,194918}};
		int[][]    tile_list  = {{0,0},{0,0},{1,1},{2,2},{4,5},{8,11},{16,23},{32,47},{65,95},{131,190},{262,380},{525,761}};
		int[]      zoom_list  = {0,1,2,3,4,5,6,7,8,9,10,11};
		for (int i = 0; i<12; i+=1) {
			//Given 'type'
			double[] coord       = coord_list[i];
			int[]    abspx       = abspx_list[i];
			int[]    tile        = tile_list[i];
			int      zoom        = zoom_list[i];
			System.out.format("MSG -- %d%n", i);
			System.out.format("MSG -- Testing Iinputs: (zoom, coord, px, tile) (%d, (%.4f,%.4f), (%d,%d), (%d,%d))%n", zoom, coord[0],coord[1], abspx[0],abspx[1], tile[0],tile[1]);
			//(double) 'type_errmargin' +- Reference Values For Lossy Conversions
			double[] err_abspx   = GmapUtils.abspx2latlonErrMargin(zoom,abspx[0],abspx[1]);
			double[] err_tile    = GmapUtils.tile2latlonErrMargin( zoom,tile[0], tile[1] );
			System.out.format("MSG -- Max Error of (px, tile) of lat-lon-zoom: ((%.4f,%.4f), (%.4f,%.4f))%n", err_abspx[0],err_abspx[1], err_tile[0],err_tile[1]);
			//Solve 'fromtype_totype' Conversions
			int[]    coord_abspx = GmapUtils.latlon2abspx(zoom,coord[0],coord[1]);
			int[]    coord_tile  = GmapUtils.latlon2tile( zoom,coord[0],coord[1]);
			double[] abspx_coord = GmapUtils.abspx2latlon(zoom,abspx[0],abspx[1]);
			double[] tile_coord  = GmapUtils.tile2latlon( zoom,tile[0], tile[1] );
			int[]    abspx_tile  = {abspx[0]/GmapUtils.TILE_SIZE, abspx[1]/GmapUtils.TILE_SIZE};
			//Check Correctness
			assert( (abspx[0]==coord_abspx[0])&&(abspx[1]==coord_abspx[0]) ); //lat-lon to abspx
			assert( (tile[0]==coord_tile[0])&&(tile[0]==coord_tile[1]) ); //lat-long to tile
			assert( Math.abs(coord[0]-abspx_coord[0])<err_abspx[0] && Math.abs(coord[1]-abspx_coord[1])<err_abspx[1]);//abspx to lat-long
			assert( Math.abs(coord[0]-tile_coord[0])<err_tile[0] && Math.abs(coord[0]-tile_coord[1])<err_tile[1]); //tile to lat-long
			assert( (tile[0]==abspx_tile[0])&&(tile[1]==abspx_tile[1]) ); //abspx to tile
		}
		System.out.format("MSG -- Tests Completed Successfully%n");
	}
}
