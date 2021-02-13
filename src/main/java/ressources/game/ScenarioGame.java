package ressources.game;

import org.json.JSONObject;
import ressources.scenario.Event;

public class ScenarioGame extends Game {
    private String name;
    private String scenarioFile;

    public ScenarioGame(GameDifficulty gameDifficulty, String name) {
        super(gameDifficulty);
        this.name = name;
    }

    public void setScenarioFile(String scenarioFile) {
        this.scenarioFile = scenarioFile;
    }


    @Override
    public void play() {
        super.play();
        /*TODO
         *  Chaque année
         *  => 4 tours <=> 4 évènements suivant l'ordre du scénario
         *  hasNotLost and scenario not finished
         *  + voir si le joueur peut arrêter le jeu à la fin du bilan
         */;
        System.out.println(this.getScenario().getName());
        System.out.println(this.getScenario().getStory());
        System.out.printf("Nous sommes en %s.%n%n", this.getScenario().getFirstSeason().capitalize());

        int year = 1;
        int seasonCount = 0;
        int eventCount = 1;
        getScenario().nextEvent(seasonCount);
        while(!hasPlayerLost()) {
            if(!isScenarioFinished()) {
                System.out.printf("%n- Année %d -%n", year);
                displayCurrentEvent(eventCount);
                int playerSolutionChoice = getPlayerChoice(getCurrentEvent().getNbChoices());
                irreversibleEventImpacts();
                playerChoiceImpacts(playerSolutionChoice);
                seasonCount += 1;
                eventCount += 1;
                getScenario().nextEvent(seasonCount);
                if(isTimeToYearEndSummary(seasonCount)) {
                    // Industry and Farm generate money and food
                    this.getTreasury().generateFarmIncome();
                    this.getTreasury().generateIndustryIncome();
                    // Year End Summary
                    displayYearEndSummary(year);
                    handlePlayerYearEndChoices();
                    UserInput.pressAnyKeyToContinue();
                    displayYearEndSummary(year);
                    // TODO Vérifier le calcul de "eliminateSupportersUntilEnoughFood()" dans population
                    boolean hasEliminatedSupporters = this.getPopulation().eliminateSupportersUntilEnoughFood(getFoodUnits());
                    if(!hasEliminatedSupporters) {
                        // TODO Augmenter la population de 1 à 10% -> manque une fonction dans population -> WILLIAM
                    }
                    // TODO addScore(calculateYearEndScore());
                    year += 1;

                }
            }
            else {
                System.out.println("Le scénario est fini. Vous avez gagné la partie.");
                System.out.println("Voulez-vous continuer en mode bac à sable, ou arrêter de jouer ?");
                break;
            }
        }
    }

    public boolean isScenarioFinished() {
        return getScenario().isScenarioFinished();
    }

    @Override
    public String toString() {
        return "scénario";
    }
}