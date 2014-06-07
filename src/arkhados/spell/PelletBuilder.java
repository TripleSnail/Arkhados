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
package arkhados.spell;

import arkhados.CollisionGroups;
import arkhados.WorldManager;
import arkhados.controls.ProjectileControl;
import arkhados.controls.SpellBuffControl;
import arkhados.controls.TimedExistenceControl;
import arkhados.entityevents.RemovalEventAction;
import arkhados.util.NodeBuilder;
import arkhados.util.UserDataStrings;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;


public class PelletBuilder extends NodeBuilder {
    private float damage;

    public PelletBuilder(float damage) {
        this.damage = damage;
    }            

    @Override
    public Node build() {
        final Sphere sphere = new Sphere(8, 8, 0.3f);
        
        final Geometry projectileGeom = new Geometry("projectile-geom", sphere);
        final Node node = new Node("projectile");
        node.attachChild(projectileGeom);
        final Material material = new Material(NodeBuilder.assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", ColorRGBA.Yellow);
        node.setMaterial(material);
        node.setUserData(UserDataStrings.SPEED_MOVEMENT, 220f);
        node.setUserData(UserDataStrings.MASS, 0.30f);
        node.setUserData(UserDataStrings.DAMAGE, this.damage);
        node.setUserData(UserDataStrings.IMPULSE_FACTOR, 0f);
        if (NodeBuilder.worldManager.isClient()) {
            // TODO: Enable these later to add removalAction
            //            node.addControl(new EntityEventControl());
            /**
             * Here we specify what happens on client side when pellet is
             * removed. In this case we want explosion effect.
             */
            //            final PelletRemovalAction removalAction = new PelletRemovalAction(assetManager);
            //            removalAction.setSmokeTrail(trail);
            //            node.getControl(EntityEventControl.class).setOnRemoval(removalAction);
        }
        final SphereCollisionShape collisionShape = new SphereCollisionShape(3);
        final RigidBodyControl physicsBody = new RigidBodyControl(collisionShape, (Float) node.getUserData(UserDataStrings.MASS));
        /**
         * We don't want projectiles to collide with each other so we give them
         * their own collision group and prevent them from colliding with that
         * group.
         */
        physicsBody.setCollisionGroup(CollisionGroups.NONE);
        physicsBody.setCollideWithGroups(CollisionGroups.NONE);
        /**
         * Add collision with characters
         */
        final GhostControl collision = new GhostControl(collisionShape);
        collision.setCollisionGroup(CollisionGroups.PROJECTILES);
        collision.setCollideWithGroups(CollisionGroups.CHARACTERS |
                CollisionGroups.WALLS);
        node.addControl(collision);
        node.addControl(physicsBody);
        node.addControl(new ProjectileControl());
        final SpellBuffControl buffControl = new SpellBuffControl();
        node.addControl(buffControl);
        return node;
    }
}
class PelletRemovalAction implements RemovalEventAction {

    private ParticleEmitter whiteTrail;
    private AssetManager assetManager;

    public PelletRemovalAction(AssetManager assetManager) {
        super();
        this.assetManager = assetManager;
    }

    private void leaveSmokeTrail(final Node worldRoot, Vector3f worldTranslation) {
        this.whiteTrail.setParticlesPerSec(0);
        worldRoot.attachChild(this.whiteTrail);
        this.whiteTrail.setLocalTranslation(worldTranslation);
        this.whiteTrail.addControl(new TimedExistenceControl(0.5F));
    }

    private void createSmokePuff(final Node worldRoot, Vector3f worldTranslation) {
        final ParticleEmitter smokePuff = new ParticleEmitter("smoke-puff", ParticleMesh.Type.Triangle, 20);
        Material materialGray = new Material(this.assetManager, "Common/MatDefs/Misc/Particle.j3md");
        materialGray.setTexture("Texture", this.assetManager.loadTexture("Effects/flame.png"));
        smokePuff.setMaterial(materialGray);
        smokePuff.setImagesX(2);
        smokePuff.setImagesY(2);
        smokePuff.setSelectRandomImage(true);
        smokePuff.setStartColor(new ColorRGBA(0.5F, 0.5F, 0.5F, 0.2F));
        smokePuff.setStartColor(new ColorRGBA(0.5F, 0.5F, 0.5F, 0.1F));
        smokePuff.getParticleInfluencer().setInitialVelocity(Vector3f.UNIT_X.mult(5.0F));
        smokePuff.getParticleInfluencer().setVelocityVariation(1.0F);
        smokePuff.setStartSize(2.0F);
        smokePuff.setEndSize(6.0F);
        smokePuff.setGravity(Vector3f.ZERO);
        smokePuff.setLowLife(0.75F);
        smokePuff.setHighLife(1.0F);
        smokePuff.setParticlesPerSec(0);
        smokePuff.setRandomAngle(true);
        smokePuff.setShape(new EmitterSphereShape(Vector3f.ZERO, 4.0F));
        worldRoot.attachChild(smokePuff);
        smokePuff.setLocalTranslation(worldTranslation);
        smokePuff.emitAllParticles();
    }

    public void exec(WorldManager worldManager, String reason) {
        //        this.leaveSmokeTrail(worldManager.getWorldRoot(), worldTranslation);
        //        this.createSmokePuff(worldManager.getWorldRoot(), worldTranslation);
    }

    public void setSmokeTrail(ParticleEmitter smoke) {
        this.whiteTrail = smoke;
    }
}