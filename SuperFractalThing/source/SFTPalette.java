//	SFTPalette
//	Converts iteration counts into colours
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

import java.awt.Color;

interface IPaletteChangeNotify
{
	void PaletteChanged();
}


public class SFTPalette implements IPalette
{
	int mPalette[];
	IPaletteChangeNotify mNotify;
    
    public static final int NMIXERS = 6;
    public static final int SINE = 0;
    public static final int LINEAR_RAMP = 1;
    public static final int EXP_RAMP = 2;
    public static final int STRIPE = 3;
    public static final int GAUSSIAN = 4;
    public static final int UNDEFINED = 5;
    public static final String[] typeNames = {"Sine", "Linear Ramp", "Exp Ramp", "Stripe", "Gaussian", "Undefined"};
    public static final int colorResolution = 0x10000;
	int mEnd_colour;
    
	int mColour[];
    double minComponentColor[] = {-1, -1, -1};
    double maxComponentColor[] = {1e99, 1e99, 1e99};
    double minMixColor[] = {-1, -1, -1};
    double maxMixColor[] = {1e99, 1e99, 1e99};
    double[][] colorStretch = new double[3][2];
    
    SFTColormap[] cm = new SFTColormap[NMIXERS];
    double[] mixerValues = new double[NMIXERS];
    int currentColormap = 0;
    
	public SFTPalette(IPaletteChangeNotify aNotify)
	{
		mNotify = aNotify;
		
		mPalette = new int[colorResolution];
		      
		mColour = new int[3];
        for (int i=0; i<3; i++) {
            colorStretch[i][0] = 1f;
            colorStretch[i][1] = 0f;
        }
        
        for (int i=0; i<NMIXERS; i++) {
            cm[i] = new SFTColormap();
        }
        mixerValues[0] = 1;
    }
	
	@Override
	public int GetColour(int i)
	{
        double co[] = new double[3];
        boolean useRGB = false;
        
		if (i==0)
			return mEnd_colour;
		
		i &= 0xffff;
		if (mPalette[i]!=0)
			return mPalette[i];
        
        co = getColor(i, true);

        for (int ci=0; ci<3; ci++)
        {
            mColour[ci] = ((int) (co[ci]*255)) & 255;
        }
        
		mPalette[i] = 0xff000000+mColour[2] + ((mColour[1])<<8) + (mColour[0]<<16);
		return mPalette[i];
	}
    
	public double[] getColor(int i, boolean doStretch)
	{
        double co[] = new double[3];
        
        double mixSum = 0;
        for (int m=0; m<NMIXERS; m++)
            mixSum += mixerValues[m];
        
        double fco[][] = new double[NMIXERS][3];
        
        for (int m=0; m<NMIXERS; m++) {
            co = cm[m].getColor(i);
            for (int ci=0; ci<3; ci++)
                fco[m][ci] = co[ci];
        }
        
        for (int m=0; m<NMIXERS; m++) {
            fco[m] = hsl2rgb(fco[m], false);
        }
        
        for (int ci=0; ci<3; ci++)
            co[ci] = 0;
        for (int m=0; m<NMIXERS; m++) {
            for (int ci=0; ci<3; ci++)
                co[ci] += mixerValues[m]*fco[m][ci]/mixSum;
        }
        if (doStretch) {
            // stretch saturation and luminance
            co = rgb2hsl(co, false);
            for (int ci=1; ci<3; ci++)
                co[ci] = colorStretch[ci][0]*co[ci] + colorStretch[ci][1];
            co = hsl2rgb(co, false);
        }

		return co;
	}
    
