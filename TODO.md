# TODOs

* Setup issue tracker - github or jira, depending on ESA answer
* Migrate issues from old jira and add new migration issues
* Setup wiki - github or confluence, depending on ESA answer
* Migrate modules
  * chris-bootstrap - turn into chris-kit ✔   
  * chris-reader ✔       
  * chris-util ✔              
  * chris-atmospheric-correction-lut-bundle ✔
  * chris-atmospheric-correction-lut ✔
  * chris-atmospheric-correction ✔
  * chris-toa-reflectance-computation ✔
  * chris-cloud-screening ✔   
  * chris-geometric-correction ✔
  * chris-noise-reduction ✔   
* check if all packages have been renamed ✔
  * org.esa.beam -> org.esa.chris ✔
* check for beam occurrences
* update @since apidoc
* check for visat occurrences
* GUI stuff
  * migrate module.xml and actions ~ 
  * fix createInternalFrame, disposeInternalFrame in LabelingDialog
  * use Bundle strings
* Maybe provide a NoiseReduction Operator which combine the three steps, or document it 
  and provide a graph. Same for cloud screening? 
* move cris reader help page from snap-help ✔ 
    Check if help is working
* find place for 
  * http://www.brockmann-consult.de/beam/data/Archive_CHRIS_Tags_12000_38000_Telemetry_Data.zip
  * http://194.78.233.110/products/data/CHRIS_Additional_data/index.html
* Migrate wiki pages from the old confluence and update, maybe move some content to the help pages.
* update links in manifest.mf files
* place chrisbox-ac-lut-formatted-1nm.img somewhere and update location in readme.txt
* remove from apidoc: @version $Revision:
* Add Travis yaml file ✔
* Deploy to maven nexus from travis
* replace TiePointGeoCoding
* check for todos in code
