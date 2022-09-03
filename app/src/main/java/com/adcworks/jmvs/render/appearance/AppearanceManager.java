package com.adcworks.jmvs.render.appearance;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.media.j3d.TransformGroup;

import com.adcworks.jmvs.model.*;

public class AppearanceManager
{
	private static Map<String, Color> allocatedColours = new HashMap<>();

    private final Map<String, JMVSPlugin> plugins = new HashMap<>();
    private String plugin;
	private String defaultMode;
	private String defaultPaint;

	public AppearanceManager() {
	    ElementsManager.initialise();
	}

	public void initialisePlugins(Properties properties) {
	    System.out.println("Initialising Plugins ...");

        String[] pluginList = properties.getProperty("plugin.list").split(",");
        
        for (int i = 0; i < pluginList.length; i++) {
            
            try {                    
                String pluginClassName = properties.getProperty(
                    "plugin." + pluginList[i] + ".class"
                );
                
                if (pluginClassName == null) {
                    continue;
                }
                
                Class pluginClass = Class.forName(pluginClassName);
                
                JMVSPlugin plugin = (JMVSPlugin) pluginClass.newInstance();
                plugin.init(properties);
                plugins.put(plugin.getName(), plugin);
                
                System.out.println("Plugin Initialised: " + plugin.getName() +
                    " by: " + plugin.getAuthor() + " version: " + plugin.getVersion()
                );
                
            } catch (Exception e) {
				e.printStackTrace();
                System.out.println("Warning: Plugin failed to load [" + pluginList[i] + "]");
            }
        }

		// return properties.getProperty("plugin.default.mode");
		defaultMode = "Balls";
		defaultPaint = "CPK";
	}

	public String[] getPluginNames() {
	    return plugins.keySet().toArray(new String[0]);
	}

	public Map<String, JMVSPlugin> getPlugins() {
	    return plugins;
	}

	public void setPlugin(String plugin) {
	    this.plugin = plugin;
	}

	public String getPlugin() {
	    return this.plugin;
	}

	public String getDefaultMode() {
		return defaultMode;
	}

	public String getDefaultPaint() {
		return defaultPaint;
	}

	public TransformGroup render(Molecule molecule, String pluginName, String pluginPaintMode) {
		System.out.println(String.format("render: %s %s", pluginName, pluginPaintMode));
	    JMVSPlugin jmvsPlugin = plugins.get(pluginName);
		jmvsPlugin.setPaintMode(pluginPaintMode);
	    TransformGroup group = jmvsPlugin.render(molecule);
	    // jmvsPlugin.paint(group, pluginPaintMode);
	    return group;
	}

	public static Color getOrAllocateColour(String key)
	{
		if (allocatedColours.containsKey(key)) {
			return allocatedColours.get(key);
		}

		Color colorForKey = new Color((int) (Math.random() * 0x1000000));
		allocatedColours.put(key, colorForKey);

		return colorForKey;
	}
}	
