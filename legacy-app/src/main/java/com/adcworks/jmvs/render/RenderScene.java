package com.adcworks.jmvs.render;

import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.universe.SimpleUniverse;

import com.adcworks.jmvs.interaction.*;

public class RenderScene {
    
    public static final int RENDERED = 0;
    
    private Canvas3D canvas;
    private SimpleUniverse sUni;
    private Switch switchNode;
    private BoundingSphere bounds;
    private BranchGroup sceneRoot;
    private Transform3D	moleculeViewTransform;
    private TransformGroup moleculeRoot;
    private TransformGroup moleculeViewRoot;
    private BranchGroup	molecule3DGroup;
    private TransformGroup atomGroup;	
    private Rotator rotator;
    private Zoomer zoomer;
    private Picker picker;

    public RenderScene() {
        canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
        canvas.setSize(200, 200);
        sUni = new SimpleUniverse(canvas);
        initialise();
    }

    public Canvas3D getCanvas3D() { return canvas; }

    private void initialise() {
        bounds = new BoundingSphere(new Point3d(), 5000.0f);

        sceneRoot = new BranchGroup();
        sceneRoot.setCapability(BranchGroup.ALLOW_DETACH);

        moleculeViewTransform = new Transform3D();
        moleculeRoot = new TransformGroup(moleculeViewTransform);
        moleculeRoot.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        moleculeRoot.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        moleculeViewRoot= new TransformGroup();
        moleculeViewRoot.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        moleculeViewRoot.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        moleculeViewRoot.setCapability(Group.ALLOW_CHILDREN_READ);
        moleculeViewRoot.setCapability(Group.ALLOW_CHILDREN_WRITE);

        switchNode = new Switch(Switch.CHILD_MASK);
        switchNode.setCapability(Switch.ALLOW_SWITCH_READ);
        switchNode.setCapability(Switch.ALLOW_SWITCH_WRITE);
        switchNode.setCapability(Switch.ALLOW_CHILDREN_READ);
        switchNode.setCapability(Switch.ALLOW_CHILDREN_WRITE);
        switchNode.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
        switchNode.setWhichChild(RENDERED);	

        rotator = new Rotator(moleculeViewRoot, this); 
        rotator.setSchedulingBounds(bounds);

        zoomer = new Zoomer(moleculeViewRoot, this);
        zoomer.setSchedulingBounds(bounds);

        //picker = new Picker(this, canvas, sceneRoot, bounds);
        //picker.setSchedulingBounds(bounds);

        moleculeViewRoot.addChild(rotator);
        moleculeViewRoot.addChild(zoomer);

        initialiseEnvironment();

        moleculeViewRoot.addChild(switchNode);
        moleculeRoot.addChild(moleculeViewRoot);

        sceneRoot.addChild(moleculeRoot);
    }
    
    /**
     * Initialise Scene environmental properties.
     *
     */
    private void initialiseEnvironment() {
        
        AmbientLight ambLight = new AmbientLight(true, new Color3f(1.0f, 1.0f, 1.0f));
    	ambLight.setInfluencingBounds(bounds);
    	ambLight.setCapability(Light.ALLOW_STATE_WRITE);
    	moleculeRoot.addChild(ambLight);

        DirectionalLight dirLight  = new DirectionalLight();
        dirLight.setInfluencingBounds(bounds);
        dirLight.setEnable(true);
        moleculeRoot.addChild(dirLight);
    }    

    public void display(TransformGroup tg, double x, double y, boolean center) {
        
            clear();
            
            moleculeRoot.setTransform(centerView(x, y));
            molecule3DGroup = new BranchGroup();	
            molecule3DGroup.setCapability(BranchGroup.ALLOW_DETACH);
            molecule3DGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
            molecule3DGroup.addChild(tg);
            switchNode.addChild(molecule3DGroup);
            sceneRoot.compile();
            
            show();
    }

    private void clear() {
        canvas.stopRenderer();

        if(switchNode.numChildren() > 0) {
            sceneRoot.detach();
            switchNode.removeChild(RENDERED);
        }
    }

    private void show() {
        sUni.addBranchGraph(sceneRoot);
        canvas.startRenderer();
    }        

    private Transform3D centerView(double dX, double dY) {
        double max = Math.max(dX, dY);
        double additionalZ = max * 2;

        Transform3D centerView3D = new Transform3D();

        float backClip = (float)Math.abs(additionalZ + (additionalZ));    	

        sUni.getViewer().getView().setVisibilityPolicy(View.VISIBILITY_DRAW_ALL);
        sUni.getViewer().getView().setBackClipDistance(backClip);

        centerView3D.set(new Vector3f(0.0f, 0.0f, (float)additionalZ));
        return centerView3D;
    }
}
