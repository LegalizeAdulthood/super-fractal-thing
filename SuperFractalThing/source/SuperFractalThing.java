//	SuperFractalThing
//
//
//    Copyright 2013 Kevin Martin
//
//    This file is part of SuperFractalThing.
//
//    SuperFractalThing is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    any later version.
//
//    SuperFractalThing is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with SuperFractalThing.  If not, see <http://www.gnu.org/licenses/>.
//
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;
//import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.InternationalFormatter;
import javax.swing.text.NumberFormatter;
import javax.swing.Timer;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;

import javax.imageio.ImageIO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.Format;
import java.text.DecimalFormat;
import java.text.AttributedCharacterIterator;
import java.text.ParsePosition;

import javax.jnlp.FileContents;
import javax.jnlp.FileOpenService;
import javax.jnlp.FileSaveService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

interface SFTGui
{
	void SetCoords( BigDecimal aSize, BigDecimal x, BigDecimal y, int iterations);
	BigDecimal GetTheSize();
	int GetIterations();
	void SetIterations(int aValue);
	BigDecimal[] GetCoords();
	void StartProcessing();
	void EndProcessing();
	void SetHoverIndex(int index);
	void SetProgress(int progress, int pMax);
	void ExportImage(BufferedImage aImage);
	void SetCalculationTime(long aTime_ms);
    void AddToUndoBuffer();
}

interface LibraryLoader
{
	void LoadTheFile(File f);
	void LoadTheFile(BufferedReader br);
}



