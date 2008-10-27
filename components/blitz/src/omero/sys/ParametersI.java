/*
 *   $Id$
 *
 *   Copyright 2008 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 *
 */

package omero.sys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import omero.RList;
import omero.RLong;
import omero.RType;
import static omero.rtypes.*;

public class ParametersI extends omero.sys.Parameters {

    public ParametersI() {
        this.map = new HashMap<String, RType>();
    }

    public ParametersI(Map<String, RType> map) {
        this.map = map;
    }

    public ParametersI add(String name, RType r) {
        this.map.put(name, r);
        return this;
    }

    public ParametersI addId(long id) {
        add("id", rlong(id));
        return this;
    }

    public ParametersI addId(RLong id) {
        add("id", id);
        return this;
    }

    public ParametersI addIds(Collection<Long> longs) {
        addLongs("ids", longs);
        return this;
    }

    public ParametersI addLong(String name, long l) {
        add(name, rlong(l));
        return this;
    }

    public ParametersI addLong(String name, RLong l) {
        add(name, l);
        return this;
    }

    public ParametersI addLongs(String name, Collection<Long> longs) {
        RList rlongs = rlist();
        for (Long l : longs) {
            rlongs.add( rlong(l) );
        }
        this.map.put(name, rlongs);
        return this;
    }
}
