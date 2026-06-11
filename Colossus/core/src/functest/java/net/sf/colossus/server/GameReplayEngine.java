package net.sf.colossus.server;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.TestConstants;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.game.Dice;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.Variant;


/**
 * Replay engine for Colossus game recordings.
 * 
 * Loads a JSON recording, sets the random seed for deterministic replay,
 * creates a game with the same players/powers, and runs it to completion.
 * The recording events serve as ground truth for assertions.
 * 
 * Usage:
 *   GameReplayEngine engine = GameReplayEngine.load("path/to/recording.json");
 *   engine.runToCompletion();  // deterministic replay via seed
 *   assertEquals(5, engine.getLegionHeight("Rd04"));
 */
public class GameReplayEngine
{
    private static final Logger LOGGER = Logger.getLogger(
        GameReplayEngine.class.getName());

    private final GameReplay recording;
    private final long randomSeed;
    private GameServerSide game;
    private boolean gameCompleted = false;

    private GameReplayEngine(GameReplay recording, long seed)
    {
        this.recording = recording;
        this.randomSeed = seed;
    }

    /**
     * Load a recording from file path.
     */
    public static GameReplayEngine load(String path) throws IOException
    {
        String json = new String(java.nio.file.Files.readAllBytes(
            java.nio.file.Paths.get(path)));

        // Extract seed from header
        long seed = extractSeed(json);
        GameReplay replay = GameReplay.parse(json);
        return new GameReplayEngine(replay, seed);
    }

    /**
     * Run the game to completion using the recorded seed for determinism.
     * After this, getGame()/getLegion() etc. reflect final state.
     */
    public void runToCompletion()
    {
        Dice.setReplaySeed(randomSeed);
        Constants.END_AFTER_FIRST_BATTLE = false;

        Options.setStartupTest(false);
        Options startOptions = new Options(Constants.OPTIONS_START, true);
        startOptions.setOption(Options.autoPickColor, true);
        startOptions.setOption(Options.autoPlay, true);
        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            TestConstants.TEST_COLOSSUS_HOME, false, true);
        serverOptions.loadOptions();
        serverOptions.setOption(Options.autoQuit, true);
        serverOptions.setOption(Options.autoStop, false);

        // Set up players from recording
        List<GameReplay.PlayerSetup> players = recording.getPlayers();
        for (int i = 0; i < players.size(); i++)
        {
            GameReplay.PlayerSetup p = players.get(i);
            serverOptions.setOption(Options.playerName + i, p.name);
            // Force all to AI for headless replay
            serverOptions.setOption(Options.playerType + i, "SimpleAI");
            serverOptions.setOption(Options.playerPowers + i, p.powers);
        }

        Variant variant = VariantSupport.loadVariantByName(
            recording.getVariantName(), true);
        game = new GameServerSide(whatNextManager, serverOptions, variant);
        game.startNewGameAndWaitUntilOver(null);
        gameCompleted = true;
    }

    // --- State inspection ---

    public GameServerSide getGame()
    {
        return game;
    }

    public GameReplay getRecording()
    {
        return recording;
    }

    public long getSeed()
    {
        return randomSeed;
    }

    public boolean isCompleted()
    {
        return gameCompleted;
    }

    /**
     * Get a legion by marker ID from the final game state.
     */
    public LegionServerSide getLegion(String markerId)
    {
        if (game == null) return null;
        return game.getLegionByMarkerId(markerId);
    }

    /**
     * Get a player by name.
     */
    public Player getPlayer(String name)
    {
        if (game == null) return null;
        return game.getPlayerByName(name);
    }

    // --- Recording queries (what SHOULD have happened) ---

    public List<GameReplay.ReplayEvent> findEvents(String method)
    {
        return recording.findEvents(method);
    }

    public List<GameReplay.ReplayEvent> findEvents(String player, String method)
    {
        return recording.findEvents(player, method);
    }

    public int getEventCount()
    {
        return recording.getEventCount();
    }

    /**
     * Get event at specific sequence number.
     */
    public GameReplay.ReplayEvent getEventAt(int seq)
    {
        for (GameReplay.ReplayEvent e : recording.getEvents())
        {
            if (e.seq == seq) return e;
        }
        return null;
    }

    /**
     * Get all events up to a sequence number (for verifying a prefix of the game).
     */
    public List<GameReplay.ReplayEvent> getEventsUpTo(int seq)
    {
        List<GameReplay.ReplayEvent> result =
            new java.util.ArrayList<GameReplay.ReplayEvent>();
        for (GameReplay.ReplayEvent e : recording.getEvents())
        {
            if (e.seq > seq) break;
            result.add(e);
        }
        return result;
    }

    /**
     * Find the first event matching method after a given sequence number.
     */
    public GameReplay.ReplayEvent findNextEvent(String method, int afterSeq)
    {
        for (GameReplay.ReplayEvent e : recording.getEvents())
        {
            if (e.seq > afterSeq && e.method.equals(method))
            {
                return e;
            }
        }
        return null;
    }

    /**
     * Get the maximum turn number reached in the recording.
     */
    public int getMaxTurn()
    {
        int max = 0;
        for (GameReplay.ReplayEvent e : recording.getEvents())
        {
            if (e.turn > max) max = e.turn;
        }
        return max;
    }

    /**
     * Verify the game completed without errors.
     * Call after runToCompletion().
     */
    public boolean hasErrors()
    {
        return net.sf.colossus.util.ErrorUtils
            .checkErrorDuringFunctionalTest();
    }

    // --- Seed extraction from raw JSON ---

    private static long extractSeed(String json)
    {
        int pos = json.indexOf("\"randomSeed\":");
        if (pos < 0) return -1;
        int start = pos + 13;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
            || json.charAt(end) == '-'))
        {
            end++;
        }
        return Long.parseLong(json.substring(start, end));
    }
}
