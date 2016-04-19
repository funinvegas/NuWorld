/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NuWorld;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.util.TempVars;
import java.util.List;

    
/**
 *
 * @author funin_000
 */
public class PlayerController extends BetterCharacterControl implements ActionListener {

    private InputManager inputManager;
    // Track the up/down state of the direction inputs
    private boolean[] arrowKeys = new boolean[4];
    private boolean crouchPressed = false;
    private boolean jumpPressed = false;
    private float blockSize;
    private Camera camera;
    private Node targetNode = null;
    private boolean flying = false;
    private long lastJumpAttemptTime = 0;
    float designatedVelocity;
    float maxFall;
        
    public PlayerController(InputManager inputManager, Camera camera, float blockSize, float radius, float height, float mass) {
        super(radius, height, mass);
        this.inputManager = inputManager;
        registerInputs();
        this.blockSize = blockSize;
        this.camera = camera;
        designatedVelocity = 12 * blockSize;
        maxFall = designatedVelocity * 4;
    }
    public void setTargetNode(Node target) {
        this.targetNode = target;
    }
    private void registerInputs() {
        inputManager.addListener(this, "move_left");
        inputManager.addListener(this, "move_right");
        inputManager.addListener(this, "move_up");
        inputManager.addListener(this, "move_down");
        inputManager.addListener(this, "jump");
        inputManager.addListener(this, "crouch");

    }
    
    @Override
    public void update(float tpf) {
        super.update(tpf);
        float playerMoveSpeed = ((blockSize * 360.5f) * tpf);
        Vector3f camDir = camera.getDirection().mult(playerMoveSpeed);
        Vector3f camLeft = camera.getLeft().mult(playerMoveSpeed);
        walkDirection.set(0, 0, 0);
        if(arrowKeys[0]){ walkDirection.addLocal(camDir); }
        if(arrowKeys[1]){ walkDirection.addLocal(camLeft.negate()); }
        if(arrowKeys[2]){ walkDirection.addLocal(camDir.negate()); }
        if(arrowKeys[3]){ walkDirection.addLocal(camLeft); }
        //walkDirection.setY(0);
        setWalkDirection(walkDirection);
        if (targetNode != null) {
            Vector3f playerLoc = targetNode.getWorldTranslation();
            playerLoc.setY(playerLoc.getY() + blockSize * 1.5f);
            playerLoc = playerLoc.add(camDir.normalize().mult(blockSize * -0.5f));
        }
    } 
    @Override
    protected CollisionShape getShape() {
        //TODO: cleanup size mess..
        //CapsuleCollisionShape capsuleCollisionShape = new CapsuleCollisionShape(getFinalRadius(), (getFinalHeight() - (2 * getFinalRadius())));
        //CapsuleCollisionShape capsuleCollisionShape = new CapsuleCollisionShape(getFinalRadius(), getFinalRadius()/8);
        CylinderCollisionShape cylCollisionShape = new CylinderCollisionShape(new Vector3f(getFinalRadius(), getFinalHeight() / 2 - getFinalRadius() / 4, getFinalRadius()), 1);
        SphereCollisionShape capsuleCollisionShape = new SphereCollisionShape(getFinalRadius());
        CompoundCollisionShape compoundCollisionShape = new CompoundCollisionShape();
        Vector3f addLocation = new Vector3f(0, (getFinalHeight() / 2.0f) + 0.1f, 0);
        compoundCollisionShape.addChildShape(cylCollisionShape, addLocation);
        addLocation = new  Vector3f(0, getFinalRadius(), 0);
        compoundCollisionShape.addChildShape(capsuleCollisionShape, addLocation);
        return compoundCollisionShape;
    }

