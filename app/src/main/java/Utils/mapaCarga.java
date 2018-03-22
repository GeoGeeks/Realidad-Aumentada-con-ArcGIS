package Utils;

import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.LayerList;

import java.io.Serializable;

/**
 * Created by geogeeks on 2/02/2018.
 */
//pueda convertir un objeto en un montón de bytes y pueda luego recuperarlo, el objeto necesita ser Serializable

public class mapaCarga implements Serializable {

    public ArcGISMap mapa;
    public LayerList layers;

    public mapaCarga(String urlMapa){
        mapa = new ArcGISMap(urlMapa);
        layers = mapa.getOperationalLayers();
    }

    public ArcGISMap getMap(){
        return mapa;
    }

    public LayerList getLayers(){
        return layers;
    }

}
