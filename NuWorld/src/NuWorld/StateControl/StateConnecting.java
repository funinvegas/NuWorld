/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorld.StateControl;

import NuWorld.NuWorldMain;
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.niftygui.NiftyJmeDisplay;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.events.NiftyMousePrimaryClickedEvent;

/**
 *
 * @author funin_000
 */
public class StateConnecting extends AbstractAppState {
      
    NiftyJmeDisplay niftyDisplay;
    Nifty nifty;
    NuWorldMain app;
   
    @Override
    public void initialize(AppStateManager stateManager, Application ap) {
        this.app = (NuWorldMain)ap;
        this.niftyDisplay = app.getNiftyDisplay();
        this.nifty = this.niftyDisplay.getNifty();
        nifty.gotoScreen("connectMenu");
        app.enableCursor();
        nifty.subscribeAnnotations(this);
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
        nifty.unsubscribeAnnotations(this);
    }

    @NiftyEventSubscriber(id="cancelButton") 
    public void onCancelClick(String id, NiftyMousePrimaryClickedEvent event) {
        System.out.println("element with id [" + id + "] clicked at [" + event.getMouseX() + ", " + event.getMouseY() + "]"); 
        app.stop();
    }

    @NiftyEventSubscriber(id="connectButton") 
    public void onConnectClick(String id, NiftyMousePrimaryClickedEvent event) {
        System.out.println("element with id [" + id + "] clicked at [" + event.getMouseX() + ", " + event.getMouseY() + "]"); 
        String serverIP = nifty.getScreen("connectMenu").findNiftyControl("serverIP", TextField.class).getRealText();
        app.setServerConnection(serverIP);
        app.startRunning(this);
    }

}
