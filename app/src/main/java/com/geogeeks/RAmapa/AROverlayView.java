package com.geogeeks.RAmapa;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.opengl.Matrix;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import Utils.ARPoint;
import Utils.LocationHelper;

/**
 * Created by geogeeks on 19/01/2018.
 */

public class AROverlayView extends View {

    Context context;
    private float[] rotatedProjectionMatrix = new float[16];
    private Location currentLocation;
    private List<ARPoint> arPoints = new ArrayList<ARPoint>();
    private double rangoVision = 2000;

    private boolean flagParqueaderos,flagRestaurantes;


    public AROverlayView(Context context) {
        super(context);

        this.context = context;

        //Demo points
        /*arPoints = new ArrayList<ARPoint>() {{
            add(new ARPoint("Ed. Cerca", 4.657166, -74.092469, 2599));
            add(new ARPoint("P. virrey", 4.674178, -74.056095, 2600));
            //add(new ARPoint("Subway", 4.673248, -74.051376, 2600));
        }};*/

    }

    public void updateRotatedProjectionMatrix(float[] rotatedProjectionMatrix) {
        this.rotatedProjectionMatrix = rotatedProjectionMatrix;
        this.invalidate();
    }

    public void updateCurrentLocation(Location currentLocation){
        this.currentLocation = currentLocation;
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentLocation == null) {
            return;
        }

        final int radius = 20;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.NORMAL));
        paint.setTextSize(60);
        paint.setColor(getResources().getColor(R.color.color_parqu_ar));
        /*if(flagParqueaderos==true){
            paint.setColor(getResources().getColor(R.color.color_parqu_ar));
        }else if(flagRestaurantes){
            paint.setColor(getResources().getColor(R.color.color_res_ar));
        }*/


        for (int i = 0; i < arPoints.size(); i ++) {

            // Se transfornma a una referencia que se compatible con la camara
            float[] currentLocationInECEF = LocationHelper.WSG84toECEF(currentLocation);
            float[] pointInECEF = LocationHelper.WSG84toECEF(arPoints.get(i).getLocation());
            float[] pointInENU = LocationHelper.ECEFtoENU(currentLocation, currentLocationInECEF, pointInECEF);
                //vector de referenncia
            float[] cameraCoordinateVector = new float[8];
            Matrix.multiplyMV(cameraCoordinateVector, 0, rotatedProjectionMatrix, 0, pointInENU, 0);

            // cameraCoordinateVector[2] is z, that always less than 0 to display on right position
            // if z > 0, the point will display on the opposite
            if (cameraCoordinateVector[2] < 0) {
                float x  = (0.5f + cameraCoordinateVector[0]/cameraCoordinateVector[3]) * canvas.getWidth();
                float y = (0.5f - cameraCoordinateVector[1]/cameraCoordinateVector[3]) * canvas.getHeight();
                Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.pin_parqu_ar1);
                canvas.drawBitmap(bmp,x- (5*arPoints.get(i).getName().length() / 2),y-70, paint);
                //canvas.drawCircle(x, y, radius, paint);
                canvas.drawText(arPoints.get(i).getName(), x - (30 * arPoints.get(i).getName().length() / 2), y - 80, paint);

                /*
                if(flagParqueaderos==true&&arPoints.get(i).getType().equals("Parqueadero")){
                    Bitmap bmpPar = BitmapFactory.decodeResource(getResources(), R.mipmap.pin_parqu_ar1);
                    canvas.drawBitmap(bmp,x- (5*arPoints.get(i).getName().length() / 2),y-70, paint);
                    //canvas.drawCircle(x, y, radius, paint);
                    canvas.drawText(arPoints.get(i).getName(), x - (30 * arPoints.get(i).getName().length() / 2), y - 80, paint);
                }else if(flagRestaurantes==true&&arPoints.get(i).getType().equals("Restaurante")){
                    Bitmap bmpRes = BitmapFactory.decodeResource(getResources(), R.mipmap.pin_restaurantes_selec);
                    canvas.drawBitmap(bmp,x- (5*arPoints.get(i).getName().length() / 2),y-70, paint);
                    //canvas.drawCircle(x, y, radius, paint);
                    canvas.drawText(arPoints.get(i).getName(), x - (30 * arPoints.get(i).getName().length() / 2), y - 80, paint);
                }*/
            }
        }
    }

    public void agregarArPoints(ARPoint p){
        arPoints.add(p);
    }

    public List<ARPoint> obtenerArPoints(){
        return arPoints;
    }

    public String getNombreLugar(ARPoint arPo){
        return arPo.getName();
    }

    public boolean getFlagParqu(){
        return flagParqueaderos;
    }

    public boolean getFlagRest(){
        return flagRestaurantes;
    }

    public void setFlagParqueaderos(boolean b){
        flagParqueaderos = b;
    }

    public void setFlagRestaurantes(boolean b){flagRestaurantes = b;}
}

