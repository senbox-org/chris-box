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
      https://senbox.atlassian.net/browse/CHRIS-20 ✔
    * ArrayIndexOutOfBoundsException occurs when trying to compute probabilistic cloud mask
      https://senbox.atlassian.net/browse/CHRIS-19 ✔
  * atmospheric-correction    
    * Performing AC on NR product lead to exception:  
      https://senbox.atlassian.net/browse/CHRIS-18 ✔
    * Results are a bit different compared to BEAM. 
      Values are still reasonable, but it should be checked why this happens.    
      Check with another product.
  * MP: geometric correction
    * Cannot be performed because of this issue
      https://senbox.atlassian.net/browse/CHRIS-15 ✔
  * SE: TOA Reflectance Computation      
    * Test if this is working ✔
    * https://senbox.atlassian.net/browse/CHRIS-18 ✔
      Masks change after processing TOA Reflectance
  * noise-reduction   
    Noise reduction creates horizontal stripes in radiance_1
    https://senbox.atlassian.net/browse/CHRIS-25✔
  * SE: Feature extraction
    * Test if this is working ✔
  * SE: Add about-box https://senbox.atlassian.net/browse/CHRIS-6
    * About box added ✔
    * Update Image about_chris.png ✔
    * find place for ✔ 
      * http://www.brockmann-consult.de/beam/data/Archive_CHRIS_Tags_12000_38000_Telemetry_Data.zip ✔
          * now located at http://step.esa.int/auxdata/chris-box/Archive_CHRIS_Tags_12000_38000_Telemetry_Data.zip
          * New telemetry archive is http://step.esa.int/auxdata/chris-box/Archive_CHRIS_Telemetry_Data_20201028.zip
      * http://194.78.233.110/products/data/CHRIS_Additional_data/index.html ✔
        * no replacement needed. This will be further used.
      * place chrisbox-ac-lut-formatted-1nm.img somewhere and update location in readme.txt ✔
        * now located at http://step.esa.int/auxdata/chris-box/chrisbox-ac-lut-formatted-1nm.img
  * fix enable state of actions (depending on selected product SE: ✔
  * replace TiePointGeoCoding SE: ✔
   
* check for todos in code

* Migrate wiki pages from the old confluence and update, maybe move some content to the help pages.
* update links in manifest.mf files
* check references in help files (to other help files an external links)
* update copyright year in ops and where else?
* update ReleaseNotes.md

* check for beam occurrences again
* check for visat occurrences again

* Deployment to maven nexus from travis

