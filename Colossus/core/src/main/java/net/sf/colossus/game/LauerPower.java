package net.sf.colossus.game;


public enum LauerPower
{
    ADMINISTRATOR("Administrator",
        "You have learned to gain power through others. When upward recruiting, you need one less"
        + " of the lower character than normal. So one warbear may recruit a unicorn, two of any"
        + " creature may recruit a guardian, and 0 Titans may recruit a warlock. You may recruit any"
        + " character available in a given terrain with any creature in your group worth at least 25%"
        + " more points. As the defender in a battle you may recruit before the battle, in addition"
        + " during or after. As the attacker you may recruit before the battle in addition to"
        + " summoning an angel, and recruiting after the battle. "
        , Status.PARTIAL),

    AMOEBA("Amoeba",
        "You are an amorphous mass, nearly impossible to destroy. In order to be removed from the"
        + " game, every legion must be destroyed! You start the game with 4 of each tower creature to"
        + " make 3 legions, each having 4 creatures. During battles, your legion marker fights as a"
        + " Titan and is treated as a lord but is worth 0 points, and has no effect if it dies. Your"
        + " group is considered destroyed if the legion marker is the only character left in it. If"
        + " your legion marker is killed, but you win the battle, the same legion marker is placed on"
        + " top of your legion. You may have at most seven characters (other than your marker) in a"
        + " legion. Despite having a legion marker, all your legions may flee. You do not earn"
        + " angels. "
        , Status.PARTIAL),

    ARCHER("Archer",
        "You have trained to strike at range. All your characters have the ability to range"
        + " strike, and may do so while engaged, and never suffer penalties for range. In addition,"
        + " any usual range striking character may range strike twice (instead of once) during your"
        + " strike phase (though the gained rangestrike does not gain advantages of the character,"
        + " such as exception 13.5). All of your creatures may range strike during counterstrike. You"
        + " may recruit gorgons and rangers from the dead pile if none are left in the caretaker's"
        + " stacks. You may rangestrike through occupied hexes, elevated hexes, cliff hexes, and"
        + " trees at -1 to hit penalty per hex (this could result in needing 7's, hence automatically"
        + " missing). Skill three characters may rangestrike at range 4. "
        , Status.PARTIAL),

    ARCHMAGE("Archmage",
        "You have powerful magic powers. Your Titan rangestrikes with all the advantages of a"
        + " warlock. Whenever you roll a five for movement, your Titan group may teleport to any"
        + " unoccupied space five spaces away, ignoring arrows and enemy groups, but not traversing"
        + " the same hexside twice. In any battle which the defender does not flee, during any one"
        + " strike phase you may declare you are casting the fire spell. Each character in your"
        + " opponent's group takes floor(its strength/3) damage. "
        , Status.PARTIAL),

    ARMOURER("Armourer",
        "You have learned to build protection. Your non * creatures take floor(2 strength / 3)"
        + " extra hits to kill, your * creatures take floor(strength / 3) extra hits to kill. "
        , Status.FULL),

    AVIAN("Avian",
        "You have special training for flying. All your characters have the ability to fly at"
        + " speed 4. In addition, your * characters may hover. A hovering characters is not affected"
        + " by the terrain in its hex (but is affected by the boundaries of the hex), and can not"
        + " strike at any other characters as long as there are enemy characters in the same hex with"
        + " hits left. Enemy characters never gain dice or skill for terrain benefits when striking"
        + " at a hovering character, except for extra dice from a volcano. Hovering characters are"
        + " not penalized for swinging up slope or dune. Two characters may be in the same hex if one"
        + " is hovering and the other is not. Before each of your movement phases, you have your ogg"
        + " strike phase. You may take an unengaged * creature and make an ogg attack: your character"
        + " dies but pick an enemy character (not a lord) that your creature can move in the same hex"
        + " as this movement phase, and make one strike at + strength dice (no carry is possible"
        + " here), potentially killing the enemy now (there is no counterstrike in the ogg phase)."
        + " You may make more than one ogg attack per phase, declare another ogg attack after the"
        + " last one has been resolved. You may recruit gorgons and rangers from the dead pile if"
        + " none are left in the caretaker's stacks. You start with two extra gargoyles. "
        , Status.PARTIAL),

