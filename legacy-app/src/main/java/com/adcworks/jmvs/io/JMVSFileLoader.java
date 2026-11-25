package com.adcworks.jmvs.io;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import com.adcworks.jmvs.gui.JMVSPanel;
import com.adcworks.jmvs.io.cif.CIFReader;
import com.adcworks.jmvs.model.Molecule;

/**
 * File Loader Event Listener.
 *  
 * @author Ali
 *
 */
public class JMVSFileLoader implements ActionListener {

    private String FILE_DLG_LOAD_TITLE = "Load Molecular Data File";
    
    private Frame parent;
    private JMVSPanel jmvs;
    private FileDialog dlgFile;
    
    /**
     * Create the JMVS file loader.
     * @param parent
     * @param jmvs
     */
    public JMVSFileLoader(Frame parent, JMVSPanel jmvs) {
        this.parent = parent;
        this.jmvs = jmvs;
        this.dlgFile = new FileDialog(parent, FILE_DLG_LOAD_TITLE, FileDialog.LOAD);
    }
    
    /**
     * Process the action.
     */
    public void actionPerformed(ActionEvent event) {

        dlgFile.setModal(true);
        dlgFile.show();
		
		String fileName = dlgFile.getFile();
		
		if (fileName != null) {
		    try {
		        
		        String moleculeFile = dlgFile.getDirectory() + File.separator + fileName;
				System.out.println(moleculeFile);
				
	            MoleculeReader moleculeReader = new CIFReader();
	            Molecule molecule = moleculeReader.read(new File(moleculeFile));
	            jmvs.setMolecule(molecule);
		    
		    } catch (Exception e) {
                e.printStackTrace();
		        System.out.println("Could not load the molecule file. " + e);
		    }
		}
    }
}
