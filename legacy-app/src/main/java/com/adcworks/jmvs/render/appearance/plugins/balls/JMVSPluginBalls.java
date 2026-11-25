package com.adcworks.jmvs.render.appearance.plugins.balls;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.media.j3d.*;
import javax.vecmath.*;

import com.adcworks.jmvs.model.*;
import com.adcworks.jmvs.render.appearance.AppearanceManager;
import com.adcworks.jmvs.render.appearance.JMVSPlugin;

/**
 * Space-filling display mode.
 * https://www.umass.edu/microbio/rasmol/rasbonds.htm
 */
public class JMVSPluginBalls implements JMVSPlugin {

    private static final String PLUGIN_NAME 	= "Balls";
    private static final String PLUGIN_AUTHOR 	= "Allistair Crossley, adc works";
    private static final String PLUGIN_VERSION 	= "1.0";
    private static final String PLUGIN_INFO 	= "";

    private final float RADII_SCALE_FACTOR = 10.0f;
    private static final Map<String, Float> UNITED_RADII_ELEMENTS;
    static {
        UNITED_RADII_ELEMENTS = new HashMap<>();
        UNITED_RADII_ELEMENTS.put("C", 1.872f);
        UNITED_RADII_ELEMENTS.put("N", 1.507f);
        UNITED_RADII_ELEMENTS.put("O", 1.400f);
        UNITED_RADII_ELEMENTS.put("S", 1.848f);
    }
    
    private final String[] paintModes = {"CPK", "Chain", "Group", "Structure"};
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

    public TransformGroup render(Molecule molecule)
    {
        TransformGroup ballsTG = new TransformGroup();
        ballsTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        ballsTG.setCapability(Group.ALLOW_CHILDREN_READ);

        for (Model model : molecule.getModels()) {
            for (Chain chain : model.getChains()) {
                for (Residue residue : chain.getResidues()) {
                    for (Atom atom : residue.getAtoms()) {

	                    Transform3D atomPosition = new Transform3D();
	                    atomPosition.set(0.1, 
	                        new Vector3d(
	                            atom.getX() - molecule.midX, 
	                            atom.getY() - molecule.midY, 
	                            atom.getZ() - molecule.midZ
	                        )
	                    );
	                    
	                    TransformGroup atomGroup = new TransformGroup(atomPosition);
	                    atomGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	                    atomGroup.setCapability(Group.ALLOW_CHILDREN_READ);
	                    atomGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);

	                    Material atomMaterial = getMaterial(chain, residue, atom);
	                    atomMaterial.setCapability(Material.ALLOW_COMPONENT_READ);
	                    atomMaterial.setCapability(Material.ALLOW_COMPONENT_WRITE);
	                    atomMaterial.setLightingEnable(true);
	                    
	                    Appearance atomAppearance = new Appearance();
	                    atomAppearance.setCapability(Appearance.ALLOW_MATERIAL_READ);
	                    atomAppearance.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
	                    atomAppearance.setMaterial(atomMaterial);

                        float radius = ElementsManager.getElement(atom.getSymbol()).getVanDerWaalsRadius();

                        /*
                        Implement united radii for molecules without hydrogens
                        https://www.umass.edu/microbio/rasmol/rasbonds.htm

                        The radii for common elements are listed below. If the PDB file contains hydrogen atoms, the
                        van der Waals radii are used; if not, slightly larger 'united atom' radii are
                        used for C, N, O, and S as detailed below.
                        */
                        if (!molecule.hasHydrogenAtoms()) {
                            if (atom.isElement(UNITED_RADII_ELEMENTS.keySet().toArray(new String[0]))) {
                                radius = UNITED_RADII_ELEMENTS.get(atom.getSymbol());
                            }
                        }

	                    int divisions = Integer.parseInt(properties.getProperty("plugin.balls.divisions"));
	                    
	                    Sphere atomGeometry = new Sphere(
                            radius * RADII_SCALE_FACTOR,
                            Sphere.GENERATE_NORMALS | Sphere.ENABLE_APPEARANCE_MODIFY, 
                            divisions
	                    );
		                    
	                    atomGeometry.setAppearance(atomAppearance);
	                    atomGeometry.setUserData(atom.getSymbol());
	
	                    atomGroup.addChild(atomGeometry);		
	                    ballsTG.addChild(atomGroup);
                    }
                }
            }
        }

        return ballsTG;
    }

    public String[] getPaintModes() {
        return paintModes;
    }      

    private Material getMaterial(Chain chain, Residue residue, Atom atom)
    {
        Material material = new Material();

        String colourKey = null;
        if (paintMode.equals("CPK")) {
            colourKey = "cpk." + ElementsManager.getElement(atom.getSymbol()).getCPKColourIndex();
        } else if (paintMode.equals("Group")) {
            colourKey = "residue." + residue.getName();
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

    public void paint(TransformGroup molecule, String mode)
    {

    }
}
