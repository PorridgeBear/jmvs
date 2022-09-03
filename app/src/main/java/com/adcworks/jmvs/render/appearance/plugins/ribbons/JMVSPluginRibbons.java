package com.adcworks.jmvs.render.appearance.plugins.ribbons;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import javax.media.j3d.*;
import javax.vecmath.*;

import com.adcworks.jmvs.model.*;
import com.adcworks.jmvs.render.appearance.AppearanceManager;
import com.adcworks.jmvs.render.appearance.JMVSPlugin;
import com.sun.j3d.utils.shader.StringIO;

/**
 * Ribbons Plugin.
 */
public class JMVSPluginRibbons implements JMVSPlugin {

    private static final String PLUGIN_NAME 	= "Ribbons";
    private static final String PLUGIN_AUTHOR 	= "Allistair Crossley, adc works";
    private static final String PLUGIN_VERSION 	= "1.0";
    private static final String PLUGIN_INFO 	= "";
    
    private static final double RIBBON_WIDTH_HELIX 	= 3.0d;
    private static final double RIBBON_WIDTH_SHEET 	= 2.0d;
    private static final double RIBBON_WIDTH_TURN 	= 1.0d;

    private String[] paintModes = {"Chain", "Group", "Structure"};
    private Properties properties;

    private String paintMode = paintModes[0];

    public String getName() {
        return PLUGIN_NAME;
    }    

    public String getAuthor() {
        return PLUGIN_AUTHOR;
    }

    public String getVersion() {
        return PLUGIN_VERSION;
    }

    public String getInformation() {
        return PLUGIN_INFO;
    }

    public void init(Properties properties) {
        this.properties = properties;
    }

    public void setPaintMode(String paintMode) {
        this.paintMode = paintMode;
    }

