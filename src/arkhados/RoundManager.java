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
package arkhados;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.scene.Node;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import arkhados.messages.SetPlayersCharacterMessage;
import arkhados.messages.roundprotocol.ClientWorldCreatedMessage;
import arkhados.messages.roundprotocol.CreateWorldMessage;
import arkhados.messages.roundprotocol.NewRoundMessage;
import arkhados.messages.roundprotocol.PlayerReadyForNewRoundMessage;
import arkhados.messages.roundprotocol.RoundFinishedMessage;
import arkhados.messages.roundprotocol.RoundStartCountdownMessage;
import arkhados.util.PlayerDataStrings;
import arkhados.util.UserDataStrings;

/**
 *
 * @author william
 */
public class RoundManager extends AbstractAppState implements MessageListener {

    private WorldManager worldManager;
    private SyncManager syncManager;
    private AppStateManager stateManager;
    private Application app;
    private ClientMain clientMain = null;
    private ClientHudManager hudManager = null;
    private int currentRound = 0;
    private int rounds = 3;
    private boolean roundRunning = false;
    private float roundStartCountDown = 0.0f;
    private static final Logger logger = Logger.getLogger(RoundManager.class.getName());

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        logger.setLevel(Level.ALL);
        logger.log(Level.INFO, "Initializing RoundManager");
        super.initialize(stateManager, app);
        this.worldManager = stateManager.getState(WorldManager.class);
        this.syncManager = stateManager.getState(SyncManager.class);
        this.stateManager = stateManager;
        this.syncManager.addObject(-1, this.worldManager);
        this.app = app;

