package game;
import exceptions.MissingEventsException;
import exceptions.MissingParsingKeysException;
import game.needs.*;
import game.saving.GameSaver;
import game.saving.JSONGameSaver;
import gameplay.*;
import listeners.SatisfactionDecreasedListener;
import listeners.SatisfactionIncreasedListener;
import publisher.EventManager;
import republic.Republic;
import republic.economy.Resources;
import republic.factions.Population;
import parser.Parser;
import parser.JSONParser;

import java.io.File;

public abstract class Game {
    protected String playerName;
    protected Republic republic;
    protected GamePlay gamePlay;
    protected final GameDifficulty gameDifficulty;
    protected double score;
    protected int eventCount = 1;
    public EventManager events;
    private Parser parser;
    private GameSaver gameSaver;
    private String filePath;
    private boolean isASavedGame = false;

    public Game(GameDifficulty gameDifficulty, String playerName) {
        this.playerName = playerName;
        this.gameDifficulty = gameDifficulty;
        this.score = GameRules.INITIAL_SCORE * (1 / gameDifficulty.getDifficultyCoefficient());
        this.events = new EventManager("satisfaction_increased", "satisfaction_decreased");
        this.events.subscribe("satisfaction_decreased", new SatisfactionDecreasedListener(this));
        this.events.subscribe("satisfaction_increased", new SatisfactionIncreasedListener(this));
    }

