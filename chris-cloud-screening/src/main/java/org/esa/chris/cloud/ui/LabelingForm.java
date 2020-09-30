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
package org.esa.chris.cloud.ui;

import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.ui.color.ColorTableCellEditor;
import org.esa.snap.ui.color.ColorTableCellRenderer;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.text.NumberFormat;

/**
 * Cloud labeling form.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
class LabelingForm extends JPanel {

    private final JTable table;
    private final JCheckBox checkBox;

    LabelingForm(LabelingFormModel formModel) {
        table = new JTable(formModel.getTableModel());

        table.setDefaultRenderer(Color.class, createColorRenderer());
        table.setDefaultEditor(Color.class, createColorEditor());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.getColumnModel().getColumn(LabelingFormModel.TableModel.BRIGHTNESS_COLUMN).setCellRenderer(
                createBrightnessRenderer());
        table.getColumnModel().getColumn(LabelingFormModel.TableModel.OCCURRENCE_COLUMN).setCellRenderer(
                createOccurrenceRenderer());

        final JScrollPane tablePane = new JScrollPane(table);
        tablePane.getViewport().setPreferredSize(table.getPreferredSize());
        final JPanel tablePanel = new JPanel(new BorderLayout(2, 2));
        tablePanel.add(tablePane, BorderLayout.CENTER);
        tablePanel.setBorder(BorderFactory.createTitledBorder("Cloud Labeling"));

        checkBox = new JCheckBox("Calculate probabilistic cloud mask", false);
        checkBox.setToolTipText("If selected, a probabilistic cloud mask is calculated");

        final BindingContext bc = new BindingContext(formModel.getPropertyContainer());
        bc.bind("probabilistic", checkBox);
        bc.bindEnabledState("probabilistic", true, "probabilisticEnabled", true);

        final JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkBoxPanel.add(checkBox);
        checkBoxPanel.setBorder(BorderFactory.createTitledBorder("Processing Parameters"));

        setLayout(new BorderLayout(4, 4));
        add(tablePanel, BorderLayout.CENTER);
        add(checkBoxPanel, BorderLayout.SOUTH);
    }

    void prepareHide() {
        // nothing to do
    }

    void prepareShow() {
        // nothing to do
    }

    JTable getTable() {
        return table;
    }

    JCheckBox getCheckBox() {
        return checkBox;
    }

    private static TableCellEditor createColorEditor() {
        return new ColorTableCellEditor();
    }

    private static TableCellRenderer createColorRenderer() {
        return new ColorTableCellRenderer();
    }

    private static TableCellRenderer createBrightnessRenderer() {
        final NumberFormat numberFormat = NumberFormat.getInstance();

        numberFormat.setMinimumIntegerDigits(1);
        numberFormat.setMaximumIntegerDigits(1);
        numberFormat.setMinimumFractionDigits(3);
        numberFormat.setMaximumFractionDigits(3);

        return new FormattedNumberRenderer(numberFormat);
    }

    private static TableCellRenderer createOccurrenceRenderer() {
        final NumberFormat numberFormat = NumberFormat.getPercentInstance();

        numberFormat.setMinimumFractionDigits(1);
        numberFormat.setMaximumFractionDigits(3);

        return new FormattedNumberRenderer(numberFormat);
    }
}
