package net.sf.colossus.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.variant.Variant;

/**
 * Loads a game recording and replays it for testing.
 * 
 * Usage:
 *   GameReplay replay = GameReplay.load("path/to/recording.json");
 *   replay.startGame();  // creates server + clients with autoPlay
 *   // game runs automatically via AI; recording is for reference
 *   replay.assertNoErrors();
 * 
 * For targeted assertions on specific actions, use getEvent(n) to
 * inspect what happened at each step.
 */
public class GameReplay
{
    private static final Logger LOGGER = Logger.getLogger(
        GameReplay.class.getName());

    private final String rawJson;
    private final List<ReplayEvent> events;
    private final String variantName;
    private final List<PlayerSetup> players;

    private GameServerSide game;

    public static class PlayerSetup
    {
        public final String name;
        public final String type;
        public final String powers;

        PlayerSetup(String name, String type, String powers)
        {
            this.name = name;
            this.type = type;
            this.powers = powers;
        }
    }

    public static class ReplayEvent
    {
        public final int seq;
        public final int turn;
        public final String phase;
        public final String player;
        public final String method;
        public final List<String> args;

        ReplayEvent(int seq, int turn, String phase, String player,
            String method, List<String> args)
        {
            this.seq = seq;
            this.turn = turn;
            this.phase = phase;
            this.player = player;
            this.method = method;
            this.args = args;
        }

        @Override
        public String toString()
        {
            return "Event#" + seq + " t" + turn + " " + phase + " "
                + player + "." + method + "(" + args + ")";
        }
    }

    private GameReplay(String rawJson, String variantName,
        List<PlayerSetup> players, List<ReplayEvent> events)
    {
        this.rawJson = rawJson;
        this.variantName = variantName;
        this.players = players;
        this.events = events;
    }

    /**
     * Load a recording from file path.
     */
    public static GameReplay load(String path) throws IOException
    {
        String json = new String(Files.readAllBytes(Paths.get(path)));
        return parse(json);
    }

    /**
     * Parse recording JSON. Minimal parser (no dependency on JSON library).
     */
    static GameReplay parse(String json)
    {
        // Extract variant
        String variant = extractString(json, "variant");

        // Extract players
        List<PlayerSetup> players = new ArrayList<PlayerSetup>();
        int playersStart = json.indexOf("\"players\":[");
        int playersEnd = json.indexOf("]", playersStart + 11);
        String playersBlock = json.substring(playersStart, playersEnd + 1);
        int idx = 0;
        while (true)
        {
            int namePos = playersBlock.indexOf("\"name\":\"", idx);
            if (namePos < 0) break;
            String name = extractAfter(playersBlock, namePos + 8);
            int typePos = playersBlock.indexOf("\"type\":\"", namePos);
            String type = extractAfter(playersBlock, typePos + 8);
            int powersPos = playersBlock.indexOf("\"powers\":\"", namePos);
            String powers = extractAfter(playersBlock, powersPos + 10);
            players.add(new PlayerSetup(name, type, powers));
            idx = powersPos + 10;
        }

        // Extract events
        List<ReplayEvent> events = new ArrayList<ReplayEvent>();
        int eventsStart = json.indexOf("\"events\":[");
        if (eventsStart >= 0)
        {
            String eventsBlock = json.substring(eventsStart + 10);
            int pos = 0;
            while (true)
            {
                int seqPos = eventsBlock.indexOf("\"seq\":", pos);
                if (seqPos < 0) break;
                int seq = extractInt(eventsBlock, seqPos + 6);
                int turnPos = eventsBlock.indexOf("\"turn\":", seqPos);
                int turn = extractInt(eventsBlock, turnPos + 7);
                int phasePos = eventsBlock.indexOf("\"phase\":\"", seqPos);
                String phase = extractAfter(eventsBlock, phasePos + 9);
                int playerPos = eventsBlock.indexOf("\"player\":\"", seqPos);
                String player = extractAfter(eventsBlock, playerPos + 10);
                int methodPos = eventsBlock.indexOf("\"method\":\"", seqPos);
                String method = extractAfter(eventsBlock, methodPos + 10);

                List<String> args = new ArrayList<String>();
                int argsPos = eventsBlock.indexOf("\"args\":[", seqPos);
                if (argsPos >= 0)
                {
                    int argsEnd = eventsBlock.indexOf("]", argsPos + 8);
                    String argsStr = eventsBlock.substring(argsPos + 8, argsEnd);
                    if (!argsStr.trim().isEmpty())
                    {
                        for (String a : argsStr.split(","))
                        {
                            args.add(a.trim().replace("\"", ""));
                        }
                    }
                }

                events.add(new ReplayEvent(seq, turn, phase, player,
                    method, args));
                // Advance past this event (find next closing brace)
                int eventEnd = eventsBlock.indexOf("}", argsPos);
                pos = (eventEnd > 0) ? eventEnd + 1 : seqPos + 10;
            }
        }

        return new GameReplay(json, variant, players, events);
    }

    public String getVariantName() { return variantName; }
    public List<PlayerSetup> getPlayers() { return players; }
    public List<ReplayEvent> getEvents() { return events; }
    public int getEventCount() { return events.size(); }
    public ReplayEvent getEvent(int index) { return events.get(index); }

    /**
     * Find events matching a method name.
     */
    public List<ReplayEvent> findEvents(String method)
    {
        List<ReplayEvent> result = new ArrayList<ReplayEvent>();
        for (ReplayEvent e : events)
        {
            if (e.method.equals(method))
            {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Find events for a specific player and method.
     */
    public List<ReplayEvent> findEvents(String player, String method)
    {
        List<ReplayEvent> result = new ArrayList<ReplayEvent>();
        for (ReplayEvent e : events)
        {
            if (e.player.equals(player) && e.method.equals(method))
            {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Start a full game with the recorded setup (all players as AI with autoPlay).
     * The game runs to completion. Use for regression testing.
     */
    public void startGameAndWaitUntilOver(String colossusHomePath)
    {
        Options.setStartupTest(true);

        Options startOptions = new Options(Constants.OPTIONS_START, true);
        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            colossusHomePath, false, true);
        serverOptions.loadOptions();

        // Set up players from recording
        for (int i = 0; i < players.size(); i++)
        {
            PlayerSetup p = players.get(i);
            serverOptions.setOption(Options.playerName + i, p.name);
            serverOptions.setOption(Options.playerType + i,
                p.type.contains("Human") ? "SimpleAI" : p.type);
            serverOptions.setOption(Options.playerPowers + i, p.powers);
        }
        serverOptions.setOption(Options.autoPickColor, true);
        serverOptions.setOption(Options.autoPlay, true);

        Variant variant = VariantSupport.loadVariantByName(variantName, true);
        game = new GameServerSide(whatNextManager, serverOptions, variant);
        game.startNewGameAndWaitUntilOver(null);
    }

    public GameServerSide getGame() { return game; }

    // --- Minimal JSON helpers (no external dependency) ---

    private static String extractString(String json, String key)
    {
        int pos = json.indexOf("\"" + key + "\":\"");
        if (pos < 0) return "";
        return extractAfter(json, pos + key.length() + 4);
    }

    private static String extractAfter(String s, int start)
    {
        int end = s.indexOf("\"", start);
        if (end < 0) return "";
        return s.substring(start, end);
    }

    private static int extractInt(String s, int start)
    {
        int end = start;
        while (end < s.length() && (Character.isDigit(s.charAt(end))
            || s.charAt(end) == '-'))
        {
            end++;
        }
        try
        {
            return Integer.parseInt(s.substring(start, end));
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }
}
