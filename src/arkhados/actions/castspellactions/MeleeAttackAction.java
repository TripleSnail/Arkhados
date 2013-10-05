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
package arkhados.actions.castspellactions;

import arkhados.actions.EntityAction;
import arkhados.controls.CharacterPhysicsControl;
import arkhados.controls.DebugControl;
import arkhados.controls.InfluenceInterfaceControl;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.Arrow;
import java.util.List;

/**
 *
 * @author william
 */
public class MeleeAttackAction extends EntityAction {

    // TODO: Make melee attack effect more generic
    private float damage;
    private float range;

    public MeleeAttackAction(float damage, float range) {
        this.damage = damage;
        this.range = range;
    }

    @Override
    public boolean update(float tpf) {
        CharacterPhysicsControl physicsControl = super.spatial.getControl(CharacterPhysicsControl.class);
        Vector3f hitDirection = physicsControl.getTargetLocation().subtract(super.spatial.getLocalTranslation()).normalizeLocal().multLocal(this.range);

        physicsControl.setViewDirection(hitDirection);
        PhysicsSpace space = physicsControl.getPhysicsSpace();
        Vector3f to = super.spatial.getLocalTranslation().add(hitDirection);
        super.spatial.getControl(DebugControl.class).drawArrow(super.spatial.getLocalTranslation(), to);

        List<PhysicsRayTestResult> results = space.rayTest(spatial.getLocalTranslation().setY(3f), to.setY(3f));
        for (PhysicsRayTestResult result : results) {
            PhysicsCollisionObject collisionObject = result.getCollisionObject();
            Object userObject = collisionObject.getUserObject();
            if (!(userObject instanceof Node)) {
                continue;
            }
            Node node = (Node) userObject;
            if (node == super.spatial) {
                continue;
            }
            InfluenceInterfaceControl influenceControl = node.getControl(InfluenceInterfaceControl.class);
            if (influenceControl != null) {
                influenceControl.doDamage(this.damage);
                break;
            }
        }
        return false;
    }
}