    public void setColorRanges() {
        double co[] = new double[3];
        for (int p=1; p<3; p++) {
            minComponentColor[p] = 1e99;
            maxComponentColor[p] = -1;
            minMixColor[p] = 1e99;
            maxMixColor[p] = -1;
        }
        
        for (int i=0; i<colorResolution; i+=100) {
            for (int m=0; m<NMIXERS; m++) {
                // components are in hsl space
                co = cm[m].getColor(i);
                // set range
                if (mixerValues[m] > 0) {
                    for (int p=0; p<3; p++) {
                        if (co[p] < minComponentColor[p])
                            minComponentColor[p] = co[p];
                        else if (co[p] > maxComponentColor[p])
                            maxComponentColor[p] = co[p];
                    }
                }
            }
            // mix is in rgb space
            co = getColor(i, false);
            co = rgb2hsl(co, false);
            for (int p=0; p<3; p++) {
                if (co[p] < minMixColor[p])
                    minMixColor[p] = co[p];
                else if (co[p] > maxMixColor[p])
                    maxMixColor[p] = co[p];
            }
        }
        
        // stretch as y = colorStretch[0]*x + colorStretch[1]
        for (int p=0; p<3; p++) {
            if (maxMixColor[p] != minMixColor[p]) {
                colorStretch[p][0] = (maxComponentColor[p] - minComponentColor[p])/(maxMixColor[p] - minMixColor[p]);
                colorStretch[p][1] = minComponentColor[p] - colorStretch[p][0]*minMixColor[p];
            } else {
                colorStretch[p][0] = 1;
                colorStretch[p][1] = 0;
            }
        }
//        System.out.format("Luminance max/min: %f, %f, mix: %f, %f%n", minComponentColor[2], maxComponentColor[2], minMixColor[2], maxMixColor[2]);
//        System.out.format("Luminance color stretch: %f, %f%n", colorStretch[2][0], colorStretch[2][1]);
    }

	
	public int GetAverageColour(int i0, int i1, int i2, int i3)
	{
		int c1,c2,c3,c4,c;
		
		c1 = GetColour(i0);
		c2 = GetColour(i1);
		c3 = GetColour(i2);
		c4 = GetColour(i3);
		
		c = ((c1&0xff)+(c2&0xff)+(c3&0xff)+(c4&0xff)+2)>>2;
		c += (((c1&0xff00)+(c2&0xff00)+(c3&0xff00)+(c4&0xff00)+2)>>2) & 0xff00;
		c += (((c1&0xff0000)+(c2&0xff0000)+(c3&0xff0000)+(c4&0xff0000)+2)>>2) & 0xff0000;
		c += 0xff000000;
		return c;
	}
	public int GetAverageColour(int i0, int i1, int i2, int i3,int i4, int i5, int i6, int i7, int i8)
	{
		int c1,c2,c3,c4,c5,c6,c7,c8,c9,c;
		
		c1 = GetColour(i0);
		c2 = GetColour(i1);
		c3 = GetColour(i2);
		c4 = GetColour(i3);
		c5 = GetColour(i4);
		c6 = GetColour(i5);
		c7 = GetColour(i6);
		c8 = GetColour(i7);
		c9 = GetColour(i8);
		
		c = ((c1&0xff)+(c2&0xff)+(c3&0xff)+(c4&0xff)+(c5&0xff)+(c6&0xff)+(c7&0xff)+(c8&0xff)+(c9&0xff)+4)/9;
		c += (((c1&0xff00)+(c2&0xff00)+(c3&0xff00)+(c4&0xff00)+(c5&0xff00)+(c6&0xff00)+(c7&0xff00)+(c8&0xff00)+(c9&0xff00)+4)/9) & 0xff00;
		c += (((c1&0xff0000)+(c2&0xff0000)+(c3&0xff0000)+(c4&0xff0000)+(c5&0xff0000)+(c6&0xff0000)+(c7&0xff0000)+(c8&0xff0000)+(c9&0xff0000)+4)/9) & 0xff0000;
		c += 0xff000000;
		return c;
	}
    
	public void GetGradientValues(double m[], double pSine[][], Color aColours[])
	{
        cm[currentColormap].getValues(pSine);
        for (int i=0; i<NMIXERS; i++)
            m[i] = mixerValues[i];
	}

	public void SetGradientValues(double m[], double pSine[][], Color aEnd)
	{
        cm[currentColormap].setValues(pSine);
        for (int i=0; i<NMIXERS; i++)
            mixerValues[i] = m[i];
        setColorRanges();
		mPalette = new int[colorResolution];
		
		mNotify.PaletteChanged();
	}
    
