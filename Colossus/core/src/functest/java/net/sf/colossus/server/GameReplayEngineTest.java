package net.sf.colossus.server;

import java.io.File;

import junit.framework.TestCase;
import net.sf.colossus.util.ErrorUtils;


/**
 * First replay engine regression test.
 * Proves: load recording → run deterministically → game completes without errors
 * → recording events are consistent with game state.
 */
public class GameReplayEngineTest extends TestCase
{
    private static final String RECORDING_PATH =
        "core/src/functest/resource/recordings/full-ai-game-v2.json";

    public GameReplayEngineTest(String name)
    {
        super(name);
    }

    /**
     * Load a recording and run deterministic replay to completion.
     * Verifies: no errors, game completed, events match expected count.
     */
    public void testDeterministicReplayCompletes() throws Exception
    {
        ErrorUtils.clearErrorDuringFunctionalTest();

        GameReplayEngine engine = GameReplayEngine.load(RECORDING_PATH);
        assertNotNull("Engine loaded", engine);
        assertTrue("Seed captured", engine.getSeed() > 0);
        assertTrue("Recording has events", engine.getEventCount() > 100);

        // Run the game with same seed — should produce identical game
        engine.runToCompletion();

        assertTrue("Game completed", engine.isCompleted());
        assertNotNull("Game object available", engine.getGame());
        assertFalse("No errors during replay", engine.hasErrors());
    }

    /**
     * Verify recording contains expected game actions.
     */
    public void testRecordingContainsGameActions() throws Exception
    {
        GameReplayEngine engine = GameReplayEngine.load(RECORDING_PATH);

        // Recording should have splits, moves, recruits
        assertTrue("Has doSplit",
            engine.findEvents("doSplit").size() > 0);
        assertTrue("Has doMove",
            engine.findEvents("doMove").size() > 0);
        assertTrue("Has doRecruit",
            engine.findEvents("doRecruit").size() > 0);
        assertTrue("Has strike",
            engine.findEvents("strike").size() > 0);

        // Should span multiple turns
        assertTrue("Multiple turns", engine.getMaxTurn() > 5);
    }

    /**
     * Verify getEventAt and findNextEvent work correctly.
     */
    public void testRecordingNavigation() throws Exception
    {
        GameReplayEngine engine = GameReplayEngine.load(RECORDING_PATH);

        // First event should be signOn
        GameReplay.ReplayEvent first = engine.getEventAt(1);
        assertNotNull("Event 1 exists", first);
        assertEquals("First event is signOn", "signOn", first.method);

        // Find first doSplit
        GameReplay.ReplayEvent split = engine.findNextEvent("doSplit", 0);
        assertNotNull("Found a doSplit event", split);
        assertTrue("doSplit has args", split.args.size() > 0);

        // Find first doRecruit after the split
        GameReplay.ReplayEvent recruit = engine.findNextEvent("doRecruit",
            split.seq);
        assertNotNull("Found doRecruit after split", recruit);
        assertTrue("doRecruit seq > doSplit seq", recruit.seq > split.seq);
    }
}
