package com.example.position2shp;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.util.ListenableList;

public class MainActivity extends AppCompatActivity {

	TextView textViewGpsX, textViewGpsY, textViewGpsZ, label;

	private static final String[] LOCATION_PERMS = {
			Manifest.permission.ACCESS_FINE_LOCATION
	};
	private static final int INITIAL_REQUEST = 1337;
	private static final int LOCATION_REQUEST = INITIAL_REQUEST + 3;

	final DecimalFormat df = new DecimalFormat("#.#####");
	final DecimalFormat dfUtm = new DecimalFormat("#.##");
	public double time = 0;

	PointCollection polygonPoints;
	Feature myFeature;

	private SensorManager sm;
	private LocationManager lm;

	private MyLocationListener listener;

	private LocationDisplay mLocationDisplay;

	RadioButton utmOrWgs;
	Button start;
	RadioButton addEdit;

	WGS84ToUTM wgsToUtm = new WGS84ToUTM();

	PointCollection polyLineCollection;
	Polyline tracking;

	boolean gps_enabled = false;
	boolean network_enabled = false;

	static String mShapefilePath2;

	Location net_loc = null, gps_loc = null;
	GraphicsOverlay overlay;

	boolean trackingStarted;

	double lat = 47.5;
	double longi = 9.01;

	double utm1Old, utm2Old = 0;

	private MapView mMapView;
	ArcGISMap map;

	Shapefile backgroundShp;

	Shapefile editShp;