    public TransformGroup render(Molecule molecule) {

        TransformGroup ribbonsTG = new TransformGroup();
        ribbonsTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        ribbonsTG.setCapability(Group.ALLOW_CHILDREN_READ);

        Vector3d a = new Vector3d();
        Vector3d b = new Vector3d();
        Vector3d c = new Vector3d();
        Vector3d d = new Vector3d();
        Vector3d dPrev = new Vector3d();

        ColoringAttributes ca = new ColoringAttributes();
        ca.setShadeModel(ColoringAttributes.SHADE_GOURAUD);

        PolygonAttributes pa = new PolygonAttributes();
        pa.setCullFace(PolygonAttributes.CULL_NONE);
        pa.setBackFaceNormalFlip(true);

        ShaderAppearance appearance = new ShaderAppearance();
        appearance.setCapability(ShaderAppearance.ALLOW_SHADER_PROGRAM_WRITE);
        appearance.setMaterial(new Material());
        appearance.setPolygonAttributes(pa);
        appearance.setColoringAttributes(ca);

        String vertexProgram = null;
        String fragmentProgram = null;
        Shader[] shaders = new Shader[2];
        String[] attrNames = { "numLights" };

        try {
            vertexProgram = Files.readString(new File("src/main/resources/phong.vert").toPath());
            fragmentProgram = Files.readString(new File("src/main/resources/phong.frag").toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        shaders[0] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL,
                Shader.SHADER_TYPE_VERTEX,
                vertexProgram);
        shaders[1] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL,
                Shader.SHADER_TYPE_FRAGMENT,
                fragmentProgram);

        GLSLShaderProgram gouraudSP = new GLSLShaderProgram();
        gouraudSP.setShaders(shaders);

//        appearance.setShaderProgram(gouraudSP);

        Residue residueThis = null;
        Residue residueNext = null;

        for (Model model : molecule.getModels()) {
            for (Chain chain : model.getChains()) {
                
                // skip chains with heterogen residues/atoms
                if (chain.containsHeterogens()) {
                	continue;
               	}
                
	            Object[] residues = chain.getResidues().toArray();

                ArrayList<SplinePoint3D> guideCoordsN = new ArrayList<>();
	            ArrayList<SplinePoint3D> guideCoordsP = new ArrayList<>();

                //
            	// Algorithm for ribbon models of proteins, Carson & Bugg 1986
            	//
            
	            for (int r = 0; r < residues.length - 1; r++) {
	
	            	// 1. define the peptide plane
	            		            	
	            	residueThis = (Residue) residues[r];            	
	                Atom caThis = residueThis.getAlphaCarbon();
	                Atom oxThis = residueThis.getCarbonylOxygen();
	
	                residueNext = (Residue) residues[r + 1];
	                Atom caNext = residueNext.getAlphaCarbon();
	                Atom oxNext = residueNext.getCarbonylOxygen();

	                // check that the atoms were found
	                if (caThis == null || oxThis == null || 
	                    caNext == null || oxNext == null) {
	                    continue;
	                }
	                
	                a.set(
	                	caNext.getX() - caThis.getX(), 
	                	caNext.getY() - caThis.getY(), 
	                	caNext.getZ() - caThis.getZ()
	               	);
	               	
	                b.set(
	                	oxThis.getX() - caThis.getX(), 
	                	oxThis.getY() - caThis.getY(), 
	                	oxThis.getZ() - caThis.getZ()
	                );
	                
	                // normal to peptide plane pointing away from helix axis
	                c.cross(a, b);
	                
	                // lies parallel to peptide plane and perpendicular to a
	                d.cross(c, a); 
	                
	                c.normalize();
	                d.normalize();
	                
	                // 2. generate guide coordinates
	                
	                Vector3d p = new Vector3d();
	                p.set(
	                	(caThis.getX() + caNext.getX()) / 2.0d, 
	                	(caThis.getY() + caNext.getY()) / 2.0d, 
	                	(caThis.getZ() + caNext.getZ()) / 2.0d
	               	);

	                // translate helices away from axis for more room
	                if (residueThis.isHelixPart()) {
//                        System.out.println(String.format("Scaling helix part %s", residueThis.getId()));
	                    c.scale(RIBBON_WIDTH_HELIX);
	                    p.add(c);
	                }

	                if (residueThis.isSheetPart()) {
//                        System.out.println(String.format("Scaling sheet part %s", residueThis.getId()));
                        if (residueNext.isSheetPart()) {
                            d.scale(RIBBON_WIDTH_SHEET);
                        } else {
                            d.scale(0.1);
                        }

                        // TODO: as we get toward end of the sheet residue sequence, taper via scaling
	                }
	                
	                // handle carbonyl oxygen flip
	                Vector3d d2 = new Vector3d();              
	                if (r > 0 && dPrev.dot(d) < 0) {
	                	d2.set(-d.x, -d.y, -d.z);	                	
	                } else {
	                	d2.set(d.x, d.y, d.z);
	                }
	                dPrev = new Vector3d(d2);
	                
	                // create and store points
	                
	                Vector3d pP = new Vector3d(p);
	                Vector3d pN = new Vector3d(p);
	                
	                pP.add(d2);               
	                pN.sub(d2);

                    Color3f colour = getMaterial(residueThis);
//                    System.out.println(String.format("Residue this: %s", colour));
	                guideCoordsN.add(new SplinePoint3D(pN.x, pN.y, pN.z, colour));
	                guideCoordsP.add(new SplinePoint3D(pP.x, pP.y, pP.z, colour));
	            }
	            
	            // 3. construct ribbon geometry
	            
	            int resolution = guideCoordsN.size() * 20;
	            SplineCurve3D c1 = new SplineCurve3D(guideCoordsN, 3, resolution);
	            SplineCurve3D c3 = new SplineCurve3D(guideCoordsP, 3, resolution);
	            
	            SplinePoint3D[] splineN = c1.getPoints();
	            SplinePoint3D[] splineP = c3.getPoints();

				int coord = -1;
	            QuadArray geometry = new QuadArray(
	                4 * (splineN.length - 1), 
	                GeometryArray.COORDINATES | GeometryArray.NORMALS | GeometryArray.COLOR_3
	            );

	            for (int i = 0; i <  splineN.length - 1; i++) {
	
	                SplinePoint3D spN1 = splineN[i];
	                SplinePoint3D spP1 = splineP[i];
	                SplinePoint3D spN2 = splineN[i + 1];
	                SplinePoint3D spP2 = splineP[i + 1];

	                Point3d v1 = new Point3d(
	                    spN1.x - molecule.midX, 
	                    spN1.y - molecule.midY, 
	                    spN1.z - molecule.midZ
	                );
	                
	                Point3d v2 = new Point3d(
	                    spP1.x - molecule.midX, 
	                    spP1.y - molecule.midY, 
	                    spP1.z - molecule.midZ
	                );
	                
	                Point3d v3 = new Point3d(
	                    spN2.x - molecule.midX, 
	                    spN2.y - molecule.midY, 
	                    spN2.z - molecule.midZ
	                );
	                
	                Point3d v4 = new Point3d(
	                    spP2.x - molecule.midX, 
	                    spP2.y - molecule.midY, 
	                    spP2.z - molecule.midZ
	                );
	                
	                Vector3d vec1 = new Vector3d(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);
	                Vector3d vec2 = new Vector3d(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
	                Vector3d vn = new Vector3d();
	                vn.cross(vec1, vec2);
	                vn.normalize();

	                geometry.setCoordinate(++coord, v1);
	                geometry.setNormal(coord, new Vector3f((float) vn.x,(float) vn.y,(float) vn.z));
                    geometry.setColor(coord, spN1.colour);
	                
	                geometry.setCoordinate(++coord, v3);
	                geometry.setNormal(coord, new Vector3f((float) vn.x,(float) vn.y,(float) vn.z));
                    geometry.setColor(coord, spN1.colour);
	                
	                geometry.setCoordinate(++coord, v4);
	                geometry.setNormal(coord, new Vector3f((float) vn.x,(float) vn.y,(float) vn.z));
                    geometry.setColor(coord, spN1.colour);
	                
	                geometry.setCoordinate(++coord, v2);
	                geometry.setNormal(coord, new Vector3f((float) vn.x,(float) vn.y,(float) vn.z));
                    geometry.setColor(coord, spN1.colour);
	            }
	
		        Shape3D shape = new Shape3D(geometry, appearance);
	            shape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
	            shape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
	            ribbonsTG.addChild(shape);
	        }
            
            return ribbonsTG;
        }

        return null;
    }

    private Color3f getMaterial(Residue residue)
    {
        Chain chain = residue.getChain();

        String colourKey = null;
        if (paintMode.equals("Group")) {
            colourKey = "residue." + residue.getName().toLowerCase();
        }

        String rgbString = properties.getProperty(
                colourKey != null ? colourKey.toLowerCase() : "",
                "255,255,255"
        );

//        System.out.println(String.format("%s %s", colourKey, rgbString));

        if (paintMode.equals("Chain")) {
            Color color = AppearanceManager.getOrAllocateColour(chain.getId());
            rgbString = String.format("%d,%d,%d", color.getRed(), color.getGreen(), color.getBlue());
        } else if (paintMode.equals("Structure"))  {
            if (residue.isHelixPart()) {
                rgbString = "240,0,128";
            } else if (residue.isSheetPart()) {
                rgbString = "255,255,0";
            } else if (residue.isTurnPart()) {
                rgbString = "96,128,255";
            } else {
                rgbString = "255,255,255";
            }
        }

        String[] rgb = rgbString.split(",");
        Color3f colour = new Color3f(
                Float.parseFloat(rgb[0]) / 255.0f,
                Float.parseFloat(rgb[1]) / 255.0f,
                Float.parseFloat(rgb[2]) / 255.0f
        );

        return colour;
    }

    public String[] getPaintModes() {
        return paintModes;
    }      
    
    /**
     * Paint the molecule geometry.
     * @param molecule
     * @param mode
     */
    public void paint(TransformGroup molecule, String mode) {
        
    }
    
    /**
     * Spline 3D Point
     * 
     * @author Ali
     *
     */
    class SplinePoint3D {
        
        public double x, y, z;
        public Color3f colour;

        public SplinePoint3D() {
            x = y = z = 0.0d;
        }

        public SplinePoint3D(double x, double y, double z, Color3f colour) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.colour = colour;
        }        

        public String toString() {
            return "x=" + x + ", y=" + y + ", z=" + z;
        }
    }
    