public class SuperFractalThing  extends JApplet implements SFTGui, ActionListener, LibraryLoader
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 0;//get rid of warning
	static SftComponent mComp;
	static JFormattedTextField mSize_box;
	static JFormattedTextField mPos_x_box;
	static JFormattedTextField mPos_y_box;
	static JFormattedTextField mIterations_box;
	static BigDecimal mPos_x;
	static BigDecimal mPos_y;
	static BigDecimal mSize;
	JLabel mSize_label;
	JProgressBar mProgress_bar;
	JButton mCancel_button;
	JLabel mIterations_label;
	PositionLibrary mLibrary;
	JLabel mTime_label;
	JMenuBar mMenu_bar;
	ExportDialog mDialog;
	PaletteDialog mPalette_dialog;
	OptionsDialog mOptions_dialog;
	SFTPalette mPalette;
	UndoBuffer mUndo_buffer;
	JMenuItem mRedo_item;
	JMenuItem mUndo_item;
	
	static JFrame mFrame;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JFrame f = new JFrame("SuperFractalThing");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		int N_CPUS = Runtime.getRuntime().availableProcessors();
		
		System.out.println("Num CPUS");
		System.out.println(N_CPUS);
		System.out.println(java.lang.Runtime.getRuntime().maxMemory()); 
	    SuperFractalThing ap = new SuperFractalThing();
	    ap.init();
	    ap.start();

    
	    f.add("Center", ap);
        f.pack();
        f.setVisible(true);
 
        mFrame = f;

 	}

	public void start()
	{
        mUndo_buffer = new UndoBuffer();
	    initComponents();	
	}
	
	public void SetProgress(int aProgress, int pMax)
	{
		mProgress_bar.setMaximum(pMax);
		mProgress_bar.setValue(aProgress);
	}

    public void AddToUndoBuffer()
    {
    	BigDecimal coords[] = GetCoords();
		mUndo_buffer.Push(coords[0],coords[1],GetTheSize(),GetIterations());
		if (mUndo_item!=null)
			mUndo_item.setEnabled(mUndo_buffer.CanUndo());
		if (mRedo_item!=null)
			mRedo_item.setEnabled(mUndo_buffer.CanRedo());
    }
    
	public void SetCalculationTime(long aTime_ms)
	{
		String text;
		if (aTime_ms>=0)
			text = "  Last calculation time: " + Double.toString((double)aTime_ms / 1000 )+" seconds";
		else
			text="";
		mTime_label.setText(text);
	}
	
	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();
		
		if (command=="Cancel")
		{
			mComp.Cancel();
		}
		if (command=="Open")
		{
			FileOpenService fos; 

		    try { 
		        fos = (FileOpenService)ServiceManager.lookup("javax.jnlp.FileOpenService"); 
		    } catch (UnavailableServiceException e) { 
		        fos = null; 
		   	 	JFileChooser chooser = new JFileChooser();
			 
			    FileNameExtensionFilter filter = new FileNameExtensionFilter("SuperFractalThingFile",  "txt");
			    chooser.setFileFilter(filter);
			    int returnVal = chooser.showOpenDialog(this);
			    if(returnVal == JFileChooser.APPROVE_OPTION)
			    {
			       System.out.println("You chose to open this file: " +
			            chooser.getSelectedFile().getName());
			       
			       File f = chooser.getSelectedFile();
			       
			       LoadTheFile(f);
			       
			    } 
			    return;
		        
		    } 

		    if (fos != null) { 
		        try { 
		            // ask user to select a file through this service 
		            FileContents fc = fos.openFileDialog(null, null); 
		            // ask user to select multiple files through this service 
		            //FileContents[] fcs = fos.openMultiFileDialog(null, null); 
		            
		            InputStream is = fc.getInputStream();
		            BufferedReader br = new BufferedReader( new InputStreamReader(is));		
		            LoadTheFile(br);
		        } catch (Exception e) { 
		            e.printStackTrace(); 
		        } 
		    } 
		    
/*	
*/
		}
		else if (command=="Save")
		{
			String str;
			double size =  GetTheSize().doubleValue();
			size*=0.5f;
			
			str="s="+Double.toString(size)+"\n";
			str+="r="+mPos_x_box.getText()+"\n";
			str+="i="+mPos_y_box.getText()+"\n";
			str+="iteration_limit="+mIterations_box.getText().replaceAll(",", "")+"\n";
			
			ByteArrayInputStream bis;
			try {
				bis = new ByteArrayInputStream(str.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			FileSaveService fss; 
		    try { 
		        fss = (FileSaveService)ServiceManager.lookup("javax.jnlp.FileSaveService"); 
		    } catch (UnavailableServiceException e) { 
		        fss = null; 
		    } 
		    
		    if (fss!=null)
		    {
		    	String[] exts={"txt"};
		  
				try {
					fss.saveFileDialog(null,exts,bis,"sft_save.txt");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		    else
		    {
		   	 	JFileChooser chooser = new JFileChooser();
			 
			    FileNameExtensionFilter filter = new FileNameExtensionFilter("SuperFractalThingFile",  "txt");
			    chooser.setFileFilter(filter);
			    int returnVal = chooser.showSaveDialog(this);
			    if(returnVal == JFileChooser.APPROVE_OPTION)
			    {
			       System.out.println("You chose to open this file: " +
			            chooser.getSelectedFile().getName());
			       
			       File f = chooser.getSelectedFile();
			       
			       BufferedWriter file;
					try {
						file = new BufferedWriter(new FileWriter(f));
						file.write(str);
						file.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					};
			    } 
			    return;
		    }
		}
		else if (command=="Reset")
		{
			mPos_x_box.setValue(new BigDecimal(-0.75));
			mPos_y_box.setValue(new BigDecimal(0.0));
			mSize_box.setValue(new BigDecimal(3.0));
			mIterations_box.setValue(new Integer(1024));
			mSize = new BigDecimal(3.0);
			AddToUndoBuffer();
			mComp.Refresh();
			mComp.repaint();						
		}
		else if (command=="Export PNG")
		{
			
			//ExportDialog dialog = new ExportDialog(mFrame, mComp);
			boolean res = mDialog.Run();
			if (!res)
				return;
			
			if (mDialog.GetWidth()>50000 || mDialog.GetHeight()>50000 || mDialog.GetWidth()<32 || mDialog.GetHeight()<32)
			{
				JOptionPane.showMessageDialog(mFrame,
					    "Invalid Image Size",
					    "Error",
					    JOptionPane.WARNING_MESSAGE);
				return;
		
			}
			try
			{
				mComp.ExportCalculation(mDialog.GetWidth(), mDialog.GetHeight(), mDialog.GetSuperSample());
			} catch (OutOfMemoryError e) {
				// TODO Auto-generated catch block
				JOptionPane.showMessageDialog(mFrame,
					    "Out of Memory!\n Try using a 64 bit browser.",
					    "Error",
					    JOptionPane.WARNING_MESSAGE);
				EndProcessing();
				return;
			};
			
/*			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				ImageIO.write(mComp.GetImage(),"PNG",bos);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return;
			}

			FileSaveService fss; 
		    try { 
		        fss = (FileSaveService)ServiceManager.lookup("javax.jnlp.FileSaveService"); 
		    } catch (UnavailableServiceException e) { 
		        fss = null; 
		        return;
		    } 
		    
		    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		    String[] exts={"png"};
			try {
				fss.saveFileDialog(null,exts,bis,"sft_exp.png");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
*/		}
		else if (command=="Refresh")
		{
			mComp.Refresh();
			mComp.repaint();			
		}		    
		else if (command=="About")
		{
			//JOptionPane.showMessageDialog(mFrame, "Abitrary(ish) precision Mandelbrot set rendering in Java.\n\nVersion 0.1\n\n(c) Kevin Martin","SuperFractalThing Java",JOptionPane.PLAIN_MESSAGE);
			AboutDialog ad = new AboutDialog(mFrame,mComp);
			ad.Run();
		}
		else if (command=="Palette")
		{
			mPalette_dialog.Run();
		}
		else if (command=="Options")
		{
			mOptions_dialog.Run();
			mComp.SetSuperSampleType(mOptions_dialog.GetSuperSampleType());
			mComp.SetNumThreads(mOptions_dialog.GetNumThreads());
		}
		else if (command == "Undo")
		{
			if (mUndo_buffer.CanUndo())
			{
				mUndo_buffer.Undo();
				mPos_x_box.setValue(mUndo_buffer.GetX());
				mPos_y_box.setValue(mUndo_buffer.GetY());
				mSize_box.setValue(mUndo_buffer.GetSize());
				mIterations_box.setValue(mUndo_buffer.GetIterations());
				mComp.Refresh();
				mComp.repaint();			
				mUndo_item.setEnabled(mUndo_buffer.CanUndo());
				mRedo_item.setEnabled(mUndo_buffer.CanRedo());
			}
		}
		else if (command == "Redo")
		{
			if (mUndo_buffer.CanRedo())
			{
				mUndo_buffer.Redo();
				mPos_x_box.setValue(mUndo_buffer.GetX());
				mPos_y_box.setValue(mUndo_buffer.GetY());
				mSize_box.setValue(mUndo_buffer.GetSize());
				mIterations_box.setValue(mUndo_buffer.GetIterations());
				mComp.Refresh();
				mComp.repaint();			
				mUndo_item.setEnabled(mUndo_buffer.CanUndo());
				mRedo_item.setEnabled(mUndo_buffer.CanRedo());
			}
		}
	}
	
	public void ExportImage(BufferedImage aImage)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ImageIO.write(aImage,"PNG",bos);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}

		
		FileSaveService fss; 
	    try { 
	        fss = (FileSaveService)ServiceManager.lookup("javax.jnlp.FileSaveService"); 
	    } catch (UnavailableServiceException e) { 
	        fss = null; 
	    } 
	    
	    if (fss!=null)
	    {
		    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		    String[] exts={"png"};
			try {
				fss.saveFileDialog(null,exts,bis,"sft_exp.png");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	    	
	    }
	    else
	    {
		    JFileChooser chooser = new JFileChooser();
			 
		    FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG",  "png");
		    chooser.setFileFilter(filter);
		    int returnVal = chooser.showSaveDialog(this);
		    if(returnVal == JFileChooser.APPROVE_OPTION)
		    {
		       
		       File f = chooser.getSelectedFile();
		       
		       FileOutputStream fs;
				try {
					fs = new FileOutputStream(f);
			       fs.write(bos.toByteArray());
			       fs.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
	    }
	    return;		
	}

	public void SetHoverIndex(int index)
	{
		String str = Integer.toString(index);
		mIterations_label.setText(str);
	}
	
	public void LoadTheFile(File f)
    {
    	try
    	{
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			LoadTheFile(br);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    public void LoadTheFile(BufferedReader br)
    {
    	try
    	{
			String line1 = br.readLine();
			String line2 = br.readLine();
			String line3 = br.readLine();
			String line4 = br.readLine();
			
			
			if (line1.startsWith("s=") && line2.startsWith("r=") && line3.startsWith("i=") && line4.startsWith("iteration_limit="))
			{
				//double size = Double.parseDouble(line1.substring(2));
				//mSize_box.setText(Double.toString(size*2));
				BigDecimal size = new BigDecimal(line1.substring(2));
				size = size.add(size);
				mSize_box.setText(size.toString());
				mPos_x_box.setText(line2.substring(2));
				mPos_y_box.setText(line3.substring(2));
				mIterations_box.setValue(Integer.parseInt(line4.substring(16)));
				mComp.repaint();
				AddToUndoBuffer();
				mComp.DoCalculation();
				mComp.repaint();
				
			}
		

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
	public void SetCoords( BigDecimal aSize, BigDecimal x, BigDecimal y, int iterations)
	{
		mSize_box.setValue(aSize);
		mPos_x_box.setValue(x);
		mPos_y_box.setValue(y);
		mIterations_box.setValue(iterations);
	}
	
	public BigDecimal GetTheSize()
	{
		return new BigDecimal(mSize_box.getText());
	}
	public int GetIterations()
	{
		return Integer.parseInt(mIterations_box.getText().replaceAll(",", ""));
	}
	public void SetIterations(int aValue)
	{
//		mIterations_box.setText(Integer.toString(aValue));
		mIterations_box.setValue(aValue);
	}
	public BigDecimal[] GetCoords()
	{
		BigDecimal[] x=new BigDecimal[2];
		x[0]=new BigDecimal(mPos_x_box.getText());
		x[1]=new BigDecimal(mPos_y_box.getText());
		return x;
	}
    public void initComponents()
    {     
        //setLayout(new BorderLayout());
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.gridwidth=8;
        //p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        mComp = new SftComponent(this);
        mPalette = new SFTPalette(mComp);
        mComp.SetPalette( mPalette );
        
        p.add(mComp,gbc);

        p.addMouseListener(mComp);
        p.addMouseMotionListener(mComp);
        add("North", p);
        
        gbc.gridy+=1;
        gbc.gridwidth=7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mProgress_bar = new JProgressBar(0, 1024*768);
        mProgress_bar.setSize(new Dimension(896,20));
        mProgress_bar.setPreferredSize(new Dimension(896,20));
        p.add(mProgress_bar,gbc);
        mProgress_bar.setVisible(false);
        
        gbc.gridx=7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth=1;
        mCancel_button = new JButton("Cancel");
        p.add(mCancel_button,gbc);
        mCancel_button.setVisible(false);
        mCancel_button.addActionListener(this);
        
        gbc.ipady=(mProgress_bar.getHeight()-mCancel_button.getHeight())/2;
        gbc.gridx=0;
        gbc.gridy+=1;
        gbc.gridwidth=1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mSize_label=new JLabel("Horizontal size", null, JLabel.LEFT);
        p.add(mSize_label,gbc);
       
        gbc.gridx=1;
        gbc.gridwidth=2; 
        gbc.fill = GridBagConstraints.HORIZONTAL;
        DecimalFormat format = new DecimalFormat("#.#####E0");
        mSize = new BigDecimal(1.5);
        mSize_box = new JFormattedTextField(format);
        mSize_box.setPreferredSize(new Dimension(400,20));
        //mSize_box.setAlignmentY(1);
        p.add(mSize_box, gbc);
 
        gbc.gridx=6;
        gbc.gridwidth=2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mIterations_label = new JLabel("Iterations:", null, JLabel.CENTER);
        p.add(mIterations_label,gbc);
        
        gbc.ipady=0;
        gbc.gridx=0;
        gbc.gridy+=1;
        gbc.gridwidth=1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(new JLabel("Real position", null, JLabel.LEFT),gbc);
        
        gbc.gridx=1;
        gbc.gridwidth=7;
        //Format f2 = new BigDecimalFormat();
        InternationalFormatter f2 = new InternationalFormatter();
        f2.setFormat(new BigDecimalFormat());
        f2.setAllowsInvalid(false);
        
        //f2.setMaximumFractionDigits(1000);
        mPos_x = new BigDecimal(-0.5);
        mPos_x_box = new JFormattedTextField(f2);
        mPos_x_box.setPreferredSize(new Dimension(200,20));
        //mPos_x_box.setAlignmentY(1);
        p.add(mPos_x_box, gbc);
        
        gbc.gridx=0;
        gbc.gridy+=1;
        gbc.gridwidth=1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(new JLabel("Imaginary position", null, JLabel.LEFT),gbc);

        gbc.gridx=1;
        gbc.gridwidth=7;
        mPos_y = new BigDecimal(0);
        mPos_y_box = new JFormattedTextField(f2);
        mPos_y_box.setPreferredSize(new Dimension(200,20));
        //mPos_y_box.setAlignmentY(1);
        p.add(mPos_y_box, gbc);   
 
        
        gbc.gridx=0;
        gbc.gridy+=1;
        gbc.gridwidth=1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(new JLabel("Iteration Limit", null, JLabel.LEFT),gbc);

        gbc.gridx=1;
        gbc.gridwidth=4;
        NumberFormat iformat = NumberFormat.getInstance();// new DecimalFormat("#################");
        mIterations_box = new JFormattedTextField(iformat);
        mIterations_box.setPreferredSize(new Dimension(400,20));
        p.add(mIterations_box, gbc);   
        
        gbc.gridx=5;
        gbc.gridwidth=2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mTime_label =new JLabel("", null, JLabel.CENTER);
        p.add(mTime_label, gbc);   

        mComp.CreateImage();

        
 	   //Menu bar
	    JMenuBar menuBar = new JMenuBar();
	    JMenu menu = new JMenu("SuperFractalThing");
	    JMenu navigate = new JMenu("Controls");
 
	    JMenuItem menuItem = new JMenuItem("Refresh");
        menuItem.addActionListener(this);
        navigate.add(menuItem);
 
	    menuItem = new JMenuItem("Undo");
        menuItem.addActionListener(this);
        navigate.add(menuItem);
        mUndo_item = menuItem;
        mUndo_item.setEnabled(false);
       
	    menuItem = new JMenuItem("Redo");
        menuItem.addActionListener(this);
        navigate.add(menuItem);
        mRedo_item = menuItem;
        mRedo_item.setEnabled(false);
        
        menuItem = new JMenuItem("Reset");
        menuItem.addActionListener(this);
        navigate.add(menuItem);
        
        
        menuItem = new JMenuItem("Open");
        menuItem.addActionListener(this);
        menu.add(menuItem);
        
        menuItem = new JMenuItem("Save");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Export PNG");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Palette");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Options");
        menuItem.addActionListener(this);
        menu.add(menuItem);


        menuItem = new JMenuItem("About");
        menuItem.addActionListener(this);
        menu.add(menuItem);
      
        menuBar.add(menu);
        menuBar.add(navigate);
        
       	mLibrary = new PositionLibrary(menuBar, this);

       	setJMenuBar(menuBar);
	    mMenu_bar = menuBar;
	    
		mDialog = new ExportDialog(mFrame, mComp);
		mPalette_dialog = new PaletteDialog(mFrame, mComp, mPalette);
		mOptions_dialog = new OptionsDialog(mFrame, mComp);

		mComp.SetSuperSampleType(mOptions_dialog.GetSuperSampleType());
		mComp.SetNumThreads(mOptions_dialog.GetNumThreads());

    }
    
    public void StartProcessing()
    {
    	mMenu_bar.getComponent(0).setEnabled(false);
    	mMenu_bar.getComponent(1).setEnabled(false);
       	mMenu_bar.getComponent(2).setEnabled(false);
           	SetProgress(0,1024);
    	mProgress_bar.setVisible(true);
    	mCancel_button.setVisible(true);
        mSize_label.setVisible(false);
        mIterations_label.setVisible(false);
        mSize_box.setVisible(false);
    }
    
    public void EndProcessing()
    {
    	mMenu_bar.getComponent(0).setEnabled(true);
    	mMenu_bar.getComponent(1).setEnabled(true);
    	mMenu_bar.getComponent(2).setEnabled(true);
    	mProgress_bar.setVisible(false);
    	mCancel_button.setVisible(false);
    	mSize_label.setVisible(true);
        mIterations_label.setVisible(true);
        mSize_box.setVisible(true);    	
   }


}


class SftComponent extends Component implements MouseInputListener, Runnable, ActionListener, IPaletteChangeNotify
{
	private static final long serialVersionUID = 0;//get rid of warning
	BufferedImage mImage;
	BigDecimal mPos,mPosi;
	BigDecimal mSize;
	int mMax_iterations;
	int mResolution_x;
	int mResolution_y;
	SFTGui mGui;
	Timer mTimer;
	boolean mProcessing;
	IndexBuffer2D mBuffer;
	IndexBuffer2D mExport_buffer;
	IPalette mPalette;
	CalculationManager mCalculation;
	SuperSampleType mSuper_sample_type;
	int mNum_threads;
	long mStart_time;
	boolean mPressed;
	int mPressed_x;
	int mPressed_y;
	int mSelected_x;
	int mSelected_y;
	int mDragged_size;
	
	public SftComponent(SFTGui aGui)
	{
		mGui = aGui;
	}
	public void CreateImage()
	{
		mResolution_x = 1024;
		mResolution_y = 768;
		mSize = new BigDecimal(3.0);
		mPos = new BigDecimal(-0.75,MathContext.DECIMAL128);
		mPosi= new BigDecimal(0,MathContext.DECIMAL128);
		mMax_iterations = 1024;
		
		mGui.SetCoords(mSize,mPos,mPosi,mMax_iterations);
		
        mImage = new BufferedImage(mResolution_x, mResolution_y, BufferedImage.TYPE_INT_ARGB);
        
        UpdateImage();
	}
	
	void SetPalette( IPalette aPalette)
	{
		mPalette = aPalette;
	}
	
	void SetSuperSampleType(SuperSampleType aType)
	{
		mSuper_sample_type = aType;
	}
	void SetNumThreads(int aNumber)
	{
		mNum_threads = aNumber;
		if (mNum_threads<1)
			mNum_threads=1;
		if (mNum_threads>1024)
			mNum_threads=1024;
	}	
	
	@Override
	public void PaletteChanged()
	{
		mImage = mBuffer.MakeTexture(mPalette, mSuper_sample_type);
		repaint();
	}
	
	public BufferedImage GetImage()
	{
		return mImage;
	}
	
	public void run()
	{
		UpdateImage();
		mProcessing=false;
	}
	
	void Refresh()
	{
		if (mCalculation!=null)
		{
			if (mMax_iterations != mGui.GetIterations())
				mMax_iterations = mGui.GetIterations();
			else
			{
				mMax_iterations= Math.max(mMax_iterations, mCalculation.GetNewLimit());
				mGui.SetIterations(mMax_iterations);
			}
		}
		DoCalculation();
	}
	
	void DoCalculation()
	{
		mBuffer = DoCalculation(mResolution_x, mResolution_y, mSuper_sample_type);
	}

	void ExportCalculation(int aResolution_x, int aResolution_y, SuperSampleType aSuper_sample)
	{
		mExport_buffer = DoCalculation(aResolution_x, aResolution_y, aSuper_sample);
	}
	
	IndexBuffer2D DoCalculation(int aResolution_x, int aResolution_y, SuperSampleType aSuper_sample)
	{
		BigDecimal coords[] = new BigDecimal[2];
		
		mGui.StartProcessing();
		mStart_time = System.currentTimeMillis();
		mGui.SetCalculationTime( -1);
		coords = mGui.GetCoords();
		mMax_iterations = mGui.GetIterations();
		mSize = mGui.GetTheSize();
		mPos = coords[0];
		mPosi = coords[1];	
		int scale = mGui.GetTheSize().scale();
		int precision = mGui.GetTheSize().precision();
		int expo=0;
		precision = scale -precision + 8;
		
		IndexBuffer2D buffer=null;
		
		switch (aSuper_sample)
		{
		case SUPER_SAMPLE_NONE:
			buffer = new IndexBuffer2D(aResolution_x,aResolution_y);
			break;
		case SUPER_SAMPLE_2X:
			buffer = new IndexBuffer2D(aResolution_x+1,aResolution_y*2+1);
			break;
		case SUPER_SAMPLE_4X:
			buffer = new IndexBuffer2D(aResolution_x*2,aResolution_y*2);
			break;
		case SUPER_SAMPLE_4X_9:
			buffer = new IndexBuffer2D(aResolution_x*2+1,aResolution_y*2+1);
			break;
		case SUPER_SAMPLE_9X:
			buffer = new IndexBuffer2D(aResolution_x*3,aResolution_y*3);
			break;
		
		}
	
		CalculationManager calc = new CalculationManager();
		mCalculation = calc;
		
		double size;
		BigDecimal bd280 = new BigDecimal(1e-280);
		if (mSize.compareTo( bd280)<0)
		{
			BigDecimal mod_size = mSize;
			while (mod_size.compareTo( bd280)<0)
			{
				mod_size=mod_size.movePointRight(1);
				expo+=1;
			}
			size = mod_size.doubleValue();
				
		}
		else
		{
			size = mSize.doubleValue();
		}
		
		calc.SetCoordinates(mPos,mPosi,(size/2*mResolution_x)/mResolution_y,expo, new MathContext(precision));
		calc.SetBuffer(buffer, aSuper_sample);
		calc.SetIterationLimit(mMax_iterations);
		calc.SetAccuracy(1);
		calc.ThreadedCalculation(mNum_threads);
		
		if (mTimer==null)
		{
			mTimer = new Timer(100, this);
			mTimer.setInitialDelay(100);
		}
		mTimer.start(); 
		return buffer;
	}
	
	void UpdateImage()
	{ 		
		BigDecimal coords[] = new BigDecimal[2];
		
		coords = mGui.GetCoords();
		mMax_iterations = mGui.GetIterations();
		mSize = mGui.GetTheSize();
		mPos = coords[0];
		mPosi = coords[1];
		
		BigDecimal bigx  = mPos;
		BigDecimal bigy  = mPosi;
		
		int scale = mGui.GetTheSize().scale();
		int precision = mGui.GetTheSize().precision();
		int expo=0;
		
		precision = scale -precision + 8;
		
		mSuper_sample_type = SuperSampleType.SUPER_SAMPLE_2X;
		
		switch (mSuper_sample_type)
		{
		case SUPER_SAMPLE_NONE:
			mBuffer = new IndexBuffer2D(mResolution_x,mResolution_y);
			break;
		case SUPER_SAMPLE_2X:
			mBuffer = new IndexBuffer2D(mResolution_x+1,mResolution_y*2+1);
			break;
		case SUPER_SAMPLE_4X:
			mBuffer = new IndexBuffer2D(mResolution_x*2,mResolution_y*2);
			break;
		case SUPER_SAMPLE_4X_9:
			mBuffer = new IndexBuffer2D(mResolution_x*2+1,mResolution_y*2+1);
			break;
		case SUPER_SAMPLE_9X:
			mBuffer = new IndexBuffer2D(mResolution_x*3,mResolution_y*3);
			break;
		
		}

		double size;
		BigDecimal bd280 = new BigDecimal(1e-280);
		if (mSize.compareTo( bd280)<0)
		{
			BigDecimal mod_size = mSize;
			while (mod_size.compareTo( bd280)<0)
			{
				mod_size=mod_size.movePointRight(1);
				expo+=1;
			}
			size = mod_size.doubleValue();
				
		}
		else
		{
			size = mSize.doubleValue();
		}
		
		CalculationManager calc = new CalculationManager();
		calc.SetCoordinates(bigx,bigy,(size/2*mResolution_x)/mResolution_y,expo, new MathContext(precision));
		calc.SetBuffer(mBuffer, mSuper_sample_type);
		calc.SetIterationLimit(mMax_iterations);
		calc.SetAccuracy(1);
		calc.InitialiseCalculation();
		calc.CalculateSector(0,1,1);
		mGui.AddToUndoBuffer();
		
        mImage = mBuffer.MakeTexture(mPalette, mSuper_sample_type);
	}
	
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        
        g2.drawImage(mImage,0,0,null);
        g2.setColor (Color.gray);
        
        if (mDragged_size>0)
        {
        	int width = mDragged_size;
        	int height = (mDragged_size * 768)/1024;
          	g2.draw3DRect(mPressed_x-width/2, mPressed_y - height/2, width, height,true);
            
        }
    }
    
    public Dimension getPreferredSize(){
        return new Dimension(1024, 768);
    }
    
    
	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub

		if (mCalculation!=null && mCalculation.GetIsProcessing())
			return;
		if (mTimer!=null && mTimer.isRunning())
			return;
		
		int x = arg0.getX()-getX();
		int y = arg0.getY()-getY();
		
		if (arg0.getClickCount()==2)
		{
			mMax_iterations = mGui.GetIterations();
			if (mCalculation!=null)
			{
				mMax_iterations= Math.max(mMax_iterations, mCalculation.GetNewLimit());
				mGui.SetIterations(mMax_iterations);
			}
			
			double x_mul = x*1.0/mResolution_y - mResolution_x * 0.5/mResolution_y;
			double y_mul = (0.5*mResolution_y - y)/mResolution_y;
			
			BigDecimal x_offset = mSize.multiply(new BigDecimal(x_mul));
			BigDecimal y_offset = mSize.multiply(new BigDecimal(y_mul));
			
			int size_scale = mSize.scale();
			if (x_offset.scale() > size_scale+4)
				x_offset = x_offset.setScale( size_scale+4, BigDecimal.ROUND_HALF_DOWN);
			if (y_offset.scale() > size_scale+4)
				y_offset = y_offset.setScale( size_scale+4, BigDecimal.ROUND_HALF_DOWN);
			
			mPos = mPos.add( x_offset );
			mPosi = mPosi.add( y_offset );
			mSize = mSize.multiply(new BigDecimal(0.2));
			
			mPos = mPos.stripTrailingZeros();
			mPosi = mPosi.stripTrailingZeros();
			mSize = mSize.stripTrailingZeros();
			
			//mPos = mPos.add( new BigDecimal((x) * mSize/mResolution_y - mSize*mResolution_x/mResolution_y/2) );
			//mPosi = mPosi.add( new BigDecimal((mResolution_y/2-y) * mSize/mResolution_y));
			//mSize *= 0.2;
			
			//mSize_box.setText(Double.toString(mSize));
			mGui.SetCoords(mSize,mPos,mPosi, mMax_iterations);
			
			mGui.AddToUndoBuffer();
			DoCalculation();
			repaint();
		}
		else
		{
			if (mDragged_size>0)
			{
				int s = mDragged_size/2;
				if (x - mSelected_x <= s && mSelected_x -x <= s)
				{
					s = (mDragged_size*768)/1024/2;
					if (y - mSelected_y < s && mSelected_y-y < s)
					{
						double x_mul = mSelected_x*1.0/mResolution_y - mResolution_x * 0.5/mResolution_y;
						double y_mul = (0.5*mResolution_y - mSelected_y)/mResolution_y;
						
						BigDecimal x_offset = mSize.multiply(new BigDecimal(x_mul));
						BigDecimal y_offset = mSize.multiply(new BigDecimal(y_mul));
						
						int size_scale = mSize.scale();
						if (x_offset.scale() > size_scale+4)
							x_offset = x_offset.setScale( size_scale+4, BigDecimal.ROUND_HALF_DOWN);
						if (y_offset.scale() > size_scale+4)
							y_offset = y_offset.setScale( size_scale+4, BigDecimal.ROUND_HALF_DOWN);

						mPos = mPos.add( x_offset);
						mPosi = mPosi.add( y_offset);
						mSize = mSize.multiply(new BigDecimal(mDragged_size/1024.0));

						mPos = mPos.stripTrailingZeros();
						mPosi = mPosi.stripTrailingZeros();
						mSize = mSize.stripTrailingZeros();
						
						//mPos = mPos.add( new BigDecimal((mSelected_x) * mSize/mResolution_y - mSize*mResolution_x/mResolution_y/2) );
						//mPosi = mPosi.add( new BigDecimal((mResolution_y/2-mSelected_y) * mSize/mResolution_y));
						//mSize *= mDragged_size/1024.0;
						mGui.SetCoords(mSize,mPos,mPosi, mMax_iterations);
						mGui.AddToUndoBuffer();
						DoCalculation();
						repaint();
					}
				}
			}
		}
		if (mDragged_size!=0)
		{
			mDragged_size = 0;
			repaint();
		}
	}
	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mousePressed(MouseEvent arg0)
	{
		mPressed = true;
		mPressed_x = arg0.getX()-getX();
		mPressed_y = arg0.getY()-getY();
		
	}
	@Override
	public void mouseReleased(MouseEvent arg0)
	{
		mPressed = false;		
	}
	@Override
	public void mouseDragged(MouseEvent arg0)
	{
		int x = arg0.getX()-getX();
		int y = arg0.getY()-getY();
		
		mDragged_size =2*Math.abs(x-mPressed_x);
		int ds2 = (Math.abs(y-mPressed_y)*2*1024)/768;
		mDragged_size = Math.max(mDragged_size, ds2);
		repaint();
		mSelected_x = mPressed_x;
		mSelected_y = mPressed_y;
	}
	@Override
	public void mouseMoved(MouseEvent arg0)
	{
		if (mCalculation!=null && mCalculation.GetIsProcessing())
			return;
		
		int x = arg0.getX()-getX();
		int y = mResolution_y - arg0.getY()-getY();
		
		if (x<0 || x>=mResolution_x || y<0 || y>=mResolution_y)
			return;
		
		if (mSuper_sample_type == SuperSampleType.SUPER_SAMPLE_4X)
		{
			x*=2;
			y*=2;
		}
		else if (mSuper_sample_type == SuperSampleType.SUPER_SAMPLE_4X_9)
		{
			x=x*2+1;
			y=y*2+1;
		}
		else if (mSuper_sample_type == SuperSampleType.SUPER_SAMPLE_9X)
		{
			x=x*3+1;
			y=y*3+1;
		}
		
		if (mBuffer!=null && y<mBuffer.GetHeight() && x<mBuffer.GetWidth())
		{
			int index = mBuffer.GetValue(x,y);
			mGui.SetHoverIndex(index);
		}		
	}
	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		if (!mCalculation.GetIsProcessing())
		{
			if (mExport_buffer!=null)
			{
		        BufferedImage image = mExport_buffer.MakeTexture(mPalette, mCalculation.GetSuperSampleType());
		        mExport_buffer = null;
				mGui.ExportImage(image);
			}
			else
			{
		        mImage = mBuffer.MakeTexture(mPalette, mSuper_sample_type);
				repaint();
				//mMax_iterations = mCalculation.GetNewLimit();
			}
			mTimer.stop();
			mGui.EndProcessing();
			mGui.SetCalculationTime( System.currentTimeMillis() - mStart_time);
		}
		else
		{
			if (mExport_buffer!=null)
				mGui.SetProgress(mCalculation.GetProgress(), mExport_buffer.GetWidth()* mExport_buffer.GetHeight());
			else
				mGui.SetProgress(mCalculation.GetProgress(), mBuffer.GetWidth()* mBuffer.GetHeight());
		}
	}
	
	void Cancel()
	{
		if (mCalculation!=null)
		{
			mCalculation.Cancel();
		}
	}
}


class BigDecimalFormatter extends NumberFormatter
{
	private static final long serialVersionUID = 0;

	BigDecimalFormatter()
	{
		//setAllowsInvalid(false);
	}
	
	public Object stringToValue(String text) //throws ParseException
	{
		if("".equals(text.trim()))
		{
		return null;
		}
		char ds = getDefaultLocaleDecimalSeparator();

 
		try
		{
			String val = text;
			if(ds != '.')
			{
				val = val.replace(".", "").replace(ds, '.');
			}
			return new BigDecimal(val);
		} catch(NumberFormatException e)
		{
			return null;
		}
	}
	
	public String valueToString(Object value) //Áthrows ParseException
	{
		if (value!=null)
			return value.toString();
		else
			return null;
	}
	 
	private char getDefaultLocaleDecimalSeparator()
	{
		DecimalFormatSymbols symbols = new DecimalFormat("0").getDecimalFormatSymbols();
		char ds = symbols.getDecimalSeparator();
		return ds;
	}

}


class BigDecimalFormat extends Format
{
	private static final long serialVersionUID = 0;//get rid of warning
	BigDecimal mOld_value;
	String mOld_string;

	String Format(Object number)
	{
		BigDecimal x=(BigDecimal)number;
		return x.toString();
	}
	
	public AttributedCharacterIterator formatToCharacterIterator(Object obj)
	{
		return null;
	}

	public Object 	parseObject(String source)
	{
		mOld_string = null;
		try
		{
			BigDecimal x= new BigDecimal(source);
			mOld_value = x;
			if (source.endsWith(".") || source.contentEquals("-0"))
				mOld_string = source;
			return x;
		}
		catch (NumberFormatException e)
		{
			if (source.length()==0)
			{
				mOld_value=null;
				mOld_string=null;
				return null;
			}
			if (source.equals("-"))
			{
				mOld_string = source;
				mOld_value = new BigDecimal(0);
				return mOld_value;
			}
			return mOld_value;
		}
	}

	@Override
	public StringBuffer format(Object arg0, StringBuffer arg1,
			FieldPosition arg2)
	{
		if (mOld_string!=null && mOld_value==arg0)
		{
			arg1.append(mOld_string);
			return arg1;
		}
		
		BigDecimal x=(BigDecimal)arg0;
		String str = x.toString();
		arg1.append(str);
		return arg1;
	}

	@Override
	public Object parseObject(String arg0, ParsePosition arg1)
	{
		return parseObject(arg0);
	}
}