    public Republic getRepublic() {
        return this.republic;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public int getYear() {
        return gamePlay.getYear();
    }

    public int getEventCount() {
        return this.eventCount;
    }

    public Season getCurrentSeason() {
        return this.gamePlay.getCurrentSeason();
    }

    public String getFileName() {
        File file = new File(this.filePath);
        return file.getName();
    }

    public GameDifficulty getGameDifficulty() {
        return this.gameDifficulty;
    }

    public double getScore() {
        return this.score;
    }

    public static void displayIntroduction() {
        String welcome = "\"Bonjour et bienvenue dans un jeu vid??o ?? la crois??e entre Tropico et Reigns !\"";
        String gameRole = "\"Vous incarnerez un jeune dictateur en herbe sur une ??le tropicale, fra??chement ??lu comme Pr??sident.";
        String gameGoal = "Vous aurez la lourde t??che de faire prosp??rer cette nouvelle mini-r??publique.\"";
        System.out.printf("%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%s%n", welcome);
        System.out.println("===================");
        System.out.printf("%s%n%s%n", gameRole, gameGoal);
        System.out.println("===================");
    }

    /**
     * Load game data from the configuration file
     * @param gameParameters containing GameDifficulty and configuration file path
     * @throws MissingParsingKeysException Keys are missing in configuration file
     */
    public void load(GameParameters gameParameters) throws MissingParsingKeysException {
        this.filePath = gameParameters.getFilePath();
        setParserAndGameSaver(this.filePath, gameParameters);
        openFile(gameParameters.getFilePath());

        if(canLoadGame()) {
            setGamePlay();
            if(doesPlayerHasGameSave() && GamePlayerInput.doesPlayerWantsToUseGameSave()) {
                setSavedGameStartParameters();
                this.isASavedGame = true;
            }
            setRepublic();
        }
        else {
            throw new MissingParsingKeysException("Cannot load game. Something is missing in the configuration file.");
        }
    }

    public void openFile(String filePath) {
        try {
            this.parser.openFile(filePath);
        } catch (Exception ex) {
            ex.printStackTrace();
            shutDown();
        }
    }

    public boolean canLoadGame() {
        if(parser.canParseFile()) {
            return parser.isGameStartParameterDifficultyInJson();
        }
        return false;
    }

    public void setGamePlay() {
        try {
            this.gamePlay = this.parser.parseGamePlay();
        } catch (Exception ex) {
            ex.printStackTrace();
            shutDown();
        }
    }

    public boolean doesPlayerHasGameSave() {
        String savePath = this.getSavePath();
        File file = new File(savePath);
        return file.exists() && doesFileContainsChosenDifficulty(file);
    }

    public boolean doesFileContainsChosenDifficulty(File file) {
        return this.parser.doesChosenDifficultyHasSavedGame(file, this.gameDifficulty);
    }

    public void setSavedGameStartParameters() {
        openFile(getSavePath());
        this.parser.setGameStartParameterDifficulty(this.gameDifficulty.name());// setGameDifficulty in JSONParser;
        this.gamePlay.setCurrentSeason(this.parser.getSavedCurrentSeason());
        this.gamePlay.setYear(this.parser.getSavedYear());
        this.eventCount = this.parser.getSavedEventCount();
        this.score = this.parser.getSavedScore();
    }

    public void deleteSavedFile(String filePath) {
        this.gameSaver.deleteFile(filePath);
    }

    public static void shutDown() {
        System.out.println("Le jeu est termin??.");
        System.exit(0);
    }

    public void setParserAndGameSaver(String filePath, GameParameters gameParameters) {
        if(filePath.toLowerCase().endsWith(".json")) {
            this.parser = new JSONParser();
            this.parser.setGameParametersChosen(gameParameters);
            this.gameSaver = new JSONGameSaver(this);
        }
    }

    public void setRepublic() {
        try {
            Population population = this.parser.parsePopulation();
            Resources resources = this.parser.parseResources();
            this.republic = new Republic(population, resources);
            this.republic.events = this.events;
        } catch (Exception ex) {
            ex.printStackTrace();
            shutDown();
        }
    }
    /**
     * Launches the game if game conditions are set (republic and events)
     * @throws NullPointerException Republic is not fully set
     * @throws MissingEventsException Events are missing in file so the scenario cannot be fully played
     *                                or sandbox have no events in one of the seasons
     */
    public void launchGame() throws NullPointerException, MissingEventsException {
        if(this.republic.isSet()) {
            if(this.gamePlay.canPlayEvents()) {
                GamePlayerInput.displayScaredOrNotScared("Continuer");
                if(GamePlayerInput.wantsToQuitGame()) { shutDown(); }
                System.out.printf("%nLancement du jeu...%n");
                displayPregame();
                play();
            }
            else {
                throw new MissingEventsException("There is not enough events in at least one of the seasons.");
            }
        }
        else {
            System.out.printf("%n%nArr??t du jeu...%n");
            throw new NullPointerException("Republic properties are not set.");
        }
    }

    public void displayPregame() {
        GamePlayerInput.pressAnyKeyToContinue();
        displayGameModeAndDifficulty();
        this.gamePlay.displayContext();
        GamePlayerInput.pressAnyKeyToContinue();
        System.out.printf("%n%n%n%n%n%n%n%n%n%n%n%n%n%n%nVous commencez avec ces param??tres de jeu : %n");
        displaySummary();
    }

    public void displayGameModeAndDifficulty() {
        System.out.printf("%nMode de jeu : %s | En difficult?? : %s%n", toString(), this.gameDifficulty.toString());
    }

    public void play() {
        this.gamePlay.nextEvent();
        playsGame();
        addEndGameScore();
        finalSummary();
        handlePlayerEndGame();
        if(this.isASavedGame) { deleteSavedFile(getSavePath()); }
    }

    public abstract boolean keepsPlaying();

    public abstract void playsGame();

    public abstract void handlePlayerEndGame();

    public boolean isPlayerWinning() {
        return isScorePositive() && this.republic.isGlobalSatisfactionRateOkay(this.gameDifficulty.getDifficultyCoefficient());
    }

    public boolean isScorePositive() {
        return this.score >= 0;
    }
    
    /**
     * Handles current season, set next season and event
     * Launches year end summary when needed
     */
    public void playCurrentGameTurn() {
        System.out.printf("%n%n-- Nous sommes en %s de la %de ann??e --%n", this.gamePlay.getCurrentSeason().capitalize(), getYear() + 1);
        handleCurrentSeason(this.eventCount);

        this.gamePlay.nextSeason();
        this.gamePlay.nextEvent();

        if(isEndOfYear(this.eventCount)) {
            handleEndOfYear();
            this.gamePlay.nextYear();
            askPlayerWantsToKeepPlaying();
        }
        this.eventCount += 1;
    }

    /**
     * Handle the current season / event
     * Displaying the current event, event effects, player's choice and effects
     * @param nbEvents event count for displaying
     */
    public void handleCurrentSeason(int nbEvents) {
        this.gamePlay.displayCurrentEvent(nbEvents);
        this.republic.irreversibleEventEffects(getCurrentEvent());

        int playerSolutionChoice = GamePlayerInput.getPlayerEventSolutionChoice(getCurrentEvent().getNbChoices());
        playerChoiceEffects(playerSolutionChoice);
    }

    public Event getCurrentEvent() {
        return this.gamePlay.getCurrentEvent();
    }

    /**
     * Handles player's choice effects
     * @param choice player's choice
     */
    public void playerChoiceEffects(int choice) {
        Choice playerChoice = getCurrentEvent().getChoiceByPlayerChoice(choice);
        Effect choiceEffects = playerChoice.getEffects();
        this.republic.applyEffects(choiceEffects);
        if(playerChoice.hasRelatedEvents()) {
            gamePlay.placeRelatedEvents(playerChoice.getRelatedEvents());
        }
    }

    public boolean isEndOfYear(int seasonCount) {
        return seasonCount % 4 == 0 && seasonCount != 0;
    }

    /**
     * Ask every year if player wants to continue playing
     */
    public void askPlayerWantsToKeepPlaying() {
        GamePlayerInput.displayContinueOrSaveAndOrQuit();
        int playerChoice = GamePlayerInput.makeContinueOrSaveAndOrQuitChoice();
        if(playerChoice == GameInputOptions.END_YEAR_QUIT) {
            if(this.isASavedGame) {
                deleteSavedFile(getSavePath());
            }
            addEndGameScore();
            finalSummary();
            shutDown();
        }
        else if(playerChoice == GameInputOptions.END_YEAR_SAVE_AND_QUIT) {
            finalSummary();
            saveGame();
            shutDown();
        }
        else {
            System.out.printf("%n%nContinuons !%n");
        }

    }

    public void saveGame() {
        gameSaver.saveGame();
    }

    public abstract String getSavePath();

    public void handleEndOfYear() {
        endOfYearConsequencesAndChoices();
        displaySummary();
    }

    /**
     * Handles end of year consequences and choices
     * Including incomes, summary, player year end options and food consequence
     */
    public void endOfYearConsequencesAndChoices() {
        generateIncomes();
        GamePlayerInput.pressAnyKeyToContinue();

        displaySummary();

        handlePlayerYearEndChoices(0);
        GamePlayerInput.pressAnyKeyToContinue();

        killAndOrFeedCitizen();
    }

    public void generateIncomes() {
        this.republic.getResources().generateFarmIncome();
        this.republic.getResources().generateIndustryIncome();
        System.out.printf("%n%nL'agriculture a g??n??r?? cette ann??e %d unit??(s) de nourriture.", this.republic.getResources().getFoodIncomeFromFarm());
        System.out.printf("%nL'industrie, quant ?? elle, a g??n??r?? cette ann??e %d$.", this.republic.getResources().getMoneyIncomeFromIndustry());
    }

    public void killAndOrFeedCitizen() {
        int nbCitizensEliminated = this.republic.getPopulation().getNbSupportersToEliminateToHaveEnoughFood(this.republic.getFoodUnits());
        boolean hasEliminatedSupporters = this.republic.getPopulation().eliminateSupportersUntilEnoughFood(nbCitizensEliminated);
        this.republic.feedPopulation();
        applyFoodConsequences(hasEliminatedSupporters, nbCitizensEliminated);
    }

    public void applyFoodConsequences(boolean hasEliminatedSupporters, int nbCitizensEliminated) {
        if(hasEliminatedSupporters) {
            System.out.println("La population a diminu?? car vous n'aviez pas assez de nourriture.");
            System.out.printf("%nVous avez perdu %d citoyens.%n", nbCitizensEliminated);
        }
        else {
            int nbNewCitizens = this.republic.getPopulation().increasePopulationRandomly();
            System.out.println("F??licitation, vous avez assez de nourriture pour nourriture toute votre population.");
            System.out.println("Vous avez m??me du surplus.");
            System.out.printf("%nAinsi, la population a augment?? de %d citoyens.%n", nbNewCitizens);
        }
        GamePlayerInput.pressAnyKeyToContinue();
    }

    public void displaySummary() {
        if(getYear() > 0) {
            System.out.printf("%n%n- Bilan de cette %de ann??e -%n", getYear() + 1);
        }
        this.republic.getPopulation().displaySummary();
        System.out.println();
        this.republic.getResources().displaySummary();
        System.out.printf("%n%n- Score : %.2f -%n", this.score);
        GamePlayerInput.pressAnyKeyToContinue();
    }

    public void handlePlayerYearEndChoices(int nbChoicesDone) {
        if(nbChoicesDone < 0) {
            throw new IllegalArgumentException("Number of choices done at year end can't be negative");
        }
        displayPlayerYearEndChoices();
        int playerYearEndChoice = GamePlayerInput.chooseEndYearOption();
        this.republic.playerEndYearChoiceImpacts(playerYearEndChoice, nbChoicesDone);
        if(canRedoYearEndChoice(playerYearEndChoice)) {
            nbChoicesDone += 1;
            GamePlayerInput.pressAnyKeyToContinue();
            handlePlayerYearEndChoices(nbChoicesDone);
        }
    }

    public boolean canRedoYearEndChoice(int playerYearEndChoice) {
        return playerYearEndChoice == GameInputOptions.YEAR_END_BRIBE_CHOICE || playerYearEndChoice == GameInputOptions.YEAR_END_BUY_FOOD_CHOICE;
    }

    public void displayPlayerYearEndChoices() {
        System.out.printf("%n%nEn cette fin d'ann??e, vous avez plusieurs options qui se pr??sente ?? vous pour tenter de sauver votre r??publique de l'insurrection.%n");
        System.out.println("Option 1 : Ne rien faire");
        System.out.println("Option 2 : Pot-de-vin ?? une faction (co??t par partisan : 15$)");
        System.out.println("\t=> Possible sur toute faction sauf les Loyalistes");
        System.out.println("\t=> +10 points de pourcentage de satisfaction sur la faction choisie");
        System.out.printf("\t=> Diminution de la satisfaction des Loyalistes ?? hauteur du prix du pot-de-vin (prix / %d)%n", GameRules.BRIBE_FACTION_DECREASE_LOYALISTS_SATISFACTION);
        System.out.println("Option 3 : March?? alimentaire (co??t par unit?? : 8$)");
        System.out.println("\t=> Rappel : 4 unit??s de nourriture par citoyen sont n??cessaires");
        System.out.println("Entrez votre choix :");
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void addScore(double scoreToAdd) {
        setScore(this.score + scoreToAdd);
    }

    /**
     * Player can catch up if his score is not negative and if there are citizen left
     * If he can, he can choose one of the year end options
     * @return if player caught up with the game
     */
    public boolean didPlayerFailedCatchingUp() {
        if(!canCatchUp()) {
            displayPlayerLostAndCannotCatchUp();
            GamePlayerInput.pressAnyKeyToContinue();
            return true;
        }
        else {
            displayPlayerLostButCanCatchUp();
            GamePlayerInput.pressAnyKeyToContinue();
            endOfYearConsequencesAndChoices();
            if(!isPlayerWinning()) {
                System.out.printf("%n%nDommage, malgr?? ce dernier effort, vous avez perdu la partie...%n");
                GamePlayerInput.pressAnyKeyToContinue();
                return true;
            }
        }
        return false;
    }

    /**
     * You cannot get score to positive value by bribing faction
     * You cannot get global satisfaction rising because no population
     * @return if player can catch up the game
     */
    public boolean canCatchUp() {
        return isScorePositive() && this.republic.isThereAnyPopulation();
    }

    public void finalSummary() {
        System.out.printf("%n==========================%n");
        System.out.printf("%n- Voici votre bilan final : -%n");
        displaySummary();
        System.out.printf("%n- Voici votre score final : %.2f -%n", getScore());
    }

    public void addEndGameScore() {
        addScore(getYear() * GameRules.END_SCORE_POINTS_PER_YEAR);
        addScore(this.republic.getPopulationScore());
        addScore(this.republic.getIndustryRateScore());
        addScore(this.republic.getMoneyScore());
        addScore(this.republic.getFarmRateScore());
        addScore(this.republic.getFoodScore());
    }

    public void displayPlayerLostAndCannotCatchUp() {
        System.out.printf("%nUn dernier bilan vous sera affich?? mais le jeu est fini pour vous. %nGAME OVER.%n");
    }

    public void displayPlayerLostButCanCatchUp() {
        System.out.println("Un dernier bilan vous sera affich??.");
        System.out.println("Cependant, peut-??tre que vous pouvez encore sauver votre r??publique.");
        System.out.println("Essayez d'am??liorer la satisfaction globale de votre r??publique...");
    }

    public void displayGameIsFinished() {
        System.out.printf("%nVotre partie en mode %s est termin??e.%n", toString());
    }
}