    @Override
    public void prePhysicsTick(PhysicsSpace space, float tpf) {
        checkOnGround();
        if (wantToUnDuck && checkCanUnDuck()) {
            setHeightPercent(1);
            wantToUnDuck = false;
            ducked = false;
        }
        TempVars vars = TempVars.get();

        Vector3f currentVelocity = vars.vect2.set(velocity);
        boolean velocityModified = false;
        // dampen existing x/z forces
        float existingLeftVelocity = velocity.dot(localLeft);
        float existingForwardVelocity = velocity.dot(localForward);
        float existingVertVelocity = velocity.dot(localUp);
        Vector3f counter = vars.vect1;
        existingLeftVelocity = existingLeftVelocity * physicsDamping;
        existingForwardVelocity = existingForwardVelocity * physicsDamping;
        Vector3f leveledWalkDirection = walkDirection.normalize();
        if (!flying) {
            leveledWalkDirection.y = 0;
            // dampen out small virtical movements, but let larger things like jumping
            // and falling happen as they want.
            if (Math.abs(existingVertVelocity) < FastMath.ZERO_TOLERANCE) {// && existingVertVelocity > maxFall) {
                //existingVertVelocity = 0;
            }
            if (existingVertVelocity < maxFall) {
                //existingVertVelocity = existingVertVelocity + maxFall;             
            }
            existingVertVelocity = 0;
        } else {
            if (jumpPressed) {
                leveledWalkDirection.y = blockSize;
            } else {
                if (crouchPressed) {
                    leveledWalkDirection.y = -blockSize;
                }
            }
            existingVertVelocity = existingVertVelocity * physicsDamping;
        }
        counter.set(-existingLeftVelocity, -existingVertVelocity, -existingForwardVelocity);
        localForwardRotation.multLocal(counter);
        velocity.addLocal(counter);

        //leveledWalkDirection.normalize();
        //float designatedVelocity = leveledWalkDirection.length();
        if (designatedVelocity > 0) {
            Vector3f localWalkDirection = vars.vect1;
            //normalize walkdirection
            localWalkDirection.set(leveledWalkDirection).normalizeLocal();
            //check for the existing velocity in the desired direction
            float existingVelocity = velocity.dot(localWalkDirection);
            //calculate the final velocity in the desired direction
            float finalVelocity = designatedVelocity - existingVelocity;
            //System.out.println("finalVelocity = " + finalVelocity);
            localWalkDirection.multLocal(finalVelocity);
            if (!flying) {
                localWalkDirection.y = 0;
            }
            //add resulting vector to existing velocity
            velocity.addLocal(localWalkDirection);
            //System.out.println("Local WD = " + localWalkDirection.toString());
            //System.out.println("" + designatedVelocity + "  designated Velocity ");
        }
        if(currentVelocity.distance(velocity) > FastMath.ZERO_TOLERANCE) {
            System.out.println("Setting velocity to " + velocity.toString());
            rigidBody.setLinearVelocity(velocity);
        }
        if (jump) {
            //TODO: precalculate jump force
            Vector3f rotatedJumpForce = vars.vect1;
            rotatedJumpForce.set(jumpForce);
            rigidBody.applyImpulse(localForwardRotation.multLocal(rotatedJumpForce), Vector3f.ZERO);
            jump = false;
        }
        if (!flying) {
            rigidBody.setGravity(new Vector3f(0,-19 * blockSize,0));
        } else {
            rigidBody.setGravity(new Vector3f(0,0,0));
        }
        rigidBody.setFriction(0);
        rigidBody.setRestitution(0);
        vars.release();
    }
    
    /**
     * This checks if the character is on the ground by doing a ray test.
     * /
    protected void checkOnGround() {
        TempVars vars = TempVars.get();
        Vector3f location = vars.vect1;
        Vector3f rayVector = vars.vect2;
        float height = getFinalHeight();
        location.set(localUp).multLocal(height).addLocal(this.location);
        rayVector.set(localUp).multLocal(-height - 0.1f).addLocal(location);
        List<PhysicsRayTestResult> results = space.rayTest(location, rayVector);
        vars.release();
        for (PhysicsRayTestResult physicsRayTestResult : results) {
            if (!physicsRayTestResult.getCollisionObject().equals(rigidBody)) {
                onGround = true;
                return;
            }
        }
        onGround = false;
    }*/

    public void onAction(String actionName, boolean isPressed, float tpf) {
        if(actionName.equals("move_up")){
            arrowKeys[0] = isPressed; // TODO Magic number directions
        }
        else if(actionName.equals("move_right")){
            arrowKeys[1] = isPressed;
        }
        else if(actionName.equals("move_left")){
            arrowKeys[3] = isPressed;
        }
        else if(actionName.equals("move_down")){
            arrowKeys[2] = isPressed;
        }
        else if(actionName.equals("crouch")){
            crouchPressed = isPressed;
        }
        else if(actionName.equals("jump")){
            if (isPressed && !jumpPressed) {
                long now = System.currentTimeMillis();
                if( now - lastJumpAttemptTime < 1000) {
                    flying = !flying;
                }
                lastJumpAttemptTime = now;
            }
            if (isPressed) {
                jump();
            }
            jumpPressed = isPressed;
        }

    }
}
