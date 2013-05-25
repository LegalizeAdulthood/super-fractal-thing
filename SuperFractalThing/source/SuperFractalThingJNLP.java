import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.jnlp.FileContents;
import javax.jnlp.FileOpenService;
import javax.jnlp.FileSaveService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;


public class SuperFractalThingJNLP extends SuperFractalThing
{
	private static final long serialVersionUID = 1;//get rid of warning

	public SuperFractalThingJNLP()
	{
		// TODO Auto-generated constructor stub
	}
	public static void main(String[] args)
	{
		System.out.println("JNLP");
		
		SuperFractalThing ap = new SuperFractalThingJNLP();
	    ap.start();
	}
	
	@Override
	void OpenFile()
	{
		FileOpenService fos; 
	
	    try
	    { 
	        fos = (FileOpenService)ServiceManager.lookup("javax.jnlp.FileOpenService"); 
	    }
	    catch (UnavailableServiceException e)
	    { 
	    	super.OpenFile();
		    return;
	    }
	    
	    if (fos != null)
	    { 
	        try
	        { 
	            // ask user to select a file through this service 
	            FileContents fc = fos.openFileDialog(null, null); 
	            // ask user to select multiple files through this service 
	            //FileContents[] fcs = fos.openMultiFileDialog(null, null); 
	            
	            InputStream is = fc.getInputStream();
	            BufferedReader br = new BufferedReader( new InputStreamReader(is));		
	            LoadTheFile(br);
	        }
	        catch (Exception e)
	        { 
	            e.printStackTrace(); 
	        } 
	    }        
    } 	
	
	@Override
	void SaveFile(String str)
	{
		FileSaveService fss; 
	    try
	    { 
	        fss = (FileSaveService)ServiceManager.lookup("javax.jnlp.FileSaveService"); 
	    }
	    catch (UnavailableServiceException e)
	    { 
	        fss = null; 
	    } 
	    
	    if (fss!=null)
	    {
			
			ByteArrayInputStream bis;
			try
			{
				bis = new ByteArrayInputStream(str.getBytes("UTF-8"));
			}
			catch (UnsupportedEncodingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			
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
	    	super.SaveFile(str);
	    }
	}

}
