/*
 *	@(#)Sphere.java 1.33 99/01/21 11:22:35
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

import javax.media.j3d.*;
import javax.vecmath.*;

/**
 * Sphere is a geometry primitive created with a given radius and resolution.
 * It is centered at the origin.
 * <p>
 * When a texture is applied to a Sphere, it is mapped CCW from the back
 * of the sphere.
 */

public class Sphere extends Primitive {

  /**
   * Sphere shape identifier, used by <code>getShape</code>.
   *
   * @see Sphere#getShape
   */
  public static final int BODY = 0;

  static final int MID_REZ_DIV = 15;
  float  radius;
  int    divisions;

  /* Added By A D Crossley For USERDATA to be added to all Shape3ds making sphere */
  Object userData = null;

  /**
   *   Constructs a Sphere of a given radius. Normals are generated
   *   by default, texture coordinates are not. The resolution defaults to  
   *   15 divisions along sphere's axes. Appearance defaults to white.
   *   @param radius Radius
   */
  public Sphere (float radius) {
    this(radius,  GENERATE_NORMALS, MID_REZ_DIV);
  }

  /**  
   *   Constructs a default Sphere of radius of 1.0.
   *   Resolution defaults to 15 divisions. Appearance defaults to white.
   */
  public Sphere() {
    this(1.0f, GENERATE_NORMALS, MID_REZ_DIV);
  }

  /**  
   *   Constructs a Sphere of a given radius and appearance.
   *   @param radius Radius
   *   @param appearance Appearance
   */

  public Sphere (float radius, Appearance ap) {
    this(radius, GENERATE_NORMALS, MID_REZ_DIV, ap);
  }

  /**  
   *   Constructs a Sphere of a given radius and appearance with
   *   additional parameters specified by the Primitive flags.
   *   @param radius Radius
   *   @param flags 
   *   @param ap appearance
   */
  public Sphere(float radius, int primflags, Appearance ap) {
    this(radius, primflags, MID_REZ_DIV, ap);
  }

  /**  
   *   Constructs a Sphere of a given radius and number of divisions
   *   with additional parameters specified by the Primitive flags.
   *   Appearance defaults to white.
   *   @param radius Radius
   *   @param divisions Divisions
   *   @param primflags Primflags
   */
  public Sphere(float radius, int primflags, int divisions) {
    this(radius, primflags, divisions, null);
  }

  /* Added By A D Crossley For USERDATA to be added to all Shape3ds making sphere. Makes picking easier*/
  public Sphere(float radius, int primflags, int divisions, Object uData) 
  {
    this(radius, primflags, divisions, null);
    userData = uData;
  }


  /**
   * Obtains Sphere's shape node that contains the geometry.
   * This allows users to modify the appearance or geometry.
   * @param partId The part to return (must be BODY for Spheres)
   * @return The Shape3D object associated with the partId.  If an
   * invalid partId is passed in, null is returned.
   */
  	public Shape3D getShape(int partId) 
	{
    		if (partId != BODY) 
			return null;

		Shape3D s3d = (Shape3D)((Group)getChild(0)).getChild(BODY);
		s3d.setUserData(userData);
		return s3d;
  }

  /** Obtains Sphere's shape node that contains the geometry.
   */
  	public Shape3D getShape() 
	{
		Shape3D s3d = (Shape3D)((Group)getChild(0)).getChild(BODY);
		s3d.setUserData(userData);
		return s3d;
  	}

  /** Sets appearance of the Sphere.
   */
  public void setAppearance(Appearance ap) {
    ((Shape3D)((Group)getChild(0)).getChild(BODY)).setAppearance(ap);
  }