        if (this.worldManager.isClient()) {
            this.syncManager.getClient().addMessageListener(this, CreateWorldMessage.class, NewRoundMessage.class, RoundFinishedMessage.class, RoundStartCountdownMessage.class);
            this.clientMain = (ClientMain) app;
            this.hudManager = stateManager.getState(ClientHudManager.class);

        } else if (this.worldManager.isServer()) {
            this.syncManager.getServer().addMessageListener(this, ClientWorldCreatedMessage.class, PlayerReadyForNewRoundMessage.class);
        }
        logger.log(Level.INFO, "Initialized RoundManager");
    }

    public void serverStartGame() {
        logger.log(Level.INFO, "serverStartGame");
        PlayerData.setDataForAll(PlayerDataStrings.WORLD_CREATED, false);
        PlayerData.setDataForAll(PlayerDataStrings.READY_FOR_ROUND, false);

        if (currentRound == 0) {
            this.createWorld();
        }
    }

    private void createWorld() {
        logger.log(Level.INFO, "Creating world");
        ++this.currentRound;
        this.app.enqueue(new Callable<Void>() {
            public Void call() throws Exception {
                if (currentRound > 1) {
                    cleanupPreviousRound();
                }

                logger.log(Level.INFO, "Enabling worldManager");
                worldManager.setEnabled(true);
                logger.log(Level.INFO, "Loading level");
                worldManager.loadLevel();
                logger.log(Level.INFO, "Attaching level");
                worldManager.attachLevel();

                if (worldManager.isClient()) {
                    syncManager.getClient().send(new ClientWorldCreatedMessage());
                }


                if (worldManager.isServer()) {
                    logger.log(Level.INFO, "Broadcasting CreateWorldMessage");
                    syncManager.getServer().broadcast(new CreateWorldMessage());
                    logger.log(Level.INFO, "Enablind syncManager");
                    syncManager.setEnabled(true);
                    syncManager.startListening();
                }
                return null;
            }
        });
    }

    private void createCharacters() {
        logger.log(Level.INFO, "Creating characters");
        if (this.worldManager.isServer()) {
            this.app.enqueue(new Callable<Void>() {
                public Void call() throws Exception {

                    int i = 0;
                    for (PlayerData playerData : PlayerData.getPlayers()) {
                        Vector3f startingLocation = new Vector3f(WorldManager.STARTING_LOCATIONS[i++]);
                        startingLocation.setY(7.0f);
                        final String heroName = playerData.getStringData(PlayerDataStrings.HERO);
                        long entityId = worldManager.addNewEntity(heroName, startingLocation, new Quaternion());
                        playerData.setData(PlayerDataStrings.ENTITY_ID, entityId);
                    }

                    logger.log(Level.INFO, "Created characters");

                    for (PlayerData playerData : PlayerData.getPlayers()) {
                        long entityId = playerData.getLongData(PlayerDataStrings.ENTITY_ID);
                        syncManager.getServer().broadcast(new SetPlayersCharacterMessage(entityId, playerData.getId()));
                    }

                    logger.log(Level.INFO, "Informing players of their characters");
                    return null;
                }
            });
        }

        this.syncManager.getServer().broadcast(new RoundStartCountdownMessage(5));
        this.roundStartCountDown = 5f;
    }

    private void startNewRound() {
        logger.log(Level.INFO, "Starting new round");
        if (this.worldManager.isServer()) {
            this.syncManager.getServer().broadcast(new NewRoundMessage());
        }
        this.roundRunning = true;
        if (this.worldManager.isClient()) {
            this.clientMain.getUserCommandManager().setEnabled(true);
            this.hudManager.startRound();
        }
    }

    private void cleanupPreviousRound() {
        logger.log(Level.INFO, "Cleaning up previous round");
        worldManager.clear();
        this.syncManager.addObject(-1, this.worldManager);
        if (this.worldManager.isClient()) {
            this.stateManager.getState(ClientHudManager.class).clear();
        }
    }

    private void endRound() {
        logger.log(Level.INFO, "Ending round");
        if (this.worldManager.isServer()) {
            this.syncManager.getServer().broadcast(new RoundFinishedMessage());
            PlayerData.setDataForAll(PlayerDataStrings.WORLD_CREATED, false);
            PlayerData.setDataForAll(PlayerDataStrings.READY_FOR_ROUND, false);
            logger.log(Level.INFO, "Disabling syncManager");

            this.syncManager.setEnabled(false);
            this.syncManager.stopListening();
        }
        this.roundRunning = false;
        logger.log(Level.INFO, "Enabling worldManager");

        this.worldManager.setEnabled(false);

        if (this.worldManager.isClient()) {
            this.clientMain.getUserCommandManager().setEnabled(false);

        }

        // TODO: Add wait time so players can watch their stats and get ready

        if (this.worldManager.isServer() && this.currentRound < this.rounds) {
            this.createWorld();
        }
    }

    @Override
    public void update(float tpf) {
        if (this.roundStartCountDown > 0f) {
            this.roundStartCountDown -= tpf;
            if (this.worldManager.isServer()) {
                if (this.roundStartCountDown <= 0f) {
                    this.startNewRound();
                }
            } else if (this.worldManager.isClient()) {
                this.hudManager.setSecondsLeftToStart((int) this.roundStartCountDown);
            }
        }

        if (!this.roundRunning) {
            return;
        }

        if (this.worldManager.isServer()) {
            int aliveAmount = 0;
            for (PlayerData playerData : PlayerData.getPlayers()) {
                long entityId = playerData.getLongData(PlayerDataStrings.ENTITY_ID);
                Node character = (Node) this.worldManager.getEntity(entityId);
                if ((Float) character.getUserData(UserDataStrings.HEALTH_CURRENT) > 0f) {
                    ++aliveAmount;
                    if (aliveAmount > 1) {
                        break;
                    }
                }
            }
            if (aliveAmount == 0) {
                this.endRound();
            }
        }
    }

    private boolean allClientsWorldReady() {
        for (PlayerData playerData : PlayerData.getPlayers()) {
            if (!playerData.getBooleanData(PlayerDataStrings.WORLD_CREATED)) {
                System.out.println("Not all players are ready yet");
                return false;
            }
        }
        logger.log(Level.INFO, "All players have created world");

        return true;
    }

    private boolean allReadyForRound() {
        for (PlayerData playerData : PlayerData.getPlayers()) {
            if (!playerData.getBooleanData(PlayerDataStrings.READY_FOR_ROUND)) {
                System.out.println("Not all players are ready yet");
                return false;
            }
        }
        System.out.println("All players are ready!");
        return true;
    }

    @Override
    public void cleanup() {
        super.cleanup();
    }

    public void messageReceived(Object source, Message m) {
        if (this.worldManager.isClient()) {
            this.clientMessageReceived(source, m);
        } else if (this.worldManager.isServer()) {
            this.serverMessageReceived((HostedConnection) source, m);
        }
    }

    private void clientMessageReceived(Object source, Message m) {
        if (m instanceof CreateWorldMessage) {
            this.createWorld();
        } else if (m instanceof NewRoundMessage) {
            this.startNewRound();
        } else if (m instanceof RoundStartCountdownMessage) {
            RoundStartCountdownMessage message = (RoundStartCountdownMessage) m;
            this.roundStartCountDown = message.getTime();
        } else if (m instanceof RoundFinishedMessage) {
            this.endRound();
        }
    }

    private void serverMessageReceived(HostedConnection client, Message m) {
        logger.log(Level.INFO, "Received {0} -message", new Object[]{m.getClass()});

        if (m instanceof ClientWorldCreatedMessage) {
            long playerId = ServerClientData.getPlayerId(client.getId());
            PlayerData.setData(playerId, PlayerDataStrings.WORLD_CREATED, true);
            if (this.allClientsWorldReady()) {
                this.createCharacters();
            }
//        } else if (m instanceof PlayerReadyForNewRoundMessage) {
//            long playerdId = ServerClientData.getPlayerId(client.getId());
//            PlayerData.setData(playerdId, PlayerDataStrings.ENTITY_ID, true);
//            if (this.allReadyForRound()) {
//                this.startNewRound();
//            }
        }
    }
}
