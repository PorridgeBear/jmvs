package com.adcworks.jmvs.render.appearance.plugins.sticks;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.media.j3d.*;
import javax.vecmath.Color3f;

import com.adcworks.jmvs.model.*;
import com.adcworks.jmvs.render.appearance.AppearanceManager;
import com.adcworks.jmvs.render.appearance.JMVSPlugin;

public class JMVSPluginSticks implements JMVSPlugin {

    private static final String PLUGIN_NAME 	= "Sticks";
    private static final String PLUGIN_AUTHOR 	= "Allistair Crossley, adc works";
    private static final String PLUGIN_VERSION 	= "1.0";
    private static final String PLUGIN_INFO 	= "";

    private String[] paintModes = {"CPK", "Chain", "Group", "Structure"};
    private Properties properties;

    private String paintMode = paintModes[0];

    public String getName() {
        return PLUGIN_NAME;
    }    

    public String getAuthor() {
        return PLUGIN_AUTHOR;
    }

    public String getVersion() {
        return PLUGIN_VERSION;
    }

    public String getInformation() {
        return PLUGIN_INFO;
    }

    public void init(Properties properties) {
        this.properties = properties;
    }

    public void setPaintMode(String paintMode) {
        this.paintMode = paintMode;
    }

    public TransformGroup render(Molecule molecule) {

        List<Bond> bonds = molecule.getModels().get(0).getBonds();

        TransformGroup aTG = new TransformGroup();
        aTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        aTG.setCapability(Group.ALLOW_CHILDREN_READ);

        LineAttributes lineAttributes = new LineAttributes();
        lineAttributes.setLineAntialiasingEnable(true);
        lineAttributes.setLineWidth(3.0f);

        float[] firstHalf = new float[6];
        float[] secondHalf = new float[6];

        for (Bond bond : bonds) {
            float x = (bond.getDst().getX() - bond.getSrc().getX()) / 2.0f + bond.getSrc().getX();
            float y = (bond.getDst().getY() - bond.getSrc().getY()) / 2.0f + bond.getSrc().getY();
            float z = (bond.getDst().getZ() - bond.getSrc().getZ()) / 2.0f + bond.getSrc().getZ();

            firstHalf[0] = bond.getSrc().getX() - molecule.midX;
            firstHalf[1] = bond.getSrc().getY() - molecule.midY;
            firstHalf[2] = bond.getSrc().getZ() - molecule.midZ;
            firstHalf[3] = x - molecule.midX;
            firstHalf[4] = y - molecule.midY;
            firstHalf[5] = z - molecule.midZ;

            secondHalf[0] = x - molecule.midX;
            secondHalf[1] = y - molecule.midY;
            secondHalf[2] = z - molecule.midZ;
            secondHalf[3] = bond.getDst().getX() - molecule.midX;
            secondHalf[4] = bond.getDst().getY() - molecule.midY;
            secondHalf[5] = bond.getDst().getZ() - molecule.midZ;

            LineArray skeletonSeg = new LineArray(2, LineArray.COORDINATES | LineArray.NORMALS);
            skeletonSeg.setCoordinates(0, firstHalf);
            skeletonSeg.setNormals(0, firstHalf);
            Appearance lineAppearance1 = new Appearance();
            Color3f colour1 = new Color3f();
            Material line1 = getMaterial(bond.getSrc());
            line1.getDiffuseColor(colour1);
            ColoringAttributes lineColourAtts1 = new ColoringAttributes(colour1, ColoringAttributes.SHADE_FLAT);
            lineAppearance1.setColoringAttributes(lineColourAtts1);
            lineAppearance1.setLineAttributes(lineAttributes);
            Shape3D lineSeg = new Shape3D(skeletonSeg, lineAppearance1);
            lineSeg.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
            lineSeg.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);

            LineArray skeletonSeg2 = new LineArray(2, LineArray.COORDINATES | LineArray.NORMALS);
            skeletonSeg2.setCoordinates(0, secondHalf);
            skeletonSeg2.setNormals(0, secondHalf);
            Appearance lineAppearance2 = new Appearance();
            Color3f colour2 = new Color3f();
            Material line2 = getMaterial(bond.getDst());
            line2.getDiffuseColor(colour2);
            ColoringAttributes lineColourAtts2 = new ColoringAttributes(colour2, ColoringAttributes.SHADE_FLAT);
            lineAppearance2.setColoringAttributes(lineColourAtts2);
            lineAppearance2.setLineAttributes(lineAttributes);
            Shape3D lineSeg2 = new Shape3D(skeletonSeg2, lineAppearance2);
            lineSeg2.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
            lineSeg2.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);

            aTG.addChild(lineSeg);
            aTG.addChild(lineSeg2);
        }
        return aTG;
    }    

    public String[] getPaintModes() {
        return paintModes;
    }      

    public void paint(TransformGroup molecule, String mode) {
        
    }

    private Material getMaterial(Atom atom)
    {
        Chain chain = atom.getResidue().getChain();
        Residue residue = atom.getResidue();

        Material material = new Material();

        String colourKey = null;
        if (paintMode.equals("CPK")) {
            colourKey = "cpk." + ElementsManager.getElement(atom.getSymbol()).getCPKColourIndex();
        } else if (paintMode.equals("Group")) {
            colourKey = "residue." + residue.getName().toLowerCase();
        }

        String rgbString = properties.getProperty(
                colourKey != null ? colourKey.toLowerCase() : "",
                "255,255,255"
        );

        if (paintMode.equals("Chain")) {
            Color color = AppearanceManager.getOrAllocateColour(chain.getId());
            rgbString = String.format("%d,%d,%d", color.getRed(), color.getGreen(), color.getBlue());
        } else if (paintMode.equals("Structure"))  {
            if (residue.isHelixPart()) {
                rgbString = "240,0,128";
            } else if (residue.isSheetPart()) {
                rgbString = "255,255,0";
            } else if (residue.isTurnPart()) {
                rgbString = "96,128,255";
            } else {
                rgbString = "255,255,255";
            }
        }

        String[] rgb = rgbString.split(",");
        Color3f colour = new Color3f(
                Float.parseFloat(rgb[0]) / 255.0f,
                Float.parseFloat(rgb[1]) / 255.0f,
                Float.parseFloat(rgb[2]) / 255.0f
        );

        material.setDiffuseColor(colour);

        return material;
    }
}
