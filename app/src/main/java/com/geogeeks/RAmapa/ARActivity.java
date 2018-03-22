package com.geogeeks.RAmapa;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatusChangedEvent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedListener;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.view.BackgroundGrid;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import Utils.ARPoint;

//import com.esri.android.map.Layer


public class ARActivity extends AppCompatActivity
        implements SensorEventListener,LocationListener{

    private SensorManager sensorManager; // permite acceder a los sensores del dispositivo
    private SurfaceView surfaceView;  //se encarga de colocar la superficie de dibujo en la ubicación correcta en la pantalla
    private FrameLayout cameraContainerLayout;
    private AROverlayView arOverlayView;
    private TextView tvCurrentLocation;
    private final static int REQUEST_CAMERA_PERMISSIONS_CODE = 11;
    public static final int REQUEST_LOCATION_PERMISSIONS_CODE = 0;

    private Camera camera;
    private ARCamera arCamera;

    public Location location;
    boolean isGPSEnabled;
    boolean isNetworkEnabled;
    boolean locationServiceAvailable;
    private LocationManager locationManager; //proporciona acceso a los servicios de localización del sistema.
    private View viewAR;

    private MapView vistaMapLittle;
    private ArcGISMap mapaLittle;

    public MainActivity mainMapa;
    public LocationDisplay locationDisplay;
    public RelativeLayout contentMap;

    final static String TAG = "ARActivity";

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 10 meters
    private static final long MIN_TIME_BW_UPDATES = 0;//1000 * 60 * 1; // 1 minute

    final float radioBuffer = (float) 1111;//radio de buffer

    private FeatureLayer restaurantes, parqueaderos, hoteles;
    private LayerList layers;
    //obtener posicion
    private Point posicion;

    //AROverlayView arOver = new AROverlayView(getBaseContext());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ArcGISRuntimeEnvironment.setLicense("runtimelite,1000,rud9088059687,none,HC5X0H4AH4YDXH46C082");

        //geoLocalizacion2();

        //contenedor de nivel superior para el contenido de la ventana que permite
        // sacar vistas interactivas de "gavetas" desde uno o ambos bordes verticales de la ventana.

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);


        contentMap = (RelativeLayout) this.findViewById(R.id.layout_miniMap);

        //agregar mapa pequeño
        createLittleMap();

        //ar content------------------------------------
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        cameraContainerLayout = (FrameLayout) findViewById(R.id.camera_container_layout);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        tvCurrentLocation = (TextView) findViewById(R.id.tv_current_location);
        arOverlayView = new AROverlayView(this);
        toggle.syncState(); //Sincroniza el estado del indicador del Drawer/ affordance con el DrawerLayout vinculado.

        Intent actAr = getIntent();
        //mapa2 = (mapaCarga) actAr.getSerializableExtra("miMapa");

    }

    public void createLittleMap(){

        mainMapa = new MainActivity();
        vistaMapLittle = this.findViewById(R.id.mapView);
        mapaLittle = new ArcGISMap(this.getResources().getString(R.string.URL_mapa_alrededores));
        //mapaLittle = mapa2.getMap();
        vistaMapLittle.setMap(mapaLittle);
        vistaMapLittle.setVisibility(View.VISIBLE);
        vistaMapLittle.setBackgroundGrid(new BackgroundGrid(Color.WHITE, Color.WHITE, 0, vistaMapLittle.getBackgroundGrid().getGridSize()));
        vistaMapLittle.setWrapAroundMode(WrapAroundMode.DISABLED);

        locationDisplay = vistaMapLittle.getLocationDisplay();
        locationDisplay.startAsync();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
        vistaMapLittle.setOnTouchListener(new IdentifyFeatureLayerTouchListener(vistaMapLittle.getContext(), vistaMapLittle));
        mapaLittle.addLoadStatusChangedListener(new LoadStatusChangedListener() {
            @Override
            public void loadStatusChanged(LoadStatusChangedEvent loadStatusChangedEvent) {

                String mapLoadStatus;
                mapLoadStatus = loadStatusChangedEvent.getNewLoadStatus().name();
                switch (mapLoadStatus) {
                    case "LOADED":
                        Toast.makeText(vistaMapLittle.getContext(),"Cargado",Toast.LENGTH_LONG).show();
                        contentMap.setVisibility(View.VISIBLE);
                        hacerConsulta(getResources().getString(R.string.URL_capa_parqueaderos));
                        LayerList layers = mapaLittle.getOperationalLayers();
                        if(!layers.isEmpty()){
                            parqueaderos = (FeatureLayer) layers.get(0);
                            restaurantes = (FeatureLayer) layers.get(1);
                            hoteles = (FeatureLayer) layers.get(2);
                        }
                        if(mapaLittle.getInitialViewpoint() != null){
                            vistaMapLittle.setViewpoint(mapaLittle.getInitialViewpoint());
                        }

                        break;
                }
            }
        });
    }

    private class IdentifyFeatureLayerTouchListener extends DefaultMapViewOnTouchListener {

        private FeatureLayer layer = null; // reference to the layer to identify features in
        // provide a default constructor
        public IdentifyFeatureLayerTouchListener(Context context, MapView mapView) {
            super(context, mapView);
        }

        // override the onSingleTapConfirmed gesture to handle a single tap on the MapView
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Toast.makeText(vistaMapLittle.getContext(),"presionado",Toast.LENGTH_LONG).show();
            /*try {
                actualizarPunto(location);
                //puntosCapa(parqueaderos);
            } catch (ExecutionException e1) {
                e1.printStackTrace();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }*/

            //puntosCapa(parqueaderos);
            //hacerConsulta(getResources().getString(R.string.URL_capa_parqueaderos));
            return super.onSingleTapConfirmed(e);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        vistaMapLittle.resume();
        requestLocationPermission();
        requestCameraPermission();
        registerSensors();
        initAROverlayView();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
    }

    @Override
    public void onPause() {
        releaseCamera();
        vistaMapLittle.pause();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
        super.onPause();
    }

    public void requestCameraPermission() {
        //colocado para hacer la verificacion en tiempo de ejecucion
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSIONS_CODE);
        } else {
            initARCameraView();
        }
    }

    private void releaseCamera() {
        if(camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            arCamera.setCamera(null);
            camera.release();
            camera = null;
        }
    }

    public void initARCameraView() {
        reloadSurfaceView();

        if (arCamera == null) {
            arCamera = new ARCamera(this, surfaceView);
        }
        if (arCamera.getParent() != null) {
            ((ViewGroup) arCamera.getParent()).removeView(arCamera);
        }
        cameraContainerLayout.addView(arCamera);
        arCamera.setKeepScreenOn(true);
        initCamera();
    }

    private void initCamera() {
        int numCams = Camera.getNumberOfCameras();
        if(numCams > 0){
            try{
                camera = Camera.open();
                camera.startPreview();
                arCamera.setCamera(camera);
            } catch (RuntimeException ex){
                Toast.makeText(this, "Camera not found", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void reloadSurfaceView() {
        if (surfaceView.getParent() != null) {
            ((ViewGroup) surfaceView.getParent()).removeView(surfaceView);
        }

        cameraContainerLayout.addView(surfaceView);
    }

    public void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSIONS_CODE);
        } else {
            initLocationService();
        }
    }

    public void initAROverlayView() {
        if (arOverlayView.getParent() != null) {
            ((ViewGroup) arOverlayView.getParent()).removeView(arOverlayView);
        }
        cameraContainerLayout.addView(arOverlayView);
    }

    private void registerSensors() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void initLocationService() {

        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            return  ;
        }

        try   {
            this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

            // Get GPS and network status
            this.isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            this.isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isNetworkEnabled && !isGPSEnabled)    {
                // cannot get location
                this.locationServiceAvailable = false;
            }

            this.locationServiceAvailable = true;

            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                if (locationManager != null)   {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    updateLatestLocation();
                }
            }

            if (isGPSEnabled)  {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                if (locationManager != null)  {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateLatestLocation();
                }
            }
        } catch (Exception ex)  {
            Log.e(TAG, ex.getMessage());

        }
    }

    //actualizar el punto de la posicion actual para hacer el buffer
    private void actualizarPunto(Location loc){
       posicion = new Point(loc.getLatitude(),loc.getLongitude(),loc.getAltitude(), SpatialReferences.getWgs84());
       //Geometry xx = GeometryEngine.project(posicion,mapaLittle.getSpatialReference());
       //posicion = (Point)xx;
    }

    public void hacerConsulta(String urlCapa) {
        try{
        //final ServiceFeatureTable serviceFT = new ServiceFeatureTable(this.getResources().getString(R.string.URL_mapa_alrededores));
        final ServiceFeatureTable serviceFT = new ServiceFeatureTable(urlCapa);
        FeatureLayer featureLayerprueba = new FeatureLayer(serviceFT);
        mapaLittle.getOperationalLayers().add(featureLayerprueba);
        serviceFT.setFeatureRequestMode(ServiceFeatureTable.FeatureRequestMode.MANUAL_CACHE);
        serviceFT.loadAsync();
        serviceFT.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                QueryParameters queryParam = new QueryParameters();
                queryParam.getOrderByFields().add(new QueryParameters.OrderBy("nombre",QueryParameters.SortOrder.DESCENDING));
                queryParam.setWhereClause("1=1");//clausula de busqueda
                queryParam.setReturnGeometry(true);
                queryParam.setOutSpatialReference(SpatialReferences.getWgs84());//referencia espacial del query

                // set all outfields
                List<String> outFields = new ArrayList<>();
                outFields.add("*");
                //arreglo de features que bota la seleccion de features en el feature layer
                final ListenableFuture<FeatureQueryResult> featureQResult = serviceFT.populateFromServiceAsync(queryParam,true,outFields);
                featureQResult.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            FeatureQueryResult result = featureQResult.get();
                            Iterator<Feature> iterator = result.iterator();
                            Feature feat;
                            //se declaran para usarlos en el while.
                            ARPoint arPoint = null;
                            actualizarPunto(location);// transforma la localización
                            Toast.makeText(vistaMapLittle.getContext(),"posicion:"+posicion.getSpatialReference().toString(),Toast.LENGTH_LONG).show();
                            Geometry buffer = GeometryEngine.buffer(posicion,radioBuffer);

                            while(iterator.hasNext()){
                                feat = iterator.next(); //recorriendo el arreglo
                                //
                                Point punto = (Point) feat.getGeometry();
                                Toast.makeText(vistaMapLittle.getContext(),"punto:"+punto.getSpatialReference().toString(),Toast.LENGTH_LONG).show();
                                //Geometry v = GeometryEngine.project(punto,mapaLittle.getSpatialReference());
                                //punto = (Point)v;
                                /// hacemos un buffer e interceptamos los puntos de la capa
                                List<Geometry> puntosIntersec = GeometryEngine.intersections(buffer,punto);
                                Iterator<Geometry> iterPoint = puntosIntersec.iterator();
                                Point p;
                                while(iterPoint.hasNext()){
                                    p=(Point)iterPoint.next(); // obtenemos los puntos resultantes de la intersecion
                                    Toast.makeText(vistaMapLittle.getContext(),"p:"+p.getSpatialReference().toString(),Toast.LENGTH_LONG).show();
                                    //GeometryEngine.project(p,mapaLittle.getSpatialReference());
                                    arPoint = new ARPoint((String) feat.getAttributes().get("nombre"),(String)feat.getFeatureTable().getFields().get(0).toString(),
                                            p.getY(),p.getX(),2400);
                                    arOverlayView.agregarArPoints(arPoint);
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        });


        }catch(Exception e){
            e.printStackTrace();
        }
    }


    private void updateLatestLocation() {
        if (arOverlayView !=null && location != null) {
            arOverlayView.updateCurrentLocation(location);
            tvCurrentLocation.setText(String.format("lat: %s \nlon: %s \naltitude: %s \n",
                    location.getLatitude(), location.getLongitude(), location.getAltitude()));
            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
            //actualizarPunto(location);
        }
    }

    ////location and sensors--------------------------------



    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrixFromVector = new float[16];
            float[] projectionMatrix = new float[16];
            float[] rotatedProjectionMatrix = new float[16];

            SensorManager.getRotationMatrixFromVector(rotationMatrixFromVector, event.values);

            if (arCamera != null) {
                projectionMatrix = arCamera.getProjectionMatrix();
            }

            Matrix.multiplyMM(rotatedProjectionMatrix, 0, projectionMatrix, 0, rotationMatrixFromVector, 0);
            this.arOverlayView.updateRotatedProjectionMatrix(rotatedProjectionMatrix);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {
        updateLatestLocation();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