	boolean wgs84 = true;
	boolean add = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
				ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
				ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
				ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{
							Manifest.permission.READ_EXTERNAL_STORAGE,
							Manifest.permission.WRITE_EXTERNAL_STORAGE,
							Manifest.permission.ACCESS_FINE_LOCATION,
							Manifest.permission.ACCESS_COARSE_LOCATION},
					1);
		}

		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		listener = new MyLocationListener(this, this);

		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, listener);
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 100, 0, listener);

		sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		setContentView(R.layout.activity_main);

		textViewGpsX = (TextView) findViewById(R.id.gps_x);
		textViewGpsY = (TextView) findViewById(R.id.gps_y);
		textViewGpsZ = (TextView) findViewById(R.id.gps_z);
		label = (TextView) findViewById(R.id.textView6);

		utmOrWgs = (RadioButton) findViewById(R.id.radioButton);
		start = (Button) findViewById(R.id.start);
		addEdit = (RadioButton) findViewById(R.id.add);
		String text = getResources().getString(R.string.addOrEdit);
		SpannableString content = new SpannableString(text);
		content.setSpan(new UnderlineSpan(), 0, 3, 0);
		addEdit.setText(content);

		mMapView = (MapView) findViewById(R.id.mapView);
		SpatialReference spatRef = SpatialReference.create(25832);
		map = new ArcGISMap(spatRef);
		mMapView.setMap(map);

		map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, lat, longi, 16);
		mMapView.setMap(map);
		mLocationDisplay = mMapView.getLocationDisplay();

		String mShapefilePath = Environment.getExternalStorageDirectory().getPath() + "/ArcGIS/Samples/Shapefile/BW_Gebiet_Kreis/AX_Gebiet_Kreis.shp";
		mShapefilePath2 = Environment.getExternalStorageDirectory().getPath() + "/ArcGIS/Samples/Shapefile/EmptyShapefileUTM/EmptyPolyline.shp";

		// create the Symbol
		SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 1.0f);
		SimpleFillSymbol fillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.TRANSPARENT, lineSymbol);
		SimpleRenderer renderer = new SimpleRenderer(fillSymbol);

		// add the feature layer to the map
		backgroundShp = new Shapefile(map, mShapefilePath, renderer);

		// create the Symbol
		SimpleLineSymbol lineSymbol2 = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 1.0f);
		renderer = new SimpleRenderer(lineSymbol);

		editShp = new Shapefile(map, mShapefilePath2, renderer);
	}

	public void startstop(View view) {
		switch (view.getId()) {
			case R.id.start:
				if(!trackingStarted) {
				/*myFeature = mTable.createFeature();
				List<Field> fielList = mTable.getFields();
				String name = fielList.get(0).getName();
				String name2 = fielList.get(1).getName();
				String name3 = fielList.get(2).getName();
				// get field
				Map<String, Object> attributes =  myFeature.getAttributes();
				polygonPoints = new PointCollection(SpatialReference.create(25832));
				polyLineCollection = new PointCollection(SpatialReference.create(25832));*/
					myFeature = editShp.getFeatureTable().createFeature();

					// get field
					Map<String, Object> attributes = myFeature.getAttributes();
					polyLineCollection = new PointCollection(SpatialReference.create(25832));

					trackingStarted = true;
					start.setText("Stop");
				}
				else {
					trackingStarted = false;
					/*PointCollection polygonPoints2 = polygonPoints;
					Polygon polyline = new Polygon(polygonPoints2);*/

					Polyline polyline = new Polyline(polyLineCollection);
					myFeature.setGeometry(polyline);
					editShp.getFeatureTable().addFeatureAsync(myFeature);

					ListenableList<GraphicsOverlay> graphOverlys = mMapView.getGraphicsOverlays();
					for (GraphicsOverlay grphOvrly : graphOverlys) {
						grphOvrly.getGraphics().clear();
					}
					editShp.getFeatureTable().loadAsync();
					start.setText("Start");
				}
				break;
		}
	}

	public void onRadioButtonClicked(View view) {
		String text = getResources().getString(R.string.WgsUtm);
		if (wgs84) {
			SpannableString content = new SpannableString(text);
			content.setSpan(new UnderlineSpan(), 8, content.length(), 0);
			utmOrWgs.setText(content);
			label.setText("UTM");
			utmOrWgs.setChecked(false);
			wgs84 = false;
		} else {
			SpannableString content = new SpannableString(text);
			content.setSpan(new UnderlineSpan(), 0, 5, 0);
			utmOrWgs.setText(content);
			label.setText("WGS84");
			utmOrWgs.setChecked(true);
			wgs84 = true;
		}
	}

	public void addEdit(View view) {
		// Check which checkbox was clicked
		String text = getResources().getString(R.string.addOrEdit);
		if (add) {
			SpannableString content = new SpannableString(text);
			content.setSpan(new UnderlineSpan(), 6, content.length(), 0);
			addEdit.setText(content);
			addEdit.setChecked(true);
			add = false;
		} else {
			SpannableString content = new SpannableString(text);
			content.setSpan(new UnderlineSpan(), 0, 3, 0);
			addEdit.setText(content);
			addEdit.setChecked(false);
			add = true;
		}
	}

	public void center(View view) {
		// Check which checkbox was clicked
		mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
		if (!mLocationDisplay.isStarted())
			mLocationDisplay.startAsync();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	private boolean canAccessLocation() {
		return (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION));
	}

	private boolean canAccessCoarseLocation() {
		return (hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION));
	}

	private boolean hasPermission(String perm) {
		return (PackageManager.PERMISSION_GRANTED == checkSelfPermission(perm));
	}

	public class MyLocationListener implements LocationListener{
		Context contet;
		Activity acti;

		MyLocationListener(Context context, Activity act) {
			this.contet = context;
			this.acti = act;
		}

		public void onLocationChanged(final Location location) {
			if (location != null) {
				try {
					gps_loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					net_loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

					double latNew = gps_loc.getLatitude();
					double longiNew = gps_loc.getLongitude();

					double[] utm = wgsToUtm.calcNorthEast(latNew, longiNew);

					if (Math.abs(latNew - lat) > 0.001 || Math.abs(longiNew - longi) > 0.001) {
						//Viewpoint vp = new Viewpoint(gps_loc.getLatitude(),gps_loc.getLongitude(), 1);
					/*Point point = new Point(utm[0],utm[1], SpatialReference.create(25832));
					Viewpoint viewpoint = new Viewpoint(point, 200000);
					map.setInitialViewpoint(viewpoint);*/

						lat = latNew;
						longi = longiNew;
					}

					if (Math.abs(utm1Old - utm[0]) > 2 || Math.abs(utm2Old - utm[1]) > 2) {
						Point point = new Point(utm[0], utm[1], SpatialReference.create(25832));
						Viewpoint viewpoint = new Viewpoint(point, 200000);
						map.setInitialViewpoint(viewpoint);
						//mMapView.setViewpoint(viewpoint);
						utm1Old = utm[0];
						utm2Old = utm[1];
						if (trackingStarted) {
							//polygonPoints.add(utm[0], utm[1]);
							polyLineCollection.add(utm[0], utm[1]);
							overlay = new GraphicsOverlay();
							mMapView.getGraphicsOverlays().add(overlay);
							SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 3);
							tracking = new Polyline(polyLineCollection);

							overlay.getGraphics().add(new Graphic(tracking, lineSymbol));
						}
					}

					if (!mLocationDisplay.isStarted())
						mLocationDisplay.startAsync();

					if (wgs84) {
						textViewGpsX.setText(String.valueOf(df.format(gps_loc.getLongitude())));
						textViewGpsY.setText(String.valueOf(df.format(gps_loc.getLatitude())));
						textViewGpsZ.setText(String.valueOf(df.format(gps_loc.getAltitude())));
					} else {
						textViewGpsX.setText(String.valueOf(dfUtm.format(utm[0])));
						textViewGpsY.setText(String.valueOf(dfUtm.format(utm[1])));
						textViewGpsZ.setText("");
					}
				}
				catch (SecurityException ex)  {

					ActivityCompat.requestPermissions(this.acti, new String[]{
									Manifest.permission.ACCESS_FINE_LOCATION,
									Manifest.permission.ACCESS_COARSE_LOCATION},
							1);
				}
			}
		}

		public void onProviderDisabled(String provider)
		{}

		public void onProviderEnabled(String provider)
		{}

		public void onStatusChanged(String provider, int status, Bundle extras)
		{}
	}
}