	void setCurrentColormap(int colormap)
	{
        currentColormap = colormap;
	}
    
	void setCmapType(int mixer, int hslBaseType)
	{
        cm[mixer].setGlobalType(hslBaseType);
	}
    
	void setCmapType(int mixer, int component, int hslComponentType)
	{
        cm[mixer].setComponentType(component, hslComponentType);
	}
    
	void setCmapType(int hslBaseType[], int hslComponentType[][])
	{
        for (int m=0; m<NMIXERS; m++) {
//            System.out.format("setCmapType mixer %d%n", m);
            cm[m].setGlobalType(hslBaseType[m]);
            for (int i=0; i<3; i++) {
//                System.out.format("setCmapType mixer %d component %d%n", m, i);
                cm[m].setComponentType(i, hslComponentType[m][i]);
            }
        }
	}
    
	void getCmapType(int hslBaseType[], int hslComponentType[][])
	{
        for (int m=0; m<NMIXERS; m++) {
            hslBaseType[m] = cm[m].cmapType;
            for (int i=0; i<3; i++)
                hslComponentType[m][i] = cm[m].getComponentType(i);
        }
	}
    
    public double[]  rgb2hsl( double[] rgb, boolean debug)
    {
        
        int huezone = 0;
        double rr, gg, bb;
        double hp = 0;
        double h, s, l;
        double r = rgb[0];
        double g = rgb[1];
        double b = rgb[2];
        
        double cMax = Math.max(Math.max(r, g), Math.max(g, b));
        double cMin = Math.min(Math.min(r, g), Math.min(g, b));
        double chroma = cMax - cMin;
        
        if (chroma == 0) {
            hp = -1;
        } else if (cMax == r) {
            hp = (g - b)/chroma % 6f;
        } else if (cMax == g) {
            hp = (b - r)/chroma + 2f;
        } else if (cMax == b) {
            hp = (r - g)/chroma + 4f;
        }
        h = hp*60f;
        l = (cMax + cMin)/2f;
        if (chroma == 0)
            s = 0;
        else {
            s = chroma/(1f - Math.abs(2f*l - 1f));
        }
        if (h < 0)
            h = h + 360;
        
        return new double[] { h, s, l };
    }
    
   
    public double[] hsl2rgb( double[] hsl, boolean debug )
    {
        double r = 0;
        double g = 0;
        double b = 0;
        double h = hsl[0];
        double s = hsl[1];
        double l = hsl[2];
        
        double chroma = (1 - Math.abs(2*l - 1))*s;
        double hp = h/60f;
        double x = chroma*(1 - Math.abs(hp % 2f - 1));
        if (debug == true) {
            System.out.format("chroma = %f, hp = %f, x = %f%n", chroma, hp, x);
        }

        if (hp == -1) {
            r = 0; g = 0; b = 0;
        } else if (hp < 1 && hp >= 0) {
            r = chroma; g = x; b = 0;
        } else if (hp < 2) {
            r = x; g = chroma; b = 0;
        } else if (hp < 3) {
            r = 0; g = chroma; b = x;
        } else if (hp < 4) {
            r = 0; g = x; b = chroma;
        } else if (hp < 5) {
            r = x; g = 0; b = chroma;
        } else if (hp < 6) {
            r =chroma; g = 0; b = x;
        }
        double m = l - chroma/2f;
        r = r + m;
        g = g + m;
        b = b + m;
        
        return new double[] { r, g, b };
    }
    
