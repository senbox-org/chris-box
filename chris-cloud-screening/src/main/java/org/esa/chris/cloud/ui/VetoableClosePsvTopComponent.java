package org.esa.chris.cloud.ui;

import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.UndoRedo;

/**
 * @author Marco Peters
 * @since CHRIS-Box 3.0
 */
class VetoableClosePsvTopComponent extends ProductSceneViewTopComponent {
    private boolean vetoAllowed = true;

    public VetoableClosePsvTopComponent(ProductSceneView view, UndoRedo undoRedo) {
        super(view, undoRedo);
    }

    public boolean isVetoAllowed() {
        return vetoAllowed;
    }

    public void setVetoAllowed(boolean allowVeto) {
        this.vetoAllowed = allowVeto;
    }

    @Override
    public boolean canClose() {
        if(isVetoAllowed()) {
            final Dialogs.Answer answer = Dialogs.requestDecision("Question",
                                                                  "All windows associated with the cloud labeling dialog will be closed. Do you really want to close the cloud labeling dialog?",
                                                                  false, null);
            return answer == Dialogs.Answer.YES;
        }else {
            return true;
        }
    }

}
