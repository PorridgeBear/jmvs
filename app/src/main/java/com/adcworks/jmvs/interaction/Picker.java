package com.adcworks.jmvs.interaction;

import javax.media.j3d.*;
import com.sun.j3d.utils.picking.*;
import com.sun.j3d.utils.picking.behaviors.*;
import javax.vecmath.*;

import com.adcworks.jmvs.render.*;

public class Picker extends PickMouseBehavior {
    
    private RenderScene scene;
    
    private Appearance lastApp;
    private Shape3D lastShape;
    private Appearance pickApp;

    public Picker(RenderScene scene, Canvas3D canvas, BranchGroup root, Bounds bounds) {
        
            super(canvas, root, bounds);

            this.scene = scene;
            this.setSchedulingBounds(bounds);
            root.addChild(this);
           
            setTolerance(4.0f);
            pickCanvas.setMode(PickTool.BOUNDS);

            // setup the look of the pick highlight
            pickApp = new Appearance();
            ColoringAttributes colorAtts = new ColoringAttributes(new Color3f(0.0f, 1.0f, 0.0f), ColoringAttributes.SHADE_GOURAUD);
            pickApp.setColoringAttributes(colorAtts);
    }

    /**
     * use X and Y pick location to intersect an atom of the molecule
     */
    public void updateScene(int xpos, int ypos) {     

        pickCanvas.setShapeLocation(xpos, ypos);
        
        Shape3D shape = null;
        try {
            shape = (Shape3D)((pickCanvas.pickClosest()).getNode(PickResult.SHAPE3D));
        }
        catch(NullPointerException npE) {
            // no shape to pick
        }
        
        if(lastShape != null) {
            lastShape.setAppearance(lastApp);
        }
        
        if(shape != null) {
            lastApp = shape.getAppearance();
            lastShape = shape;
            shape.setAppearance(pickApp);

            String pickInf = (String)shape.getUserData();
            
            if(pickInf != null) {
               
                //scene.fireAtomPicked(pickInf);
            }		
        }
    }
}
