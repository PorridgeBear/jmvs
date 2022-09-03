package com.adcworks.jmvs.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class JMVSAbout extends JDialog implements MouseListener {	
    
    private JEditorPane text;

    public JMVSAbout(Frame parent) {
        
        super(parent, "JMVS: Java3D Molecular Visualisation System", true);

        addMouseListener(this);
        
        Dimension sSize = Toolkit.getDefaultToolkit().getScreenSize();
        setResizable(false);

        text = new JEditorPane();
        text.setContentType("text/html");

        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(Color.white);
         
        text.setText("" + 
            "<span><font>In-Development Release</font>" +
            "<br>" +
            "<span><font>adc works, 1999-2022</font>" +
            "<br>" +
            "<a href='www.adcworks.com/projects/jmvs'><font>www.adcworks.com/jmvs</span></a>");
        
        text.setEditable(false);
        text.setMargin(new Insets(10, 10, 10, 10));
        
        Image imgLogo = Toolkit.getDefaultToolkit().getImage(getClass().getResource("logo/jmvs.gif"));
        JLabel lLogo = new JLabel(new ImageIcon(imgLogo));
        
        getContentPane().add(lLogo, BorderLayout.NORTH);
        getContentPane().add(text, BorderLayout.CENTER); 

        pack();
        
        setLocation((sSize.width / 2) - (getSize().width / 2), (sSize.height / 2) - (getSize().height / 2));   
    }
        
    public void mouseClicked(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public void mouseEntered(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public void mouseExited(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public void mousePressed(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public void mouseReleased(java.awt.event.MouseEvent mouseEvent) {
         setVisible(false);
    }
    
}
