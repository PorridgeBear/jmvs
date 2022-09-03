package com.adcworks.jmvs.interaction;

import com.sun.j3d.utils.behaviors.mouse.*;
import javax.media.j3d.*;
import java.awt.event.*;

import com.adcworks.jmvs.render.RenderScene;

public class Rotator extends MouseRotate 
{
	private RenderScene moleculeScene;

   	public Rotator(TransformGroup transformGroup, RenderScene mScene) 
	{
      		super(transformGroup);
      		moleculeScene = mScene;
   	}

   	public void processMouseEvent(MouseEvent evt) 
	{
      		if (evt.getID()==MouseEvent.MOUSE_PRESSED) 
		{
                    /*
			if(moleculeScene.getMolecule().getNumAtoms() > 330)
			{
				if(moleculeScene.getCurrentDisplayMode() == DisplayModeManager.SPACEFILL)
					moleculeScene.drawMoleculeBoundBox();
			}
                     */
      		}
      		else if (evt.getID()==MouseEvent.MOUSE_RELEASED)
		{
                    /*
			if(moleculeScene.getMolecule().getNumAtoms() > 330)
		 	{
				moleculeScene.drawMoleculeRendered();
			}
                     */
		}
	     	super.processMouseEvent(evt);
 	}
}
