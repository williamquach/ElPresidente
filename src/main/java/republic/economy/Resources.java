package republic.economy;

import game.needs.GameRules;

public class Resources {
    Industry industry;
    Farm farm;

    public Resources() {
        this.industry = new Industry();
        this.farm = new Farm();
    }

    public Resources(int foodUnits, double money, int farmRate, int industryRate) throws IllegalArgumentException {
        if(!isFarmAndIndustrySumRateCorrect(farmRate, industryRate)) {
            throw new IllegalArgumentException("Industry and/or farm rate have an incorrect sum value.");
        }
        this.industry = new Industry(money, industryRate);
        this.farm = new Farm(foodUnits, farmRate);
    }

    /**
     * @param newRate the updated rate
     * @param otherOneRate the second rate
     * @return if the sum of two rates is between 0 and 100%
     */
    private Boolean isFarmAndIndustrySumRateCorrect(int newRate, int otherOneRate) {
        return newRate + otherOneRate <= 100 && newRate + otherOneRate >= 0;
    }

    public Industry getIndustry() {
        return this.industry;
    }

    public Farm getFarm() {
        return this.farm;
    }

    /**
     * Update farm rate by adding an amount of percentage points to it
     * @param percentagePoint value to add
     */
    public void updateFarmRate(int percentagePoint) {
        int finalPercentagePointToAdd = getBalancedRate(getFarmRate(), percentagePoint, getIndustryRate());
        this.farm.updateRate(finalPercentagePointToAdd);
    }

    /**
     * Update industry rate by adding an amount of percentage points to it
     * @param percentagePoint value to add
     */
    public void updateIndustryRate(int percentagePoint) {
        int finalPercentagePointToAdd = getBalancedRate(getIndustryRate(), percentagePoint, getFarmRate());
        this.industry.updateRate(finalPercentagePointToAdd);
    }

    /**
     * Get a balanced rate according to the opposite rate
     * For example 60 - 30
     * We cannot add 40 to first rate but only 10
     * @param rateToUpdate rate to update
     * @param percentagePointToAdd percentage point to add basically
     * @param oppositeRate opposite rate of the rate to update
     * @return balanced rate according to the opposite rate
     */
    private int getBalancedRate(int rateToUpdate, int percentagePointToAdd, int oppositeRate) {
        int totalNewRate = rateToUpdate + percentagePointToAdd + oppositeRate;
        if(totalNewRate > 100) {
            return 100 - oppositeRate - rateToUpdate;
        }
        if(totalNewRate < 0) {
            return -(rateToUpdate);
        }
        return percentagePointToAdd;
    }

    public int getFarmRate() {
        return this.farm.getRate();
    }

    public int getIndustryRate() {
        return this.industry.getRate();
    }

    public void generateFarmIncome() {
        addFood(getFoodIncomeFromFarm());
    }

    public void generateIndustryIncome() {
        earnMoney(getMoneyIncomeFromIndustry());
    }

    public int getMoneyIncomeFromIndustry() {
        return this.getIndustryRate() * GameRules.GENERATED_MONEY_BY_INDUSTRY_RATE;
    }

    public int getFoodIncomeFromFarm() {
        return this.getFarmRate() * GameRules.GENERATED_FOOD_BY_FARM_RATE;
    }

    public int getFoodUnits() {
        return this.farm.getFoodUnits();
    }

    public double getMoney() {
        return this.industry.getMoney();
    }

    public void addFood(int foodUnits) {
        this.farm.setFoodUnits(foodUnits + getFoodUnits());
    }

    public Resources feed(int population) {
        int res = getFoodUnits() - (population * GameRules.NEEDED_FOOD_PER_CITIZEN);
        this.farm.setFoodUnits(res);
        return this;
    }

    public void useMoney(double amount) {
        if(amount > getMoney()) {
            this.industry.setMoney(0);
        }
        else {
            this.industry.setMoney(getMoney() - amount);
        }
    }

    public void earnMoney(double amount) {
        this.industry.setMoney(getMoney() + amount);
    }

    public int buyingFoodUnitsPossible() {
        return (int)(getMoney() / GameRules.FOOD_PRICE);
    }

    public int getFoodPrice(int foodUnits) {
        return foodUnits * GameRules.FOOD_PRICE;
    }

    public void buyFood(int foodUnits) {
        useMoney(getFoodPrice(foodUnits));
        addFood(foodUnits);
    }

    public void displaySummary() {
        System.out.printf("%nRessources : %n%s%s", this.industry.toString(), this.farm.toString());
    }
}
