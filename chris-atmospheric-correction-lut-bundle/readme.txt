This module provides the lookup table for the CHRIS/Proba atmospheric
correction. In order to deploy this module

1. Place the file 'chrisbox-ac-lut-formatted-1nm.img' into the resource
   directory /src/main/resources/org/esa/chris/ac/lut
    
   This file is available from the ESA STEP server.
   http://step.esa.int/auxdata/chris-box/chrisbox-ac-lut-formatted-1nm.img


2. Run 'mvn deploy' from the module directory:

