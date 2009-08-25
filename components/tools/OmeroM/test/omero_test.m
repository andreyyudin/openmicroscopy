%  Simple OMERO.matlab test driver
%  Copyright (c) 2009, Glencoe Software, Inc.
%  See LICENSE for details.

omero_home = getenv('OMERO_HOME');
cd(omero_home)

[c,s,g] = loadOmero;
disp(s.getConfigService().getVersion());
c.closeSession();
clear g;
clear s;
clear c;
unloadOmero;
