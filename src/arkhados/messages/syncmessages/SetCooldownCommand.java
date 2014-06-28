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
package arkhados.messages.syncmessages;

import arkhados.controls.SpellCastControl;
import arkhados.messages.syncmessages.statedata.StateData;
import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Spatial;

/**
 *
 * @author william
 */
@Serializable
public class SetCooldownCommand extends StateData {
    private short spellId;
    private float cooldown;
    private boolean globalCooldown;

    public SetCooldownCommand() {
    }

    public SetCooldownCommand(int id, int spellId, float cooldown, boolean globalCooldown) {
        super(id);
        this.spellId = (short) spellId;
        this.cooldown = cooldown;
        this.globalCooldown = globalCooldown;
    }

    @Override
    public void applyData(Object target) {
        final Spatial character = (Spatial) target;
        final SpellCastControl castControl = character.getControl(SpellCastControl.class);
        castControl.setCooldown(this.spellId, this.cooldown);
        if (this.globalCooldown) {
            castControl.globalCooldown();
        }
    }
}