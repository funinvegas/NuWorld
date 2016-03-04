/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorld;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.niftygui.NiftyJmeDisplay;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.events.NiftyMousePrimaryClickedEvent;

/**
 *
 * @author funin_000
 */
public class StateStartMenu extends AbstractAppState {
    
    NiftyJmeDisplay niftyDisplay;
    Nifty nifty;
    NuWorldMain app;
   
    @Override
    public void initialize(AppStateManager stateManager, Application ap) {
        this.app = (NuWorldMain)ap;
        this.niftyDisplay = app.getNiftyDisplay();
        this.nifty = this.niftyDisplay.getNifty();
        nifty.gotoScreen("StartScreen");
        app.enableCursor();
        nifty.subscribeAnnotations(this);
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
        nifty.unsubscribeAnnotations(this);
    }

    @NiftyEventSubscriber(id="quitGame") 
    public void onQuitClick(String id, NiftyMousePrimaryClickedEvent event) {
        System.out.println("element with id [" + id + "] clicked at [" + event.getMouseX() +                     ", " + event.getMouseY() + "]"); 
        app.stop();
    }

    @NiftyEventSubscriber(id="newGame") 
    public void onStartClick(String id, NiftyMousePrimaryClickedEvent event) {
        System.out.println("element with id [" + id + "] clicked at [" + event.getMouseX() +                     ", " + event.getMouseY() + "]"); 
        app.startNewGame(this);
    }

    @NiftyEventSubscriber(id="joinGame") 
    public void onJoinClick(String id, NiftyMousePrimaryClickedEvent event) {
        System.out.println("element with id [" + id + "] clicked at [" + event.getMouseX() +                     ", " + event.getMouseY() + "]"); 
        app.joinGame(this);
    }
    
}
