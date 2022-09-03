package com.adcworks.jmvs.render.appearance;

import java.util.Properties;

import javax.media.j3d.TransformGroup;

import com.adcworks.jmvs.model.Molecule;


/**
 * JMVS Plugin Interface. All plugins must implement this interface.
 *  
 * @author Ali
 *
 */
public interface JMVSPlugin {

    /**
     * The name of the plugin.
     * @return
     */
    public String getName();
    
    /**
     * The name of the plugin author(s)
     * @return
     */
    public String getAuthor();
    
    /**
     * The plugin version.
     * @return
     */
    public String getVersion();
    
    /**
     * Additional Plugin information.
     * @return
     */
    public String getInformation();
    
    /**
     * Initialise plugin with properties
     * @param properties
     */
    public void init(Properties properties);

    public void setPaintMode(String paintMode);

    /**
     * Create the molecule geometry.
     * @return
     */
    public TransformGroup render(Molecule molecule);
    
    /**
     * The available paint modes for the geometry.
     * @return
     */
    public String[] getPaintModes();
    
    /**
     * Paint the molecule using the specified paint mode.
     * @param molecule
     * @param mode
     */
    public void paint(TransformGroup molecule, String mode);
    
}