	public String ToString()
	{
		String str="sft_palette\n";
/*
        str += mDr_di1;
		str += ",";
		str += mDr_di2;
		str += ",";
		str += mDecay_r;
		str += "\n";

		str += mDg_di1;
		str += ",";
		str += mDg_di2;
		str += ",";
		str += mDecay_g;
		str += "\n";

		str += mDb_di1;
		str += ",";
		str += mDb_di2;
		str += ",";
		str += mDecay_b;
		str += "\n";

		str += mStart_red;
		str += ",";
		str += mStart_grn;
		str += ",";
		str += mStart_blu;
		str += "\n";
		
		str += (mEnd_colour>>16) & 255;
		str += ",";
		str +=  (mEnd_colour>>8) & 255;
		str += ",";
		str +=  (mEnd_colour>>0) & 255;
		str += "\n";


		for (int i=0; i<NUM_BANDS; i++)
		{
			String s = mBands[i].ToString();
			str += s;
		}
*/
		return str;
	}
	
	public void ParseString(String aString)
	{
		String[] lines = aString.split("\n");
	
		if (!lines[0].contentEquals("sft_palette"))
			return;
/*
		String[] numbers = lines[1].split(",");
		mDr_di1 = Float.parseFloat(numbers[0]);	
		mDr_di2 = Float.parseFloat(numbers[1]);	
		mDecay_r = Float.parseFloat(numbers[2]);	

		numbers = lines[2].split(",");
		mDg_di1 = Float.parseFloat(numbers[0]);	
		mDg_di2 = Float.parseFloat(numbers[1]);	
		mDecay_g = Float.parseFloat(numbers[2]);	
		
		numbers = lines[3].split(",");
		mDb_di1 = Float.parseFloat(numbers[0]);	
		mDb_di2 = Float.parseFloat(numbers[1]);	
		mDecay_b = Float.parseFloat(numbers[2]);	

		numbers = lines[4].split(",");
		mStart_red = Integer.parseInt(numbers[0]);	
		mStart_grn = Integer.parseInt(numbers[1]);	
		mStart_blu = Integer.parseInt(numbers[2]);	
	
		numbers = lines[5].split(",");
		int r = Integer.parseInt(numbers[0]);	
		int g = Integer.parseInt(numbers[1]);	
		int b = Integer.parseInt(numbers[2]);	
		
		mEnd_colour  =(r<<16)+(g<<8)+b + 0xff000000;

		for (int i=0; i<NUM_BANDS; i++)
		{
			mBands[i].ParseString( lines[6+i]);
		}
*/
		mPalette = new int[colorResolution];
		mNotify.PaletteChanged();
	}
}

class SFTColormap
{
    public static final int HUE = 0;
    public static final int SATURATION = 1;
    public static final int LUMINANCE = 2;
    public int cmapType = SFTPalette.SINE;
    
    SFTComponentmap[] HSLComponents = new SFTComponentmap[3];
    
    public SFTColormap() {
        setGlobalType(SFTPalette.SINE);
    }
    
    public double[] getColor(int i) {
        double[] co = new double[3];
        
        for (int ci=0; ci<3; ci++) {
            co[ci] = HSLComponents[ci].getColor(i);
            while (co[ci] > 1)
                co[ci] = co[ci] - 1;
        }
        co[HUE] = co[HUE]*360;
        
        return co;
    }
    
    public void getValues(double v[][]) {
        for (int i=0; i<3; i++)
            HSLComponents[i].getValues(v[i]);
    }
    
    public void setValues(double v[][]) {
        for (int i=0; i<3; i++)
            HSLComponents[i].setValues(v[i]);
    }
    
    public void setGlobalType(int globalType) {
//        System.out.format("setGlobalType, globalType = %d%n", globalType);
        
        if (cmapType != globalType)
            cmapType = globalType;
        if (cmapType != SFTPalette.UNDEFINED)
            for (int i=0; i<3; i++) {
                setComponentType(i, globalType);
            }
//        System.out.format("Global type is now %s%n", SFTPalette.typeNames[cmapType]);
    }
    
