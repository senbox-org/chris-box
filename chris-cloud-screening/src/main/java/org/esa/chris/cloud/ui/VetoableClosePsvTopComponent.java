package org.esa.chris.cloud.ui;

import eu.esa.snap.netbeans.tile.TileUtilities;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.UndoRedo;

import java.util.Arrays;

/**
 * @author Marco Peters
 * @since CHRIS-Box 3.0
 */
class VetoableClosePsvTopComponent extends ProductSceneViewTopComponent {
    private static final String TILE_UTILITIES_CLASS_NAME = TileUtilities.class.getCanonicalName();
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
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            boolean isTilingOngoing = Arrays.stream(stackTrace).anyMatch(stackTraceElement -> {
                String className = stackTraceElement.getClassName();
                return TILE_UTILITIES_CLASS_NAME.equals(className);
            });
            if(!isTilingOngoing) {
                final Dialogs.Answer answer = Dialogs.requestDecision("Question",
                                                                      "All windows associated with the cloud labeling dialog will be closed. Do you really want to close the cloud labeling dialog?",
                                                                      false, null);
                return answer == Dialogs.Answer.YES;
            } else {
                return false;
            }
        }

        return true;
    }

}