    BERSERKER("Berserker",
        "You have trained your characters to strike with their maximum force. All your creatures"
        + " strike at +floor(strength/4) strength. At the beginning of any strike phase you may"
        + " select one or more of your characters to go berserk. A berserk character strikes twice,"
        + " but dies automatically immediately (and counts as a kill for opponent's angel summoning). "
        , Status.FULL),

    DEMON("Demon",
        "You have the power of possession. You start the game with two angels, but not Titans."
        + " During your first movement phase of any battle you may possess any character in your"
        + " group. The beast strikes both with the strike of the character and with the strike of"
        + " your demon. To kill the beast both the character and the demon must be killed. During any"
        + " subsequent one of your movement phases, you may end the possession: the demon leaves, and"
        + " the creature fights normally. If the demon has any hits left, you may move it to any of"
        + " your adjacent characters, the demon may move through characters to move many hexes,"
        + " though only move once. If the beast is killed, your demon permanently loses a strength"
        + " (though this does not end the combat). Your demon fights with the strength of a Titan"
        + " (except that is may be reduced). You are destroyed if either your demon is reduced to"
        + " strength 0 (when it is, the battle is ended as if your Titan were killed), or if you have"
        + " no remaining legions. Any group with an angel may teleport as if it had a Titan. "
        , Status.PARTIAL),

    THE_FOLKS("The Folks",
        "You have a good relation with common creatures. Whenever you enter a terrain you may"
        + " always recruit the weakest creature available in that terrain, even from the dead pile if"
        + " none are left if the caretaker's stacks. Each turn, before splitting, you may remove one"
        + " of your legions from the masterboard. In battle you may move an extra creature onto the"
        + " map board each movement phase restricted as follows: the extra creature is the weakest"
        + " available in that terrain (or any tower creature in a tower) and the total point value of"
        + " the extra creatures added during a battle does not exceed 50% the original value of the"
        + " legion. If your group is worth enough points to bring on more than 7 folks, you may bring"
        + " on two folks for (# folks you can bring on - 7) rounds. If you have enough to bring on"
        + " more than 14, 3 folks may be brought on for the appropriate number of rounds, etc. Your"
        + " extra creatures added are not worth any points. The extra creatures added do not count"
        + " towards towards the group size (if there are too many characters in the group at the end"
        + " of the battle, remove as many of the weakest available creature as is necessary). "
        , Status.PARTIAL),

    HEALER("Healer",
        "Your characters have learned to care for each other. During battle at the beginning of"
        + " the strike phase (before the mover's and the other person's strike phase) you may make"
        + " healing strikes against your characters (even if they are fully healed). When you strike"
        + " one of your characters the other character is not protected by terrain benefits (though"
        + " the healing character can still be penalized for the being in bramble), and the"
        + " \"damage\" you do removes hits, up to a maximum of fully healed. You can heal above fully"
        + " healed, but this extra healing yields temp hits which would be hurt before your character"
        + " takes hits, but goes away at the end of the strike phase. Only lords can heal lords. A"
        + " creature can not heal itself. Healing occurs before berserker declares which characters"
        + " are going berserk. When you you strikes your characters, you declare how many temp hits"
        + " to put on your characters before carrying. "
        , Status.PARTIAL),

    KNIGHT("Knight",
        "You have supplied your troops with special weapons. All of you characters may do one"
        + " rangestrike once per combat (crossbow) (~ creatures may do so during counterswing, but"
        + " not during your strike phase). All your creatures melee strike at +floor(strength/2)"
        + " strength. "
        , Status.FULL),

    LIVING_DEAD("Living dead",
        "The dead fight for you. You may recruit freely from the dead pile should none of the"
        + " creature you are recruiting be left in the caretaker's stacks. You may recruit directly"
        + " into the dead pile (even if your group is full). During combat, when your characters are"
        + " killed, you may leave them in the hex they are in. They do not occupy the hex (other"
        + " characters may move into and through the hex), and have no powers. They may attack enemy"
        + " characters in the same hex though. Enemy characters attempting to leave a hex which has"
        + " at least one corpse in it are slowed. During one of your movement phases during the"
        + " combat (or immediately after the combat ends if you win), you may revive corpses of enemy"
        + " creatures from the battle, though you can not exceed maximum stack limit, and the point"
        + " value of the creatures revived can not exceed the half the original value of the group"
        + " bringing them in as recruits. "
        , Status.PARTIAL),