    public void setComponentType(int component, int componentType) {
        if (HSLComponents[component] == null || HSLComponents[component].cmapType != componentType) {
            if (HSLComponents[component] != null)
                HSLComponents[component] = null;
            switch (componentType) {
                case SFTPalette.SINE:
                    HSLComponents[component] = new sineSFTComponentmap();
                    break;
                case SFTPalette.LINEAR_RAMP:
                    HSLComponents[component] = new linearRampSFTComponentmap();
                    break;
                case SFTPalette.EXP_RAMP:
                    HSLComponents[component] = new expRampSFTComponentmap();
                    break;
                case SFTPalette.STRIPE:
                    HSLComponents[component] = new stripeSFTComponentmap();
                    break;
                case SFTPalette.GAUSSIAN:
                    HSLComponents[component] = new gaussianSFTComponentmap();
                    break;
            }
        }
//        System.out.format("Component %d is now %s%n", component, HSLComponents[component].type());
    }
    
    public int getComponentType(int component) {
        return HSLComponents[component].cmapType;
    }
}

class SFTComponentmap
{
    double sFreqScale;
    double sFreq;
    double sAmp;
    double sPhase;
    double sOffset;
    double sShape;
    public int cmapType = -1;
    public double colorFrequency = 10000f;

    public SFTComponentmap() {}
    public double getColor(int i) {
        return 0;
    }
    
    public String type() {
        return SFTPalette.typeNames[cmapType];
    }
    
    public void getValues(double v[]) {
        for (int i=0; i<3; i++) {
            v[0] = sFreqScale;
            v[1] = sFreq;
            v[2] = sAmp;
            v[3] = sPhase;
            v[4] = sOffset;
            v[5] = sShape;
        }
    }
    public void setValues(double v[]) {
        for (int i=0; i<3; i++) {
            sFreqScale = v[0];
            sFreq = v[1];
            sAmp = v[2];
            sPhase = v[3];
            sOffset = v[4];
            sShape = v[5];
        }
    }
}

class sineSFTComponentmap extends SFTComponentmap
{

    public sineSFTComponentmap() {
        sFreqScale = 1d;
        sFreq = 25d;
        sAmp = 1d;
        sPhase = 0d;
        sOffset = 0d;
        sShape = 0.5d;
        cmapType = SFTPalette.SINE;
    }
    
    public double getColor(int i) {
        double co;
        if (sFreqScale > 0) {
            double x = i/colorFrequency;
            x = computeShape(Math.PI*(x*sFreq*sFreqScale), sShape);
            co = sOffset + (1-sOffset)*sAmp*(1 + Math.sin(sPhase + x))/2;
        } else {
            co = sOffset + (1-sOffset)*sAmp;
        }
    
        return co;
    }
    
    public double computeShape(double x, double x1) {
        double pi = Math.PI;
        x = x % (2*pi);
        x1 = x1*pi;
        
        if (x >= 0 && x < x1)
            return x*pi/(2*x1);
        else if (x >= x1 && x < pi)
            return pi*(x - x1)/(2*(pi - x1)) + pi/2;
        else if (x >= pi && x < 2*pi - x1)
            return pi*(x + x1 - 2*pi)/(2*(pi - x1)) + 3*pi/2;
        else
            return pi*(x + x1 - 2*pi)/(2*x1) + 3*pi/2;
    }
}

class linearRampSFTComponentmap extends SFTComponentmap
{
    public linearRampSFTComponentmap() {
        sFreqScale = 1d;
        sFreq = 0.5d;
        sAmp = 1d;
        sPhase = 0d;
        sOffset = 0d;
        sShape = 1d;
        cmapType = SFTPalette.LINEAR_RAMP;
    }
    
    public double getColor(int i) {
        double co = 0;
        if (sFreqScale > 0) {
            double x = i/colorFrequency;
            double dx = ((sPhase + x*sFreq)*sFreqScale)%(2*Math.PI)/(2*Math.PI);
            double rShape = 2*sShape - 1;
            
            if (rShape >= 0 && dx < rShape)
                co = sAmp*(1-dx/rShape);
            else if (rShape < 0 && dx > 1 - Math.abs(rShape)) {
                double ars = Math.abs(rShape);
                co = sAmp*(1 - 1/ars + dx/ars);
            }
            else
                co = 0;
            
            //                if (co > 1 || co < 0)
            //                    System.out.format("co = %f, dx = %f, sAmp = %f, rShape = %f%n", co, dx, sAmp, rShape);
            co = sOffset + (1-sOffset)*co;
        } else {
            co = sOffset + (1-sOffset)*sAmp;
        }
        
        return co;
    }
}

