package com.adcworks.jmvs.render.appearance.plugins.ballsandsticks;

import java.util.*;
import javax.media.j3d.*;

import com.adcworks.jmvs.model.*;
import com.adcworks.jmvs.render.appearance.JMVSPlugin;

/**
 * @author Ali
 *
 */
public class JMVSPluginBallsAndSticks implements JMVSPlugin {

    private static final String PLUGIN_NAME 	= "Balls And Sticks";
    private static final String PLUGIN_AUTHOR 	= "Allistair Crossley, adc works";
    private static final String PLUGIN_VERSION 	= "1.0";
    private static final String PLUGIN_INFO 	= "";    
    
    private String[] paintModes = {"CPK"};
    
    /**
     * @see com.adcworks.jmvs.v4.render.appearance.JMVSPlugin#getName()
     */
    public String getName() {
        return PLUGIN_NAME;
    }    
    
    /**
     * @see com.adcworks.jmvs.v4.render.appearance.JMVSPlugin#getAuthor()
     */
    public String getAuthor() {
        return PLUGIN_AUTHOR;
    }
    
    /**
     * @see com.adcworks.jmvs.v4.render.appearance.JMVSPlugin#getVersion()
     */
    public String getVersion() {
        return PLUGIN_VERSION;
    }
    
    /**
     * @see com.adcworks.jmvs.v4.render.appearance.JMVSPlugin#getInformation()
     */
    public String getInformation() {
        return PLUGIN_INFO;
    }    
    
    /**
     * @see com.adcworks.jmvs.v4.render.appearance.JMVSPlugin#init(java.util.Properties)
     */
    public void init(Properties properties) {

    }

    public void setPaintMode(String paintMode) {

    }

    /**
     * @see com.adcworks.jmvs.v4.render.appearance.JMVSPlugin#render(com.adcworks.jmvs.v4.model.Molecule)
     */
    public TransformGroup render(Molecule molecule) {
            
//
//        Atom[] atoms = m.getAtoms();
//
//        boolean renderHeteroAtoms = dOptions.isHeteroAtomsOn();
//        int sphereDiv = dOptions.getSphereDiv();
//
//        TransformGroup rTG = new TransformGroup();
//        rTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
//        rTG.setCapability(Group.ALLOW_CHILDREN_READ);
//
//        TransformGroup aTG = new TransformGroup();
//        aTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
//        aTG.setCapability(Group.ALLOW_CHILDREN_READ);
//
//        for(int i = 0; i < atoms.length; i++)
//        {
//                if( (!renderHeteroAtoms) && (atoms[i].isHetatm()) )
//                        continue;
//
//                Transform3D atomT3D = new Transform3D();
//                atomT3D.set(0.1, new Vector3d(atoms[i].getX(), atoms[i].getY(), atoms[i].getZ()));
//                TransformGroup atomTG = new TransformGroup(atomT3D);
//                atomTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
//                atomTG.setCapability(Group.ALLOW_CHILDREN_READ);
//                atomTG.setCapability(Group.ALLOW_CHILDREN_WRITE);
//
//                Appearance sphereApp = new Appearance();
//                sphereApp.setCapability(Appearance.ALLOW_MATERIAL_READ);
//                sphereApp.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
//                Material mat = new Material();
//                mat.setCapability(Material.ALLOW_COMPONENT_READ);
//                mat.setCapability(Material.ALLOW_COMPONENT_WRITE);
//                mat.setLightingEnable(true);
//                sphereApp.setMaterial(mat);
//
//                Sphere s = new Sphere(2.0f, Sphere.GENERATE_NORMALS|Sphere.ENABLE_APPEARANCE_MODIFY, sphereDiv);
//
//                s.getShape(Sphere.BODY).setUserData(atoms[i].getName());
//                s.setAppearance(sphereApp);
//
//                atomTG.addChild(s);
//                aTG.addChild(atomTG);
//        }
//
//        TransformGroup bTG = new TransformGroup();
//        bTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
//        bTG.setCapability(Group.ALLOW_CHILDREN_READ);
//
//
//        //
//        Bond[] bonds = m.getBonds();
//
//        LineAttributes lineAttributes = new LineAttributes();
//        lineAttributes.setLineAntialiasingEnable(true);
//        lineAttributes.setLineWidth(1.0f);
//
//        Appearance lineAppearance = new Appearance();
//        ColoringAttributes lineColour = new ColoringAttributes(0.5f, 0.5f, 0.5f, ColoringAttributes.SHADE_FLAT);
//        lineAppearance.setColoringAttributes(lineColour);
//        lineAppearance.setLineAttributes(lineAttributes);
//
//        float[] firstHalf = new float[6];
//        float[] secondHalf = new float[6];
//        for(int i = 0; i < bonds.length; i++)
//        {
//                float x = (float)((bonds[i].getDST().getX() - bonds[i].getSRC().getX()) / 2.0f + bonds[i].getSRC().getX());
//                float y = (float)((bonds[i].getDST().getY() - bonds[i].getSRC().getY()) / 2.0f + bonds[i].getSRC().getY());
//                float z = (float)((bonds[i].getDST().getZ() - bonds[i].getSRC().getZ()) / 2.0f + bonds[i].getSRC().getZ());
//
//                firstHalf[0] = bonds[i].getSRC().getX();
//                firstHalf[1] = bonds[i].getSRC().getY();
//                firstHalf[2] = bonds[i].getSRC().getZ();
//                firstHalf[3] = x;
//                firstHalf[4] = y;
//                firstHalf[5] = z;
//
//                secondHalf[0] = x;
//                secondHalf[1] = y;
//                secondHalf[2] = z;
//                secondHalf[3] = bonds[i].getDST().getX();
//                secondHalf[4] = bonds[i].getDST().getY();
//                secondHalf[5] = bonds[i].getDST().getZ();
//
//                LineArray skeletonSeg = new LineArray(2, LineArray.COORDINATES|LineArray.NORMALS);
//                skeletonSeg.setCoordinates(0, firstHalf);
//                skeletonSeg.setNormals(0, firstHalf);
//                Shape3D lineSeg = new Shape3D(skeletonSeg, lineAppearance);
//                lineSeg.setUserData(bonds[i].getSRC().getName());
//                lineSeg.setCapability(lineSeg.ALLOW_APPEARANCE_READ);
//                lineSeg.setCapability(lineSeg.ALLOW_APPEARANCE_WRITE);
//
//                LineArray skeletonSeg2 = new LineArray(2, LineArray.COORDINATES|LineArray.NORMALS);
//                skeletonSeg2.setCoordinates(0, secondHalf);
//                skeletonSeg2.setNormals(0, secondHalf);
//                Shape3D lineSeg2 = new Shape3D(skeletonSeg2, lineAppearance);
//                lineSeg2.setUserData(bonds[i].getDST().getName());
//                lineSeg2.setCapability(lineSeg.ALLOW_APPEARANCE_READ);
//                lineSeg2.setCapability(lineSeg.ALLOW_APPEARANCE_WRITE);
//
//                bTG.addChild(lineSeg);
//                bTG.addChild(lineSeg2);
//        }
//
//        rTG.addChild(aTG);
//        rTG.addChild(bTG);
//
//        return rTG;
        return null;
    }    

    /**
     * @see com.adcworks.jmvs.v4.render.appearance.JMVSPlugin#getPaintModes()
     */
    public String[] getPaintModes() {
        return paintModes;
    }      
    
    /**
     * Paint the molecule geometry.
     * @param molecule
     * @param mode
     */
    public void paint(TransformGroup molecule, String mode) {
        
    }
}
