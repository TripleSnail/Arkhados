/*    This file is part of Arkhados.

 Arkhados is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Arkhados is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Arkhados.  If not, see <http://www.gnu.org/licenses/>. */
package arkhados.controls;

import arkhados.util.UserDataStrings;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.Queue;

/**
 *
 * @author william
 */
public class CharacterPhysicsControl extends BetterCharacterControl {

    private Vector3f impulseToApply = null;
    private int previousRight = 0;
    private int previousDown = 0;

    public CharacterPhysicsControl(float radius, float height, float mass) {
        super(radius, height, mass);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        super.rigidBody.setUserObject(spatial);
    }

    public void setUpDownDirection(int right, int down) {
        this.saveDirection(right, down);
        if (super.spatial.getControl(InfluenceInterfaceControl.class).canMove()) {
            Vector3f newWalkDirection = new Vector3f(right, 0f, down);
            Float speedMovement = super.spatial.getUserData(UserDataStrings.SPEED_MOVEMENT);
            newWalkDirection.normalizeLocal().multLocal(speedMovement);
            super.setWalkDirection(newWalkDirection);
            if (down != 0 || right != 0) {
                super.spatial.getControl(SpellCastControl.class).safeInterrupt();
                super.setViewDirection(newWalkDirection);
            }
        }
    }

    private void saveDirection(int right, int down) {
        this.previousRight = right;
        this.previousDown = down;
    }

    public void restoreWalking() {
        Vector3f newWalkDirection = new Vector3f(this.previousRight, 0f, this.previousDown);
        Float speedMovement = super.spatial.getUserData(UserDataStrings.SPEED_MOVEMENT);
        newWalkDirection.normalizeLocal().multLocal(speedMovement);
        super.setWalkDirection(newWalkDirection);
    }

    @Override
    public void prePhysicsTick(PhysicsSpace space, float tpf) {
        if (impulseToApply != null) {
            this.rigidBody.applyImpulse(this.impulseToApply.setY(10.0f), Vector3f.ZERO);
            this.impulseToApply = null;
        } else {
            super.prePhysicsTick(space, tpf);
        }
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
    }

    public void applyImpulse(Vector3f impulse) {

        this.impulseToApply = impulse;
    }

    public void setVelocity(Vector3f velocity) {
        this.rigidBody.setLinearVelocity(velocity);
    }
}
