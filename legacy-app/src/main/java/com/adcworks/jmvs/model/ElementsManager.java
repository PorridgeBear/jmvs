package com.adcworks.jmvs.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

/**
 * Elements Manager
 * 
 * @author Ali
 */
public class ElementsManager {

    private static ElementsManager instance;
    private static HashMap<String, Element> elements = new HashMap<>();
    
    private ElementsManager() {
        initialise();
    }

    public static ElementsManager getInstance() {
        if (instance == null) {
            instance = new ElementsManager();
        }

        return instance;
    }

    public static void initialise() {
    
	    Properties elementsProperties = null;
	    
      	try {
      	    elementsProperties = new Properties();
	        ClassLoader loader = Thread.currentThread().getContextClassLoader();
	        java.io.InputStream is = 
	            loader.getResourceAsStream("elements.properties");
	        
	        if (is == null) {
	        	System.out.println("Error: Could not load elements.properties.");
		   		System.exit(0);
	        }
	        
	        elementsProperties.load(is);
		   	
        } catch (IOException ioE) {
            System.out.println("Error: Could not load elements.properties.");
            System.exit(0);
        }
        
        if (elementsProperties != null && elementsProperties.size() > 0) {
            for (Iterator iterator = elementsProperties.keySet().iterator(); iterator.hasNext(); ) {
                String key = iterator.next().toString();
                String[] values = elementsProperties.getProperty(key).split(",");

                elements.put(values[0].trim().toLowerCase(), 
                    new Element(key, values[0].trim(), values[4].trim(), values[1].trim(), values[2].trim(), values[3].trim()));
            }
        }
        
        System.out.println("Loaded Elements " + elements.size());
    }

    public static Element getElement(String key) {
        
        key = key.toLowerCase();
        
        // element symbols are max. 2 characters
        if (key.length() > 2) {
            key = key.substring(0, 2);
        }

        if (key.equals("ca") || key.equals("cb") || key.equals("cd") || key.equals("ce") || key.equals("cg")) {
            key = "c";
        }
        
        if (key.equals("nd") || key.equals("ne")) {
            key = "n";
        }        
        
        Element e = (Element) elements.get(key);
        
        if (e == null) {
            // must be an extension letter or number on 1 character symbol
            key = key.substring(0, 1);
            e = (Element) elements.get(key);
            
            // can't find it, use default
            if (e == null) {
                e = (Element) elements.get("?");
            }            
        }
        
        return e;
    }
}
