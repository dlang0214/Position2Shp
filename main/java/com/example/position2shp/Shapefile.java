package com.example.position2shp;

import android.graphics.Color;
import android.os.Environment;

import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.data.ShapefileInfo;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;

/**
 * Created by Franziska on 29.05.2018.
 */

public class Shapefile {

    FeatureLayer featureLayer;

    ShapefileFeatureTable mTable;

    Shapefile(ArcGISMap map, String mShapefilePath, SimpleRenderer renderer)
    {
        mTable = new ShapefileFeatureTable(mShapefilePath);

        mTable.loadAsync();

        mTable.addDoneLoadingListener(() -> {
            if (mTable.getLoadStatus() == LoadStatus.LOADED) {
                ShapefileInfo info = mTable.getInfo();
                featureLayer = new FeatureLayer(mTable);

                featureLayer.setRenderer(renderer);

                map.getOperationalLayers().add(featureLayer);
            }
        });
    }

    public FeatureLayer getFeatureLayer()
    {
        return this.featureLayer;
    };

    public ShapefileFeatureTable getFeatureTable() {return this.mTable; };
}