    /**
     * Spline curve based on Paul Bourke's C code
     * Source at : http://paulbourke.net/geometry/spline/
     *
     * @author  allistairc
     */
    public class SplineCurve3D {
               
        private final SplinePoint3D[] points;
           
        /** 
         * Creates a new spline curve using control points at resolution provided.
         * @param controlPoints
         * @param tension
         * @param resolution
         */
        public SplineCurve3D(List<SplinePoint3D> controlPoints, int tension, int resolution) {

            // System.out.println(String.format("Controls: %d, Resolution: %d", controlPoints.size(), resolution));

            int n = controlPoints.size() - 1;
        	int[] knots = new int[n + tension + 1];
            generateKnots(knots, n, tension);

            points = new SplinePoint3D[resolution];
            generateCurve(controlPoints.toArray(new SplinePoint3D[0]), n, knots, tension, resolution);

            // Assign colours from control points  225
            for (int i = 0; i < points.length; i++) {
                float percent = (float) i / points.length * 100;
                int colourIndex = (int) (percent / 100 * controlPoints.size());
//                System.out.println(String.format("i: %d, percent: %f, colourIndex: %d", i, percent, colourIndex));
                SplinePoint3D p = points[i];
//                System.out.println(String.format("Point colour at %d: %s", colourIndex, p.colour));
                p.colour = controlPoints.get(colourIndex).colour;
            }
        }      

