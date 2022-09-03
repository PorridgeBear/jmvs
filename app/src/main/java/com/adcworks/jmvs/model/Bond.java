package com.adcworks.jmvs.model;

/**
 *
 * @author  allistairc
 */
public class Bond {
    
    public int id;
    public Atom src;
    public Atom dst;

    
    public Bond(Atom src, Atom dst) {
        super();
        this.src = src;
        this.dst = dst;
    }

    public Atom getSrc()
    {
        return src;
    }

    public Atom getDst()
    {
        return dst;
    }
}