    LORD_OF_THE_TREES("Lord of the Trees",
        "You are loved by all trees. When you recruit in the woods, you recruit as if you have an"
        + " extra warbear in your group. So, you could always recruit a centaur or warbear; with a"
        + " warbear you could recruit a unicorn. Similarly in the (hills, swamp, jungle) you could"
        + " recruit as if having an extra (minotaur, troll, cyclops). You start the game with two"
        + " extra centaurs. You may recruit warbears, minotaurs, unicorns, trolls, and cyclops from"
        + " the dead pile if there are none left in the caretaker's stacks. All your characters may"
        + " move through, and stop at trees. You may range strike through trees. Creatures in trees"
        + " take extra strength hits to kill (this could make you have a character which dies if it"
        + " leaves the tree). At the beginning of each of your opponent's movement phases, each of"
        + " your characters in a tree regenerate its strength/6 hits (if its strength is not"
        + " divisible by six, roll a die on a result "
        , Status.FULL),

    MARINES("Marines",
        "Your troops are of superior quality. Your characters are never penalized for not being"
        + " native to the terrain (except walls and valcanoes). All your creatures melee strike at"
        + " +floor(strength/4) strength, and have ceiling(strength/5) extra hits. "
        , Status.FULL),

    MULLIGAN("Mulligan",
        "You have the ability to avoid problems. The first turn of the game all your groups may"
        + " teleport to any unoccupied space within 6 of your tower, ignoring arrows (as in a tower"
        + " teleport). You roll two dice for recruitment. If they are equal, you reroll one of the"
        + " dice. Each of your groups may use either number to move (so your groups may move"
        + " different amounts on the same turn). After rolling any [range]strike for one of your"
        + " creatures you may instead do ceiling(average number of hits) damage. "
        , Status.FULL),

    NINE_BALL("Nine Ball",
        "You have figured out how to command nine characters. Each of your legions may have nine"
        + " characters, you may fight with nine, and summon an angel to get nine in a group. You"
        + " start with an extra centaur, ogre and two gargoyles. At the beginning of a battle (or"
        + " second movement phase) you may bring on a nine ball. The nineball is a demilord with"
        + " strength (9- number of characters your group started with), and skill 4. Your opponent"
        + " does not score any points for killing the nineball, if the nineball is the only character"
        + " in your legion (or if it is the only other character is an amoeba) your legion is"
        + " destroyed. The nineball does not count against your stacking limit. Your characters are"
        + " only worth 80% the normal amount (extra pieces brought on due to folks, living dead, and"
        + " smurf are also worth 80%, .so the number brought on is not affected). You may leave"
        + " characters off during your first movement phase without having them die (if you have a"
        + " second movement phase, and do not move them on then, they do die), but still have the"
        + " option of declaring them dead during your first movement phase. "
        , Status.PARTIAL),

    NINJA("Ninja",
        "You are the master of stealth. You may move over other players' groups freely. If one of"
        + " your legions would end its entire movement on an enemy legion, you may move your legion"
        + " another space (this may be repeated). You may split off one character legions. You need"
        + " not move your groups sequentially (hence they can switch places). You may end two groups"
        + " which did not just split from each other on the same space on the masterboard, if"
        + " combined they do not exceed your stacking limit. They combine, but may not recruit this"
        + " recruitment phase. Each recruitment phase, after rolling for recruitment you may decide"
        + " that you did not like the number rolled, and reroll until that a different number is"
        + " rolled (with mulligan, chose one die, reroll that till it is different from the current"
        + " number, and the other die). Whenever one of your creatures [range]strikes you make"
        + " declare that it is making a death strike; the creature immediately dies, but gets extra"
        + " dice equal to the number of hits it currently has remaining (creatures can not use extra"
        + " hits gained from being in a tree towards the death strike). There is ninja-movement phase"
        + " after the strike phase during your round. Any of your characters in the combat which are"
        + " not engaged, and did not move in the movement phase may move (as they would during a"
        + " movement phase) during the ninja-movement phase. "
        , Status.PARTIAL),

