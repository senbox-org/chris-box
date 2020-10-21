# TODOs

* Setup issue tracker - github or jira, depending on ESA answer ✔
* Setup wiki - github or confluence, depending on ESA answer ✔
* Add Travis yaml file ✔
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
* check for beam occurrences ✔
* update @since apidoc ✔
* check for visat occurrences ✔
* remove from apidoc: @version $Revision: ✔
* GUI stuff
  * migrate module.xml and actions ✔ 
  * use Bundle strings ✔
* move chris reader help page from snap-help ✔ 
* Check if help is working ✔
* Migrate issues from old jira and add new migration issues ✔

* Check results are the same as in BEAM
  * Vertical striping correction ✔
  * noise-reduction ✔   
  * cloud screening
    * results are the same ✔
    * RGB image and classified image are not opened for labeling ✔
    * User is not warned if she tries to close RGB or classification image✔
    * Opened view are not closed ✔
    * final cloud_product image is not opened ✔    
    * storing cloud_product does not work with "Save As...". 
      https://senbox.atlassian.net/browse/CHRIS-17 ✔
    * Cloud cluster/classes image is initially not correctly zoomed to full size
      https://senbox.atlassian.net/browse/CHRIS-21 ✔
    * Cloud cluster/classes view not properly updated
      https://senbox.atlassian.net/browse/CHRIS-20
    * ArrayIndexOutOfBoundsException occurs when trying to compute probabilistic cloud mask
      https://senbox.atlassian.net/browse/CHRIS-19
  * atmospheric-correction    
    * Performing AC on NR product lead to exception:  
      https://senbox.atlassian.net/browse/CHRIS-18
    

* https://senbox.atlassian.net/browse/CHRIS-17
* https://senbox.atlassian.net/browse/CHRIS-18
* check for todos in code
* fix enable state of actions (depending on selected product)
* GUI stuff
  * fix createInternalFrame, disposeInternalFrame in LabelingDialog
* Maybe provide a NoiseReduction Operator which combine the three steps, or document it 
  and provide a graph. Same for cloud screening? 
* replace TiePointGeoCoding

* find place for 
  * http://www.brockmann-consult.de/beam/data/Archive_CHRIS_Tags_12000_38000_Telemetry_Data.zip
  * http://194.78.233.110/products/data/CHRIS_Additional_data/index.html
* Migrate wiki pages from the old confluence and update, maybe move some content to the help pages.
* update links in manifest.mf files
* check references in help files (to other help files an external links)
* place chrisbox-ac-lut-formatted-1nm.img somewhere and update location in readme.txt
* update copyright year in ops and where else?

* Deploy to maven nexus from travis

* check for beam occurrences again
* check for visat occurrences again

