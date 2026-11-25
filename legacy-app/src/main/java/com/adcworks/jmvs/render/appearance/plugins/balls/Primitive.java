/*
 *	@(#)Primitive.java 1.14 99/01/20 16:25:03
 *
 * Copyright (c) 1996-1998 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

package com.adcworks.jmvs.render.appearance.plugins.balls;

import java.util.*;
import javax.media.j3d.*;
import javax.vecmath.*;

/**
 * Base class for all Java 3D primitives. 
 */

public abstract class Primitive extends Group {
  /**
   * Specifies that normals are generated along with the positions. 
   **/
  public static final int GENERATE_NORMALS =  0x01;

  /**
   * Specifies that texture coordinates are generated along with the
   * positions. 
   **/
  public static final int GENERATE_TEXTURE_COORDS = 0x02;

  /**
   * Specifies that normals are to be flipped along the surface.
   **/
  public static final int GENERATE_NORMALS_INWARD = 0x04;

  /** 
   * Specifies that the geometry being created will not be shared by 
   * another scene graph node. By default all primitives created with
   * the same parameters share their geometry (you have 50 spheres in
   * your scene, but the geometry stored only once). A change to one
   * primitive will effect all shared nodes. 
   * You specify this flag if you do not wish to share any geometry
   * among primitives of the same parameters.
   */
  public static final int GEOMETRY_NOT_SHARED = 0x10;

  /**
   * Specifies that the ALLOW_INTERSECT
   * capability bit should be set on the generated geometry.
   * This allows the object
   * to be picked using Geometry based picking.
   */
  public static final int ENABLE_GEOMETRY_PICKING = 0x20;

  /**
   * Specifies that the ALLOW_APPEARANCE_READ and 
   * ALLOW_APPEARANCE_WRITE bits are to be set on the generated
   * geometry's Shape3D nodes.
   */
  public static final int ENABLE_APPEARANCE_MODIFY = 0x40;

  static final int SPHERE = 0x01;
  static final int CYLINDER = 0x02;
  static final int CONE = 0x04;
  static final int BOX = 0x08;

  int numTris = 0;
  int numVerts = 0;

  /**
   * Primitive flags.
   */
  int flags;

  
  /** Constructs a default primitive.
   */
  public Primitive()
  {
    flags = 0;
    setCapability(ENABLE_PICK_REPORTING);
    setCapability(ALLOW_CHILDREN_READ);
  }
       
  /** Returns total number of triangles in this primitive. 
   */
  public int getNumTriangles()
  { 
    return numTris;
  }

  /** Sets the total number of triangles in this primitive. 
   */
  public void setNumTriangles(int num)
  { 
    numTris = num;
  }

  /** Returns total number of vertices in this primitive. 
   */
  public int getNumVertices()
  { 
    return numVerts;
  }

  /** Sets total number of vertices in this primitive. 
   */
  public void setNumVertices(int num)
  { 
    numVerts = num;
  }

  /** Returns the flags of primitive (generate normal, textures, caching, etc).
   */
  public int getPrimitiveFlags()
  {
    return flags;
  }

  /** Sets the flags of primitive (generate normal, textures, caching, etc).
   */
  public void setPrimitiveFlags(int fl)
  {
    flags = fl;
  }

  /** Obtains a shape node of a subpart of the primitive.
   * @param partid identifier for a given subpart of the primitive.
   */
  public abstract Shape3D getShape(int partid);

  /** Gets the appearance of the primitive (defaults to first subpart).
   */
  public Appearance getAppearance(){
    return getShape(0).getAppearance();
  }

  /** Sets the appearance of a subpart given a partid.
   */

  public void setAppearance(int partid, Appearance ap)
  {
    getShape(partid).setAppearance(ap);
  }

  /** Sets the main appearance of the primitive (all subparts) to 
   *  same appearance.
   */
  public abstract void setAppearance(Appearance ap);

  
  /** Sets the main appearance of the primitive (all subparts) to 
   *  a default white appearance.
   */
  public void setAppearance(){
    Color3f aColor  = new Color3f(0.1f, 0.1f, 0.1f);
    Color3f eColor  = new Color3f(0.0f, 0.0f, 0.0f);
    Color3f dColor  = new Color3f(0.6f, 0.6f, 0.6f);
    Color3f sColor  = new Color3f(1.0f, 1.0f, 1.0f);

    Material m = new Material(aColor, eColor, dColor, sColor, 100.0f);
    Appearance a = new Appearance();
    m.setLightingEnable(true);
    a.setMaterial(m);
    setAppearance(a);
  }

  static Hashtable geomCache = new Hashtable(); 

  String strfloat(float x)
  {
    return (new Float(x)).toString();
  }

  protected void cacheGeometry(int kind, float a, float b, 
			    float c, int d, int e, int flags, 
			    GeomBuffer geo)
  {
    String key = new String(kind+strfloat(a)+strfloat(b)+
			    strfloat(c)+d+e+flags);
    geomCache.put(key, geo);
  }

  protected GeomBuffer getCachedGeometry(int kind, float a, float b, float c,
				      int d, int e, int flags)
  {
    String key = new String(kind+strfloat(a)+strfloat(b)+
			    strfloat(c)+d+e+flags);
    Object cache =  geomCache.get(key);

    return((GeomBuffer) cache);
  }
}

