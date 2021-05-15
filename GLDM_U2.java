import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Panel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
     Opens an image window and adds a panel below the image
*/
public class GRDM_U2 implements PlugIn {

    ImagePlus imp; // ImagePlus object
	private int[] origPixels;
	private int width;
	private int height;
	
	
    public static void main(String args[]) {
		//new ImageJ();
    	//IJ.open("/users/barthel/applications/ImageJ/_images/orchid.jpg");
    	IJ.open("orchid.jpg");
		
		GRDM_U2 pw = new GRDM_U2();
		pw.imp = IJ.getImage();
		pw.run("");
	}
    
    public void run(String arg) {
    	if (imp==null) 
    		imp = WindowManager.getCurrentImage();
        if (imp==null) {
            return;
        }
        CustomCanvas cc = new CustomCanvas(imp);
        
        storePixelValues(imp.getProcessor());
        
        new CustomWindow(imp, cc);
    }


    private void storePixelValues(ImageProcessor ip) {
    	width = ip.getWidth();
		height = ip.getHeight();
		
		origPixels = ((int []) ip.getPixels()).clone();
	}


	class CustomCanvas extends ImageCanvas {
    
        CustomCanvas(ImagePlus imp) {
            super(imp);
        }
    
    } // CustomCanvas inner class
    
    
    class CustomWindow extends ImageWindow implements ChangeListener {
         
        private JSlider jSliderBrightness;
		private JSlider jSlider2;
		private double brightness;

		CustomWindow(ImagePlus imp, ImageCanvas ic) {
            super(imp, ic);
            addPanel();
        }
    
        void addPanel() {
        	//JPanel panel = new JPanel();
        	Panel panel = new Panel();

            panel.setLayout(new GridLayout(4, 1));
            jSliderBrightness = makeTitledSilder("Helligkeit", 0, 256, 128);
            jSliderSaturation = makeTitledSilder("Sättigung", 0, 8, 4);
            jSliderContrast = makeTitledSilder("Kontrast", 0, 10, 5);
            jSliderHue = makeTitledSilder("Farbe", 0, 360, 0);
            panel.add(jSliderBrightness);
            panel.add(jSliderSaturation);
            panel.add(jSliderContrast);
            panel.add(jSliderHue);
            
            add(panel);
            
            pack();
         }
      
        private JSlider makeTitledSilder(String string, int minVal, int maxVal, int val) {
		
        	JSlider slider = new JSlider(JSlider.HORIZONTAL, minVal, maxVal, val );
        	Dimension preferredSize = new Dimension(width, 50);
        	slider.setPreferredSize(preferredSize);
			TitledBorder tb = new TitledBorder(BorderFactory.createEtchedBorder(), 
					string, TitledBorder.LEFT, TitledBorder.ABOVE_BOTTOM,
					new Font("Sans", Font.PLAIN, 11));
			slider.setBorder(tb);
			slider.setMajorTickSpacing((maxVal - minVal)/10 );
			slider.setPaintTicks(true);
			slider.addChangeListener(this);
			
			return slider;
		}
        
        private void setSliderTitle(JSlider slider, String str) {
			TitledBorder tb = new TitledBorder(BorderFactory.createEtchedBorder(),
				str, TitledBorder.LEFT, TitledBorder.ABOVE_BOTTOM,
					new Font("Sans", Font.PLAIN, 11));
			slider.setBorder(tb);
		}

		public void stateChanged( ChangeEvent e ){
			JSlider slider = (JSlider)e.getSource();

			if (slider == jSliderBrightness) {
				brightness = slider.getValue()-128;
				String str = "Helligkeit " + brightness; 
				setSliderTitle(jSliderBrightness, str); 
			}
			
			if (slider == jSliderSaturation) {
				if (slider.getValue() >40) {
					saturation = slider.getValue()-3;
				} else {
					saturation = slider.getValue()/4.0;
				}
				String str = "Sättigung " + saturation; 
				setSliderTitle(jSliderSaturation, str); 
			}
			
			if (slider == jSliderHue) {
				hue = slider.getValue();
				String str = "Farbwert " + hue; 
				setSliderTitle(jSliderHue, str); 
			}
			
			if (slider == jSliderContrast) {
				if (slider.getValue() >5) {
					contrast = slider.getValue()-(10-slider.getValue());
				} else {
					contrast = slider.getValue()/5.0;
				}
				String str = "Kontrast  " + contrast;
				setSliderTitle(jSliderContrast, str); 
			}
			
			changePixelValues(imp.getProcessor());
			
			imp.updateAndDraw();
		}

		
		private void changePixelValues(ImageProcessor ip) {
			
			// Array fuer den Zugriff auf die Pixelwerte
			int[] pixels = (int[])ip.getPixels();
			
			for (int y=0; y<height; y++) {
				for (int x=0; x<width; x++) {
					int pos = y*width + x;
					int argb = origPixels[pos];  // Lesen der Originalwerte 
					
					int r = (argb >> 16) & 0xff;
					int g = (argb >>  8) & 0xff;
					int b =  argb        & 0xff;
					
					//Umwandlung von RGB nach YUV, gemäß gegebener Formeln					
					double Y = (0.299 * r) + (0.587 * g) + (0.114 * b);
					double U =  0.493 * (b - Y);
					double V =  0.877 * (r - Y);
					
					// anstelle dieser drei Zeilen spÃ¤ter hier die Farbtransformation durchfÃ¼hren,
					// die Y Cb Cr -Werte verÃ¤ndern und dann wieder zurÃ¼cktransformieren
					//Helligkeit
					Y = Y + brightness;
					
					//Sättigung
					U = U * saturation;
					V = V * saturation;
					
					//Farbdrehung
					U = ((Math.cos(Math.toRadians(hue))* U) + (-1*Math.sin(Math.toRadians(hue))*V));
					V = ((Math.sin(Math.toRadians(hue))* U) + (Math.cos(Math.toRadians(hue))*V));
					
					//Kontrast
					U = contrast * (U - 127.5) + 127.5;
					V = contrast * (V - 127.5) + 127.5;
					
					//Zurückwandeln nach RGB, gemäß gegebener Formeln
					int rn = (int) (Y + V/0.877);
					int bn = (int) (Y + U/0.493);
					int gn = (int) (1/0.587 * Y - 0.299/0.587 * rn - 0.114/0.587 * bn);
					
					// Hier muessen die neuen RGB-Werte wieder auf den Bereich von 0 bis 255 begrenzt werden
					if (rn >=255) rn = 255;
					if (gn >=255) gn = 255;
					if (bn >=255) bn = 255;
					if (rn <=0) rn = 0;
					if (gn <=0) gn = 0;
					if (bn <=0) bn = 0;
					
					pixels[pos] = (0xFF<<24) | (rn<<16) | (gn<<8) | bn;
				}
			}
		}
		
    } // CustomWindow inner class
} 
