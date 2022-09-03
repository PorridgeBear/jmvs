package com.adcworks.jmvs.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Properties;

import com.adcworks.jmvs.io.JMVSFileLoader;
import com.adcworks.jmvs.io.MoleculeReader;
import com.adcworks.jmvs.io.cif.CIFReader;
import com.adcworks.jmvs.render.RenderScene;
import com.adcworks.jmvs.render.appearance.AppearanceManager;
import com.adcworks.jmvs.render.appearance.JMVSPlugin;
import com.adcworks.jmvs.model.Molecule;

import javax.swing.*;

public class JMVSPanel extends Panel implements ActionListener {

    private final Frame parent;
    private final JLabel statusLabel = new JLabel("");
    private final AppearanceManager appearanceManager;
    private RenderScene scene;
    private Properties properties;
    private Molecule molecule;

    private String pluginName = null;
    private String pluginPaintMode = null;

    public JMVSPanel(Frame parent, Properties properties) {
        this.parent = parent;
        this.properties = properties;
        
        appearanceManager = new AppearanceManager();
        appearanceManager.initialisePlugins(properties);
	    
        initialise();
    }

    private void initialise() {
        setLayout(new BorderLayout());

        JPanel statusPanel = new JPanel();
        add(statusPanel, BorderLayout.SOUTH);
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusPanel.add(statusLabel);

        Menu mData = new Menu("Data");
        MenuItem miLoad = new MenuItem("Load");
        miLoad.addActionListener(new JMVSFileLoader(parent, this));
        mData.add(miLoad);

        JMVSPlugin defaultPlugin = null;
        Menu mPlugins = new Menu("Plugins");
        String[] pluginNames = appearanceManager.getPluginNames();
        for (String pluginName : pluginNames) {
            JMVSPlugin plugin = appearanceManager.getPlugins().get(pluginName);

            Menu mPlugin = new Menu(pluginName);
            
	        String[] paintModes = plugin.getPaintModes();
	        for (String paintMode : paintModes) {
	            MenuItem miPaintMode = new MenuItem(paintMode);
	            miPaintMode.addActionListener(this);
                mPlugin.add(miPaintMode);
	        }
	        
	        mPlugins.add(mPlugin);
        }
                
        MenuBar mb = new MenuBar();
        mb.add(mData);
        mb.add(mPlugins);
        parent.setMenuBar(mb);

        pluginName = appearanceManager.getDefaultMode();
        pluginPaintMode = appearanceManager.getDefaultPaint();

        scene = new RenderScene();
        Component canvas = scene.getCanvas3D();
        add(canvas);

        setStatus();
    }

    public void setMolecule(Molecule molecule) {
        this.molecule = molecule;
        setStatus();

        System.out.println(String.format("Load: %s %s", pluginName, pluginPaintMode));

        scene.display(
            appearanceManager.render(molecule, pluginName, pluginPaintMode),
            molecule.minX - molecule.maxX,
            molecule.minY - molecule.maxY,
            true
        );
    }

    public void actionPerformed(ActionEvent aE) {        
        
        MenuItem mi = (MenuItem) aE.getSource();
        Menu m = (Menu) mi.getParent();
        
        pluginName = m.getLabel();
        pluginPaintMode = mi.getLabel();

        System.out.println(String.format("Menu: %s %s", pluginName, pluginPaintMode));

        if (molecule != null) {
            scene.display(
                    appearanceManager.render(molecule, pluginName, pluginPaintMode),
                    molecule.minX - molecule.maxX,
                    molecule.minY - molecule.maxY,
                    true
            );
        }

        setStatus();
    }

    private void setStatus() {
        String moleculeName = molecule != null ? molecule.getName() : "None loaded";
        String mode = pluginName != null ? pluginName : "Not selected";
        String paint = pluginPaintMode != null ? pluginPaintMode : "Not selected";
        statusLabel.setText(String.format(
                "Molecule: %s Mode: %s Paint: %s", moleculeName, mode, paint));
    }
}