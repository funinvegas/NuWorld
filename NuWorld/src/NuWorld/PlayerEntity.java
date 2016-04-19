/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorld;

import com.jme3.bullet.control.AbstractPhysicsControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 *
 * @author funin_000
 */
public class PlayerEntity {
    private Node node;
    private String name;
    private AbstractPhysicsControl control;
    public PlayerEntity(String name) {
        this.name = name;
        node = new Node(name);
    }
    public Node getNode() {
        return node;
    }
    public void addControl(AbstractPhysicsControl control) {
        if (this.control != null) {
            node.removeControl(this.control);
        }
        this.control = control;
        node.addControl(this.control);
    }

    public String getName() {
        return name;
    }
    
    public AbstractPhysicsControl getControl() {
        return control;
    }

    public Vector3f getLocation() {
        return node.getWorldTranslation();
    }
    

    public void removeControl() {
        if (this.control != null) {
            node.removeControl(this.control);
        }
        this.control = null;
    }
}
