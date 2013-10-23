import java.awt.*;
import java.awt.image.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.*;

class PaletteDisplay extends JPanel{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	SFTPalette mPalette;
    Component mComponent;
	JFrame pFrame;

	public PaletteDisplay(SFTPalette aPalette, Component aComponent) {
		mPalette = aPalette; 
		mComponent = aComponent;
		
		pFrame = new JFrame("Palette Display", null);
		pFrame.add(this);
		pFrame.setSize(800, 600);
	}
	
	public void show() {
		pFrame.setLocationRelativeTo(mComponent);
		pFrame.setVisible(true);
	}
	
	public void paint(Graphics g) {
    	int h = 70;
    	int dh = 10;
    	int w = 700;

    	BufferedImage pImage;

    	Graphics2D g2 = (Graphics2D) g;
        
    	super.paintComponent(g);
    	
        g2.setColor(Color.black);
        g2.setFont(new Font( "SansSerif", Font.PLAIN, 18 ));
        
        pImage = mPalette.drawCMap(-1, h, w);
        g2.drawString("Mix", 25, 35);
        g2.drawImage(pImage,100,0,null);
        
        for (int i=0; i<SFTPalette.NMIXERS; i++) {
            pImage = mPalette.drawCMap(i, h, w);
            g2.drawString("CMap " + (i+1), 20, 35 + (i+1)*(h + dh));
            g2.drawImage(pImage,100,(i+1)*(h + dh),null);
        	
        }
    	
        g2.setColor (Color.gray);

    }
}