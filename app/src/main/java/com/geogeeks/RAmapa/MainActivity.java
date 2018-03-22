package com.geogeeks.RAmapa;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.LayerContent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedEvent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedListener;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.view.BackgroundGrid;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;
import com.karan.churi.PermissionManager.PermissionManager;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.koushikdutta.ion.Ion;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutionException;

import Utils.mapaCarga;

//pueda convertir un objeto en un montón de bytes y pueda luego recuperarlo, el objeto necesita ser Serializable
public class MainActivity extends AppCompatActivity implements View.OnClickListener,Serializable {

    public Activity main;
    public PermissionManager permissionManager;

    public MapView vistaMap;
    public ArcGISMap map;
    private LinearLayout contentProgress, popup,layersFilter;
    private ConstraintLayout contentMap;
    public FeatureLayer  parqueaderos;
    private ImageButton btnParqueaderos, btnHoteles, btnRestaurantes, closePopup, btnAr,locate,btnFilter;
    private TextView categoria, nombreLugar, direccionLugar;
    private ImageView fotoLugar;
    public mapaCarga mapaAumented;

    public boolean flagPar,flagRestaurantes;




    private int requestCode = 2;
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};

    //Uso para localizacion
    public LocationDisplay locationDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        main=this;
        ArcGISRuntimeEnvironment.setLicense("runtimelite,1000,rud9088059687,none,HC5X0H4AH4YDXH46C082");

        // Para solicitud de permiso automático.
        // Busca dinámicamente el permiso de la aplicación dentro de Android Manifest y solicita el mismo.


        permissionManager = new PermissionManager() {};
        permissionManager.checkAndRequestPermissions(this);//Para iniciar la comprobación del permiso

        vistaMap = (MapView) findViewById(R.id.mapView);
        locationDisplay = vistaMap.getLocationDisplay();


        initRecursos();
        crearMapa();
        geoLocalizacion();

        getIntent().setAction("Already created");

    }

    private  void initRecursos(){


        contentProgress = (LinearLayout) findViewById(R.id.linearProgressBar);
        contentMap = (ConstraintLayout) findViewById(R.id.contentMap);

        locate = (ImageButton) findViewById(R.id.myLocationButton);
        locate.setOnClickListener(this);

        btnFilter = (ImageButton)findViewById(R.id.layersButton);
        btnFilter.setOnClickListener(this);

        layersFilter= (LinearLayout) findViewById(R.id.first);

        btnParqueaderos = (ImageButton) findViewById(R.id.botonParqueaderos);
        btnParqueaderos.setSelected(true);
        btnParqueaderos.setOnClickListener(this);

        btnAr = (ImageButton) findViewById(R.id.btnAumentedR);
        btnAr.setOnClickListener(this);

        popup = (LinearLayout) findViewById(R.id.contentPopup);
        categoria = (TextView) findViewById(R.id.categoria);
        nombreLugar = (TextView) findViewById(R.id.lugar);
        direccionLugar = (TextView) findViewById(R.id.direccion);
        fotoLugar = (ImageView) findViewById(R.id.fotoLugar);

        closePopup = (ImageButton) findViewById(R.id.closePopup);
        closePopup.setOnClickListener(this);

    }

    public void crearMapa(){

        vistaMap = (MapView) findViewById(R.id.mapView);
        vistaMap.setAttributionTextVisible(false);
        mapaAumented = new mapaCarga(this.getResources().getString(R.string.URL_mapa_alrededores));
        map = mapaAumented.getMap();

        vistaMap.setMap(map);

        map.addLoadStatusChangedListener(new LoadStatusChangedListener() {
            @Override
            public void loadStatusChanged(LoadStatusChangedEvent loadStatusChangedEvent) {
                String mapLoadStatus;
                mapLoadStatus = loadStatusChangedEvent.getNewLoadStatus().name();
                switch (mapLoadStatus) {
                    case "LOADED":
                        contentProgress.setVisibility(View.GONE);
                        contentMap.setVisibility(View.VISIBLE);
                        if(map.getInitialViewpoint() != null)
                            vistaMap.setViewpoint(map.getInitialViewpoint()); //Acerca o panea el mapa al punto de vista dado.

                        LayerList layers = map.getOperationalLayers(); //Obtiene la lista da capas operacionales de ArcGISmap

                        if(!layers.isEmpty()){
                            parqueaderos = (FeatureLayer) layers.get(0);

                        }
                        break;
                }
            }
        });
            //Establece el oyente que maneja los gestos táctiles para este MapView
        vistaMap.setOnTouchListener(new IdentifyFeatureLayerTouchListener(this, vistaMap));
        vistaMap.setBackgroundGrid(new BackgroundGrid(Color.WHITE, Color.WHITE, 0, vistaMap.getBackgroundGrid().getGridSize()));

        //ArcGISMap map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 4.6097100,  -74.0817500, 16);

        //Establece WrapAroundMode para determinar si el mapa debe dibujarse continuamente a lo largo del meridiano 180º.
        vistaMap.setWrapAroundMode(WrapAroundMode.DISABLED);

        //vistaMap.setMap(map);
    }

    private void geoLocalizacion() {
        try {
            locationDisplay.addDataSourceStatusChangedListener(new LocationDisplay.DataSourceStatusChangedListener() {
                @Override
                public void onStatusChanged(LocationDisplay.DataSourceStatusChangedEvent dataSourceStatusChangedEvent) {

                    if (dataSourceStatusChangedEvent.isStarted())
                        return;

                    if (dataSourceStatusChangedEvent.getError() == null)
                        return;
                }
            });
            locationDisplay.startAsync();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage().toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void mostrarPopup(String categoriaNombre, String nombre, String direccion, String foto){
        //Log.e(MainActivity.TAG, nombre+", "+direccion+", "+foto);
        categoria.setText(categoriaNombre);
        nombreLugar.setText(nombre);
        direccionLugar.setText(direccion);
        if(foto != null){
            Ion.with(fotoLugar).load(foto);
        }else{
            Ion.with(fotoLugar).load("http://geoapps.esri.co/recursos/CCU2017/bogota.jpg");
        }

        popup.setVisibility(View.VISIBLE);
    }


    @Override
    public void onPause() {
        vistaMap.pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        vistaMap.resume();
    }

    public void processIdentifyFeatureResult(Feature feature, LayerContent content){
        String nombre = "", direccion = "", foto = "";
        switch (content.getName()){
            case "Parqueaderos":
                nombre = (String) feature.getAttributes().get("Nombre");
                direccion = (String) feature.getAttributes().get("Direccion");
                foto = (String) feature.getAttributes().get("Foto");
                mostrarPopup("Parqueadero", nombre, direccion, foto);
                break;

        }
    }

    private void activarDesactivaLayer(FeatureLayer layer, ImageButton button){
        if(layer != null){
            if(button.isSelected()){
                button.setSelected(false);
                layer.setVisible(false);
            }else{
                button.setSelected(true);
                layer.setVisible(true);
            }
        }
    }


    private void activarFiltroLayers(){
        if(btnFilter.isSelected()){
            btnFilter.setSelected(false);
            layersFilter.setVisibility(View.GONE);
        }else{
            btnFilter.setSelected(true);
            layersFilter.setVisibility(View.VISIBLE);
        }
    }



    // se sobreeescribe el metodo para poder obtener los permisos que se denegaron o permitieron
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionManager.checkResult(requestCode,permissions,grantResults);
        /*para hacer un arreglo de los permisos concedidos y lo que no
        ArrayList<String> grantedPermissions = permissionManager.getStatus().get(0).granted;
        ArrayList<String> deniedPermissions = permissionManager.getStatus().get(0).denied;

        for(String item:grantedPermissions){
            txtGranted.setText(txtGranted.getText()+"\n"+item);
        }
        for(String item:deniedPermissions){
            txtDenied.setText(txtDenied.getText()+"\n"+item);
        }*/
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            this.locationDisplay.startAsync();
        } else {
            Toast.makeText(main, "locacion denegada", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.myLocationButton:
                locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
                locationDisplay.startAsync();
                break;
            case R.id.botonParqueaderos:
                activarDesactivaLayer(parqueaderos, btnParqueaderos);
                break;
            case R.id.closePopup:
                popup.setVisibility(View.GONE);
                break;
            case R.id.btnAumentedR:
                Intent actAr = new Intent(this, ARActivity.class);
                //actAr.putExtra("miMapa",mapaAumented);
                startActivity(actAr);
                break;
            case R.id.layersButton:
                activarFiltroLayers();
                break;
        }


    }


    //permite  explorar  fácilmente  el contenido del mapa tocando o
    // haciendo clic en ellos. La información devuelta se puede mostrar en ventanas emergentes u
    // otros componentes de la interfaz de usuario en su aplicación.

    private class IdentifyFeatureLayerTouchListener extends DefaultMapViewOnTouchListener {

        private FeatureLayer layer = null; // reference to the layer to identify features in

        // provide a default constructor
        public IdentifyFeatureLayerTouchListener(Context context, MapView mapView) {
            super(context, mapView);
        }

        // override the onSingleTapConfirmed gesture to handle a single tap on the MapView
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // get the screen point where user tapped
            android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());
            final ListenableFuture<List<IdentifyLayerResult>> identifyFuture = super.mMapView.identifyLayersAsync(screenPoint, 5,
                    false);

            // add a listener to the future
            identifyFuture.addDoneListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        // get the identify results from the future - returns when the operation is complete
                        List<IdentifyLayerResult> identifyLayersResults = identifyFuture.get();

                        // iterate all the layers in the identify result
                        for (IdentifyLayerResult identifyLayerResult : identifyLayersResults) {

                            // each identified layer should find only one or zero results, when identifying topmost GeoElement only
                            if (identifyLayerResult.getElements().size() > 0) {
                                GeoElement topmostElement = identifyLayerResult.getElements().get(0);
                                if (topmostElement instanceof Feature) {
                                    Feature identifiedFeature = (Feature)topmostElement;

                                    // Use feature as required, for example access attributes or geometry, select, build a table, etc...
                                    //
                                    processIdentifyFeatureResult(identifiedFeature, identifyLayerResult.getLayerContent());
                                }
                            }
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        //dealWithException(ex); // must deal with exceptions thrown from the async identify operation
                    }
                }
            });
            return super.onSingleTapConfirmed(e);
        }
    }
}