        /**
         * The positions of the subintervals of v and breakpoints, the position
         * on the curve are called knots. Breakpoints can be uniformly defined
         * by setting u[j] = j, a more useful series of breakpoints are defined
         * by the function below. This set of breakpoints localises changes to
         * the vicinity of the control point being modified.
         */
        private void generateKnots(int[] u, int n, int t) {

        	int j;

            for (j = 0; j <= u.length - 1; j++) {
                if (j < t) {
                    u[j] = 0;
                } else if (j <= n) {
                    u[j] = j - t + 1;
                } else if (j > n) {
                    u[j] = n - t + 2;	
                }
            }
        }    
        
        /**
         * Create all the points along a spline curve
         * 
         * @param  controlPoints       the control points (n of them)
         * @param  knots               the knots (of degree t)
         * @return splineCurvePoints   the 3d points (r of them)
         */
        private void generateCurve(SplinePoint3D[] controlPoints, int n, int[] knots, int t, int r) {

        	int i;
            double interval, increment;

            interval = 0;
            increment = (n - t + 2) / (double)(r - 1);
            for (i = 0; i < r - 1; i++) {
                points[i] = getSplinePoint(knots, n, t, interval, controlPoints);
                interval += increment;
            }

            points[r - 1] = controlPoints[n];
        }    
        
        /**
         * Creates a point on the curve
         *
         * @param v   the position (range 0 to n - t + 2)
         * @return p  the 3d point
         */
        private SplinePoint3D getSplinePoint(int[] u, int n, int t, double v, SplinePoint3D[] controlPoints) {
        	
        	int k;
            double b;
            SplinePoint3D p = new SplinePoint3D();

            for (k = 0; k <= n; k++) {
                b = splineBlend(k, t, u, v);
                p.x += controlPoints[k].x * b;
                p.y += controlPoints[k].y * b;
                p.z += controlPoints[k].z * b;
            }
            
            return p;
        }

        /**
         * Recursively calculate the blending value
         * If the numerator and denominator are 0 the expression is 0.
         * If the deonimator is 0 the expression is 0
         */
        private double splineBlend(int k, int t, int[] u, double v) {
        	
            double value;

            if (t == 1) {
                if ((u[k] <= v) && (v < u[k + 1])) {
                    value = 1;
                } else {
                    value = 0;
                } 
            } else {
              
                if ((u[k + t - 1] == u[k]) && (u[k + t] == u[k + 1])) {
                    value = 0;
                } else if (u[k + t - 1] == u[k]) {
                    value = (u[k + t] - v) / (u[k + t] - u[k + 1]) * splineBlend(k + 1, t - 1, u, v);
                } else if (u[k + t] == u[k + 1]) {
                    value = (v - u[k]) / (u[k + t - 1] - u[k]) * splineBlend(k, t - 1, u, v);
                } else {
                    value = (v - u[k]) / (u[k + t - 1] - u[k]) * splineBlend(k, t - 1, u, v) + 
                        (u[k + t] - v) / (u[k + t] - u[k + 1]) * splineBlend(k + 1, t - 1, u, v);
                }
           }
           
            return(value);
        }
        
        /**
         * Return the final 3d point set
         */
        public SplinePoint3D[] getPoints() {
            return points;
        }
        
        /**
         * Return a string representation
         */
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            for(int i = 0; i < points.length; i++) {
                buffer.append("POINT "); buffer.append(i);
                if(points[i] != null) {
                    buffer.append(": X="); buffer.append(points[i].x);
                    buffer.append(", Y="); buffer.append(points[i].y);
                    buffer.append(", Z="); buffer.append(points[i].z);
                } else {
                    buffer.append("N/A");
                }
                
                buffer.append("\n");
            }
            
            return buffer.toString();
        }
    }    
}
