/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.chris.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyPane;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.ui.AppContext;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Cloud screening form.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
class ScreeningForm extends JPanel {

    private final SourceProductSelector sourceProductSelector;
    private final JCheckBox wvCheckBox;
    private final JCheckBox o2CheckBox;

    ScreeningForm(AppContext appContext, final ScreeningFormModel formModel) {
        sourceProductSelector = new SourceProductSelector(appContext);

        // configure product selector
        sourceProductSelector.setProductFilter(new CloudScreeningProductFilter());
        final JComboBox<Object> comboBox = sourceProductSelector.getProductNameComboBox();
        comboBox.setPrototypeDisplayValue("[1] CHRIS_HH_HHHHHH_HHHH_HH_NR");
        final PropertyContainer pc1 = formModel.getProductPropertyContainer();
        final BindingContext bc1 = new BindingContext(pc1);
        bc1.bind("sourceProduct", comboBox);

        // create parameters panel
        final PropertyContainer pc2 = formModel.getParameterPropertyContainer();
        final BindingContext bc2 = new BindingContext(pc2);
        final JPanel panel = new PropertyPane(bc2).createPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Processing Parameters"));

        wvCheckBox = (JCheckBox) bc2.getBinding("useWv").getComponents()[0];
        o2CheckBox = (JCheckBox) bc2.getBinding("useO2").getComponents()[0];

        // disable check boxes when corresponding features are not available
        sourceProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Object selectedValue = event.getSelection().getSelectedValue();
                if (selectedValue instanceof Product) {
                    final boolean available = checkFeatureAvailability((Product) selectedValue);

                    wvCheckBox.setEnabled(available);
                    o2CheckBox.setEnabled(available);
                    if (!available) {
                        wvCheckBox.setSelected(false);
                        o2CheckBox.setSelected(false);
                    }
                }
            }

            private boolean checkFeatureAvailability(Product product) {
                return product.getProductType().matches("CHRIS_M[15].*_NR");
            }
        });

        setLayout(new BorderLayout(4, 4));
        add(sourceProductSelector.createDefaultPanel(), BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
    }

    void prepareHide() {
        sourceProductSelector.releaseProducts();
    }

    void prepareShow() {
        sourceProductSelector.initProducts();
    }

    SourceProductSelector getSourceProductSelector() {
        return sourceProductSelector;
    }

    Product getSourceProduct() {
        return sourceProductSelector.getSelectedProduct();
    }

    void setSourceProduct(Product product) {
        sourceProductSelector.setSelectedProduct(product);
    }

    boolean isWvCheckBoxEnabled() {
        return wvCheckBox.isEnabled();
    }

    boolean isWvCheckBoxSelected() {
        return wvCheckBox.isSelected();
    }

    boolean isO2CheckBoxEnabled() {
        return o2CheckBox.isEnabled();
    }

    boolean isO2CheckBoxSelected() {
        return o2CheckBox.isSelected();
    }
}