  /**  
   *   Constructs a customized Sphere of a given radius, 
   *   number of divisions, and appearance, with additional parameters
   *   specified by the Primitive flags.  The resolution is defined in
   *   terms of number of subdivisions along the sphere's axes. More
   *   divisions lead to more finely tesselated objects. 
   *   <p>
   *   If the appearance is null, the sphere defaults to a white appearance.
   */
  public Sphere(float radius, int primflags, int divisions, Appearance ap) {

    super();

    double rho, drho, theta, dtheta;
    double vx, vy, vz;
    double s, t, ds, dt;
    int i, j;
    double sign;

    this.radius = radius;
    this.divisions = divisions;

    /* 
     *     The sphere algorithm evaluates spherical angles along regular
     * units. For each spherical coordinate, (theta, rho), a (x,y,z) is
     * evaluated (along with the normals and texture coordinates).
     * 
     *       The spherical angles theta varies from 0 to 2pi and rho from 0
     * to pi. Sample points depends on the number of divisions.
     */
    flags = primflags;
    
    //Depending on whether normal inward bit is set.
    if ((flags & GENERATE_NORMALS_INWARD) != 0)
      sign = -1.0;
    else sign = 1.0;

    // delta theta and delta rho depends on divsions.
    dtheta = 2.0 * Math.PI / (double) divisions;
    drho = Math.PI / (double) divisions;

    t = 0.0;
    ds = 1.0 / divisions;
    dt = 1.0 / divisions;

    GeomBuffer cache = getCachedGeometry(Primitive.SPHERE,
					    radius, 0.0f, 0.0f, 
					    divisions, 0, primflags);
    Shape3D shape;

    if (cache != null) {
      shape = new Shape3D(cache.getComputedGeometry());
      shape.setUserData(userData);

      numVerts += cache.getNumVerts();
      numTris += cache.getNumTris();
    } else {
      // Create a geometry buffer with given number points allocated.    
      GeomBuffer gbuf = new GeomBuffer(divisions*(divisions + 1)* 2);
      
      for (i = divisions - 1; i >= 0; i--) {
	rho = i * drho;
	gbuf.begin(GeomBuffer.QUAD_STRIP);
	s = 0.0;
	for (j = 0 ;j <= divisions; j++) {
	  
	  // Takes care of boundary case.
	  if (j == divisions)	  
	    theta = 0.0;
	  else 
	    theta =((double) j) * dtheta;
	  
	  // First quad vertex.
	  // Evaluate spherical coords to get unit sphere positions. 
	  vx = -Math.sin(theta) * Math.sin(rho);
	  vy = Math.cos(theta) * Math.sin(rho);
	  vz = sign * Math.cos(rho);
	  
	  // Send to buffer.
	  gbuf.normal3d( vx*sign, vy*sign, vz*sign );
	  gbuf.texCoord2d(s, t + dt);
	  gbuf.vertex3d( vx*radius, vy*radius, vz*radius );
	  
	  // Second quad vertex.
	  vx = -Math.sin(theta) * Math.sin(rho+drho);
	  vy = Math.cos(theta) * Math.sin(rho+drho);
	  vz = sign * Math.cos(rho+drho);
	  
	  // Send to Buffer
	  gbuf.normal3d( vx*sign, vy*sign, vz*sign );
	  gbuf.texCoord2d(s, t);
	  gbuf.vertex3d( vx*radius, vy*radius, vz*radius );
	  
	  // increment s
	  s += ds;
	}
	gbuf.end();
	t += dt;
      }

      shape = new Shape3D(gbuf.getGeom(flags));
      shape.setUserData(userData);

      numVerts = gbuf.getNumVerts();
      numTris = gbuf.getNumTris();
      if ((primflags & Primitive.GEOMETRY_NOT_SHARED) == 0) {
	cacheGeometry(Primitive.SPHERE,
		      radius, 0.0f, 0.0f, 
		      divisions, 0, primflags, gbuf);
      }
    }

    if ((flags & ENABLE_APPEARANCE_MODIFY) != 0) {
	shape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
	shape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
    }

    // Rotate it 90 deg to get textures match up.
    Matrix4d rotMat = new Matrix4d();
    Matrix4d objectMat = new Matrix4d();
    rotMat.setIdentity();
    objectMat.setIdentity();
    Transform3D trans = new Transform3D();
    rotMat.rotX(-Math.PI/2.0);
    objectMat.mul(objectMat, rotMat);
    trans.set(objectMat);

    TransformGroup objTrans = new TransformGroup();
    objTrans.setCapability(ALLOW_CHILDREN_READ);
    objTrans.setTransform(trans);
    objTrans.addChild(shape);
    this.addChild(objTrans);
    if (ap == null) {
      setAppearance();
    }
    else setAppearance(ap);

  }

    /**
     * Used to create a new instance of the node.  This routine is called
     * by <code>cloneTree</code> to duplicate the current node.
     * <code>cloneNode</code> should be overridden by any user subclassed
     * objects.  All subclasses must have their <code>cloneNode</code>
     * method consist of the following lines:
     * <P><blockquote><pre>
     *     public Node cloneNode(boolean forceDuplicate) {
     *         UserSubClass usc = new UserSubClass();
     *         usc.duplicateNode(this, forceDuplicate);
     *         return usc;
     *     }
     * </pre></blockquote>
     * @param forceDuplicate when set to <code>true</code>, causes the
     *  <code>duplicateOnCloneTree</code> flag to be ignored.  When
     *  <code>false</code>, the value of each node's
     *  <code>duplicateOnCloneTree</code> variable determines whether
     *  NodeComponent data is duplicated or copied.
     *
     * @see Node#cloneTree
     * @see Node#duplicateNode
     * @see NodeComponent#setDuplicateOnCloneTree
     */
    public Node cloneNode(boolean forceDuplicate) {
        Sphere s = new Sphere(radius, flags, divisions, getAppearance());
        s.duplicateNode(this, forceDuplicate);

        return s;
    }

    /**
     * Copies all node information from <code>originalNode</code> into
     * the current node.  This method is called from the
     * <code>cloneNode</code> method which is, in turn, called by the
     * <code>cloneTree</code> method.
     * <P>
     * For any <i>NodeComponent</i> objects
     * contained by the object being duplicated, each <i>NodeComponent</i>
     * object's <code>duplicateOnCloneTree</code> value is used to determine
     * whether the <i>NodeComponent</i> should be duplicated in the new node
     * or if just a reference to the current node should be placed in the
     * new node.  This flag can be overridden by setting the
     * <code>forceDuplicate</code> parameter in the <code>cloneTree</code>
     * method to <code>true</code>.
     *
     * @param originalNode the original node to duplicate.
     * @param forceDuplicate when set to <code>true</code>, causes the
     *  <code>duplicateOnCloneTree</code> flag to be ignored.  When
     *  <code>false</code>, the value of each node's
     *  <code>duplicateOnCloneTree</code> variable determines whether
     *  NodeComponent data is duplicated or copied.
     *
     * @see Node#cloneTree
     * @see Node#cloneNode
     * @see NodeComponent#setDuplicateOnCloneTree
     */
    public void duplicateNode(Node originalNode, boolean forceDuplicate) {
        super.duplicateNode(originalNode, forceDuplicate);
    }

}