    PROPHETESS("Prophetess",
        "You have the ability to foresee the future. You may look at any players' legions at any"
        + " time. You may split your groups after rolling for movement. You may recruit with any"
        + " character as if it is any creature it may downward recruit to (ie a ranger may recruit as"
        + " a lion, centaur, troll or ogre). Any character may recruit as any tower creature."
        + " Whenever one of your creatures [range]strike, roll twice and take either result (you may"
        + " change the target for the reroll). You do not need to announce carries before rolling"
        + " your strikes, even when the carry will be for a higher skill. "
        , Status.FULL),


    UPSIDE_DOWN_CAKE("Upside-Down Cake",
        "Everything seems upside down. When you roll for recruitment, you may either use the"
        + " number rolled, or the number facing down (7 - the number rolled), and may have one group"
        + " move the other number. Whenever one of your creatures [range]strikes, you may either use"
        + " the numbers rolled, or take the roll facing down (you choose for the entire strike, not"
        + " just some of the dice). "
        , Status.FULL),

    VAMPIRE("Vampire",
        "You are able to drain the life out of others. Your first power is that nobody may flee"
        + " from you. If you win a combat, you also get half points for all the characters your"
        + " opponent killed in that battle. During recruitment, you may attempt to overrun your"
        + " opponent's groups. If one of your groups reaches an opponent's, you may immediately"
        + " compare your group value (sum of all points of all characters) with your opponent's. If"
        + " yours is at least 2.5 times your opponent's, then the opposing group dies, you collect"
        + " full points and (if your group had not moved its full allotment already) your group"
        + " continues movement. You may not overrun if the hex your group will reach after movement"
        + " is already occupied by one of your groups. Your Titans are [(x+1) * 4] lords and may be"
        + " summoned in lieu of an angel. During battle, during your movement phase, if your have a"
        + " Titan it may leave the battle and go to any group of yours which is not engaged, and has"
        + " room (your opponent still scores points for your Titan). Alternatively your Titan may go"
        + " into stasis, where it may either be summoned back into your group (fully healed) during"
        + " the battle, after the battle, or die. You start the game with two Titans (and no angels),"
        + " gain 1 Titan strength every 50 points, do not earn angels, and are not destroyed until"
        + " both Titans are killed. Any group of yours may recruit a new Titans in a tower, provided"
        + " you currently have a dead Titan. The new Titan is a 6 * 4 lord, and goes up a strength"
        + " for every fifty points you gain beyond your current score. "
        , Status.PARTIAL),

    YAHTZEE("Yahtzee",
        "Lady luck is on your side. Whenever one of your creatures [range]strikes, you may reroll"
        + " any dice you want to, and take the second roll of those dice as the final result. If you"
        + " roll the same roll for recruitment you did last turn, you may roll again. "
        , Status.PARTIAL), // PARTIAL: AI auto-takes max, human needs UI to select dice

    ZOMBIE("Zombie",
        "You have learned to raise the dead. You may recruit any creature from the dead pile"
        + " available in the terrain with any creature worth at least as many points. After rolling"
        + " for movement, but before moving, you may take creatures out of your groups and put them"
        + " in the dead pile. At the end of any battle which you won, you may gain characters. You"
        + " may add any of the creatures which died during that battle to your winning group (up to a"
        + " maximum of seven characters in your group). You may raise creatures which flee. You may"
        + " not raise lords or demi lords. At the end of a battle, you may remove non lords from your"
        + " group (and may destroy your group this way). This is done at the same time as the raising"
        + " of characters, but before recruiting / angel summoning. Your Titan's death does not end a"
        + " battle; if you go on to win the battle place your Titan in the group before recruiting /"
        + " wining angels. If your group is already full, remove a character. Start the game with 2"
        + " lions, trolls, cyclops, rangers, gorgons, warbears and minotaurs in the dead pile."
        , Status.PARTIAL);

    public enum Status { FULL("✓"), PARTIAL("◐"), NOT_IMPLEMENTED("✗");
        private final String symbol; Status(String s) { symbol = s; }
        public String getSymbol() { return symbol; } }

    private final String displayName;
    private final String description;
    private final Status status;
    LauerPower(String d, String desc, Status s) { displayName=d; description=desc; status=s; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getFullDescription() { return description; }
    public Status getStatus() { return status; }
    public String getLabel() { return displayName; }
    public static LauerPower[] getAll() { return values(); }
    @Override public String toString() { return displayName; }
}