class expRampSFTComponentmap extends SFTComponentmap
{
    public expRampSFTComponentmap() {
        sFreqScale = 1d;
        sFreq = 0.5d;
        sAmp = 1d;
        sPhase = 0d;
        sOffset = 0d;
        sShape = 1d;
        cmapType = SFTPalette.EXP_RAMP;
    }
    
    public double getColor(int i) {
        double co = 0;
        if (sFreqScale > 0) {
            double x = i/colorFrequency;
            double dx = ((sPhase + x*sFreq)*sFreqScale)%(2*Math.PI)/(2*Math.PI);
            double rShape = 2*sShape - 1;
            
            if (rShape >= 0)
                co = sAmp*Math.exp(-dx/rShape);
            else if (rShape < 0 ) {
                double ars = Math.abs(rShape);
                co = sAmp*Math.exp(-(1-dx)/ars);
            }
            else
                co = 0;
            
            //                if (co > 1 || co < 0)
            //                    System.out.format("co = %f, dx = %f, sAmp = %f, rShape = %f%n", co, dx, sAmp, rShape);
            co = sOffset + (1-sOffset)*co;
        } else {
            co = sOffset + (1-sOffset)*sAmp;
        }
        
        return co;
    }
}

class stripeSFTComponentmap extends SFTComponentmap
{
    public stripeSFTComponentmap() {
        sFreqScale = 1d;
        sFreq = 0.5d;
        sAmp = 1d;
        sPhase = 0d;
        sOffset = 0d;
        sShape = 1d;
        cmapType = SFTPalette.STRIPE;
    }
    
    public double getColor(int i) {
        double co = 0;
        if (sFreqScale > 0) {
            double x = i/colorFrequency;
            double dx = ((sPhase + x*sFreq)*sFreqScale)%(2*Math.PI)/(2*Math.PI);
            double rShape = 2*sShape - 1;
            double ars = Math.abs(rShape);
            double adx = Math.abs(dx);
            
            if (rShape >= 0)
                if (adx < ars)
                    co = sAmp;
                else
                    co = 0;
            else if (rShape < 0 ) {
                if (adx > ars)
                    co = sAmp;
                else
                    co = 0;
            }
            else
                co = 0;
            
            //                if (co > 1 || co < 0)
            //                    System.out.format("co = %f, dx = %f, sAmp = %f, rShape = %f%n", co, dx, sAmp, rShape);
            co = sOffset + (1-sOffset)*co;
        } else {
            co = sOffset + (1-sOffset)*sAmp;
        }
        
        return co;
    }
}


class gaussianSFTComponentmap extends SFTComponentmap
{
    public gaussianSFTComponentmap() {
        sFreqScale = 1d;
        sFreq = 0.5d;
        sAmp = 1d;
        sPhase = 0d;
        sOffset = 0d;
        sShape = 1d;
        cmapType = SFTPalette.GAUSSIAN;
    }
    
    public double getColor(int i) {
        double co = 0;
        if (sFreqScale > 0) {
            double x = i/colorFrequency;
            double dx = ((sPhase + x*sFreq*sFreqScale))%(2*Math.PI)/(2*Math.PI);
            double rShape = 2*sShape - 1;
            double ars = 0.3*rShape*rShape;
            double adx = (dx - 0.5)*(dx - 0.5);
            double gt = Math.exp(-adx/ars);
            
            if (rShape >= 0)
                co = sAmp*gt;
            else if (rShape < 0 ) {
                co = sAmp*(1-gt);
            }
            else
                co = 0;
            
            //                if (co > 1 || co < 0)
            //                    System.out.format("co = %f, dx = %f, sAmp = %f, rShape = %f%n", co, dx, sAmp, rShape);
            co = sOffset + (1-sOffset)*co;
        } else {
            co = sOffset + (1-sOffset)*sAmp;
        }
        
        return co;
    }
}
